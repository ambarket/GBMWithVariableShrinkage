package gbm;
import gbm.Attribute.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

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
		public static DataSplit splitDataIntoChildren(Dataset dataset, boolean[] inParent, int minExamplesInNode, double meanResponseInParent, double squaredErrorBeforeSplit) {
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
			dataSplit.inLeftChild = new boolean[dataset.numberOfExamples];
			dataSplit.inRightChild = new boolean[dataset.numberOfExamples];
			dataSplit.inMissingChild = new boolean[dataset.numberOfExamples];
			int leftC = 0, rightC = 0, missingC= 0;
			for (int instanceNum = 0; instanceNum < dataset.numberOfExamples; instanceNum++) {
				if (inParent[instanceNum]) {
					switch (dataSplit.node.whichChild(dataset.instances[instanceNum])) {
						case 1:
							dataSplit.inLeftChild[instanceNum] = true;
							leftC++;
							break;
						case 2:
							dataSplit.inRightChild[instanceNum] = true;
							rightC++;
							break;
						case 3:
							dataSplit.inMissingChild[instanceNum] = true;
							missingC++;
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
			BestSplit bestSplit = null;

			BestSplit tmpSplit = null;
			// Find the best attribute to split on 
			for (int splitPredictorIndex = 0; splitPredictorIndex < parameters.dataset.numberOfPredictors; splitPredictorIndex ++) {
				
				if (parameters.dataset.predictorTypes[splitPredictorIndex] == Type.Numeric) {
					SplitSnapshot snapshot = new SplitSnapshot(parameters.squaredErrorBeforeSplit);
					int firstSplitIndex = initializeFirstNumericalSplit_ReturnFirstSplitIndex(parameters, snapshot, splitPredictorIndex);
					tmpSplit = findBestNumericalSplit(parameters, snapshot, splitPredictorIndex, firstSplitIndex);
				
				} else if (parameters.dataset.predictorTypes[splitPredictorIndex] == Type.Categorical) {
					HashMap<String, SumCountAverage> sumCountAverageByCategory = initializeSumCountAverageByCategory(parameters, splitPredictorIndex);
					tmpSplit = findBestCategoricalSplit(parameters, splitPredictorIndex, sumCountAverageByCategory);
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
		
		// Used only by getOptimalSplit to pass data around without a ton of arguments.
		private static class SplitSnapshot {
			public SumCountAverage left = new SumCountAverage(), right= new SumCountAverage();
			private double 	currentLeftError = Double.MAX_VALUE, currentRightError = Double.MAX_VALUE, 
					currentTotalError = Double.MAX_VALUE, minimumTotalError = Double.MAX_VALUE;
			public double improvement;
			double squaredErrorBeforeSplit;
			
			SplitSnapshot(double squaredErrorBeforeSplit) {
				this.minimumTotalError = squaredErrorBeforeSplit;
				this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
			}
			
			public void recomputeErrors() {
				currentLeftError = left.getSumOfSquares() - (left.getMean() * left.getSum());
				currentRightError = right.getSumOfSquares() - (right.getMean() * right.getSum());
				currentTotalError = currentLeftError + currentRightError;
			}
			
			// TODO: What is this formula and why/how is it used in GBM. Do I need to use it?
			public void recomputeImprovement() {
				improvement = left.getCount() * right.getCount() * (left.getMean() - right.getMean()) * (left.getMean() - right.getMean()) / left.getCount() + right.getCount();
				double improvement2 = squaredErrorBeforeSplit - (currentTotalError);
				if (improvement2 == improvement) {
					System.out.println();
				}
			}
		}

		private static class FindOptimalSplitParameters {
			public FindOptimalSplitParameters(Dataset dataset, boolean[] inParent, int minExamplesInNode, double squaredErrorBeforeSplit) {
				this.dataset = dataset;
				this.inParent = inParent;
				this.minExamplesInNode = minExamplesInNode;
				this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
			}
			public final Dataset dataset;
			public final boolean[] inParent;
			public final int minExamplesInNode;
			public final double squaredErrorBeforeSplit;
		}
		
		/**
		 * Loop through data and initialize sum, getSumOfSquares(), counts, and means of the left and right children in the first
		 *	possible split based upon snapshot.minNumberOfExampleInNode
		 * @param snapshot
		 * @return The first "lastLeftIndex" - the index into the numericalPredictorSortedIndexMap for the last example in
		 * 	the left child
		 */
		private static int initializeFirstNumericalSplit_ReturnFirstSplitIndex(FindOptimalSplitParameters parameters, SplitSnapshot snapshot, int splitPredictorIndex) {
			int sortedExampleIndex = 0;
			int lastSortedExampleIndexInLeft = -1;
			double currentY = 0.0;	
			while(snapshot.left.getCount() < parameters.minExamplesInNode) {
				if (parameters.inParent[parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex]]) {
					currentY = parameters.dataset.responses[parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex]].getPsuedoResponse();
					snapshot.left.addData(currentY);
					lastSortedExampleIndexInLeft = sortedExampleIndex;
				}
				sortedExampleIndex++;
			}
			
			while(sortedExampleIndex < parameters.dataset.numberOfExamples) {
				if (parameters.inParent[parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex]]) {
					currentY = parameters.dataset.responses[parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex]].getPsuedoResponse();
					snapshot.right.addData(currentY);
				}
				sortedExampleIndex++;
			}
			
			return lastSortedExampleIndexInLeft;
		}
		
		/**
		 * Iterate through all possible split points, store the information of the best split and return it.
		 * @param snapshot
		 * @param firstSplitIndex
		 * @return
		 */
		private static BestSplit findBestNumericalSplit(FindOptimalSplitParameters parameters, SplitSnapshot snapshot, int splitPredictorIndex, int firstSplitIndex) {
			BestSplit bestSplit = new BestSplit(parameters.squaredErrorBeforeSplit);

			int lastLeftIndex = firstSplitIndex + 1;
			int firstRightIndex = lastLeftIndex + 1;
			while(snapshot.right.getCount() > parameters.minExamplesInNode) {
				if (lastLeftIndex >= parameters.dataset.numberOfExamples) {
					throw new IllegalStateException("There must be less than 2 * minExamplesInNode "
							+ "examples in DataSplit.getOptimalSplit. Shouldn't be possible.");
				}
				int realLastLeftIndex = parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][lastLeftIndex];
				int realFirstRightIndex = parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][firstRightIndex];
				Attribute lastLeftAttribute = parameters.dataset.instances[realLastLeftIndex][splitPredictorIndex];
				Attribute firstRightAttribute = parameters.dataset.instances[realFirstRightIndex][splitPredictorIndex];
				if (parameters.inParent[realLastLeftIndex]) {
					double y = parameters.dataset.responses[realLastLeftIndex].getPsuedoResponse();
					snapshot.left.addData(y);
					snapshot.right.subtractData(y);
					
					// Find next sortedExampleIndex that maps to a real example in the parent, this will be the first example in the right child for this split.
					while (!parameters.inParent[realFirstRightIndex]) {
						// Note: This should never throw indexOutOfBounds b/c there must always be >= minExamplesInNode examples that will fall into the right child.
						realFirstRightIndex = parameters.dataset.numericalPredictorSortedIndexMap[splitPredictorIndex][++firstRightIndex];
						firstRightAttribute = parameters.dataset.instances[realFirstRightIndex][splitPredictorIndex];
					}
					// We don't want to split if the two values are the same as it would lead to inconsistent results,
					//	need to move to the next split point. If the values differ, evaluate the new error and update bestSplit if necessary
					if (lastLeftAttribute.compareTo(firstRightAttribute) != 0) {
						snapshot.recomputeErrors();
						if (DoubleCompare.lessThan(snapshot.currentTotalError, snapshot.minimumTotalError)) {
							bestSplit.splitPredictorType = Type.Numeric;
							bestSplit.splitPredictorIndex = splitPredictorIndex;
							bestSplit.numericSplitValue = (lastLeftAttribute.getNumericValue() + firstRightAttribute.getNumericValue())/2;
							bestSplit.leftSquaredError = snapshot.currentLeftError;
							bestSplit.rightSquaredError = snapshot.currentRightError;
							bestSplit.leftInstanceCount = snapshot.left.getCount();
							bestSplit.rightInstanceCount = snapshot.right.getCount();
							bestSplit.leftMeanResponse = snapshot.left.getMean();
							bestSplit.rightMeanResponse = snapshot.right.getMean();
							bestSplit.success = true;
							snapshot.minimumTotalError = snapshot.currentTotalError;
						}
					}
				}
				lastLeftIndex = firstRightIndex;
				firstRightIndex = lastLeftIndex + 1;
			}
			return bestSplit;
		}
		
		private static HashMap<String, SumCountAverage> initializeSumCountAverageByCategory(FindOptimalSplitParameters parameters, int splitPredictorIndex) {
			HashMap<String, HashSet<Integer>> categoryToExampleIndexMap = parameters.dataset.categoricalPredictorIndexMap.get(splitPredictorIndex);
			
			HashMap<String, SumCountAverage> SumCountAverageByCategory = new HashMap<String, SumCountAverage>();
			
			for (Entry<String, HashSet<Integer>> entry: categoryToExampleIndexMap.entrySet()) {
				SumCountAverage categoryData = new SumCountAverage();
				for (Integer exampleIndex : entry.getValue()) {
					if (parameters.inParent[exampleIndex]) {
						double psuedoResponse = parameters.dataset.responses[exampleIndex].getPsuedoResponse();
						categoryData.addData(psuedoResponse);
					}
				}
				SumCountAverageByCategory.put(entry.getKey(), categoryData);
			}
			return SumCountAverageByCategory;
		}
		
		private static class CategoryAverageComparator implements Comparator<Map.Entry<String, SumCountAverage>> {
			@Override
			public int compare(Map.Entry<String, SumCountAverage> arg0, Map.Entry<String, SumCountAverage> arg1) {
				return DoubleCompare.compare(arg0.getValue().getMean(), arg1.getValue().getMean());
			}
		}
		
		private static BestSplit findBestCategoricalSplit(FindOptimalSplitParameters parameters, int splitPredictorIndex, HashMap<String, SumCountAverage> sumCountAverageByCategory ) {
			BestSplit bestSplit = new BestSplit(parameters.squaredErrorBeforeSplit);
			SplitSnapshot snapshot = new SplitSnapshot(parameters.squaredErrorBeforeSplit);
			
			ArrayList<Map.Entry<String, SumCountAverage>> sortedEntries = new ArrayList<Map.Entry<String, SumCountAverage>>(sumCountAverageByCategory.entrySet());
			
			Collections.sort(sortedEntries, new CategoryAverageComparator());
			// Start with everything in the right child
			HashSet<String> leftCategories = new HashSet<String>();
			HashSet<String> rightCategories = new HashSet<String>();
			for (Map.Entry<String, SumCountAverage> entry : sortedEntries) {
				snapshot.right.addSumCountAverage(entry.getValue());
				rightCategories.add(entry.getKey());
			}
			// Move one category at a time into the left child, recompute errors, and keep the best split

			for (Map.Entry<String, SumCountAverage> entry : sortedEntries) {
				leftCategories.add(entry.getKey());
				rightCategories.remove(entry.getKey());
				snapshot.left.addSumCountAverage(entry.getValue());
				snapshot.right.subtractSumCountAverage(entry.getValue());
				
				if (snapshot.left.getCount() < parameters.minExamplesInNode || snapshot.right.getCount() < parameters.minExamplesInNode) {
					continue;
				}
				
				snapshot.recomputeErrors();
				
				if (DoubleCompare.lessThan(snapshot.currentTotalError, snapshot.minimumTotalError)) {
					bestSplit.splitPredictorType = Type.Categorical;
					bestSplit.splitPredictorIndex = splitPredictorIndex;
					bestSplit.leftCategories = new HashSet<String>(leftCategories);
					bestSplit.rightCategories = new HashSet<String>(rightCategories);
					bestSplit.leftSquaredError = snapshot.currentLeftError;
					bestSplit.rightSquaredError = snapshot.currentRightError;
					bestSplit.leftInstanceCount = snapshot.left.getCount();
					bestSplit.rightInstanceCount = snapshot.right.getCount();
					bestSplit.leftMeanResponse = snapshot.left.getMean();
					bestSplit.rightMeanResponse = snapshot.right.getMean();
					bestSplit.success = true;
					snapshot.minimumTotalError = snapshot.currentTotalError;
				}
			}
			return bestSplit;
		}
		
		public String toString() {
			return node.toString();
		}
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
	
