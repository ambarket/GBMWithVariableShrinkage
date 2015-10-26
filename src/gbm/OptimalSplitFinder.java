package gbm;

import gbm.Attribute.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import utilities.DoubleCompare;
import utilities.SumCountAverage;

public class OptimalSplitFinder implements Callable<BestSplit> {
	FindOptimalSplitParameters parameters;
	int splitPredictorIndex;
	public OptimalSplitFinder(FindOptimalSplitParameters parameters, int splitPredictorIndex) {
		this.parameters = parameters;
		this.splitPredictorIndex = splitPredictorIndex;
	}
	
	@Override
	public BestSplit call() {
		BestSplit tmpSplit = null;
		if (parameters.dataset.predictorTypes[splitPredictorIndex] == Type.Numeric) {
			SplitSnapshot snapshot = new SplitSnapshot(parameters.squaredErrorBeforeSplit);
			int firstSplitIndex = initializeFirstNumericalSplit_ReturnFirstSplitIndex(parameters, snapshot, splitPredictorIndex);
			tmpSplit = findBestNumericalSplit(parameters, snapshot, splitPredictorIndex, firstSplitIndex);
		
		} else if (parameters.dataset.predictorTypes[splitPredictorIndex] == Type.Categorical) {
			HashMap<String, SumCountAverage> sumCountAverageByCategory = initializeSumCountAverageByCategory(parameters, splitPredictorIndex);
			tmpSplit = findBestCategoricalSplit(parameters, splitPredictorIndex, sumCountAverageByCategory);
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
	
	
	private static class CategoryAverageComparator implements Comparator<Map.Entry<String, SumCountAverage>> {
		@Override
		public int compare(Map.Entry<String, SumCountAverage> arg0, Map.Entry<String, SumCountAverage> arg1) {
			return DoubleCompare.compare(arg0.getValue().getMean(), arg1.getValue().getMean());
		}
	}
}
		
