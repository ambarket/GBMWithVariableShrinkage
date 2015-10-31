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

import utilities.Logger;
import dataset.Attribute;
public class RegressionTree {
	public enum LearningRatePolicy {CONSTANT, VARIABLE};
	// class members
	private int minExamplesInNode;
	private int maxNumberOfSplits;
	private double maxLearningRate;
	private int sampleSize;
	private LearningRatePolicy learningRatePolicy;
	
	public TreeNode root;
	
	public RegressionTree(GbmParameters parameters, int sampleSize) {
		setMinObsInNode(parameters.minExamplesInNode);
		setMaxNumberOfSplits(parameters.maxNumberOfSplits);
		setMaxLearningRate(parameters.maxLearningRate);
		setSampleSize(sampleSize);
		this.learningRatePolicy = parameters.learningRatePolicy;
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
	
	public double getLearnedValue(Attribute[] instance_x) {
		if (root == null) {
			throw new IllegalStateException("Should never call getLearnedValue on a tree with a null root");
		}
		TerminalNode leaf = root.getLearnedTerminalNode(instance_x);
		if (learningRatePolicy == LearningRatePolicy.CONSTANT) {
			return maxLearningRate * leaf.terminalValue;
		} else {
			return maxLearningRate * leaf.instanceCount / sampleSize * leaf.terminalValue;
		}
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
		Queue<DataSplit> leaves = new LinkedList<DataSplit>();
		DataSplit rootSplit = DataSplit.splitDataIntoChildren(dataset, inSample, minExamplesInNode, meanResponseInParent, squaredErrorBeforeSplit);
		if (rootSplit == null) {
			int count = 0;
			for (int i = 0; i < inSample.length; i++) {
				count += (inSample[i]) ? 1 : 0;
			}
			TreeNode unSplitRoot = new TreeNode(meanResponseInParent, squaredErrorBeforeSplit, count);
			return unSplitRoot;
		}
		leaves.add(rootSplit);
		
		int numOfSplits = 1;
		PriorityQueue<PossibleChild> possibleChildren = new PriorityQueue<PossibleChild>();
		while (numOfSplits < maxNumberOfSplits) {
			while(!leaves.isEmpty()) {
				DataSplit parent = leaves.poll();
				DataSplit left = null, right = null, missing = null;
				if (parent.node.leftChild == null && minExamplesInNode * 2 <= parent.node.leftTerminalNode.instanceCount) {
					left = DataSplit.splitDataIntoChildren(dataset, parent.inLeftChild, minExamplesInNode, parent.node.leftTerminalNode.terminalValue, parent.node.leftTerminalNode.squaredError);
				}
				if (parent.node.rightChild == null && minExamplesInNode * 2 <= parent.node.rightTerminalNode.instanceCount) {
					right = DataSplit.splitDataIntoChildren(dataset, parent.inRightChild, minExamplesInNode, parent.node.rightTerminalNode.terminalValue, parent.node.rightTerminalNode.squaredError);
				}
				if (parent.node.missingChild == null && minExamplesInNode * 2 <= parent.node.missingTerminalNode.instanceCount) {
					missing = DataSplit.splitDataIntoChildren(dataset, parent.inMissingChild, minExamplesInNode, parent.node.missingTerminalNode.terminalValue, parent.node.missingTerminalNode.squaredError);
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
			numOfSplits++;
		}
		return rootSplit.node;
	}
	
	public void print_nodes() throws IOException {
		root.printTree(new OutputStreamWriter(System.out));
	}
	
	private class PossibleChild implements Comparable<PossibleChild>{
		DataSplit parent;
		DataSplit child;
		int whichNode; // 1 = left, 2 = right, 3 = missing
		double errorImprovement;
		PossibleChild(DataSplit parent, DataSplit child, int whichNode, double errorImprovement) {
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
