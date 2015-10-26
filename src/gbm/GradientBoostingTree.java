package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.ArrayList;
//import gbt.ranker.RegressionTree.TerminalType;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class GradientBoostingTree {
	public static ExecutorService executor = Executors.newFixedThreadPool(4);
	// class members
	private double bagFraction; 
	private double maxLearningRate;
	private int numOfTrees;
	
	// tree related parameters
	private int minExamplesInNode;
	private int maxNumberOfSplits;
	
	private GbmDataset trainingData;
	private GbmDataset validationData;
	
	public GradientBoostingTree(Dataset trainingData, Dataset validationData) {
		this(trainingData, validationData, 0.5, 0.05, 100, 10, 3);
	}
	
	public GradientBoostingTree(Dataset trainingData, Dataset validationData, double bagFraction, double learningRate, 
			int numOfTrees, int minExamplesInNode, int maxNumberOfSplits)
		{
		this.trainingData = new GbmDataset(trainingData);
		this.validationData = new GbmDataset(validationData);
		setBagFraction(bagFraction);
		setMaxLearningRate(learningRate);
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
		
		if (minExamplesInNode * 2 > trainingData.getNumberOfExamples()) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of examples int he dataset must be >= minExamplesInNode * 2");
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
	
	public class ResultFunction {
		// class members
		public double initialValue;
		public ArrayList<RegressionTree> trees;
		public double learningRate;
		public int numberOfPredictors;
		
		public ArrayList<Double> trainingError;
		public ArrayList<Double> validationError;
		
		// construction function
		ResultFunction(double learningRate, double intialValue, int numberOfPredictors) {
			this.initialValue = intialValue;
			this.learningRate = learningRate;
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
		public double predictLabel(Attribute[] instance_x) {
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
	public ResultFunction buildGradientBoostingMachine() {

		
		// Initialize the function approximation to the mean response in the training data
		double meanTrainingResponse = trainingData.calcMeanResponse();
		ResultFunction function = new ResultFunction(maxLearningRate, meanTrainingResponse, trainingData.getNumberOfPredictors());
		
		// Initialize predictions of all instances to the initial function value.
		trainingData.initializePredictions(meanTrainingResponse);
		validationData.initializePredictions(meanTrainingResponse);
	
		// begin the boosting process
		StopWatch timer = (new StopWatch());
		for (int iterationNum = 0; iterationNum < numOfTrees; iterationNum++) {
			timer.start();
			// Update the current pseudo responses (gradients) of all the training instances.
			trainingData.updatePseudoResponses();
			
			// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
			int[] shuffledIndices = RandomSample.fisherYatesShuffle(trainingData.getNumberOfExamples());
			int sampleSize = (int)(bagFraction * shuffledIndices.length);
			boolean[] inSample = new boolean[trainingData.getNumberOfExamples()];
			for (int i = 0; i < sampleSize; i++ ) {
				inSample[shuffledIndices[i]] = true;
			}
			
			// Fit a regression tree to predict the current pseudo responses on the training data.
			RegressionTree tree = (new RegressionTree(minExamplesInNode, maxNumberOfSplits, maxLearningRate)).build(trainingData, inSample);
			
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

}