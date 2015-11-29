package gbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import dataset.Attribute;
import dataset.Dataset;
import regressionTree.LearningRateTerminalValuePair;
import regressionTree.RegressionTree;
import utilities.SumCountAverage;

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
	
	// Should be interesting to see the average learning rate applied to each example in the dataset.
	public SumCountAverage[] avgTrainingLearningRates;
	public SumCountAverage[] avgTestLearningRates;
	
	public ArrayList<SumCountAverage> avgLearningRatesForEachTree;
	public ArrayList<SumCountAverage> avgExamplesInNodeForEachTree;

	Dataset dataset;
	
	public GbmDataset(Dataset dataset) {
		this.dataset = dataset;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		
		this.trainingPredictions = new double[numberOfTrainingExamples];
		this.trainingPseudoResponses = new double[numberOfTrainingExamples];
		this.testPredictions = new double[numberOfTestExamples];
		this.testPseudoResponses = new double[numberOfTestExamples];
		this.avgTrainingLearningRates = new SumCountAverage[numberOfTrainingExamples];
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			avgTrainingLearningRates[i] = new SumCountAverage();
		}
		this.avgTestLearningRates = new SumCountAverage[numberOfTestExamples];
		for (int i = 0; i < numberOfTestExamples; i++) {
			avgTestLearningRates[i] = new SumCountAverage();
		}
		avgLearningRatesForEachTree = new ArrayList<SumCountAverage>();
		avgExamplesInNodeForEachTree = new ArrayList<SumCountAverage>();
	}
	//-------------------------------Boosting Helper Methods-----------------------------
	public void initializePredictions(double initialValue) {
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			trainingPredictions[i] = initialValue;
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			testPredictions[i] = initialValue;
		}
	}
	
	public void updatePredictionsWithLearnedValueFromNewTree(RegressionTree tree) {
		SumCountAverage averageLRForThisTree = new SumCountAverage();
		Attribute[][] trainingInstances = dataset.getTrainingInstances();
		Attribute[][] testInstances = dataset.getTestInstances();
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			LearningRateTerminalValuePair pair = tree.getLearningRateTerminalValuePair(trainingInstances[i]);
			trainingPredictions[i] += pair.learningRate * pair.terminalValue;
			avgTrainingLearningRates[i].addData(pair.learningRate);
			averageLRForThisTree.addData(pair.learningRate);
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			LearningRateTerminalValuePair pair = tree.getLearningRateTerminalValuePair(testInstances[i]);
			testPredictions[i] += pair.learningRate * pair.terminalValue;
			avgTestLearningRates[i].addData(pair.learningRate);
			averageLRForThisTree.addData(pair.learningRate);
		}
		avgLearningRatesForEachTree.add(averageLRForThisTree);
		avgExamplesInNodeForEachTree.add(tree.getAverageNumberOfExamplesInNode());
	}

	public void updatePseudoResponses() {
		Attribute[] trainingResponses = dataset.getTrainingResponses();
		Attribute[] testResponses = dataset.getTestResponses();
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			trainingPseudoResponses[i] = (trainingResponses[i].getNumericValue() - trainingPredictions[i]);
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
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
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			meanY += trainingPseudoResponses[i];
		}
		meanY = meanY / numberOfTrainingExamples;
		return meanY;
	}
	
	public double calcMeanTrainingPseudoResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			if (inSample[i]) {
				meanY += trainingPseudoResponses[i];
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcTrainingRMSE(ResultFunction function) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			double tmp = (function.getLearnedValue(getTrainingInstances()[i])- responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= numberOfTrainingExamples;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE() {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			double tmp = (trainingPredictions[i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= numberOfTrainingExamples;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE(boolean[] inSample) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		double count = 0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
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
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
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
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			double tmp = (testPredictions[i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= numberOfTestExamples;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(boolean[] inSample) {
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		double count = 0;
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
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
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		double rmse = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTestExamples; i++) {
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

	public Map<Integer, HashMap<String, HashSet<Integer>>> getCategoricalPredictorIndexMap() {
		return dataset.getCategoricalPredictorIndexMap();
	}
	
	public int[] getShuffledIndicies() {
		return dataset.getShuffledIndicies();
	}
	
	public String getPerExamplePrintOut(double[] trainingPredictionsAtOptimalNumberOfTrees, double[] testPredictionsAtOptimalNumberOfTrees) {
		StringBuffer printOut = new StringBuffer();
		SumCountAverage avgOverallLearningRate = new SumCountAverage();
		int numberOfTrainingExamples = getNumberOfTrainingExamples();
		int numberOfTestExamples = getNumberOfTestExamples();
		int[] shuffledIndicies = getShuffledIndicies();
		
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			avgOverallLearningRate.addData(avgTrainingLearningRates[i].getMean());
			
		}
		
		for (int i = 0; i < numberOfTestExamples; i++) {
			avgOverallLearningRate.addData(avgTestLearningRates[i].getMean());
		}
		Attribute[] trainingResponses = dataset.getTrainingResponses();
		Attribute[] testResponses = dataset.getTestResponses();
		printOut.append("Training Data [OriginalFileLineNum\tTargetResponse\tPredictionAtOptimalNOT\tResidual\tAvgLearningRate(All Trees)]\n");
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			printOut.append(String.format("%d\t"
					+ "%.5f\t"
					+ "%.5f\t"
					+ "%.5f\t"
					+ "%.8f\n",
					shuffledIndicies[i], 
					trainingResponses[i].getNumericValue(),
					trainingPredictionsAtOptimalNumberOfTrees[i],
					trainingPredictionsAtOptimalNumberOfTrees[i] - trainingResponses[i].getNumericValue(),
					avgTrainingLearningRates[i].getMean()
				));
		}
		
		printOut.append("Test Data [OriginalFileLineNum\tTargetResponse\tPredictionAtOptimalNOT\tResidual\tAvgLearningRate(All Trees)]\n");
		for (int i = 0; i < numberOfTestExamples; i++) {
			printOut.append(String.format("%d\t"
					+ "%.5f\t"
					+ "%.5f\t"
					+ "%.5f\t"
					+ "%.8f\n",
					shuffledIndicies[i + numberOfTrainingExamples], 
					testResponses[i].getNumericValue(),
					testPredictionsAtOptimalNumberOfTrees[i],
					testPredictionsAtOptimalNumberOfTrees[i] - testResponses[i].getNumericValue(),
					avgTestLearningRates[i].getMean()
				));		}
		
		return String.format("Learning Rate Avg of Per Example Averages: %.8f\nLearning Rate Std Dev of Per Example Averages: %.8f\n", 
				avgOverallLearningRate.getMean(), avgOverallLearningRate.getRootMeanSquaredError()) + printOut.toString();
	}
}
