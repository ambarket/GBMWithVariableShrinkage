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
import java.util.concurrent.Future;

import utilities.DoubleCompare;
import utilities.SumCountAverage;
import dataset.Attribute;
import dataset.Attribute.Type;

public class OptimalSplitFinder implements Callable<BestSplit> {
	//-----------------------------------------Public Static Methods to getOptimalSplit------------------------------------------------
	public static class TreeNodeAndInParentPair {
		public TreeNode node;
		public boolean[] inParent;
	}
	
	public static TreeNodeAndInParentPair getOptimalSplit(GbmDataset dataset, int[] trainingDataToChildMap, int childNum, int minExamplesInNode, double meanResponseInParent, double squaredErrorBeforeSplit) {
		// Make a boolean array unique to this split to indicate what training examples were in the parent.
		TreeNodeAndInParentPair pair = new TreeNodeAndInParentPair();
		pair.inParent = new boolean[dataset.getNumberOfTrainingExamples()];
		for (int i = 0; i < trainingDataToChildMap.length; i++) {
			pair.inParent[i] = trainingDataToChildMap[i] == childNum;
		}
	
		// Find the best attribute to split on
		BestSplit bestSplit = null, tmpSplit = null;
		FindOptimalSplitParameters parameters = new FindOptimalSplitParameters(dataset, pair.inParent, minExamplesInNode, squaredErrorBeforeSplit);
		int numOfPredictors = parameters.dataset.getNumberOfPredictors();
		ArrayList<Future<BestSplit>> splits = new ArrayList<Future<BestSplit>> ();
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
		
		if (bestSplit == null || !bestSplit.success) {
			return null;
		}
		
		// Create a new tree node representing the best split.
		pair.node = new TreeNode(bestSplit, meanResponseInParent, minExamplesInNode);
		
		return pair;
	}
	
	public static TreeNodeAndInParentPair getOptimalSplitSingleThread(GbmDataset dataset, int[] trainingDataToChildMap, int childNum, int minExamplesInNode, double meanResponseInParent, double squaredErrorBeforeSplit) {
		// Make a boolean array unique to this split to indicate what training examples were in the parent.
		TreeNodeAndInParentPair pair = new TreeNodeAndInParentPair();
		pair.inParent = new boolean[dataset.getNumberOfTrainingExamples()];
		for (int i = 0; i < trainingDataToChildMap.length; i++) {
			pair.inParent[i] = trainingDataToChildMap[i] == childNum;
		}
	
		// Find the best attribute to split on
		BestSplit bestSplit = null, tmpSplit = null;
		FindOptimalSplitParameters parameters = new FindOptimalSplitParameters(dataset, pair.inParent, minExamplesInNode, squaredErrorBeforeSplit);
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
		
		if (bestSplit == null || !bestSplit.success) {
			return null;
		}
		
		// Create a new tree node representing the best split.
		pair.node = new TreeNode(bestSplit, meanResponseInParent, minExamplesInNode);
		
		return pair;
	}
	
	//------Actual class implementing Callable, private constructor so only way to use it is through the getOptimalSplit methods------------------------------------------------
	private FindOptimalSplitParameters parameters;
	private int splitPredictorIndex;
	private OptimalSplitFinder(FindOptimalSplitParameters parameters, int splitPredictorIndex) {
		this.parameters = parameters;
		this.splitPredictorIndex = splitPredictorIndex;
	}
	
	@Override
	public BestSplit call() {
		BestSplit tmpSplit = null;
		if (parameters.dataset.getPredictorTypes()[splitPredictorIndex] == Type.Numeric) {
			SplitSnapshot snapshot = new SplitSnapshot(parameters.squaredErrorBeforeSplit);
			int firstSplitIndex = initializeFirstNumericalSplit_ReturnFirstSplitIndex(parameters, snapshot, splitPredictorIndex);
			tmpSplit = findBestNumericalSplit(parameters, snapshot, splitPredictorIndex, firstSplitIndex);
		
		} else if (parameters.dataset.getPredictorTypes()[splitPredictorIndex] == Type.Categorical) {
			HashMap<String, SumCountAverage> sumCountAverageByCategory = initializeSumCountAverageByCategory(parameters, splitPredictorIndex);
			tmpSplit = findBestCategoricalSplit(parameters, splitPredictorIndex, sumCountAverageByCategory);
			sumCountAverageByCategory.clear();
		}
		
		return tmpSplit;
	}
	
	
	/**
	 * Loop through data and initialize sum, getSumOfSquares(), counts, and means of the left and right children in the first
	 *	possible split based upon snapshot.minNumberOfExampleInNode
	 * @param snapshot
	 * @return The first "lastLeftIndex" - the index into the numericalPredictorSortedIndexMap for the last example in
	 * 	the left child
	 */
	private static int initializeFirstNumericalSplit_ReturnFirstSplitIndex(FindOptimalSplitParameters parameters, SplitSnapshot snapshot, int splitPredictorIndex) {
		// Going to be accessing these a lot so just grab the pointer.
		int[][] numericalPredictorSortedIndexMap = parameters.dataset.getNumericalPredictorSortedIndexMap();
		
		Attribute[][] instances = parameters.dataset.getTrainingInstances();
		int realExampleIndex = -1;
		
		int sortedExampleIndex = 0;
		int lastSortedExampleIndexInLeft = -1;
		double currentY = 0.0;
		
		// Start with minimum in the left
		while(sortedExampleIndex < parameters.dataset.getNumberOfTrainingExamples() && 
				snapshot.left.getCount() < parameters.minExamplesInNode) {
			realExampleIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex];
			if (parameters.inParent[realExampleIndex]) {
				currentY = parameters.dataset.trainingPseudoResponses[realExampleIndex];
				if (instances[realExampleIndex][splitPredictorIndex].isMissingValue()) {
					break; // All remaining values will be missing too
				} else {
					snapshot.left.addData(currentY);
				}
				lastSortedExampleIndexInLeft = sortedExampleIndex;
			}
			sortedExampleIndex++;
		}

