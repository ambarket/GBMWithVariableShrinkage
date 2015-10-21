package gbm;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


/*
 *  The following class implements a regression tree
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import utilities.Logger;
public class RegressionTree {
	public enum TerminalType {
		AVERAGE
	}

	// class members
	private int minExamplesInNode;
	private int maxNumberOfSplits;
	private TerminalType terminalType;
	
	public TreeNode root;
	
	// construction function
	public RegressionTree() {
		this(10, 3, TerminalType.AVERAGE);
	}
	
	public RegressionTree(int minExamplesInNode, int maxNumberOfSplits, TerminalType terminalType) {
		setMinObsInNode(minExamplesInNode);
		setMaxNumberOfSplits(maxNumberOfSplits);
		setTerminalType(terminalType);
		root = null;
	}
	
	public void setTerminalType(TerminalType terminalType) {
		this.terminalType = terminalType;
	}
	
	public void setMinObsInNode(int minExamplesInNode) {
		if (minExamplesInNode < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "The min obs in node value must be >= 1");
			System.exit(0);
		}
		this.minExamplesInNode = minExamplesInNode;
	}
	
	public void setMaxNumberOfSplits(int maxNumberOfSplits) {
		if (maxNumberOfSplits < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "The maxNumberOfSplits must be >= 1");
			System.exit(0);
		}
		this.maxNumberOfSplits = maxNumberOfSplits;
	}
	
	public double getLearnedValue(Attribute[] instance_x) {
		if (root == null) {
			throw new IllegalStateException("Should never call getLearnedValue on a tree with a null root");
		}
		
		return root.getLearnedValue(instance_x);
	}
	
	// build the regression tree
	public RegressionTree build(Dataset dataset, boolean[] inSample) {
		// Calculate error before splitting
		double mean = dataset.calcMeanY(inSample);
		double squaredError = 0.0;
		for (Response y : dataset.responses) {
			squaredError += (y.getPsuedoResponse() - mean) * (y.getPsuedoResponse() - mean);
		}
		// build the regression tree
		//root = tree_builder(instances, labels_y, squaredError, 0);
		root = buildTree_MaxNumberOfSplits(dataset, inSample, mean, squaredError);
		if (root == null) {
			throw new IllegalStateException("buildTree_MaxNumberOfSplits returned a null root node.");
		}
		return this;
	}
	
	private TreeNode buildTree_MaxNumberOfSplits(Dataset dataset, boolean[] inSample, double missingTerminalValue, double squaredErrorBeforeSplit) {
		Queue<DataSplit> leaves = new LinkedList<DataSplit>();
		DataSplit rootSplit = DataSplit.splitDataIntoChildren(dataset, inSample, minExamplesInNode, missingTerminalValue, squaredErrorBeforeSplit, terminalType);
		if (rootSplit == null) {
			int count = 0;
			for (int i = 0; i < inSample.length; i++) {
				count += (inSample[i]) ? 1 : 0;
			}
			TreeNode unSplitRoot = new TreeNode(missingTerminalValue, squaredErrorBeforeSplit, count);
			return unSplitRoot;
		}
		leaves.add(rootSplit);
		
		int numOfSplits = 1;
		PriorityQueue<PossibleChild> possibleChildren = new PriorityQueue<PossibleChild>();
		while (numOfSplits < maxNumberOfSplits) {
			while(!leaves.isEmpty()) {
				DataSplit parent = leaves.poll();
				DataSplit left = null, right = null;
				if (parent.node.leftChild == null && minExamplesInNode * 2 <= parent.node.leftInstanceCount) {
					left = DataSplit.splitDataIntoChildren(dataset, parent.inLeftChild, minExamplesInNode, parent.node.leftTerminalValue, parent.node.leftSquaredError, terminalType);
				}
				if (parent.node.rightChild == null && minExamplesInNode * 2 <= parent.node.rightInstanceCount) {
					right = DataSplit.splitDataIntoChildren(dataset, parent.inRightChild, minExamplesInNode, parent.node.rightTerminalValue, parent.node.rightSquaredError, terminalType);
				}
				if (left != null) {
					possibleChildren.add(new PossibleChild(parent, left, true, left.node.squaredErrorBeforeSplit - (left.node.leftSquaredError + left.node.rightSquaredError)));
				}
				if (right != null) {
					possibleChildren.add(new PossibleChild(parent, right, false, right.node.squaredErrorBeforeSplit - (right.node.leftSquaredError + right.node.rightSquaredError)));
				}
			}
			if (possibleChildren.isEmpty()) {
				throw new IllegalStateException("No possible children to pick from in tree builder. I dont think this is possible");
			}
			PossibleChild bestChild = possibleChildren.poll();
			if (bestChild.left) {
				bestChild.parent.node.leftChild = bestChild.child.node;
			} else {
				bestChild.parent.node.rightChild = bestChild.child.node;
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
		boolean left;
		double errorImprovement;
		PossibleChild(DataSplit parent, DataSplit child, boolean left, double errorImprovement) {
			this.parent = parent;
			this.child = child;
			this.left = left;
			this.errorImprovement = errorImprovement;
		}
		
		public int compareTo(PossibleChild that) {
			return (int) Math.signum(that.errorImprovement - this.errorImprovement);
		}
	}
}
