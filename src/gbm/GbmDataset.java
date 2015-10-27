package gbm;

import java.util.HashMap;
import java.util.HashSet;

import gbm.GradientBoostingTree.ResultFunction;

/**
 * Wraps a Dataset object and adds an array for storing the current predictions and pseudo responses for each
 * instance in the dataset. Allows multiple gbm models to share a single dataset without duplicating data
 * @author ambar_000
 *
 */
public class GbmDataset {
	public double[] pseudoResponses;
	
	public double[] predictions;

	Dataset dataset;
	
	public GbmDataset(Dataset dataset) {
		this.dataset = dataset;
		this.predictions = new double[getNumberOfExamples()];
		this.pseudoResponses = new double[getNumberOfExamples()];
	}
	//-------------------------------Boosting Helper Methods-----------------------------
	public void initializePredictions(double initialValue) {
		predictions = new double[getNumberOfExamples()];
		for (int i = 0; i < getNumberOfExamples(); i++) {
			predictions[i] = initialValue;
		}
	}
	
	public void updatePredictionsWithLearnedValueFromNewTree(RegressionTree tree) {
		Attribute[][] instances = dataset.getInstances();
		for (int i = 0; i < getNumberOfExamples(); i++) {
			// Learning rate will be accounted for in getLearnedValue;
			predictions[i] += tree.getLearnedValue(instances[i]);
		}
	}

	public void updatePseudoResponses() {
		Attribute[] responses = dataset.getResponses();
		for (int i = 0; i < getNumberOfExamples(); i++) {
			pseudoResponses[i] = (responses[i].getNumericValue() - predictions[i]);
		}
	}
	
	public double calcMeanResponse() {
		return dataset.calcMeanResponse();
	}
	
	public double calcMeanResponse(boolean[] inSample) {
		return dataset.calcMeanResponse(inSample);
	}
	
	public double calcMeanPseudoResponse() {
		double meanY = 0.0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			meanY += pseudoResponses[i];
		}
		meanY = meanY / getNumberOfExamples();
		return meanY;
	}
	
	public double calcMeanPseudoResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			if (inSample[i]) {
				meanY += pseudoResponses[i];
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calculateRootMeanSquaredError(ResultFunction function) {
		Attribute[] responses = dataset.getResponses();
		Attribute[][] instances = dataset.getInstances();
		double rmse = 0.0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			double tmp = (function.getLearnedValue(instances[i]) - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= getNumberOfExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calculateRootMeanSquaredError() {
		Attribute[] responses = dataset.getResponses();
		double rmse = 0.0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			double tmp = (predictions[i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= getNumberOfExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calculateRootMeanSquaredError(boolean[] inSample) {
		Attribute[] responses = dataset.getResponses();
		double rmse = 0.0;
		double count = 0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			if (inSample[i]) {
				double tmp = (predictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calculateRootMeanSquaredError(boolean[] inSample, boolean negate) {
		if (!negate) {
			return calculateRootMeanSquaredError(inSample);
		}
		Attribute[] responses = dataset.getResponses();
		double rmse = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfExamples(); i++) {
			if (!inSample[i]) {
				double tmp = (predictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	//-----------------------------GETTERS--------------------------------------------------------
	public int getNumberOfExamples() {
		return dataset.getNumberOfExamples();
	}

	public int getNumberOfPredictors() {
		return dataset.getNumberOfPredictors();
	}

	public Attribute.Type[] getPredictorTypes() {
		return dataset.getPredictorTypes();
	}

	public String[] getPredictorNames() {
		return dataset.getPredictorNames();
	}

	public Attribute[][] getInstances() {
		return dataset.getInstances();
	}

	public Attribute.Type getResponseType() {
		return dataset.getResponseType();
	}

	public String getResponseName() {
		return dataset.getResponseName();
	}

	public Attribute[] getResponses() {
		return dataset.getResponses();
	}

	public int[][] getNumericalPredictorSortedIndexMap() {
		return dataset.getNumericalPredictorSortedIndexMap();
	}

	public HashMap<Integer, HashMap<String, HashSet<Integer>>> getCategoricalPredictorIndexMap() {
		return dataset.getCategoricalPredictorIndexMap();
	}
}
