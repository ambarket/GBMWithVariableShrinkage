package regressionTree;

import java.util.ArrayList;

import dataset.Attribute.Type;

/*
 *  The following class is used for split point in the regression tree
 */
public class BestSplit {
	public boolean success = false;
	public Type splitPredictorType = null;
	public int splitPredictorIndex = 0;
	public double numericSplitValue = 0;
	public ArrayList<String> leftCategories = null;
	public ArrayList<String> rightCategories = null;
	public double leftSquaredError = 0.0;
	public double rightSquaredError = 0.0;
	public double missingSquaredError = 0.0;
	public int leftInstanceCount = 0;
	public int rightInstanceCount = 0;
	public int missingInstanceCount = 0;
	public double leftMeanResponse = 0.0;
	public double rightMeanResponse = 0.0;
	public double missingMeanResponse = 0.0;
	
	public double squaredErrorBeforeSplit = 0.0;
	public BestSplit(double squaredErrorBeforeSplit) {
		this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
	}
	
	public void updateLeftAndRightCategories(ArrayList<String> left, ArrayList<String> right) {
		if (leftCategories == null) {leftCategories = new ArrayList<>();}
		if (rightCategories == null) {rightCategories = new ArrayList<>();}
		int i;
		for (i = 0; i < left.size(); i++) {
			if (i < leftCategories.size()) {
				leftCategories.set(i, left.get(i));
			} else {
				leftCategories.add(left.get(i));
			}
		}
		for (; i < leftCategories.size(); i++) {
			leftCategories.remove(i);
		}
		
		for (i = 0; i < right.size(); i++) {
			if (i < rightCategories.size()) {
				rightCategories.set(i, right.get(i));
			} else {
				rightCategories.add(right.get(i));
			}
		}
		for (; i < rightCategories.size(); i++) {
			rightCategories.remove(i);
		}
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
		return squaredErrorBeforeSplit - (leftSquaredError + rightSquaredError + missingSquaredError);
	}
}
