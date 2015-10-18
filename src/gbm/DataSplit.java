package gbm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import utilities.DoubleCompare;
import utilities.Logger;
import utilities.StopWatch;

	/*
	 *  The following class is used to define a split in the regression tree method
	 */
	public class DataSplit {
		public TreeNode node = new TreeNode();
		public ArrayList<ArrayList<Double>> leftInstances = new ArrayList<ArrayList<Double>>();
		public ArrayList<ArrayList<Double>> rightInstances = new ArrayList<ArrayList<Double>>();
		public ArrayList<Double> leftLabels = new ArrayList<Double>(); 
		public ArrayList<Double> rightLabels= new ArrayList<Double>(); 

		private DataSplit () {}

		/*
		 *  Split data into the left node and the right node based on the best splitting point.
		 */
		public static int calls = 0;
		public static DataSplit splitDataIntoChildren(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, int minObsInNode, double errorWithoutSplit, RegressionTree.TerminalType terminalType) {
			calls++;
			DataSplit dataSplit = new DataSplit();
			
			// Find the optimal attribute/value combination to perform the split. If no split can be found then return null.
			StopWatch timer = (new StopWatch()).start();
			BestSplit bestSplit = get_optimal_split(instances_x, labels_y, minObsInNode, errorWithoutSplit);
			Logger.println(Logger.LEVELS.DEBUG, "\t\t\t Found optimal split " + timer.getElapsedSeconds());
			
			if (!bestSplit.success) {
				return null;
			} 
			
			dataSplit.node.squaredErrorBeforeSplit = errorWithoutSplit;
			dataSplit.node.splitAttribute = bestSplit.splitAttribute;
			dataSplit.node.splitValue = bestSplit.splitValue;
			dataSplit.node.leftSquaredError = bestSplit.leftSquaredError;
			dataSplit.node.rightSquaredError = bestSplit.rightSquaredError;
				
			// map training data to the correct child
			for (int instanceNum = 0; instanceNum < labels_y.size(); instanceNum ++) {
				ArrayList<Double> ithInstance = instances_x.get(instanceNum);
				if (ithInstance.get(dataSplit.node.splitAttribute) < dataSplit.node.splitValue) {
					// append to the left instances
					dataSplit.leftInstances.add(ithInstance);
					dataSplit.leftLabels.add(labels_y.get(instanceNum));
				} else {
					// append to the right instances
					dataSplit.rightInstances.add(ithInstance);
					dataSplit.rightLabels.add(labels_y.get(instanceNum));
				}
			}

			// calculate terminal values for the new children
			if (terminalType == RegressionTree.TerminalType.AVERAGE) {
				dataSplit.node.leftTerminalValue = 0.0;
				for (Double label : dataSplit.leftLabels) {
					dataSplit.node.leftTerminalValue += label;
				}
				dataSplit.node.leftTerminalValue /= dataSplit.leftLabels.size();

				dataSplit.node.rightTerminalValue = 0.0;
				for (Double label : dataSplit.rightLabels) {
					dataSplit.node.rightTerminalValue += label;
				}
				dataSplit.node.rightTerminalValue /= dataSplit.rightLabels.size();
			} else {
				Logger.println(Logger.LEVELS.DEBUG, "Unknown terminal terminalType in DataSplit constructor");
				System.exit(0);
			}
			dataSplit.node.leftObsInNode = dataSplit.leftInstances.size();
			dataSplit.node.rightObsInNode = dataSplit.rightInstances.size();
			return dataSplit;
		}
		
		public String toString() {
			return node.toString();
		}
		 
		 /*
		 *  The following function gets the best split given the data
		 */
		protected static BestSplit get_optimal_split(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> labels_y, int minObsInNode, double errorWithoutSplit) {
		 
			
			BestSplit bestSplit = new BestSplit();
			
			int numberOfExamples = instances_x.size();
			int numberOfAttributes = instances_x.get(0).size();
			
			double minimumError = errorWithoutSplit;
			
			// Find the best attribute to split on 
			for (int currentSplitAttribute = 0; currentSplitAttribute < numberOfAttributes; currentSplitAttribute ++) {
				double currentLeftError = Double.MAX_VALUE;
				double currentRightError = Double.MAX_VALUE;
				double currentError = Double.MAX_VALUE;
				
				StopWatch timer = (new StopWatch()).start();
				// Sort data based on the value of currentSplitAttribute
				ArrayList<ListData> sortedExamples = new ArrayList<ListData>();
				for (int exampleIndex = 0; exampleIndex < numberOfExamples; exampleIndex ++) {	
					sortedExamples.add(new ListData(instances_x.get(exampleIndex).get(currentSplitAttribute), labels_y.get(exampleIndex)));
				}
				Collections.sort(sortedExamples, new ListDataComparator());
				Logger.println(Logger.LEVELS.DEBUG, "\t\t\t\t Sorted data in " + timer.getElapsedSeconds());
				
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
						bestSplit.splitAttribute = currentSplitAttribute;
						bestSplit.splitValue = (sortedExamples.get(splitIndex).getX() + sortedExamples.get(splitIndex + 1).getX())/2;
						bestSplit.leftSquaredError = currentLeftError;
						bestSplit.rightSquaredError = currentRightError;
						bestSplit.splitIndex = splitIndex;
						minimumError = currentError;
						bestSplit.success = true;
					}
				}
			}
			return bestSplit;
		}
	}
	
	// Class used for sorting
	class ListData {
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
	
	class ListDataComparator implements Comparator<ListData> {
		public int compare(ListData arg0, ListData arg1) {
			return arg0.getX().compareTo(
					arg1.getX());
		}
	}
	
	/*
	 *  The following class is used for split point in the regression tree
	 */
	class BestSplit {
		public int splitAttribute;
		public double splitValue;
		public double leftSquaredError;
		public double rightSquaredError;
		public boolean success;
		public int splitIndex;
		
		// construction function
		BestSplit() {
			splitAttribute = 0;
			splitValue    = 0.0;
			leftSquaredError = Double.MAX_VALUE;
			rightSquaredError = Double.MAX_VALUE;
			success        = false; // by default, it fails
		}
		public String toString() {
			return "SplitAttribute: " + splitAttribute + "\n" +
					"splitValue: " + splitValue + "\n" +
					"splitIndex: " + splitIndex + "\n" +
					"leftSquaredError: " + leftSquaredError + "\n" +
					"rightSquaredError: " + rightSquaredError + "\n" +
					"success: " + success + "\n";
		}
	}
	
