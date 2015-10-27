package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.ArrayList;
//import gbt.ranker.RegressionTree.TerminalType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utilities.DoubleCompare;
import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class GradientBoostingTree {
	public static ExecutorService executor = Executors.newFixedThreadPool(20);
	
	public static class GbmParameters {
		// class members
		public double bagFraction; 
		public double maxLearningRate;
		public int numOfTrees;
		
		// tree related parameters
		public int minExamplesInNode;
		public int maxNumberOfSplits;
		
		public GbmParameters(double bagFraction, double maxLearningRate, int numOfTrees, int minExamplesInNode, int maxNumberOfSplits) {
			setBagFraction(bagFraction);
			setMaxLearningRate(maxLearningRate);
			setNumOfTrees(numOfTrees);
			setMinObsInNode(minExamplesInNode);
			setmaxNumberOfSplits(maxNumberOfSplits);
		}

		private void setBagFraction(double bagFraction) {
			if (bagFraction <= 0 || bagFraction > 1) {
				Logger.println(Logger.LEVELS.DEBUG, "BagFraction must be in range (0, 1]");
				System.exit(0);
			}
			this.bagFraction = bagFraction;
		}
		
		private void setMaxLearningRate(double maxLearningRate) {
			if (maxLearningRate <= 0) {
				Logger.println(Logger.LEVELS.DEBUG, "Learning rate must be >= 0");
				System.exit(0);
			}
			this.maxLearningRate = maxLearningRate;
		}
		
		private void setNumOfTrees(int numOfTrees) {
			if (numOfTrees < 1) {
				Logger.println(Logger.LEVELS.DEBUG, "Number of trees must be >= 1");
				System.exit(0);
			}
			this.numOfTrees = numOfTrees;
		}
		
		private void setmaxNumberOfSplits(int maxNumberOfSplits) {
			if (maxNumberOfSplits < 1) {
				Logger.println(Logger.LEVELS.DEBUG, "The tree's maxNumberOfSplits must be >= 1");
				System.exit(0);	
			}
			this.maxNumberOfSplits = maxNumberOfSplits;
		}
		
		private void setMinObsInNode(int minExamplesInNode) {
			if (minExamplesInNode < 1) {
				Logger.println(Logger.LEVELS.DEBUG, "MinObsInNode must be >= 1");
				System.exit(0);
			}
		
			this.minExamplesInNode = minExamplesInNode;
		}
		
		// get parameters
		public double getBagFraction() {
			return bagFraction;
		}
		
		public double getMaxLearningRate() {
			return maxLearningRate;
		}
		
		public int getNumOfTrees() {
			return numOfTrees;
		}
		
		public int getMinObsInNode() {
			return minExamplesInNode;
		}
		
		public int getMaxNumberOfSplits() {
			return maxNumberOfSplits;
		}
		
	}

	public static class ResultFunction {
		// class members
		public double initialValue;
		public ArrayList<RegressionTree> trees;
		public int numberOfPredictors;
		
		public ArrayList<Double> trainingError;
		public ArrayList<Double> validationError;
		
		public GbmParameters parameters;
		// construction function
		ResultFunction(GbmParameters parameters, double intialValue, int numberOfPredictors) {
			this.initialValue = intialValue;
			this.parameters = parameters;
			this.numberOfPredictors = numberOfPredictors;
			trees = new ArrayList<RegressionTree> ();
			trainingError = new ArrayList<Double>();
			validationError = new ArrayList<Double>();
		}
		
		
		public void addTree(RegressionTree newTree, double trainingError, double validationError) {
			this.trees.add(newTree);
			this.trainingError.add(trainingError);
			this.validationError.add(validationError);
		}
		// the following function is used to estimate the function
		public double getLearnedValue(Attribute[] instance_x) {
			double result = initialValue;
			
			if (trees.size() == 0) {
				return result;
			}
			
			Iterator<RegressionTree> iter = trees.iterator();
			while (iter.hasNext()) {
				RegressionTree tree = iter.next();
				// Learning rate is accoutned for in the tree itself.
				result += tree.getLearnedValue(instance_x);
			}
			
			return result;
		}
		
		public double[] calcRelativeInfluences() {
			double[] relativeInfluences = new double[numberOfPredictors];
			for (RegressionTree tree : trees) {
				calcRelativeInfluenceHelper(relativeInfluences, tree.root);
			}
			for (int i = 0; i < relativeInfluences.length; i++) {
				relativeInfluences[i] /= trees.size();
			}
			double sum = 0;
			for (int i = 0; i < relativeInfluences.length; i++) {
				sum += relativeInfluences[i];
			}
			for (int i = 0; i < relativeInfluences.length; i++) {
				relativeInfluences[i] *= 100;
				relativeInfluences[i] /= sum;
			}
			return relativeInfluences;
		}
		
		private void calcRelativeInfluenceHelper(double[] relativeInfluences, TreeNode node) {
			if (node == null) return;
			relativeInfluences[node.splitPredictorIndex] += Math.round(((node.squaredErrorBeforeSplit - (node.leftSquaredError + node.rightSquaredError))) * 10) / 10.0;
			calcRelativeInfluenceHelper(relativeInfluences, node.leftChild);
			calcRelativeInfluenceHelper(relativeInfluences, node.rightChild);
		}
	}
	
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
		ResultFunction function = new ResultFunction(parameters, meanTrainingResponse, trainingData.getNumberOfPredictors());
		
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
					new ResultFunction(parameters, meanResponse, training.getNumberOfPredictors()),
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
			System.out.println("Training: " + avgTrainingError + " Last Validation Error: " + lastAvgValidationError + " Validation Error: " + avgValidError + " Difference: " + (lastAvgValidationError - avgValidError));
			if (DoubleCompare.lessThan(avgValidError, lastAvgValidationError)) {
				lastAvgValidationError = avgValidError;
			} else {
				break;
			}
		}

		return new CrossValidatedResultFunctionEnsemble(parameters, steppers, lastTreeIndex);
	}
	
	private static class CrossValidationStepper implements Callable<Void> {
		GbmParameters parameters;
		ResultFunction function;
		GbmDataset dataset;
		boolean[] inSample;
		int stepSize;
		int numOfExamplesInSample;

		public CrossValidationStepper(GbmParameters parameters, ResultFunction function, GbmDataset dataset, boolean[] inSample, int numOfExamplesInSample, int stepSize) {
			this.parameters = parameters;
			this.function = function;
			this.dataset = dataset;
			this.inSample = inSample;
			this.numOfExamplesInSample = numOfExamplesInSample;
			this.stepSize = stepSize;
		}
		
		@Override
		public Void call() throws Exception {
			StopWatch timer = (new StopWatch()).start();
			for (int iteration = 0; iteration < stepSize; iteration++) {
				// Update the current pseudo responses (gradients) of all the training instances.
				dataset.updatePseudoResponses();
				
				// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
				int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(dataset.getNumberOfExamples());
				int sampleSize = (int)(parameters.bagFraction * numOfExamplesInSample);
				boolean[] inCurrentSample = new boolean[dataset.getNumberOfExamples()];
				int selected = 0, i = 0;
				while(selected < sampleSize) {
					if (this.inSample[shuffledIndices[i]]) {
						inCurrentSample[shuffledIndices[i]] = true;
						selected++;
					}
					i++;
				}
				
				// Fit a regression tree to predict the current pseudo responses on the training data.
				RegressionTree tree = (new RegressionTree(parameters.minExamplesInNode, parameters.maxNumberOfSplits, parameters.maxLearningRate)).build(dataset, inCurrentSample);
				
				// Update our predictions for each training and validation instance using the new tree.
				dataset.updatePredictionsWithLearnedValueFromNewTree(tree);

				// Calculate the training and validation error for this iteration.
				double trainingRMSE = dataset.calculateRootMeanSquaredError(inSample);
				double validationRMSE = dataset.calculateRootMeanSquaredError(inSample, true); // SecondFlag indicates to negate the inSample array
				
				// Add the tree to the function approximation, keep track of the training and validation errors.
				function.addTree(tree, trainingRMSE, validationRMSE);
			}
			Logger.println(Logger.LEVELS.INFO, "\tFinished " + stepSize + " in : " + timer.getElapsedSeconds());
			return null;
		}
	}
	
	public static class CrossValidatedResultFunctionEnsemble {
		GbmParameters parameters;
		public ResultFunction[] functions;
		public double avgInitialValue;
		public int optimalNumberOfTrees, totalNumberOfTrees;
		public int stepSize, numOfFolds;
		public double[] avgCvValidationErrors;
		public double[] avgCvTrainingErrors;
		
		public CrossValidatedResultFunctionEnsemble(GbmParameters parameters, CrossValidationStepper[] steppers, int totalNumberOfTrees) {
			this.parameters = parameters;
			this.totalNumberOfTrees = totalNumberOfTrees;
			this.stepSize = steppers[0].stepSize;
			this.numOfFolds = steppers.length;
			
			this.functions = new ResultFunction[steppers.length];
			this.avgInitialValue = 0.0;
			for (int i = 0; i < steppers.length; i++) {
				functions[i] = steppers[i].function;
				avgInitialValue += functions[i].initialValue;
			}
			this.avgInitialValue /= functions.length;

			// Optimal number of trees is the point where average cross validation error is minimized.
			double minAvgValidationError = Double.MAX_VALUE;
			this.avgCvTrainingErrors = new double[totalNumberOfTrees];
			this.avgCvValidationErrors = new double[totalNumberOfTrees];
			for (int i = 0; i < totalNumberOfTrees; i++) {
				for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
					avgCvTrainingErrors[i] += functions[functionIndex].trainingError.get(i);
					avgCvValidationErrors[i] += functions[functionIndex].validationError.get(i);
				}
				if (DoubleCompare.lessThan(avgCvValidationErrors[i], minAvgValidationError)) {
					minAvgValidationError = avgCvValidationErrors[i];
					this.optimalNumberOfTrees = i;
				}
			}
			for (int i = 0; i < totalNumberOfTrees; i++) {
				avgCvTrainingErrors[i] /= functions.length;
				avgCvValidationErrors[i] /= functions.length;
			}
			
			if (optimalNumberOfTrees == totalNumberOfTrees) {
				System.out.println("Warning: The optimal number was trees was equivalent to the number of trees grown. Consider running longer");
			}
		}
		
		// the following function is used to estimate the function
		public double getLearnedValue(Attribute[] instance_x) {
			double avgLearnedValue = 0.0;
			for (int j = 0; j < functions.length; j++) {
				avgLearnedValue += functions[j].getLearnedValue(instance_x);
			}
			avgLearnedValue /= functions.length;
			return avgLearnedValue;
			
			/* Not sure which way makes more sense.
			double result = avgInitialValue;
			for (int i = 0; i < optimalNumberOfTrees; i++) {
				double avgLearnedValue = 0.0;
				for (int j = 0; j < functions.length; j++) {
					avgLearnedValue += functions[j].trees.get(i).getLearnedValue(instance_x);
				}
				avgLearnedValue /= functions.length;
				result += avgLearnedValue;
			}
			return result;
			*/
		}
		
		public double[] calcRelativeInfluences() {
			double[] relativeInfluences = new double[functions[0].numberOfPredictors];
			int totalNumOfTrees = 0;
			for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
				for (RegressionTree tree : functions[functionIndex].trees) {
					calcRelativeInfluenceHelper(relativeInfluences, tree.root);
				}
				totalNumOfTrees += functions[functionIndex].trees.size();
			}
			
			for (int i = 0; i < relativeInfluences.length; i++) {
				relativeInfluences[i] /= totalNumOfTrees;
			}
			double sum = 0;
			for (int i = 0; i < relativeInfluences.length; i++) {
				sum += relativeInfluences[i];
			}
			for (int i = 0; i < relativeInfluences.length; i++) {
				relativeInfluences[i] *= 100;
				relativeInfluences[i] /= sum;
			}
			return relativeInfluences;
		}
		
		private void calcRelativeInfluenceHelper(double[] relativeInfluences, TreeNode node) {
			if (node == null) return;
			relativeInfluences[node.splitPredictorIndex] += Math.round(((node.squaredErrorBeforeSplit - (node.leftSquaredError + node.rightSquaredError))) * 10) / 10.0;
			calcRelativeInfluenceHelper(relativeInfluences, node.leftChild);
			calcRelativeInfluenceHelper(relativeInfluences, node.rightChild);
		}
	}
}