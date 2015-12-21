
package gbm;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dataset.Dataset;
import gbm.cv.CrossValidationStepper;
import gbm.cv.IterativeCrossValidatedResultFunctionEnsemble;
import parameterTuning.ParameterTuningParameters;
import regressionTree.RegressionTree;
import utilities.Logger;
//import gbt.ranker.RegressionTree.TerminalType;
import utilities.Logger.LEVELS;
import utilities.RandomSample;
import utilities.StopWatch;

public class GradientBoostingTree {
	public static ExecutorService executor = null;
	/*
	 *  fit a regression function using the Gradient Boosting Tree method.
	 *  On success, return function; otherwise, return null. 
	 */
	public static ResultFunction buildGradientBoostingMachine(GbmParameters parameters, Dataset basicDataset) {
		GbmDataset gbmDataset = new GbmDataset(basicDataset, 1);
				
		// Initialize the function approximation to the mean response in the training data
		double meanTrainingResponse = gbmDataset.calcMeanTrainingResponse();
		ResultFunction function = new ResultFunction(parameters, meanTrainingResponse, gbmDataset.getPredictorNames());
		
		// Initialize predictions of all instances to the initial function value.
		gbmDataset.initializePredictions(meanTrainingResponse);
		int numberOfTrainingExamples = gbmDataset.getNumberOfTrainingExamples();
		int treeSampleSize = (int)(parameters.bagFraction * numberOfTrainingExamples);
		if (parameters.minExamplesInNode * 2 > treeSampleSize) {
			Logger.println(Logger.LEVELS.INFO, "parameters.bagFraction * gbmDataset.getNumberOfTrainingExamples() "
					+ "must be >= minExamplesInNode * 2 in order to grow a tree "
					+ "Just returning a function with no trees.");
			return function;
		}
	
		// begin the boosting process
		StopWatch timer = (new StopWatch());
		for (int iterationNum = 0; iterationNum < parameters.numOfTrees; iterationNum++) {
			timer.start();
			// Update the current pseudo responses (gradients) of all the training instances.
			gbmDataset.updatePseudoResponses();
			
			// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
			int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(numberOfTrainingExamples);
			boolean[] inSample = new boolean[numberOfTrainingExamples];
			for (int i = 0; i < treeSampleSize; i++ ) {
				inSample[shuffledIndices[i]] = true;
			}
			
			// Fit a regression tree to predict the current pseudo responses on the training data.
			
			RegressionTree tree = (new RegressionTree(parameters, treeSampleSize)).build(gbmDataset, inSample, iterationNum+1);
			
			// Update our predictions and all error calculations for each training and validation instance using the new tree.
			gbmDataset.updateAllMetadataBasedOnNewTree(tree);

			// Add the tree to the function approximation.
			function.addTree(tree);
			
			Logger.println(Logger.LEVELS.DEBUG, "\tAdded 1 tree in : " + timer.getElapsedSeconds());
		}
		return function;
	}
	
