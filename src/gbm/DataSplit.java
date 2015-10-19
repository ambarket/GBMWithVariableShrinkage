package gbm;
import java.util.ArrayList;

import utilities.DoubleCompare;
import utilities.Logger;
import utilities.StopWatch;

	/*
	 *  The following class is used to define a split in the regression tree method
	 */
	public class DataSplit {

		public TreeNode node = new TreeNode();
		
		public boolean[] inLeftChild, inRightChild;

		private DataSplit () {}

		/*
		 *  Split data into the left node and the right node based on the best splitting point.
		 */
		public static DataSplit splitDataIntoChildren(Dataset dataset, boolean[] inParent, int minObsInNode, double errorWithoutSplit, RegressionTree.TerminalType terminalType) {
			DataSplit dataSplit = new DataSplit();
			
			// Find the optimal attribute/value combination to perform the split. If no split can be found then return null.
			StopWatch timer = (new StopWatch()).start();
			BestSplit bestSplit = getOptimalSplit(dataset, inParent, minObsInNode, errorWithoutSplit);
			Logger.println(Logger.LEVELS.DEBUG, "\t\t\t Found optimal split " + timer.getElapsedSeconds());
			
			if (!bestSplit.success) {
				return null;
			} 
			
			dataSplit.node.squaredErrorBeforeSplit = errorWithoutSplit;
			dataSplit.node.splitAttribute = bestSplit.splitAttribute;
			dataSplit.node.splitValue = bestSplit.splitValue;
			dataSplit.node.leftSquaredError = bestSplit.leftSquaredError;
			dataSplit.node.rightSquaredError = bestSplit.rightSquaredError;
			dataSplit.node.leftInstanceCount = bestSplit.leftInstanceCount;
			dataSplit.node.rightInstanceCount = bestSplit.rightInstanceCount;
			
			// map training data to the correct child, can't really do this inside of getOptimalSplit because would
			//	have to copy the array each time the error is minimized.
			dataSplit.inLeftChild = new boolean[dataset.numOfExamples];
			dataSplit.inRightChild = new boolean[dataset.numOfExamples];
			for (int instanceNum = 0; instanceNum < dataset.numOfExamples; instanceNum++) {
				if (inParent[instanceNum]) {
					ArrayList<Double> instance = dataset.instances_x.get(instanceNum);
					if (instance.get(dataSplit.node.splitAttribute) < dataSplit.node.splitValue) {
						// append to the left instances
						dataSplit.inLeftChild[instanceNum] = true;
					} else {
						// append to the right instances
						dataSplit.inRightChild[instanceNum] = true;
					}
				}
			}

			// calculate terminal values for the new children
			if (terminalType == RegressionTree.TerminalType.AVERAGE) {
				dataSplit.node.leftTerminalValue = bestSplit.leftMeanResponse;
				dataSplit.node.rightTerminalValue = bestSplit.rightMeanResponse;
			} else {
				Logger.println(Logger.LEVELS.DEBUG, "Unknown terminal terminalType in DataSplit constructor");
				System.exit(0);
			}

			return dataSplit;
		}
		
		public String toString() {
			return node.toString();
		}
		 
		 /*
		  *  The following function gets the best split given the data
		  */
		protected static BestSplit getOptimalSplit(Dataset dataset, boolean[] inParent, int minObsInNode, double errorWithoutSplit) {
			BestSplit bestSplit = new BestSplit();
			double minimumError = errorWithoutSplit;
			
			// Find the best attribute to split on 
			for (int currentSplitAttribute = 0; currentSplitAttribute < dataset.numOfAttributes; currentSplitAttribute ++) {
				double currentLeftError = Double.MAX_VALUE;
				double currentRightError = Double.MAX_VALUE;
				double currentError = Double.MAX_VALUE;
				
				// Find left mean
				double 	sumLeft = 0.0, sumOfSquaresLeft = 0.0, meanLeft = 0.0, 
						sumRight = 0.0, sumOfSquaresRight = 0.0, meanRight = 0.0,
						currentY = 0.0;	
				int countLeft = 0, countRight = 0, sortedExampleIndex = 0;
				
				int lastSortedExampleIndexInLeft = -1;
				while(countLeft < minObsInNode - 1) {
					if (inParent[dataset.sortedAttributeIndices[currentSplitAttribute][sortedExampleIndex]]) {
						currentY = dataset.responses_y.get(dataset.sortedAttributeIndices[currentSplitAttribute][sortedExampleIndex]);
						sumLeft += currentY;
						sumOfSquaresLeft += currentY * currentY;
						countLeft++;
						lastSortedExampleIndexInLeft = sortedExampleIndex;
					}
					sortedExampleIndex++;
				}
				meanLeft = sumLeft / countLeft;
				
				while(sortedExampleIndex < dataset.numOfExamples) {
					if (inParent[dataset.sortedAttributeIndices[currentSplitAttribute][sortedExampleIndex]]) {
						currentY = dataset.responses_y.get(dataset.sortedAttributeIndices[currentSplitAttribute][sortedExampleIndex]);
						sumRight += currentY;
						sumOfSquaresRight += currentY * currentY;
						countRight++;
					}
					sortedExampleIndex++;
				}
				meanRight = sumRight / countRight;
				
							
				/* Find the best split. SplitIndex is the last example in the left child.
				 * Note: First split is Left = [0, minObsInNode) 					Right = [minObsInNode, numberOfExamples).
				 *        last split is Left = [0, numberOfExamples - minObsInNode) Right = [numberOfExamples - minObsInNode, numberOfExamples)
				 */
				sortedExampleIndex = lastSortedExampleIndexInLeft + 1;
				int nextSortedExampleIndex = -1;
				while(countRight > minObsInNode) {
					if (sortedExampleIndex >= dataset.numOfExamples) {
						throw new IllegalStateException("There must be less than 2 * minObsInNode examples in DataSplit.getOptimalSplit. Shouldn't be possible.");
					}
					int realExampleIndex = dataset.sortedAttributeIndices[currentSplitAttribute][sortedExampleIndex];
					nextSortedExampleIndex = sortedExampleIndex + 1;
					if (inParent[realExampleIndex]) {
						double y = dataset.responses_y.get(realExampleIndex);
						sumLeft += y;
						sumOfSquaresLeft += y*y;
						countLeft ++;
						meanLeft = sumLeft / countLeft;
						
						sumRight -= y;
						sumOfSquaresRight -= y*y;
						countRight --;
						meanRight = sumRight / countRight;
						
						// Find next sortedExampleIndex that maps to a real example in the parent.
						nextSortedExampleIndex = sortedExampleIndex + 1;
						int nextRealExampleIndex = dataset.sortedAttributeIndices[currentSplitAttribute][nextSortedExampleIndex];
						while (!inParent[nextRealExampleIndex]) {
							// Note: This should never throw indexOutOfBounds b/c there must always be >= minObsInNode examples that will fall into the right child.
							nextRealExampleIndex = dataset.sortedAttributeIndices[currentSplitAttribute][++nextSortedExampleIndex];
						}
						// We don't want to split if the two values are the same as it would lead to inconsistent results, need to move to the next split point. 
						if (DoubleCompare.equals(dataset.instances_x.get(realExampleIndex).get(currentSplitAttribute), dataset.instances_x.get(nextRealExampleIndex).get(currentSplitAttribute))) {
							sortedExampleIndex = nextSortedExampleIndex;
							continue;
						} 

						currentLeftError = sumOfSquaresLeft - (2 * meanLeft * sumLeft) + (countLeft * meanLeft * meanLeft);
						currentRightError = sumOfSquaresRight - (2 * meanRight * sumRight) + (countRight * meanRight * meanRight);
						currentError = currentLeftError + currentRightError;
						
						if (DoubleCompare.lessThan(currentError, minimumError)) {
							bestSplit.splitAttribute = currentSplitAttribute;
							bestSplit.splitValue = (dataset.instances_x.get(realExampleIndex).get(currentSplitAttribute) + dataset.instances_x.get(nextRealExampleIndex).get(currentSplitAttribute))/2;
							bestSplit.leftSquaredError = currentLeftError;
							bestSplit.rightSquaredError = currentRightError;
							bestSplit.leftInstanceCount = countLeft;
							bestSplit.rightInstanceCount = countRight;
							bestSplit.leftMeanResponse = meanLeft;
							bestSplit.rightMeanResponse = meanRight;
							bestSplit.success = true;
							minimumError = currentError;
						}
					}
					sortedExampleIndex = nextSortedExampleIndex;
				}
			}
			return bestSplit;
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
		public int leftInstanceCount;
		public int rightInstanceCount;
		public double leftMeanResponse;
		public double rightMeanResponse;
		
		// construction function
		BestSplit() {
			splitAttribute = 0;
			splitValue    = 0.0;
			leftSquaredError = Double.MAX_VALUE;
			rightSquaredError = Double.MAX_VALUE;
			leftInstanceCount = 0;
			rightInstanceCount = 0;
			leftMeanResponse = 0.0;
			rightMeanResponse = 0.0;
			success        = false; // by default, it fails
		}
		public String toString() {
			return "SplitAttribute: " + splitAttribute + "\n" +
					"splitValue: " + splitValue + "\n" +
					"leftInstanceCount: " + leftInstanceCount + "\n" +
					"leftMeanResponse: " + leftMeanResponse + "\n" +
					"leftSquaredError: " + leftSquaredError + "\n" +
					"rightInstanceCount: " + rightInstanceCount + "\n" +
					"rightMeanResponse: " + rightMeanResponse + "\n" +
					"rightSquaredError: " + rightSquaredError + "\n" +
					"success: " + success + "\n";
		}
	}
	
