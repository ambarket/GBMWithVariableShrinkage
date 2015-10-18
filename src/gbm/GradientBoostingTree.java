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
	
	public GradientBoostingTree() {
		this(0.5, 0.05, 100, 10, 3);
	}
	
	public GradientBoostingTree(double bagFraction, double learningRate, 
			int numOfTrees, int minObsInNode, int maxTreeDepth)
		{
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
	public ResultFunction buildGradientBoostingMachine(ArrayList<ArrayList<Double>> instances_x, 
			ArrayList<Double> labels_y)
	{
		int numOfInstances = instances_x.size();
		
		if (numOfInstances != labels_y.size() || numOfInstances == 0 ) {
			Logger.println(Logger.LEVELS.DEBUG, "Error: The input_x size should not be zero " + "" +
					"and should match the size of input_y");
			return null;
		}
		
		// get an initial guess of the function
		double meanY = 0.0;
		Iterator<Double> iter = labels_y.iterator();
		while (iter.hasNext()) {
			meanY += iter.next();
		}
		meanY = meanY / numOfInstances;
		
		// initialize the final result
		ResultFunction function = new ResultFunction(learningRate, meanY, instances_x.get(0).size());
		
		// prepare the iteration
		double[] hypothesisValue = new double[numOfInstances];
		// initialize h_value
		for (int i = 0; i < numOfInstances; i++) {
			hypothesisValue[i] = meanY;
		}
		
		// begin the boosting process
		int iterationNum = 0;

		
		StopWatch timer = (new StopWatch());
		
		// Used to track which indices into the instances_x and labels_y structures are in the sample for the current iteration.
		//boolean[] inSample = new boolean[numOfInstances];
		while (iterationNum < numOfTrees) {
			timer.start();
			// calculate the gradient
			ArrayList<Double> gradient = new ArrayList<Double>();
			for (int i = 0; i < labels_y.size(); i++) {
				gradient.add(labels_y.get(i) - hypothesisValue[i]);
			}
			
			// we need to sample randomly without replacement
			int[] shuffledIndices = RandomSample.fisherYatesShuffle(numOfInstances);
			
			// data for growing trees
			ArrayList<ArrayList<Double>> train_x = new ArrayList<ArrayList<Double>> (); 
			ArrayList<Double> train_y = new ArrayList<Double> ();
			
			int sampleSize = (int)(bagFraction * shuffledIndices.length);
			for (int i = 0; i < sampleSize; i++ ) {
				//inSample[shuffledIndices[i]] = true;
				train_y.add(gradient.get(shuffledIndices[i]));
				train_x.add(instances_x.get(shuffledIndices[i]));
			}
			
			
			

			// fit a regression tree and add it to the list of trees
			RegressionTree tree = (new RegressionTree(minObsInNode, maxTreeDepth, TerminalType.AVERAGE)).build(train_x, train_y);
	
			function.trees.add(tree);
			
			// update hypothesis information, prepare for the next iteration
			for (int i = 0; i < numOfInstances; i++) {
				hypothesisValue[i] += learningRate * tree.getLearnedValue(instances_x.get(i));
			}
			
			// next iteration
			iterationNum += 1;
			Logger.println(Logger.LEVELS.DEBUG, "\tAdded 1 tree in : " + timer.getElapsedSeconds());
		}
		return function;
	}

}