	public static IterativeCrossValidatedResultFunctionEnsemble crossValidate(GbmParameters parameters, Dataset dataset, ParameterTuningParameters tuningParameters, int runNumber, int submissionNumber, StopWatch globalTimer) {
		StopWatch ensembleTimer = new StopWatch().start();
		String runDataDir = tuningParameters.runDataOutputDirectory + dataset.parameters.minimalName + String.format("/Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber);
		int numOfFolds = tuningParameters.CV_NUMBER_OF_FOLDS;
		int stepSize = tuningParameters.CV_STEP_SIZE;
		if (numOfFolds <= 1) {
			throw new IllegalStateException("Number of folds must be > 1 for cross validation");
		}
		
		CrossValidationStepper[] steppers = new CrossValidationStepper[numOfFolds+1];

		// Partition the data set into k folds. All done with boolean index masks into the original dataset
		int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(dataset.getNumberOfTrainingExamples());
		int foldSize = shuffledIndices.length / numOfFolds;
		boolean[][] trainingInEachFold = new boolean[numOfFolds+1][shuffledIndices.length];
		for (int i = 0; i < numOfFolds; i++) {
			int first = i * foldSize, last = (i * foldSize) + (numOfFolds-1)*foldSize;
			for (int j = first; j < last; j++) {
				int safeIndex = j % shuffledIndices.length;
				trainingInEachFold[i][shuffledIndices[safeIndex]] = true;
			}
		}
		// Also simaltaneously train a gbm on the entire training set.
		for (int i = 0; i < dataset.getNumberOfTrainingExamples(); i++) {
			trainingInEachFold[numOfFolds][i] = true;
		}
		
		for (int i = 0; i < numOfFolds+1; i++) {
			double meanResponse =  dataset.calcMeanTrainingResponse(trainingInEachFold[i]);
			steppers[i] = new CrossValidationStepper(parameters, 
					new ResultFunction(parameters, meanResponse, dataset.getPredictorNames()),
					new MinimalistGbmDataset(dataset, stepSize, i < numOfFolds),
					trainingInEachFold[i],
					(numOfFolds-1)*foldSize, 
					stepSize);
			
			steppers[i].dataset.initializePredictions(meanResponse);
		}
		
		IterativeCrossValidatedResultFunctionEnsemble ensemble = new IterativeCrossValidatedResultFunctionEnsemble(parameters, steppers);
		
		int treeSampleSize = (int)(parameters.bagFraction * (numOfFolds-1)*foldSize);
		if (parameters.minExamplesInNode * 2 > treeSampleSize) {
			Logger.println(Logger.LEVELS.INFO, "parameters.bagFraction * (numOfFolds-1)*foldSize "
					+ "must be >= minExamplesInNode * 2 in order to grow a tree "
					+ "Just returning null");
			return null;
		}
		
		int lastTreeIndex = -1;
		double lastAvgValidationError = Double.MAX_VALUE;
		Queue<Future<Void>> futures = new LinkedList<Future<Void>>();
		int remainingStepsPastMinimum = 3; // Keep going to collect more error data for graphs.
		StopWatch timer = new StopWatch().start();

		Runtime runTime = Runtime.getRuntime();
		double maxMemory = runTime.maxMemory(), totalMemory = runTime.totalMemory(), freeMemory = runTime.freeMemory();
		double memoryPossiblyAvailableInGigs = (maxMemory- (totalMemory -freeMemory)) / 1000000000.0;
		
		while (lastTreeIndex + stepSize < parameters.numOfTrees && remainingStepsPastMinimum > 0) {
			lastTreeIndex += stepSize;
			for (int i = 0; i < numOfFolds+1; i++) {
				futures.add(executor.submit(steppers[i]));
			}
			for (int i = 0; i < numOfFolds+1; i++) {
				try {
					futures.poll().get();
				} catch (InterruptedException | ExecutionException e) {
					System.err.println(StopWatch.getDateTimeStamp());
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			ensemble.updateToIncludeTreesFromLastCvStep(lastTreeIndex);
			
			Logger.println(LEVELS.DEBUG, 
					"Training: " + ensemble.avgCvTrainingErrors.get(lastTreeIndex-1) + 
					"\nLast Avg Validation Error: " + lastAvgValidationError + 
					"\nCurrent Avg Validation Error: " + ensemble.avgValidationErrorOfLastStep + 
					"\nDifference: " + (lastAvgValidationError - ensemble.avgValidationErrorOfLastStep));
			
			if (ensemble.avgValidationErrorOfLastStep < lastAvgValidationError) {
				lastAvgValidationError = ensemble.avgValidationErrorOfLastStep;
				remainingStepsPastMinimum = 3;
			} else {
				remainingStepsPastMinimum--;
				if (remainingStepsPastMinimum == 2) {
					Logger.println(LEVELS.INFO, "Reached minimum after " + lastTreeIndex + " iterations");
				}
			}
			
			// Update memory usage variables
			maxMemory = runTime.maxMemory() / 1000000000.0;
			totalMemory = runTime.totalMemory() / 1000000000.0;
			freeMemory = runTime.freeMemory() / 1000000000.0;
		    memoryPossiblyAvailableInGigs = (maxMemory- (totalMemory -freeMemory));
		    
			// Print a status message at most every two minutes
			if (timer.getElapsedMinutes() % 2 > 1) {
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] [Run%d] [Test %d / %d] [Iterations %d] [CvError %.4f]\n\t"
						+ "SubDirectory: %s\n\t" 
						+ "This test running time: %s \t"
						+ "Total dataset running time: %s \n%s", 
						dataset.parameters.minimalName, runNumber, submissionNumber, tuningParameters.totalNumberOfTests, (lastTreeIndex+1), ensemble.avgValidationErrorOfLastStep,
						parameters.getRunDataSubDirectory(), 
						timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit(),
						String.format("\tMaxMem: %.2f\tTotalMem: %.2f\tFreeMem: %.2f\tUsedMem: %.2f\tAvailableMem: %.2f",
								maxMemory, totalMemory, freeMemory, totalMemory - freeMemory, memoryPossiblyAvailableInGigs)));
			}
			// Check our resource limits.
			if (memoryPossiblyAvailableInGigs < .05) {
				System.err.println(StopWatch.getDateTimeStamp() + "Breaking early because we are almost out of memory! Memory possibly available: " + memoryPossiblyAvailableInGigs);
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(tuningParameters.runDataOutputDirectory + dataset.parameters.minimalName + "/Run" + runNumber + "/parametersThatRanOutOfMemory.txt", true));
					bw.write("MemoryAvailable: " + memoryPossiblyAvailableInGigs + runDataDir + "\n");
					bw.flush();
					bw.close();
				} catch (IOException e) {
					System.err.println("Failed to log that following parameters ran out of memory: " + runDataDir);
					e.printStackTrace();
				}
				break;
			}
			
			/*
			if (ensembleTimer.getElapsedSeconds() > 10800) {
				System.err.println(StopWatch.getDateTimeStamp() + "Breaking early because we ran out of time!");
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(tuningParameters.runDataOutputDirectory + dataset.parameters.minimalName + "/Run" + runNumber + "/parametersThatRanOutOfTime.txt", true));
					bw.write(runDataDir + "\n");
					bw.flush();
					bw.close();
				} catch (IOException e) {
					System.err.println("Failed to log that following parameters ran out of time: " + runDataDir);
					e.printStackTrace();
				}
				break;
			}
			*/
		}

		ensemble.timeInSeconds = ensembleTimer.getElapsedSeconds();
		return ensemble;
	}
}