		// And everything else except missing values in right
		while(sortedExampleIndex < parameters.dataset.getNumberOfTrainingExamples()) {
			realExampleIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex];
			if (parameters.inParent[realExampleIndex]) {
				currentY = parameters.dataset.trainingPseudoResponses[realExampleIndex];
				if (instances[realExampleIndex][splitPredictorIndex].isMissingValue()) {
					break; // All remaining values will be missing too
				} else {
					snapshot.right.addData(currentY);
				}
			}
			sortedExampleIndex++;
		}
		
		// And all the missing values in their own node where they will stay.
		while(sortedExampleIndex < parameters.dataset.getNumberOfTrainingExamples()) {
			realExampleIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][sortedExampleIndex];
			if (parameters.inParent[realExampleIndex]) {
				currentY = parameters.dataset.trainingPseudoResponses[realExampleIndex];
				if (!instances[realExampleIndex][splitPredictorIndex].isMissingValue()) {
					throw new IllegalStateException("Made it to the missing value loop, yet its not a missing value. Logic is broken somewhere");
				} else {
					snapshot.missing.addData(currentY);
				}
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
		// Going to be accessing these a lot so just grab the pointer.
		int[][] numericalPredictorSortedIndexMap = parameters.dataset.getNumericalPredictorSortedIndexMap();
		Attribute[][] instances = parameters.dataset.getTrainingInstances();
		
		BestSplit bestSplit = new BestSplit(parameters.squaredErrorBeforeSplit);

		int lastLeftIndex = firstSplitIndex + 1;
		int firstRightIndex = lastLeftIndex + 1;
		while(snapshot.right.getCount() > parameters.minExamplesInNode) {
			if (lastLeftIndex >= parameters.dataset.getNumberOfTrainingExamples()) {
				throw new IllegalStateException("There must be less than 2 * minExamplesInNode "
						+ "examples in OptimalSplitFinder.findBestNumericalSplit. Shouldn't be possible.");
			}
			int realLastLeftIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][lastLeftIndex];
			int realFirstRightIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][firstRightIndex];
			Attribute lastLeftAttribute = instances[realLastLeftIndex][splitPredictorIndex];
			Attribute firstRightAttribute = instances[realFirstRightIndex][splitPredictorIndex];
			if (parameters.inParent[realLastLeftIndex]) {
				if (lastLeftAttribute.isMissingValue()) {
					return bestSplit; // All will be missing values after this so were done with possible split points.
				}
				double y = parameters.dataset.trainingPseudoResponses[realLastLeftIndex];
				snapshot.left.addData(y);
				snapshot.right.subtractData(y);
				
				// Find next sortedExampleIndex that maps to a real example in the parent, this will be the first example in the right child for this split.
				while (!parameters.inParent[realFirstRightIndex]) {
					// Note: This should never throw indexOutOfBounds b/c there must always be >= minExamplesInNode examples that will fall into the right child.
					realFirstRightIndex = numericalPredictorSortedIndexMap[splitPredictorIndex][++firstRightIndex];
					firstRightAttribute = instances[realFirstRightIndex][splitPredictorIndex];
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
						bestSplit.leftInstanceCount = snapshot.left.getCount();
						bestSplit.leftMeanResponse = snapshot.left.getMean();
						
						bestSplit.rightSquaredError = snapshot.currentRightError;
						bestSplit.rightInstanceCount = snapshot.right.getCount();
						bestSplit.rightMeanResponse = snapshot.right.getMean();
						
						// These actually won't change, could set them earlier.
						bestSplit.missingSquaredError = snapshot.currentMissingError;
						bestSplit.missingInstanceCount = snapshot.missing.getCount();
						bestSplit.missingMeanResponse = snapshot.missing.getMean();
						
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
		HashMap<String, HashSet<Integer>> categoryToExampleIndexMap = parameters.dataset.getCategoricalPredictorIndexMap().get(splitPredictorIndex);
		
		HashMap<String, SumCountAverage> SumCountAverageByCategory = new HashMap<String, SumCountAverage>();
		
		for (Entry<String, HashSet<Integer>> entry: categoryToExampleIndexMap.entrySet()) {
			SumCountAverage categoryData = new SumCountAverage();
			for (Integer exampleIndex : entry.getValue()) {
				if (parameters.inParent[exampleIndex]) {
					double pseudoResponse = parameters.dataset.trainingPseudoResponses[exampleIndex];
					categoryData.addData(pseudoResponse);
				}
			}
			SumCountAverageByCategory.put(entry.getKey(), categoryData);
		}
		return SumCountAverageByCategory;
	}
	
	private static BestSplit findBestCategoricalSplit(FindOptimalSplitParameters parameters, int splitPredictorIndex, HashMap<String, SumCountAverage> sumCountAverageByCategory ) {
		BestSplit bestSplit = new BestSplit(parameters.squaredErrorBeforeSplit);
		SplitSnapshot snapshot = new SplitSnapshot(parameters.squaredErrorBeforeSplit);
		
		// Associate the missing branch with all the missing values for this splitPredictorIndex. Also remove them
		//	so they won't be included in the sortedEntries used to compute the best left/right split below.
		snapshot.missing = sumCountAverageByCategory.remove(Attribute.MISSING_CATEGORY);
		
		ArrayList<Map.Entry<String, SumCountAverage>> sortedEntries = new ArrayList<Map.Entry<String, SumCountAverage>>(sumCountAverageByCategory.entrySet());
		
		Collections.sort(sortedEntries, new CategoryAverageComparator());
		// Start with everything in the right child
		ArrayList<String> leftCategories = new ArrayList<String>();
		ArrayList<String> rightCategories = new ArrayList<String>();
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
			entry.setValue(null);
			
			if (snapshot.left.getCount() < parameters.minExamplesInNode || snapshot.right.getCount() < parameters.minExamplesInNode) {
				continue;
			}
			
			snapshot.recomputeErrors();
			
			if (DoubleCompare.lessThan(snapshot.currentTotalError, snapshot.minimumTotalError)) {
				bestSplit.splitPredictorType = Type.Categorical;
				bestSplit.splitPredictorIndex = splitPredictorIndex;
				if (bestSplit.leftCategories != null) {bestSplit.leftCategories.clear();}
				if (bestSplit.rightCategories != null) {bestSplit.rightCategories.clear();}
				bestSplit.updateLeftAndRightCategories(leftCategories, rightCategories);
				
				bestSplit.leftSquaredError = snapshot.currentLeftError;
				bestSplit.leftInstanceCount = snapshot.left.getCount();
				bestSplit.leftMeanResponse = snapshot.left.getMean();
				
				bestSplit.rightSquaredError = snapshot.currentRightError;
				bestSplit.rightInstanceCount = snapshot.right.getCount();
				bestSplit.rightMeanResponse = snapshot.right.getMean();
				
				// These actually won't change, could set them earlier.
				bestSplit.missingSquaredError = snapshot.currentMissingError;
				bestSplit.missingInstanceCount = snapshot.missing.getCount();
				bestSplit.missingMeanResponse = snapshot.missing.getMean();
				
				bestSplit.success = true;
				snapshot.minimumTotalError = snapshot.currentTotalError;
			}
		}
		sortedEntries.clear();
		return bestSplit;
	}
	
	private static class CategoryAverageComparator implements Comparator<Map.Entry<String, SumCountAverage>> {
		@Override
		public int compare(Map.Entry<String, SumCountAverage> arg0, Map.Entry<String, SumCountAverage> arg1) {
			return DoubleCompare.compare(arg0.getValue().getMean(), arg1.getValue().getMean());
		}
	}

	// Used only by getOptimalSplit to pass data around without a ton of arguments.
	private static class SplitSnapshot {
		public SumCountAverage left = new SumCountAverage(), right = new SumCountAverage(), missing = new SumCountAverage();
		public double currentLeftError = Double.MAX_VALUE, currentRightError = Double.MAX_VALUE, currentMissingError = Double.MAX_VALUE,
					currentTotalError = Double.MAX_VALUE, minimumTotalError = Double.MAX_VALUE;
		
		SplitSnapshot(double squaredErrorBeforeSplit) {
			this.minimumTotalError = squaredErrorBeforeSplit;
		}
		
		public void recomputeErrors() {
			currentLeftError = left.getSumOfSquares() - (left.getMean() * left.getSum());
			currentRightError = right.getSumOfSquares() - (right.getMean() * right.getSum());
			currentMissingError = missing.getSumOfSquares() - (missing.getMean() * missing.getSum());
			currentTotalError = currentLeftError + currentRightError + currentMissingError;
		}
	}
	
	private static class FindOptimalSplitParameters {
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
}
		
