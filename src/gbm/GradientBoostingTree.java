package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import gbm.cv.CrossValidatedResultFunctionEnsemble;
import gbm.cv.CrossValidationStepper;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dataset.Dataset;
import regressionTree.RegressionTree;
import utilities.DoubleCompare;
import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;
//import gbt.ranker.RegressionTree.TerminalType;
import utilities.Logger.LEVELS;

public class GradientBoostingTree {
	public static ExecutorService executor = Executors.newCachedThreadPool();

	/*
	 *  fit a regression function using the Gradient Boosting Tree method.
	 *  On success, return function; otherwise, return null. 
	 */
	public static ResultFunction buildGradientBoostingMachine(GbmParameters parameters, Dataset training, Dataset validation) {
		GbmDataset trainingData = new GbmDataset(training);
		GbmDataset validationData = new GbmDataset(validation);
		
		if (parameters.minExamplesInNode * 2 > trainingData.getNumberOfExamples()) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of examples int he dataset must be >= minExamplesInNode * 2");
			System.exit(0);
		}
		
		// Initialize the function approximation to the mean response in the training data
		double meanTrainingResponse = trainingData.calcMeanResponse();
		ResultFunction function = new ResultFunction(parameters, meanTrainingResponse, trainingData.getPredictorNames());
		
		// Initialize predictions of all instances to the initial function value.
		trainingData.initializePredictions(meanTrainingResponse);
		validationData.initializePredictions(meanTrainingResponse);
	
		// begin the boosting process
		StopWatch timer = (new StopWatch());
		for (int iterationNum = 0; iterationNum < parameters.numOfTrees; iterationNum++) {
			timer.start();
			// Update the current pseudo responses (gradients) of all the training instances.
			trainingData.updatePseudoResponses();
			
			// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
			int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(trainingData.getNumberOfExamples());
			int sampleSize = (int)(parameters.bagFraction * shuffledIndices.length);
			boolean[] inSample = new boolean[trainingData.getNumberOfExamples()];
			for (int i = 0; i < sampleSize; i++ ) {
				inSample[shuffledIndices[i]] = true;
			}
			
			// Fit a regression tree to predict the current pseudo responses on the training data.
			RegressionTree tree = (new RegressionTree(parameters.minExamplesInNode, parameters.maxNumberOfSplits, parameters.maxLearningRate)).build(trainingData, inSample);
			
			// Update our predictions for each training and validation instance using the new tree.
			trainingData.updatePredictionsWithLearnedValueFromNewTree(tree);
			validationData.updatePredictionsWithLearnedValueFromNewTree(tree);

			// Calculate the training and validation error for this iteration.
			double trainingRMSE = trainingData.calculateRootMeanSquaredError();
			double validationRMSE = validationData.calculateRootMeanSquaredError();
			
			// Add the tree to the function approximation, keep track of the training and validation errors.
			function.addTree(tree, trainingRMSE, validationRMSE);
			
			Logger.println(Logger.LEVELS.DEBUG, "\tAdded 1 tree in : " + timer.getElapsedSeconds());
		}
		return function;
	}
	
	public static CrossValidatedResultFunctionEnsemble crossValidate(GbmParameters parameters, Dataset training, int numOfFolds, int stepSize) {
		if (numOfFolds <= 1) {
			throw new IllegalStateException("Number of folds must be > 1 for cross validation");
		}
		
		CrossValidationStepper[] steppers = new CrossValidationStepper[numOfFolds];

		// Partition the data set into k folds. All done with boolean index masks into the original dataset
		int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(training.getNumberOfExamples());
		int foldSize = shuffledIndices.length / numOfFolds;
		boolean[][] trainingInEachFold = new boolean[numOfFolds][shuffledIndices.length];
		for (int i = 0; i < numOfFolds; i++) {
			int first = i * foldSize, last = (i * foldSize) + (numOfFolds-1)*foldSize;
			for (int j = first; j < last; j++) {
				int safeIndex = j % shuffledIndices.length;
				trainingInEachFold[i][shuffledIndices[safeIndex]] = true;
			}
		}
		
		for (int i = 0; i < numOfFolds; i++) {
			double meanResponse =  training.calcMeanResponse(trainingInEachFold[i]);
			steppers[i] = new CrossValidationStepper(parameters, 
					new ResultFunction(parameters, meanResponse, training.getPredictorNames()),
					new GbmDataset(training),
					trainingInEachFold[i],
					(numOfFolds-1)*foldSize, 
					stepSize);
			
			steppers[i].dataset.initializePredictions(meanResponse);
		}
		
		int lastTreeIndex = -1;
		double lastAvgValidationError = Double.MAX_VALUE;
		double avgValidError = 0.0;
		double avgTrainingError = 0.0;
		Queue<Future<Void>> futures = new LinkedList<Future<Void>>();
		while (lastTreeIndex + stepSize < parameters.numOfTrees) {
			lastTreeIndex += stepSize;
			for (int i = 0; i < numOfFolds; i++) {
				futures.add(executor.submit(steppers[i]));
			}
			avgValidError = 0.0;
			for (int i = 0; i < numOfFolds; i++) {
				try {
					futures.poll().get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					System.exit(1);
				}
				avgValidError += steppers[i].function.validationError.get(lastTreeIndex);
				avgTrainingError += steppers[i].function.trainingError.get(lastTreeIndex);
			}
			avgValidError /= numOfFolds;
			avgTrainingError /= numOfFolds;
			Logger.println(LEVELS.DEBUG, 
					"Training: " + avgTrainingError + 
					"\nLast Avg Validation Error: " + lastAvgValidationError + 
					"\nCurrent Avg Validation Error: " + avgValidError + 
					"\nDifference: " + (lastAvgValidationError - avgValidError));
			if (DoubleCompare.lessThan(avgValidError, lastAvgValidationError)) {
				lastAvgValidationError = avgValidError;
			} else {
				break;
			}
		}

		return new CrossValidatedResultFunctionEnsemble(parameters, steppers, lastTreeIndex);
	}
}