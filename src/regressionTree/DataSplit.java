package regressionTree;
import gbm.GbmDataset;
import gbm.GradientBoostingTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dataset.Attribute;
import dataset.Attribute.Type;
import utilities.DoubleCompare;
import utilities.Logger;
import utilities.StopWatch;
import utilities.SumCountAverage;

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
			BestSplit bestSplit = getOptimalSplit(new FindOptimalSplitParameters(dataset, inParent, minExamplesInNode, squaredErrorBeforeSplit));
			Logger.println(Logger.LEVELS.DEBUG, "\t\t\t Found optimal split " + timer.getElapsedSeconds());
			
			// If no split can be found then return null.
			if (!bestSplit.success) {
				return null;
			}
			
			// Build a new tree node based on the best split information
			dataSplit.node = new TreeNode(bestSplit, meanResponseInParent);
			
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

		public String toString() {
			return node.toString();
		}
}
	

	class FindOptimalSplitParameters {
		public FindOptimalSplitParameters(GbmDataset dataset, boolean[] inParent, int minExamplesInNode, double squaredErrorBeforeSplit) {
			this.dataset = dataset;
			this.inParent = inParent;
			this.minExamplesInNode = minExamplesInNode;
			this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
		}
		public final GbmDataset dataset;
		public final boolean[] inParent;
		public final int minExamplesInNode;
		public final double squaredErrorBeforeSplit;
	}
	
	// Used only by getOptimalSplit to pass data around without a ton of arguments.
	class SplitSnapshot {
		public SumCountAverage left = new SumCountAverage(), right = new SumCountAverage();
		public double currentLeftError = Double.MAX_VALUE, currentRightError = Double.MAX_VALUE,
					currentTotalError = Double.MAX_VALUE, minimumTotalError = Double.MAX_VALUE;
		
		SplitSnapshot(double squaredErrorBeforeSplit) {
			this.minimumTotalError = squaredErrorBeforeSplit;
		}
		
		public void recomputeErrors() {
			currentLeftError = left.getSumOfSquares() - (left.getMean() * left.getSum());
			currentRightError = right.getSumOfSquares() - (right.getMean() * right.getSum());
			currentTotalError = currentLeftError + currentRightError;
		}
		
		/* TODO: What is this formula and why/how is it used in GBM. Do I need to use it?
		public void recomputeImprovement() {
			improvement = left.getCount() * right.getCount() * (left.getMean() - right.getMean()) * (left.getMean() - right.getMean()) / left.getCount() + right.getCount();
			double improvement2 = squaredErrorBeforeSplit - (currentTotalError);
			if (improvement2 == improvement) {
				System.out.println();
			}
		}
		*/
	}
	
	/*
	 *  The following class is used for split point in the regression tree
	 */
	class BestSplit {
		public boolean success = false;
		public Type splitPredictorType = null;
		public int splitPredictorIndex = 0;
		public double numericSplitValue = 0;
		public HashSet<String> leftCategories = null;
		public HashSet<String> rightCategories = null;
		public double leftSquaredError = 0.0;
		public double rightSquaredError = 0.0;
		public int leftInstanceCount = 0;
		public int rightInstanceCount = 0;
		public double leftMeanResponse = 0.0;
		public double rightMeanResponse = 0.0;
		
		public double squaredErrorBeforeSplit = 0.0;
		public BestSplit(double squaredErrorBeforeSplit) {
			this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
		}
		public String toString() {
			return "SplitAttribute: " + splitPredictorIndex + "\n" +
					"splitValue: " + numericSplitValue + "\n" +
					"leftInstanceCount: " + leftInstanceCount + "\n" +
					"leftMeanResponse: " + leftMeanResponse + "\n" +
					"leftSquaredError: " + leftSquaredError + "\n" +
					"rightInstanceCount: " + rightInstanceCount + "\n" +
					"rightMeanResponse: " + rightMeanResponse + "\n" +
					"rightSquaredError: " + rightSquaredError + "\n" +
					"success: " + success + "\n";
		}
		
		public double getErrorImprovement() {
			return squaredErrorBeforeSplit - (leftSquaredError + rightSquaredError);
		}
	}
	
