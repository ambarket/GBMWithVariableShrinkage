package regressionTree;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


/*
 *  The following class implements a regression tree
 */

import gbm.GbmDataset;
import gbm.GbmParameters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import regressionTree.OptimalSplitFinder.TreeNodeAndInParentPair;
import utilities.Logger;
import utilities.MersenneTwisterFast;
import utilities.SumCountAverage;
import dataset.Attribute;
public class RegressionTree {
	public enum LearningRatePolicy {CONSTANT, VARIABLE, REVISED_VARIABLE};
	public enum SplitsPolicy {CONSTANT, INCREASING, RANDOM};
	// class members
	private int minExamplesInNode;
	private int maxNumberOfSplits;
	private double maxLearningRate;
	private double minLearningRate;
	private int sampleSize;
	private LearningRatePolicy learningRatePolicy;
	private SplitsPolicy splitsPolicy;
	
	private double learningRateToExampleCountRatio;
	public TreeNode root;
	
	public int actualNumberOfSplits = 0; // Will be set after growing
	
	public RegressionTree(GbmParameters parameters, int sampleSize) {
		setMinObsInNode(parameters.minExamplesInNode);
		setMaxNumberOfSplits(parameters.maxNumberOfSplits);
		setMaxLearningRate(parameters.maxLearningRate);
		if (parameters.learningRatePolicy != LearningRatePolicy.REVISED_VARIABLE) {
			this.minLearningRate = parameters.maxLearningRate; // Wont ever be used
		} else {
			setMinLearningRate(parameters.minLearningRate); 
		}
		setSampleSize(sampleSize);
		this.learningRatePolicy = parameters.learningRatePolicy;
		this.splitsPolicy = parameters.splitsPolicy;
		
		// No need to recalculate this everytime since its constant for this regression tree
		this.learningRateToExampleCountRatio = (maxLearningRate - minLearningRate) / (sampleSize - minExamplesInNode);
	
		root = null;
	}
	
	private void setSampleSize(int sampleSize) {
		if (sampleSize < minExamplesInNode) {
			Logger.println(Logger.LEVELS.INFO, "sampleSize must be > minExamplesInNode, but was " + sampleSize + " and minExamplesInNode was " + minExamplesInNode);
			System.exit(0);
		}
		this.sampleSize = sampleSize;
	}
	
	private void setMaxLearningRate(double maxLearningRate) {
		if (maxLearningRate <= 0) {
			Logger.println(Logger.LEVELS.INFO, "Learning rate must be > 0");
			System.exit(0);
		}
		this.maxLearningRate = maxLearningRate;
	}
	
	private void setMinLearningRate(double minLearningRate) {
		if (minLearningRate <= 0) {
			Logger.println(Logger.LEVELS.INFO, "Learning rate must be > 0");
			System.exit(0);
		}
		this.minLearningRate = minLearningRate;
	}
	
	public void setMinObsInNode(int minExamplesInNode) {
		if (minExamplesInNode < 1) {
			Logger.println(Logger.LEVELS.INFO, "The min obs in node value must be >= 1");
			System.exit(0);
		}
		this.minExamplesInNode = minExamplesInNode;
	}
	
	public void setMaxNumberOfSplits(int maxNumberOfSplits) {
		if (maxNumberOfSplits < 1) {
			Logger.println(Logger.LEVELS.INFO, "The maxNumberOfSplits must be >= 1");
			System.exit(0);
		}
		this.maxNumberOfSplits = maxNumberOfSplits;
	}
	
