package gbm;

import java.util.ArrayList;

import regressionTree.RegressionTree;
import dataset.Attribute;

/**
 * Exactly
 */
public class DummyResultFunction extends ResultFunction{
	
	// construction function
	DummyResultFunction(GbmParameters parameters, double intialValue, String[] predictorNames) {
		super(parameters, intialValue, predictorNames);
	}
	
	public void addTree(RegressionTree newTree, double trainingError, double validationError, double testError) {
		//Save space and time by not storing these trees, all error calculations have already been done 
		//	this.trees.add(newTree);
		this.trainingError.add(trainingError);
		this.validationError.add(validationError);
		this.testError.add(testError);
		this.sumOfSplits += newTree.actualNumberOfSplits;
	}
	
	public double getAvgNumberOfSplits() {
		return sumOfSplits / trainingError.size();
	}
	
	// the following function is used to estimate the function
	public double getLearnedValue(Attribute[] instance_x) {
		throw new UnsupportedOperationException("Can't call getLearnedValue on a DummyResultFunction");
	}
	
	public String getRelativeInfluencesString() {
		throw new UnsupportedOperationException("Can't call getRelativeInfluencesString on a DummyResultFunction");
	}
	
	public String getRelativeInfluencesString(int numberOfTrees) {
		throw new UnsupportedOperationException("Can't call getRelativeInfluencesString on a DummyResultFunction");
	}
	
	public double[] calcRelativeInfluences() {
		throw new UnsupportedOperationException("Can't call calcRelativeInfluences on a DummyResultFunction");
	}

	public double[] calcRelativeInfluences(int numberOfTrees) {
		throw new UnsupportedOperationException("Can't call calcRelativeInfluences on a DummyResultFunction");
	}
	
	public String getSummary() {
		return String.format("\nTotalNumberOfTrees: %d \n"
						+ "Training RMSE: %f \n"
						+ "Validation RMSE: %f \n"
						+ "Test RMSE: %f \n\n",
				trainingError.size(),
				trainingError.get(trainingError.size()-1),
				validationError.get(trainingError.size()-1),
				testError.get(trainingError.size()-1));
	}
}