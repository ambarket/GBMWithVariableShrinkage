
package gbm;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dataset.Dataset;
import gbm.cv.CrossValidatedResultFunctionEnsemble;
import gbm.cv.CrossValidationStepper;
import gbm.cv.IterativeCrossValidatedResultFunctionEnsemble;
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
	
	public static IterativeCrossValidatedResultFunctionEnsemble crossValidate(GbmParameters parameters, Dataset training, int numOfFolds, int stepSize) {
		StopWatch ensembleTimer = new StopWatch().start();
		if (numOfFolds <= 1) {
			throw new IllegalStateException("Number of folds must be > 1 for cross validation");
		}
		
		CrossValidationStepper[] steppers = new CrossValidationStepper[numOfFolds+1];

		// Partition the data set into k folds. All done with boolean index masks into the original dataset
		int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(training.getNumberOfTrainingExamples());
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
		for (int i = 0; i < training.getNumberOfTrainingExamples(); i++) {
			trainingInEachFold[numOfFolds][i] = true;
		}
		
		for (int i = 0; i < numOfFolds+1; i++) {
			double meanResponse =  training.calcMeanTrainingResponse(trainingInEachFold[i]);
			steppers[i] = new CrossValidationStepper(parameters, 
					new ResultFunction(parameters, meanResponse, training.getPredictorNames()),
					new MinimalistGbmDataset(training, stepSize, i < numOfFolds),
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
		while (lastTreeIndex + stepSize < parameters.numOfTrees && remainingStepsPastMinimum > 0 && ensembleTimer.getElapsedSeconds() < 5400) {
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
			
			ensemble.updateToReflectLastStepLastCvStop(lastTreeIndex);

			double newAvgValidationError = ensemble.avgCvValidationErrors.get(lastTreeIndex-1);
			
			Logger.println(LEVELS.DEBUG, 
					"Training: " + ensemble.avgCvTrainingErrors.get(lastTreeIndex-1) + 
					"\nLast Avg Validation Error: " + lastAvgValidationError + 
					"\nCurrent Avg Validation Error: " + newAvgValidationError + 
					"\nDifference: " + (lastAvgValidationError - newAvgValidationError));
			
			if (newAvgValidationError < lastAvgValidationError) {
				lastAvgValidationError = newAvgValidationError;
				remainingStepsPastMinimum = 3;
			} else {
				remainingStepsPastMinimum--;
				if (remainingStepsPastMinimum == 2) {
					Logger.println(LEVELS.INFO, "Reached minimum after " + lastTreeIndex + " iterations");
				}
			}
			// If we have less than a gig left, need to just print out what we got
			/*
			double memoryPossiblyAvailableInGigs = (Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())) / 1000000000.0;
			if (memoryPossiblyAvailableInGigs < 1.0) {
				System.out.println(StopWatch.getDateTimeStamp() + "Breaking early because we are almost out of memory! Memory possibly available: " + memoryPossiblyAvailableInGigs);
				break;
			}
			*/
			
			Logger.println(LEVELS.INFO, "Completed " + lastTreeIndex + " iterations in " + timer.getElapsedSeconds() + " seconds. Cv Error: " + newAvgValidationError);
		}
		
		ensemble.finalizeEnsemble(ensembleTimer);
		
		return ensemble;
	}
}