	public LearningRateTerminalValuePair getLearningRateTerminalValuePair(Attribute[] instance_x) {
		if (root == null) {
			throw new IllegalStateException("Should never call getLearnedValue on a tree with a null root");
		}
		TerminalNode leaf = root.getLearnedTerminalNode(instance_x);
		double learningRate = 0.0;
		if (learningRatePolicy == LearningRatePolicy.CONSTANT) {
			learningRate = maxLearningRate;
		} else if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			learningRate = (((leaf.instanceCount - minExamplesInNode) * learningRateToExampleCountRatio) + minLearningRate);
		} else {
			learningRate = Math.min(0.5, maxLearningRate * leaf.instanceCount / sampleSize);
		} 
		return new LearningRateTerminalValuePair(learningRate, leaf.terminalValue);
	}
	
	public double getLearnedValueWithLearningRateApplied(Attribute[] instance_x) {
		if (root == null) {
			throw new IllegalStateException("Should never call getLearnedValue on a tree with a null root");
		}
		TerminalNode leaf = root.getLearnedTerminalNode(instance_x);
		double learningRate = 0.0;
		if (learningRatePolicy == LearningRatePolicy.CONSTANT) {
			learningRate = maxLearningRate;
		} else if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			learningRate = (((leaf.instanceCount - minExamplesInNode) * learningRateToExampleCountRatio) + minLearningRate);
		} else {
			learningRate = Math.min(0.5, maxLearningRate * leaf.instanceCount / sampleSize);
		} 
		return learningRate * leaf.terminalValue;
	}
	
	public RegressionTree build(GbmDataset dataset, boolean[] inSample) {
		// Calculate error before splitting
		double mean = dataset.calcMeanTrainingPseudoResponse(inSample);
		double squaredError = 0.0;
		for (double pseudoResponse : dataset.trainingPseudoResponses) {
			squaredError += (pseudoResponse - mean) * (pseudoResponse - mean);
		}
		// build the regression tree
		root = buildTree_MaxNumberOfSplits(dataset, inSample, mean, squaredError);
		if (root == null) {
			throw new IllegalStateException("buildTree_MaxNumberOfSplits returned a null root node.");
		}
		return this;
	}
	
	private TreeNode buildTree_MaxNumberOfSplits(GbmDataset dataset, boolean[] inSample, double meanResponseInParent, double squaredErrorBeforeSplit) {
		Queue<TreeNodeAndInParentPair> leaves = new LinkedList<TreeNodeAndInParentPair>();
		int numOfExamples = dataset.getNumberOfTrainingExamples();
		int[] trainingDataToChildMap = new int[numOfExamples];
		int count = 0;
		for (int i = 0; i < inSample.length; i++) {
			trainingDataToChildMap[i] = (inSample[i]) ? 1 : 0;
			count += trainingDataToChildMap[i];
		}
		TreeNodeAndInParentPair rootSplit = OptimalSplitFinder.getOptimalSplitSingleThread(dataset, trainingDataToChildMap, 1, minExamplesInNode, meanResponseInParent, squaredErrorBeforeSplit);
		if (rootSplit == null) {
			TreeNode unSplitRoot = new TreeNode(meanResponseInParent, squaredErrorBeforeSplit, count);
			return unSplitRoot;
		}
		leaves.add(rootSplit);

		actualNumberOfSplits = 1;
		PriorityQueue<PossibleChild> possibleChildren = new PriorityQueue<PossibleChild>();
		int maxSplits = 0;
		switch(splitsPolicy) {
			case CONSTANT: maxSplits = maxNumberOfSplits; break;
			case RANDOM: maxSplits = 1 + (new MersenneTwisterFast().nextInt(maxNumberOfSplits)); /*System.out.println(maxSplits)*/;break;
			case INCREASING: throw new UnsupportedOperationException();
		}

		while (actualNumberOfSplits < maxSplits) {
			while(!leaves.isEmpty()) {
				TreeNodeAndInParentPair parent = leaves.poll();
				// map training data to the correct child
				Attribute[][] trainingInstances = dataset.getTrainingInstances();
				for (int instanceNum = 0; instanceNum < numOfExamples; instanceNum++) {
					//if (parent.inParentHash.contains(instanceNum)) {
					if (parent.inParent[instanceNum]) {
						trainingDataToChildMap[instanceNum] = parent.node.whichChild(trainingInstances[instanceNum]);
					} else {
						trainingDataToChildMap[instanceNum] = 0;
					}
				}
				TreeNodeAndInParentPair left = null, right = null, missing = null;
				if (parent.node.leftChild == null && minExamplesInNode * 2 <= parent.node.leftTerminalNode.instanceCount) {
					left = OptimalSplitFinder.getOptimalSplitSingleThread(dataset, trainingDataToChildMap, 1, minExamplesInNode, parent.node.leftTerminalNode.terminalValue, parent.node.leftTerminalNode.squaredError);
				}
				if (parent.node.rightChild == null && minExamplesInNode * 2 <= parent.node.rightTerminalNode.instanceCount) {
					right = OptimalSplitFinder.getOptimalSplitSingleThread(dataset, trainingDataToChildMap, 2, minExamplesInNode, parent.node.rightTerminalNode.terminalValue, parent.node.rightTerminalNode.squaredError);
				}
				if (parent.node.missingChild == null && minExamplesInNode * 2 <= parent.node.missingTerminalNode.instanceCount) {
					missing = OptimalSplitFinder.getOptimalSplitSingleThread(dataset, trainingDataToChildMap, 3, minExamplesInNode, parent.node.missingTerminalNode.terminalValue, parent.node.missingTerminalNode.squaredError);
				}
				if (left != null) {
					possibleChildren.add(new PossibleChild(parent, left, 1, left.node.getSquaredErrorImprovement()));
				}
				if (right != null) {
					possibleChildren.add(new PossibleChild(parent, right, 2, right.node.getSquaredErrorImprovement()));
				}
				if (missing != null) {
					possibleChildren.add(new PossibleChild(parent, missing, 3, missing.node.getSquaredErrorImprovement()));
				}
			}
			if (possibleChildren.isEmpty()) {
				return rootSplit.node; // This happens when we don't have enough examples to split anymore.
			}
			PossibleChild bestChild = possibleChildren.poll();
			if (bestChild.whichNode == 1) {
				bestChild.parent.node.leftChild = bestChild.child.node;
			} else if (bestChild.whichNode == 2){
				bestChild.parent.node.rightChild = bestChild.child.node;
			} else {
				bestChild.parent.node.missingChild = bestChild.child.node;
			}
			
			leaves.add(bestChild.child);
			actualNumberOfSplits++;
		}
		return rootSplit.node;
	}
	
	/**
	 * Average should always just be number of examples / number of terminal nodes.
	 * But this will also allow us to get the standard deviation which may be interesting.
	 * @return
	 */
	public SumCountAverage getAverageNumberOfExamplesInNode() {
		return root.sumNumberOfExamplesInTerminalNodes(new SumCountAverage());
	}
	
	public void print_nodes() throws IOException {
		root.printTree(new OutputStreamWriter(System.out));
	}
	
	private class PossibleChild implements Comparable<PossibleChild>{
		TreeNodeAndInParentPair parent;
		TreeNodeAndInParentPair child;
		int whichNode; // 1 = left, 2 = right, 3 = missing
		double errorImprovement;
		PossibleChild(TreeNodeAndInParentPair parent, TreeNodeAndInParentPair child, int whichNode, double errorImprovement) {
			this.parent = parent;
			this.child = child;
			this.whichNode = whichNode;
			this.errorImprovement = errorImprovement;
		}
		
		public int compareTo(PossibleChild that) {
			return (int) Math.signum(that.errorImprovement - this.errorImprovement);
		}
	}
}
