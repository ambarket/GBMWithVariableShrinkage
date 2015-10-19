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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import utilities.Logger;
import utilities.StopWatch;
public class RegressionTree {
	public enum TerminalType {
		AVERAGE
	}

	// class members
	private int minObsInNode;
	private int maxDepth;
	private TerminalType terminalType;
	
	public TreeNode root;
	
	// construction function
	public RegressionTree() {
		this(10, 3, TerminalType.AVERAGE);
	}
	
	public RegressionTree(int minObsInNode, int maxDepth, TerminalType terminalType) {
		setMinObsInNode(minObsInNode);
		setMaxDepth(maxDepth);
		setTerminalType(terminalType);
		root = null;
	}
	
	public void setTerminalType(TerminalType terminalType) {
		this.terminalType = terminalType;
	}
	
	public void setMinObsInNode(int minObsInNode) {
		if (minObsInNode < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "The min obs in node value must be >= 1");
			System.exit(0);
		}
		this.minObsInNode = minObsInNode;
	}
	
	public void setMaxDepth(int depth) {
		if (depth < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "The max depth must be >= 1");
			System.exit(0);
		}
		maxDepth = depth;
	}
	
	public double getLearnedValue(ArrayList<Double> instances_x) {
		
		if (root == null) {
			throw new IllegalStateException("Should never call getLearnedValue on a tree with a null root");
		}
		
		TreeNode current = root;
		
		while (true) {
			if (instances_x.get(current.splitAttribute) < current.splitValue) {
				// we should consider left child
				if (current.leftChild == null) {
					return current.leftTerminalValue;
				} else {
					current = current.leftChild;
				}
			} else {
				// we should consider right child
				if (current.rightChild == null) {
					return current.rightTerminalValue;
				} else {
					current = current.rightChild;
				}
			}
		}
	}
	
	// build the regression tree
	public RegressionTree build(Dataset dataset, boolean[] inSample) {
		// Calculate error before splitting
		double mean = 0.0;
		for (Double y : dataset.responses_y) {
			mean += y;
		}
		mean /= dataset.responses_y.size();
		double squaredError = 0.0;
		for (Double y : dataset.responses_y) {
			squaredError += (y - mean) * (y - mean);
		}
		// build the regression tree
		//root = tree_builder(instances_x, labels_y, squaredError, 0);
		root = tree_builder_interaction_depth(dataset, inSample, squaredError);
		if (root == null) {
			// TODO: Instead could just make a node with arbitrary split and set both left and right to be the total mean.
			Logger.println(Logger.LEVELS.DEBUG, "Failed to split root node and get less than the current error. Something is wrong.");
			System.exit(0);
		}
		return this;
	}
	
	private TreeNode tree_builder_interaction_depth(Dataset dataset, boolean[] inSample, double squaredErrorBeforeSplit) {
		StopWatch timer = new StopWatch();
		Queue<DataSplit> leaves = new LinkedList<DataSplit>();
		DataSplit rootSplit = DataSplit.splitDataIntoChildren(dataset, inSample, minObsInNode, squaredErrorBeforeSplit, terminalType);
		if (rootSplit == null) {
			return null;
		}
		leaves.add(rootSplit);
		
		int numOfSplits = 1;
		PriorityQueue<PossibleChild> possibleChildren = new PriorityQueue<PossibleChild>();
		while (numOfSplits < maxDepth) {
			while(!leaves.isEmpty()) {
				DataSplit parent = leaves.poll();
				DataSplit left = null, right = null;
				if (parent.node.leftChild == null && minObsInNode * 2 <= parent.node.leftInstanceCount) {
					timer.start();
					left = DataSplit.splitDataIntoChildren(dataset, parent.inLeftChild, minObsInNode, parent.node.leftSquaredError, terminalType);
					Logger.println(Logger.LEVELS.DEBUG, "\t\t Split data into children in " + timer.getElapsedSeconds());
				}
				if (parent.node.rightChild == null && minObsInNode * 2 <= parent.node.rightInstanceCount) {
					right = DataSplit.splitDataIntoChildren(dataset, parent.inRightChild, minObsInNode, parent.node.rightSquaredError, terminalType);
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
	
	
	/* 
	 *  Print nodes and corresponding information
	 */
	/*
	private void print_each(TreeNode current, int depth, String leftOrRight) {
		if (current != null) {
			
			// print current node information
			Logger.println(Logger.LEVELS.DEBUG, 
					String.format("(Depth, side)=(%d,%s) with split information (%f, %d)", 
							depth, leftOrRight, current.splitValue, current.splitAttribute));
			
			print_each(current.leftChild, depth + 1, "left");
			print_each(current.rightChild, depth + 1, "right");
		}
	}
	*/
	
	/*
	 *  The following function builds a regression tree from data
	 */
	/*
	private TreeNode tree_builder(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, double squaredErrorBeforeSplit, int currentDepth) {
		// Stop if maxDepth has been reached or there aren't enough instances to split again
		if (currentDepth > maxDepth || minObsInNode * 2 > instances_x.size()) {
			return null;
		}
		

		// split the data at the split point. Null return value means no split was found that resulted in better error than currentSquaredError.
		DataSplit dataSplit = DataSplit.splitDataIntoChildren(instances_x, labels_y, minObsInNode, squaredErrorBeforeSplit, terminalType);
		if (dataSplit == null) {
			return null;
		}

				
		// append left and right side
		dataSplit.node.leftChild = tree_builder(dataSplit.leftInstances, dataSplit.leftLabels, dataSplit.node.leftSquaredError, currentDepth + 1); 
		dataSplit.node.rightChild = tree_builder(dataSplit.rightInstances, dataSplit.rightLabels, dataSplit.node.rightSquaredError, currentDepth + 1);
		
		return dataSplit.node;
	}
	*/
	
	/*
	 *  The following function builds a regression tree from data
	 */
}
