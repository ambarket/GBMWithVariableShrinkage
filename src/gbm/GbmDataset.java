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
	
	public double initialValue; 
	Dataset dataset;
	
	
	public double[] trainingPseudoResponses;
	public double[][] trainingPredictions;
	
	public double[] testPseudoResponses;
	public double[][] testPredictions;
	
	// Should be interesting to see the average learning rate applied to each example in the dataset.
	public SumCountAverage[] avgTrainingLearningRates;
	public SumCountAverage[] avgTestLearningRates;
	
	public ArrayList<SumCountAverage> avgLearningRatesForEachTree;
	public ArrayList<SumCountAverage> avgExamplesInNodeForEachTree;
	public ArrayList<Integer> actualNumberOfSplitsForEachTree;
	public SumCountAverage avgNumberOfSplitsAcrossAllTrees;
	
	public ArrayList<Double> trainingError;
	public ArrayList<Double> validationError;
	public ArrayList<Double> testError;

	// Needed to iteratively build the aggregated boosted tree, without requiring the actual trees to be stored. 
	//	Trying to save space.
	public int stepSize;
	
	public double[][] rawPredictorInfluences;
	
	protected GbmDataset() {};
	public GbmDataset(Dataset dataset, int stepSize) {
		this.dataset = dataset;
		this.stepSize = stepSize;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		int numberOfPredictors = dataset.getNumberOfPredictors();
		
		this.trainingPredictions = new double[stepSize][numberOfTrainingExamples];
		this.trainingPseudoResponses = new double[numberOfTrainingExamples];
		this.testPredictions = new double[stepSize][numberOfTestExamples];
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
		actualNumberOfSplitsForEachTree = new ArrayList<Integer>();
		
		avgNumberOfSplitsAcrossAllTrees = new SumCountAverage();
		
		trainingError = new ArrayList<>();
		validationError = new ArrayList<>();
		testError = new ArrayList<>();
		
		rawPredictorInfluences = new double[stepSize][numberOfPredictors];
	}
	
	//-------------------------------Boosting Helper Methods-----------------------------
	public void initializePredictions(double initialValue) {
		this.initialValue = initialValue;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			trainingPredictions[stepSize-1][i] = initialValue;
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			testPredictions[stepSize-1][i] = initialValue;
		}
	}
	
	public void updatePredictionsWithLearnedValueFromNewTree(RegressionTree tree, int iterationNumber) {
		
		SumCountAverage averageLRForThisTree = new SumCountAverage();
		Attribute[][] trainingInstances = dataset.getTrainingInstances();
		Attribute[][] testInstances = dataset.getTestInstances();
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			LearningRateTerminalValuePair pair = tree.getLearningRateTerminalValuePair(trainingInstances[i]);
			trainingPredictions[iterationNumber][i] = trainingPredictions[((iterationNumber-1 >= 0) ? iterationNumber-1 : stepSize-1)][i] + pair.learningRate * pair.terminalValue;
			avgTrainingLearningRates[i].addData(pair.learningRate);
			averageLRForThisTree.addData(pair.learningRate);
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			LearningRateTerminalValuePair pair = tree.getLearningRateTerminalValuePair(testInstances[i]);
			testPredictions[iterationNumber][i] = testPredictions[((iterationNumber-1 >= 0) ? iterationNumber-1 : stepSize-1)][i] + pair.learningRate * pair.terminalValue;
			avgTestLearningRates[i].addData(pair.learningRate);
			averageLRForThisTree.addData(pair.learningRate);
		}
		avgLearningRatesForEachTree.add(averageLRForThisTree);
	}


	/**
	 * For use when building a single boosted machine w/o cross validation
	 * @param iterationNumber
	 */
	public void updatePseudoResponses() {
		updatePseudoResponses(0);
	}
	
	/**
	 * For use with cross validation procedure for finding optimal number of trees/building aggregated boosted trees.
	 * Assumes iterationNumber is in range [0, stepSize]
	 * @param iterationNumber
	 */
	public void updatePseudoResponses(int iterationNumber) {
		int lastIndex = ((iterationNumber-1 >= 0) ? iterationNumber-1 : stepSize-1);
		Attribute[] trainingResponses = dataset.getTrainingResponses();
		Attribute[] testResponses = dataset.getTestResponses();
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			trainingPseudoResponses[i] = (trainingResponses[i].getNumericValue() - trainingPredictions[lastIndex][i]);
		}
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			testPseudoResponses[i] = (testResponses[i].getNumericValue() - testPredictions[lastIndex][i]);
		}
	}
	
	/**
	 * For use with the cross validation procedure. 
	 * 	If inSample[i] is true, example i in the training set is used as training, 
	 * 	otherwise example i is in the validation fold
	 * @param newTree
	 * @param inSample
	 * @param iterationNumber Assumed this will always be in range [0. stepSize) because called from CrossValidationStepper.
	 */
	public void updateAllMetadataBasedOnNewTree(RegressionTree newTree, boolean[] inSample, int iterationNumber) {
		updatePredictionsWithLearnedValueFromNewTree(newTree, iterationNumber);

		this.trainingError.add(calcTrainingRMSE(iterationNumber, inSample));
		this.validationError.add(calcTrainingRMSE(iterationNumber, inSample, true)); // SecondFlag indicates to negate the inSample array
		this.testError.add(calcTestRMSE(iterationNumber));
		
		this.actualNumberOfSplitsForEachTree.add(newTree.actualNumberOfSplits);
		
		this.avgNumberOfSplitsAcrossAllTrees.addData(newTree.actualNumberOfSplits);
		
		this.avgExamplesInNodeForEachTree.add(newTree.getAverageNumberOfExamplesInNode());
		
		int lastIndex = ((iterationNumber-1 >= 0) ? iterationNumber-1 : stepSize-1);
		int numberOfPredictors = rawPredictorInfluences[0].length;
		for (int i = 0; i < numberOfPredictors; i++) {
			rawPredictorInfluences[iterationNumber] = rawPredictorInfluences[lastIndex];
		}
		newTree.root.calcRelativeInfluences(rawPredictorInfluences[iterationNumber]);
	}
	
	/**
	 * For use when building just a single tree without cross validation.
	 * @param newTree
	 */
	public void updateAllMetadataBasedOnNewTree(RegressionTree newTree) {
		updatePredictionsWithLearnedValueFromNewTree(newTree, 0);

		this.trainingError.add(calcTrainingRMSE(0));
		this.validationError.add( Double.NaN);
		this.testError.add(calcTestRMSE(0));
		this.avgNumberOfSplitsAcrossAllTrees.addData(newTree.actualNumberOfSplits);
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
	
	public double calcTrainingRMSE(int iterationNumber) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			double tmp = (trainingPredictions[iterationNumber][i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= numberOfTrainingExamples;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE(int iterationNumber, boolean[] inSample) {
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		double count = 0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			if (inSample[i]) {
				double tmp = (trainingPredictions[iterationNumber][i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTrainingRMSE(int iterationNumber, boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcTrainingRMSE(iterationNumber, inSample);
		}
		Attribute[] responses = dataset.getTrainingResponses();
		double rmse = 0.0;
		int count = 0;
		int numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			if (!inSample[i]) {
				double tmp = (trainingPredictions[iterationNumber][i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(int iterationNumber) {
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			double tmp = (testPredictions[iterationNumber][i] - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= numberOfTestExamples;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(int iterationNumber, boolean[] inSample) {
		Attribute[] responses = dataset.getTestResponses();
		double rmse = 0.0;
		double count = 0;
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		for (int i = 0; i < numberOfTestExamples; i++) {
			if (inSample[i]) {
				double tmp = (testPredictions[iterationNumber][i] - responses[i].getNumericValue());
				rmse += tmp * tmp;
				count++;
			}
		}
		rmse /= count;
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public double calcTestRMSE(int iterationNumber, boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcTestRMSE(iterationNumber, inSample);
		}
		Attribute[] responses = dataset.getTestResponses();
		int numberOfTestExamples = dataset.getNumberOfTestExamples();
		double rmse = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTestExamples; i++) {
			if (!inSample[i]) {
				double tmp = (testPredictions[iterationNumber][i] - responses[i].getNumericValue());
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
