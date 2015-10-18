package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import gbm.RegressionTree.TerminalType;

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
	private int minObsInNode;
	private int maxTreeDepth;
	
	private Dataset dataset;
	
	public GradientBoostingTree(Dataset dataset) {
		this(dataset, 0.5, 0.05, 100, 10, 3);
	}
	
	public GradientBoostingTree(Dataset dataset, double bagFraction, double learningRate, 
			int numOfTrees, int minObsInNode, int maxTreeDepth)
		{
		this.dataset = dataset;
		setBagFraction(bagFraction);
		setLearningRate(learningRate);
		setNumOfTrees(numOfTrees);
		setMinObsInNode(minObsInNode);
		setMaxTreeDepth(maxTreeDepth);
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
	
	private void setMaxTreeDepth(int maxTreeDepth) {
		if (maxTreeDepth < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "Max tree depth must be >= 1");
			System.exit(0);	
		}
		this.maxTreeDepth = maxTreeDepth;
	}
	
	private void setMinObsInNode(int minObsInNode) {
		if (minObsInNode < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "MinObsInNode must be >= 1");
			System.exit(0);
		}
		
		if (minObsInNode * 2 > dataset.numOfExamples) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of examples int he dataset must be >= minObsInNode * 2");
			System.exit(0);
		}
		this.minObsInNode = minObsInNode;
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
		return minObsInNode;
	}
	
	public int getMaxTreeDepth() {
		return maxTreeDepth;
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
		public double predictLabel(ArrayList<Double> instance_x) {
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
				relativeInfluences[i] /= sum;
				relativeInfluences[i] *= 100;
			}
			return relativeInfluences;
		}
		
		private void calcRelativeInfluenceHelper(double[] relativeInfluences, TreeNode node) {
			if (node == null) return;
			relativeInfluences[node.splitAttribute] += node.squaredErrorBeforeSplit - node.leftSquaredError + node.rightSquaredError;
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
		ResultFunction function = new ResultFunction(learningRate, dataset.meanY, dataset.instances_x.get(0).size());
		
		// prepare the iteration
		double[] hypothesisValue = new double[dataset.numOfExamples];
		// initialize h_value
		for (int i = 0; i < dataset.numOfExamples; i++) {
			hypothesisValue[i] = dataset.meanY;
		}
		
		// begin the boosting process
		int iterationNum = 0;

		
		StopWatch timer = (new StopWatch());
		
		/* Used to track which indices into the instances_x and responses_y structures are in the sample for the current iteration.
		 * 
		 * This will allow us to presort the instances by each attribute and save a ton of time while finding the best split point
		 * when growing the trees.
		 * 
		 */
		boolean[] inSample = new boolean[dataset.numOfExamples];
		while (iterationNum < numOfTrees) {
			timer.start();
			// calculate the gradient
			ArrayList<Double> gradient = new ArrayList<Double>();
			for (int i = 0; i < dataset.responses_y.size(); i++) {
				dataset.responses_y.set(i, (dataset.originalResponses_y.get(i) - hypothesisValue[i]));
			}
			
			// we need to sample randomly without replacement
			int[] shuffledIndices = RandomSample.fisherYatesShuffle(dataset.numOfExamples);
			
			// data for growing trees
			
			int sampleSize = (int)(bagFraction * shuffledIndices.length);
			for (int i = 0; i < sampleSize; i++ ) {
				inSample[shuffledIndices[i]] = true;
			}
			
			// fit a regression tree and add it to the list of trees
			RegressionTree tree = (new RegressionTree(minObsInNode, maxTreeDepth, TerminalType.AVERAGE)).build(dataset, inSample);
	
			function.trees.add(tree);
			
			// update hypothesis information, prepare for the next iteration
			for (int i = 0; i < dataset.numOfExamples; i++) {
				hypothesisValue[i] += learningRate * tree.getLearnedValue(dataset.instances_x.get(i));
			}
			
			// next iteration
			iterationNum += 1;
			Logger.println(Logger.LEVELS.DEBUG, "\tAdded 1 tree in : " + timer.getElapsedSeconds());
		}
		return function;
	}

}