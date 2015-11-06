package regressionTree;
import gbm.GbmDataset;
import gbm.GradientBoostingTree;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import regressionTree.OptimalSplitFinder.FindOptimalSplitParameters;
import utilities.DoubleCompare;
import utilities.Logger;
import utilities.StopWatch;

	/*
	 *  The following class is used to define a split in the regression tree method
	 */
	public class DataSplit {
		public TreeNode node;
		public boolean[] inLeftChild, inRightChild, inMissingChild;

		private DataSplit () {}

		/*
		 *  Split data into the left node and the right node based on the best splitting point.
		 */
		public static DataSplit splitDataIntoChildren(GbmDataset dataset, boolean[] inParent, int minExamplesInNode, double meanResponseInParent, double squaredErrorBeforeSplit) {
			DataSplit dataSplit = new DataSplit();
			
			StopWatch timer = (new StopWatch()).start();
			// Find the optimal attribute/value combination to perform the split.
			//BestSplit bestSplit = getOptimalSplitSingleThread(new FindOptimalSplitParameters(dataset, inParent, minExamplesInNode, squaredErrorBeforeSplit));
			BestSplit bestSplit = getOptimalSplit(new FindOptimalSplitParameters(dataset, inParent, minExamplesInNode, squaredErrorBeforeSplit));
			Logger.println(Logger.LEVELS.DEBUG, "\t\t\t Found optimal split " + timer.getElapsedSeconds());
			
			// If no split can be found then return null.
			if (!bestSplit.success) {
				return null;
			}
			
			// Build a new tree node based on the best split information
			dataSplit.node = new TreeNode(bestSplit, meanResponseInParent, minExamplesInNode);
			
			// map training data to the correct child
			int numOfExamples = dataset.getNumberOfTrainingExamples();
			dataSplit.inLeftChild = new boolean[numOfExamples];
			dataSplit.inRightChild = new boolean[numOfExamples];
			dataSplit.inMissingChild = new boolean[numOfExamples];
			//int leftC = 0, rightC = 0, missingC= 0;
			for (int instanceNum = 0; instanceNum < numOfExamples; instanceNum++) {
				if (inParent[instanceNum]) {
					switch (dataSplit.node.whichChild(dataset.getTrainingInstances()[instanceNum])) {
						case 1:
							dataSplit.inLeftChild[instanceNum] = true;
							//leftC++;
							break;
						case 2:
							dataSplit.inRightChild[instanceNum] = true;
							//rightC++;
							break;
						case 3:
							dataSplit.inMissingChild[instanceNum] = true;
							//missingC++;
							break;
						default:
							throw new IllegalStateException("Trrenode.whichChild returned an unexpected value to DataSplit.splitDataIntoChildren");
					}
				}
			}
			//System.out.println(leftC + " " + rightC + " " + missingC);
			//System.out.println(dataSplit.node.leftInstanceCount + " " + dataSplit.node.rightInstanceCount + " " + dataSplit.node.missingInstanceCount);
			return dataSplit;
		}
		
		protected static BestSplit getOptimalSplit(FindOptimalSplitParameters parameters) {
			BestSplit bestSplit = null, tmpSplit = null;
			
			ArrayList<Future<BestSplit>> splits = new ArrayList<Future<BestSplit>> ();
			// Find the best attribute to split on
			int numOfPredictors = parameters.dataset.getNumberOfPredictors();
			for (int splitPredictorIndex = 0; splitPredictorIndex < numOfPredictors; splitPredictorIndex ++) {
				splits.add(GradientBoostingTree.executor.submit(new OptimalSplitFinder(parameters, splitPredictorIndex)));
			}
			
			for (Future<BestSplit> split : splits) {
				try {
					tmpSplit = split.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				if (bestSplit == null || !bestSplit.success) {
					bestSplit = tmpSplit;
				} else if (tmpSplit != null && tmpSplit.success && 
						(DoubleCompare.lessThan(bestSplit.getErrorImprovement(), tmpSplit.getErrorImprovement()))) {
					bestSplit = tmpSplit;
				}
			}
			return bestSplit;
		}
		
		protected static BestSplit getOptimalSplitSingleThread(FindOptimalSplitParameters parameters) {
			BestSplit bestSplit = null, tmpSplit = null;

			// Find the best attribute to split on
			int numOfPredictors = parameters.dataset.getNumberOfPredictors();
			for (int splitPredictorIndex = 0; splitPredictorIndex < numOfPredictors; splitPredictorIndex ++) {
				tmpSplit = new OptimalSplitFinder(parameters, splitPredictorIndex).call();
				if (bestSplit == null || !bestSplit.success) {
					bestSplit = tmpSplit;
				} else if (tmpSplit != null && tmpSplit.success && 
						(DoubleCompare.lessThan(bestSplit.getErrorImprovement(), tmpSplit.getErrorImprovement()))) {
					bestSplit = tmpSplit;
				}
			}
			return bestSplit;
		}

		public String toString() {
			return node.toString();
		}
}