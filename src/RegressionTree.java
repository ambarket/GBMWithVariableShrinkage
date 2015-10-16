/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


/*
 *  The following class implements a regression tree
 */

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
		minObsInNode = 10;
		maxDepth = 3;
		root = null;
		terminalType = TerminalType.AVERAGE;
	}
	
	public void setTerminalType(TerminalType terminalType) {
		this.terminalType = terminalType;
	}
	
	public void setMinObsInNode(int minObsInNode) {
		if (minObsInNode < 3) {
			System.out.println("The min obs in node value must be >= 3");
			System.exit(0);
		}
		this.minObsInNode = minObsInNode;
	}
	
	public void setMaxDepth(int depth) {
		if (depth < 1) {
			System.out.println("The max depth must be >= 1");
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
	
	/* 
	 *  Print nodes and corresponding information
	 */
	private void print_each(TreeNode current, String info, String kind) {
		if (current != null) {
			// print current node information
			System.out.println("Current node information: " + 
					String.format("(Level, side)=(%s,%s) with split information (%f, %d)", 
							info, kind, current.splitValue, current.splitAttribute));
			
			print_each(current.leftChild, info + "-", "left");
			print_each(current.rightChild, info + "-", "right");
		}
	}
	
	public void print_nodes() {
		print_each(root, "", "");
	}
	
	// build the regression tree
	/*
	 *  The minObsInNode is minimum number of observations in the trees terminal nodes. 
	 */
	public void build_regression_tree(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y) {
		int numberOfInstances = instances_x.size();
		
		if (numberOfInstances != labels_y.size() || numberOfInstances == 0) {
			System.out.println("The number of instances does not match" + 
		                  " with the nunber of observations or " + 
					"the number of instances is 0");
			System.exit(0);
		}
		
		if (minObsInNode * 2 > numberOfInstances) {
			System.out.println("The number of instances is too small");
			System.exit(0);
		}
		
		// build the regression tree
		root = tree_builder(instances_x, labels_y, Double.MAX_VALUE, 0);
	}
	
	/*
	 *  The following function builds a regression tree from data
	 */
	private TreeNode tree_builder(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, double currentSquaredError, int currentDepth) {
		// Stop if maxDepth has been reached
		if (currentDepth > maxDepth || minObsInNode * 2 > instances_x.size() /*TODO: Revise to be less restrictive?*/) {
			return null;
		}
		
		// obtain the optimal split point
		BestSplit bestSplit = get_optimal_split(instances_x, labels_y, currentSquaredError);
		
		// Will be false if a split point couldn't be found that decreases error
		if (bestSplit.status == false) {
			return null;
		}
		
		// split the data at the split point
		DataSplit dataSplit = new DataSplit(instances_x, labels_y, bestSplit, terminalType);
		
		TreeNode new_node = new TreeNode(bestSplit.splitValue, bestSplit.splitAttribute,
				dataSplit.leftInstances.size(), dataSplit.rightInstances.size(), 
				dataSplit.leftTerminalValue, dataSplit.rightTerminalValue,
				bestSplit.leftSquaredError, bestSplit.rightSquaredError);
				
		// append left and right side
		new_node.leftChild = tree_builder(dataSplit.leftInstances, dataSplit.leftLabels, bestSplit.leftSquaredError, currentDepth + 1); 
		new_node.rightChild = tree_builder(dataSplit.rightInstances, dataSplit.rightLabels, bestSplit.rightSquaredError, currentDepth + 1);
		
		return new_node;
	}
	

	
	/*
	 *  The following function gets the best split given the data
	 */
	protected BestSplit get_optimal_split(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, double errorWithoutSplit) {
		BestSplit splitPoint = new BestSplit();
		
		int numberOfExamples = instances_x.size();
		int numberOfAttributes = instances_x.get(0).size();
		
		double minimumError = errorWithoutSplit;
		
		// Find the best attribute to split on 
		for (int currentSplitAttribute = 0; currentSplitAttribute < numberOfAttributes; currentSplitAttribute ++) {
			double currentLeftError = Double.MAX_VALUE;
			double currentRightError = Double.MAX_VALUE;
			double currentError = Double.MAX_VALUE;
			
			// Sort data based on the value of currentSplitAttribute
			ArrayList<ListData> sortedExamples = new ArrayList<ListData>();
			for (int exampleIndex = 0; exampleIndex < numberOfExamples; exampleIndex ++) {	
				sortedExamples.add(new ListData(instances_x.get(exampleIndex).get(currentSplitAttribute), labels_y.get(exampleIndex)));
			}
			Collections.sort(sortedExamples, new ListDataComparator());
			
			// Find left mean
			double sumLeft = 0.0, sumOfSquaresLeft = 0.0, meanLeft = 0.0;
			int    countLeft = minObsInNode - 1;
			double currentY = 0.0;
			for (int exampleIndex = 0; exampleIndex < minObsInNode - 1; exampleIndex++) {
				currentY = sortedExamples.get(exampleIndex).getY();
				sumLeft += currentY;
				sumOfSquaresLeft += currentY * currentY;
			}
			meanLeft = sumLeft / countLeft;
			
			// Find right mean
			double sumRight = 0.0, sumOfSquaresRight = 0.0, meanRight = 0.0;
			int countRight = numberOfExamples - minObsInNode + 1;
			for (int exampleIndex = minObsInNode - 1; exampleIndex < numberOfExamples; exampleIndex ++) {
				currentY = sortedExamples.get(exampleIndex).getY();
				sumRight += currentY;
				sumOfSquaresRight += currentY * currentY;
			}
			meanRight = sumRight / countRight;
						
			/* Find the best split. SplitIndex is the last example in the left child.
			 * Note: First split is Left = [0, minObsInNode) 					Right = [minObsInNode, numberOfExamples).
			 *        last split is Left = [0, numberOfExamples - minObsInNode) Right = [numberOfExamples - minObsInNode, numberOfExamples)
			 */
			for (int splitIndex = minObsInNode - 1; splitIndex < numberOfExamples - minObsInNode; splitIndex++) {
				double y = sortedExamples.get(splitIndex).getY();
				sumLeft += y;
				sumOfSquaresLeft += y*y;
				countLeft ++;
				meanLeft = sumLeft / countLeft;
				
				sumRight -= y;
				sumOfSquaresRight -= y*y;
				countRight --;
				meanRight = sumRight / countRight;
				
				// Don't want to split if the two values are the same as it would lead to inconsistent results, need to move to the next split point
				if (DoubleCompare.equals(sortedExamples.get(splitIndex).getX(), sortedExamples.get(splitIndex + 1).getX())) {
					continue;
				}
				
				currentLeftError = sumOfSquaresLeft - (2 * meanLeft * sumLeft) + (countLeft * meanLeft * meanLeft);
				currentRightError = sumOfSquaresRight - (2 * meanRight * sumRight) + (countRight * meanRight * meanRight);
				currentError = currentLeftError + currentRightError;
				
				if (DoubleCompare.lessThan(currentError, minimumError)) {
					splitPoint.splitAttribute = currentSplitAttribute;
					splitPoint.splitValue = (sortedExamples.get(splitIndex).getX() + sortedExamples.get(splitIndex + 1).getX())/2;
					splitPoint.leftSquaredError = currentLeftError;
					splitPoint.rightSquaredError = currentRightError;
					splitPoint.status = true;
					splitPoint.splitIndex = splitIndex;
					minimumError = currentError;
				}
			}
		}
		return splitPoint;
	}
		
	//----------------------------------------------HELPER CLASSES----------------------------------
	
	/*
	 *  The following class is used for split point in the regression tree
	 */
	public class BestSplit {
		public int splitAttribute;
		public double splitValue;
		public double leftSquaredError;
		public double rightSquaredError;
		public boolean status;
		public int splitIndex;
		
		// construction function
		BestSplit() {
			splitAttribute = 0;
			splitValue    = 0.0;
			leftSquaredError = Double.MAX_VALUE;
			rightSquaredError = Double.MAX_VALUE;
			status        = false; // by default, it fails
		}
		public String toString() {
			return "SplitAttribute: " + splitAttribute + "\n" +
					"splitValue: " + splitValue + "\n" +
					"splitIndex: " + splitIndex + "\n" +
					"leftSquaredError: " + leftSquaredError + "\n" +
					"rightSquaredError: " + rightSquaredError + "\n" +
					"status: " + status + "\n";
		}
	}
	
	/*
	 *  The following class is used to define a split in the regression tree method
	 */
	public class DataSplit {
		public ArrayList<ArrayList<Double>> leftInstances = new ArrayList<ArrayList<Double>>();
		public ArrayList<ArrayList<Double>> rightInstances = new ArrayList<ArrayList<Double>>();
		public ArrayList<Double> leftLabels = new ArrayList<Double>(); 
		public ArrayList<Double> rightLabels= new ArrayList<Double>(); 
		public double leftTerminalValue = 0.0;
		public double rightTerminalValue = 0.0;

		/*
		 *  Split data into the left node and the right node based on the best splitting point.
		 */
		 DataSplit (ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, BestSplit bestSplit, TerminalType terminalType) {
			for (int instanceNum = 0; instanceNum < labels_y.size(); instanceNum ++) {
				ArrayList<Double> ithInstance = instances_x.get(instanceNum);
				if (ithInstance.get(bestSplit.splitAttribute) < bestSplit.splitValue) {
					// append to the left instances
					leftInstances.add(ithInstance);
					leftLabels.add(labels_y.get(instanceNum));
				} else {
					// append to the right instances
					rightInstances.add(ithInstance);
					rightLabels.add(labels_y.get(instanceNum));
				}
			}
			
			// update terminal values
			if (terminalType == TerminalType.AVERAGE) {
				leftTerminalValue = 0.0;
				for (Double label : leftLabels) {
					leftTerminalValue += label;
				}
				leftTerminalValue /= leftLabels.size();
				
				rightTerminalValue = 0.0;
				for (Double label : rightLabels) {
					rightTerminalValue += label;
				}
				rightTerminalValue /= rightLabels.size();
			} else {
				System.out.println("Unknown terminal terminalType in DataSplit constructor");
				System.exit(0);
			}
		}
	}
	
	
	// Class used for sorting
	public class ListData {
		Double x;
		Double y;
		
		ListData(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public Double getX() {
			return x;
		}
		
		public Double getY() {
			return y;
		}
	}
	
	public class ListDataComparator implements Comparator<ListData> {
		public int compare(ListData arg0, ListData arg1) {
			return arg0.getX().compareTo(
					arg1.getX());
		}
	}
}
