package gbm;

import java.util.HashMap;
import java.util.HashSet;

import dataset.Attribute;
import dataset.Dataset;
import regressionTree.RegressionTree;

/**
 * Wraps a Dataset object and adds an array for storing the current predictions and pseudo responses for each
 * instance in the dataset. Allows multiple gbm models to share a single dataset without duplicating data
 * @author ambar_000
 *
 */
public class GbmDataset {
	public double[] trainingPseudoResponses;
	public double[] trainingPredictions;
	public double[] testPseudoResponses;
	public double[] testPredictions;

	Dataset dataset;
	
	public GbmDataset(Dataset dataset) {
		this.dataset = dataset;
		this.trainingPredictions = new double[getNumberOfTrainingExamples()];
		this.trainingPseudoResponses = new double[getNumberOfTrainingExamples()];
		this.testPredictions = new double[getNumberOfTestExamples()];
		this.testPseudoResponses = new double[getNumberOfTestExamples()];
	}
	//-------------------------------Boosting Helper Methods-----------------------------
	public void initializePredictions(double initialValue) {
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			trainingPredictions[i] = initialValue;
		}
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			testPredictions[i] = initialValue;
		}
	}
	
	public void updatePredictionsWithLearnedValueFromNewTree(RegressionTree tree) {
		Attribute[][] trainingInstances = dataset.getTrainingInstances();
		Attribute[][] testInstances = dataset.getTestInstances();
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			// Learning rate will be accounted for in getLearnedValue;
			trainingPredictions[i] += tree.getLearnedValue(trainingInstances[i]);
		}
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			testPredictions[i] += tree.getLearnedValue(testInstances[i]);
		}
	}

	public void updatePseudoResponses() {
		Attribute[] trainingResponses = dataset.getTrainingResponses();
		Attribute[] testResponses = dataset.getTestResponses();
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			trainingPseudoResponses[i] = (trainingResponses[i].getNumericValue() - trainingPredictions[i]);
		}
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			testPseudoResponses[i] = (testResponses[i].getNumericValue() - testPredictions[i]);
		}
	}
	
	public double calcMeanTrainingResponse() {
		return dataset.calcMeanTrainingResponse();
	}
	
	public double calcMeanTrainingResponse(boolean[] inSample) {
		return dataset.calcMeanTrainingResponse(inSample);
	}
	
	public double calcMeanTestResponse() {
		return dataset.calcMeanTestResponse();
	}
	
	public double calcMeanTestResponse(boolean[] inSample) {
		return dataset.calcMeanTestResponse(inSample);
	}
	
	public double calcMeanTrainingPseudoResponse() {
		double meanY = 0.0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			meanY += trainingPseudoResponses[i];
		}
		meanY = meanY / getNumberOfTrainingExamples();
		return meanY;
	}
	
	public double calcMeanTrainingPseudoResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			if (inSample[i]) {
				meanY += trainingPseudoResponses[i];
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcMeanTestPseudoResponse() {
		double meanY = 0.0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			meanY += testPseudoResponses[i];
		}
		meanY = meanY / getNumberOfTrainingExamples();
		return meanY;
	}
	
	public double calcMeanTestPseudoResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			if (inSample[i]) {
				meanY += testPseudoResponses[i];
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcTrainingRMSE(ResultFunction function) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			double tmp = (function.getLearnedValue(getTrainingInstances()[i])- responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= getNumberOfTrainingExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE() {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			double tmp = (trainingPredictions[i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= getNumberOfTrainingExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE(boolean[] inSample) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		double count = 0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			if (inSample[i]) {
				double tmp = (trainingPredictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE(boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcTrainingRMSE(inSample);
		}
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfTrainingExamples(); i++) {
			if (!inSample[i]) {
				double tmp = (trainingPredictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE() {
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			double tmp = (testPredictions[i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= getNumberOfTestExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(boolean[] inSample) {
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		double count = 0;
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			if (inSample[i]) {
				double tmp = (testPredictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcTestRMSE(inSample);
		}
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		int count = 0;
		for (int i = 0; i < getNumberOfTestExamples(); i++) {
			if (!inSample[i]) {
				double tmp = (testPredictions[i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	//-----------------------------GETTERS--------------------------------------------------------
	public int getNumberOfTrainingExamples() {
		return dataset.getNumberOfTrainingExamples();
	}
	
	public int getNumberOfTestExamples() {
		return dataset.getNumberOfTestExamples();
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

	public Attribute[][] getTrainingInstances() {
		return dataset.getTrainingInstances();
	}
	
	public Attribute[][] getTestInstances() {
		return dataset.getTestInstances();
	}

	public Attribute.Type getResponseType() {
		return dataset.getResponseType();
	}

	public String getResponseName() {
		return dataset.getResponseName();
	}

	public Attribute[] getTrainingResponses() {
		return dataset.getTrainingResponses();
	}
	
	public Attribute[] getTestResponses() {
		return dataset.getTestResponses();
	}

	public int[][] getNumericalPredictorSortedIndexMap() {
		return dataset.getNumericalPredictorSortedIndexMap();
	}

	public HashMap<Integer, HashMap<String, HashSet<Integer>>> getCategoricalPredictorIndexMap() {
		return dataset.getCategoricalPredictorIndexMap();
	}
}
