package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.ArrayList;
//import gbt.ranker.RegressionTree.TerminalType;
import java.util.Iterator;

import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class GradientBoostingTree {
	// class members
	private double bagFraction; 
	private double learningRate;
	private int numOfTrees;
	
	// tree related parameters
	private int minExamplesInNode;
	private int maxNumberOfSplits;
	
	private Dataset dataset;
	
	public GradientBoostingTree(Dataset dataset) {
		this(dataset, 0.5, 0.05, 100, 10, 3);
	}
	
	public GradientBoostingTree(Dataset dataset, double bagFraction, double learningRate, 
			int numOfTrees, int minExamplesInNode, int maxNumberOfSplits)
		{
		this.dataset = dataset;
		setBagFraction(bagFraction);
		setLearningRate(learningRate);
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
	
	private void setLearningRate(double learningRate) {
		if (learningRate <= 0) {
			Logger.println(Logger.LEVELS.DEBUG, "Learning rate must be >= 0");
			System.exit(0);
		}
		this.learningRate = learningRate;
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
		
		if (minExamplesInNode * 2 > dataset.numberOfExamples) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of examples int he dataset must be >= minExamplesInNode * 2");
			System.exit(0);
		}
		this.minExamplesInNode = minExamplesInNode;
	}
	
	// get parameters
	public double getBagFraction() {
		return bagFraction;
	}
	
	public double getLearningRate() {
		return learningRate;
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
		
		// construction function
		ResultFunction(double learningRate, double intialValue, int numberOfPredictors) {
			this.initialValue = intialValue;
			this.learningRate = learningRate;
			this.numberOfPredictors = numberOfPredictors;
			trees = new ArrayList<RegressionTree> ();
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
				
				result += learningRate * tree.getLearnedValue(instance_x);
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
		// get an initial guess of the function
		
		// initialize the final result
		double meanY = dataset.calcMeanY();
		ResultFunction function = new ResultFunction(learningRate, meanY, dataset.numberOfPredictors);
		
		// prepare the iteration
		double[] hypothesisValue = new double[dataset.numberOfExamples];
		// initialize h_value
		
		for (int i = 0; i < dataset.numberOfExamples; i++) {
			hypothesisValue[i] = meanY;
		}
		
		// begin the boosting process
		int iterationNum = 0;

		
		StopWatch timer = (new StopWatch());
		
		/* Used to track which indices into the instances and responses structures are in the sample for the current iteration.
		 * 
		 * This will allow us to presort the instances by each attribute and save a ton of time while finding the best split point
		 * when growing the trees.
		 * 
		 */
		boolean[] inSample = new boolean[dataset.numberOfExamples];
		while (iterationNum < numOfTrees) {
			timer.start();
			// calculate the gradient and store as the new responses to fit to.
			for (int i = 0; i < dataset.numberOfExamples; i++) {
				dataset.responses[i].setPsuedoResponse(dataset.responses[i].getNumericValue() - hypothesisValue[i]);
			}
			
			// we need to sample randomly without replacement
			int[] shuffledIndices = RandomSample.fisherYatesShuffle(dataset.numberOfExamples);
			
			// data for growing trees
			int sampleSize = (int)(bagFraction * shuffledIndices.length);
			for (int i = 0; i < sampleSize; i++ ) {
				inSample[shuffledIndices[i]] = true;
			}
			
			// fit a regression tree and add it to the list of trees
			RegressionTree tree = (new RegressionTree(minExamplesInNode, maxNumberOfSplits)).build(dataset, inSample);
	
			function.trees.add(tree);
			
			// update hypothesis information, prepare for the next iteration
			for (int i = 0; i < dataset.numberOfExamples; i++) {
				hypothesisValue[i] += learningRate * tree.getLearnedValue(dataset.instances[i]);
			}
			
			// next iteration
			iterationNum += 1;
			Logger.println(Logger.LEVELS.DEBUG, "\tAdded 1 tree in : " + timer.getElapsedSeconds());
		}
		return function;
	}

}