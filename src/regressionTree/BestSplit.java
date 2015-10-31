package regressionTree;

import java.util.HashSet;

import dataset.Attribute.Type;

/*
 *  The following class is used for split point in the regression tree
 */
public class BestSplit {
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
