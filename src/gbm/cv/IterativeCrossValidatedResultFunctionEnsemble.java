package gbm.cv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import dataset.Attribute;
import gbm.GbmParameters;
import gbm.MinimalistGbmDataset;
import parameterTuning.RunDataSummaryRecord.RunFileType;
import utilities.StopWatch;

public class IterativeCrossValidatedResultFunctionEnsemble {
	public String[] predictorNames;
	public GbmParameters parameters;
	public int stepSize, numOfFolds;
	
	public MinimalistGbmDataset[] cvGbmDatasets;
	public MinimalistGbmDataset allDataGbmDataset;
	
	// For calculating the optimal number of trees.
	public double minAvgValidationError;
	public ArrayList<Double> avgCvValidationErrors;
	public ArrayList<Double> avgCvTrainingErrors;
	public ArrayList<Double> avgCvTestErrors;
	
	// Implement De'ath (2007) aggregated boosted trees.
	public ArrayList<Double> cvEnsembleTrainingErrors;
	public ArrayList<Double> cvEnsembleTestErrors;
	public double[] lastCvEnsembleTrainingPredictions;
	public double[] lastCvEnsembleTestPredictions;
	
	// For prediction graphs
	public ArrayList<Double> allDataTrainingErrors;
	public ArrayList<Double> allDataTestErrors;
	public double[] allDataTrainingPredictionsAtONOT;
	public double[] allDataTestPredictionsAtONOT;
	
	// Relative Influences
	public double[] allDataRawPredictorInfluencesAtONOT;
	public double[] cvEnsembleRawPredictorInfluencesAtONOT;
	
	public int optimalNumberOfTrees, totalNumberOfTrees;
	public double timeInSeconds;
	
	public int numberOfTrainingExamples, numberOfTestExamples, numberOfPredictors;

	public ArrayList<Double> allDataAvgLearningRatesForEachTree;
	public ArrayList<Double> allDataAvgExamplesInNodeForEachTree;
	public ArrayList<Double> allDataStdDevLearningRatesForEachTree;
	public ArrayList<Double> allDataStdDevExamplesInNodeForEachTree;
	public ArrayList<Integer> allDataActualNumberOfSplitsForEachTree;
	
	public IterativeCrossValidatedResultFunctionEnsemble(GbmParameters parameters, CrossValidationStepper[] steppers) {
		
		this.predictorNames = steppers[0].dataset.getPredictorNames();
		this.parameters = parameters;
		this.stepSize = steppers[0].stepSize;
		this.numOfFolds = steppers.length-1;
		
		this.cvGbmDatasets = new MinimalistGbmDataset[steppers.length-1];
		for (int i = 0; i < steppers.length-1; i++) {
			this.cvGbmDatasets[i] = steppers[i].dataset;
		}
		this.allDataGbmDataset = steppers[steppers.length-1].dataset;

		minAvgValidationError = Double.MAX_VALUE;
		this.avgCvTrainingErrors = new ArrayList<>(parameters.numOfTrees);
		this.avgCvValidationErrors = new ArrayList<>(parameters.numOfTrees);
		this.avgCvTestErrors = new ArrayList<>(parameters.numOfTrees);
		
		this.numberOfTrainingExamples = allDataGbmDataset.getNumberOfTrainingExamples();
		this.numberOfTestExamples = allDataGbmDataset.getNumberOfTestExamples();
		this.numberOfPredictors = allDataGbmDataset.getNumberOfPredictors();
		
		// Implement De'ath (2007) aggregated boosted trees.
		this.cvEnsembleTrainingErrors = new ArrayList<>(parameters.numOfTrees);
		this.cvEnsembleTestErrors = new ArrayList<>(parameters.numOfTrees);
		this.lastCvEnsembleTrainingPredictions = new double[numberOfTrainingExamples];
		this.lastCvEnsembleTestPredictions = new double[numberOfTestExamples];
		
		// For prediction graphs
		this.allDataTrainingErrors = new ArrayList<>();
		this.allDataTestErrors = new ArrayList<>();
		this.allDataTrainingPredictionsAtONOT = new double[numberOfTrainingExamples];
		this.allDataTestPredictionsAtONOT = new double[numberOfTestExamples];
		
		// Relative Influences
		this.allDataRawPredictorInfluencesAtONOT = new double[numberOfPredictors];
		this.cvEnsembleRawPredictorInfluencesAtONOT = new double[numberOfPredictors];
		
		
		this.allDataAvgLearningRatesForEachTree = new ArrayList<>(parameters.numOfTrees);
		this.allDataAvgExamplesInNodeForEachTree = new ArrayList<>(parameters.numOfTrees);
		this.allDataStdDevLearningRatesForEachTree = new ArrayList<>(parameters.numOfTrees);
		this.allDataStdDevExamplesInNodeForEachTree = new ArrayList<>(parameters.numOfTrees);
		this.allDataActualNumberOfSplitsForEachTree = new ArrayList<>(parameters.numOfTrees);
	}
	
	public void finalizeEnsemble(StopWatch runTimer) {
		this.timeInSeconds = runTimer.getElapsedSeconds();
		this.totalNumberOfTrees = avgCvValidationErrors.size();
	}

	public void updateToReflectLastStepLastCvStop(int totalNumberOfTreesSoFar) {
		this.totalNumberOfTrees = totalNumberOfTreesSoFar;
		// Optimal number of trees is the point where average cross validation error is minimized.
		double minAvgValidationError = Double.MAX_VALUE;

		for (int i = 0; i < stepSize; i++) {
			double training = 0,validation = 0,test = 0;
			for (int functionIndex = 0; functionIndex < numOfFolds; functionIndex++) {
				training += cvGbmDatasets[functionIndex].trainingError[i];
				validation += cvGbmDatasets[functionIndex].validationError[i];
				test += cvGbmDatasets[functionIndex].testError[i];
			}
			training /= numOfFolds;
			validation /= numOfFolds;
			test /= numOfFolds;
			avgCvTrainingErrors.add(training);
			avgCvValidationErrors.add(validation);
			avgCvTestErrors.add(test);
			if (validation < minAvgValidationError) {
				minAvgValidationError = validation;
				this.optimalNumberOfTrees = totalNumberOfTrees - stepSize + i + 1;
			}
			
			allDataTrainingErrors.add(allDataGbmDataset.trainingError[i]);
			allDataTestErrors.add(allDataGbmDataset.testError[i]);
			allDataActualNumberOfSplitsForEachTree.add(allDataGbmDataset.actualNumberOfSplitsForEachTree[i]);
			allDataAvgLearningRatesForEachTree.add(allDataGbmDataset.avgLearningRatesForEachTree[i].getMean());
			allDataAvgExamplesInNodeForEachTree.add(allDataGbmDataset.avgExamplesInNodeForEachTree[i].getMean());
			allDataStdDevLearningRatesForEachTree.add(allDataGbmDataset.avgLearningRatesForEachTree[i].getRootMeanSquaredError());
			allDataStdDevExamplesInNodeForEachTree.add(allDataGbmDataset.avgExamplesInNodeForEachTree[i].getRootMeanSquaredError());
		}
		
		computeCvEnsembleErrorsForLastStep();
		
		if (this.optimalNumberOfTrees > (totalNumberOfTrees - stepSize)) {
			updateRawPredictorInfluencesAndAllDataPredictionsAtONOT(this.optimalNumberOfTrees - 1 - totalNumberOfTrees + stepSize);
		}
	}
	
	private void computeCvEnsembleErrorsForLastStep() {
		Attribute[] trainingResponses = allDataGbmDataset.getTrainingResponses();
		Attribute[] testResponses = allDataGbmDataset.getTestResponses();
		
		for (int treeNum = 0; treeNum < stepSize; treeNum++) {			
			// Update lastCVEnsembleTrainingPredictions
			for (int exampleNum = 0; exampleNum < numberOfTrainingExamples; exampleNum++) {
				lastCvEnsembleTrainingPredictions[exampleNum] = cvGbmDatasets[0].trainingPredictions[treeNum][exampleNum];
			}
			for (int foldNum = 1; foldNum < numOfFolds; foldNum++) {
				for (int exampleNum = 0; exampleNum < numberOfTrainingExamples; exampleNum++) {
					lastCvEnsembleTrainingPredictions[exampleNum] += cvGbmDatasets[foldNum].trainingPredictions[treeNum][exampleNum];
				}
			}
			for (int exampleNum = 0; exampleNum < numberOfTrainingExamples; exampleNum++) {
				lastCvEnsembleTrainingPredictions[exampleNum] /= numOfFolds;
			}
			
			// Update lastCVEnsembleTestPredictions
			for (int exampleNum = 0; exampleNum < numberOfTestExamples; exampleNum++) {
				lastCvEnsembleTestPredictions[exampleNum] = cvGbmDatasets[0].testPredictions[treeNum][exampleNum];
			}
			for (int foldNum = 1; foldNum < numOfFolds; foldNum++) {
				for (int exampleNum = 0; exampleNum < numberOfTestExamples; exampleNum++) {
					lastCvEnsembleTestPredictions[exampleNum] += cvGbmDatasets[foldNum].testPredictions[treeNum][exampleNum];
				}
			}
			for (int exampleNum = 0; exampleNum < numberOfTestExamples; exampleNum++) {
				lastCvEnsembleTestPredictions[exampleNum] /= numOfFolds;
			}
			
			// Compute training RMSE using TreeNum number of trees.
			double trainingError = 0.0;
			for (int i = 0; i < numberOfTrainingExamples; i++) {
				double tmp = (lastCvEnsembleTrainingPredictions[i] - trainingResponses[i].getNumericValue());
				trainingError += tmp * tmp;
			}
			trainingError /= numberOfTrainingExamples;
			trainingError = Math.sqrt(trainingError);
			
			// Compute test RMSE using TreeNum number of trees.
			double testError = 0.0;
			for (int i = 0; i < numberOfTestExamples; i++) {
				double tmp = (lastCvEnsembleTestPredictions[i] - testResponses[i].getNumericValue());
				testError += tmp * tmp;
			}
			testError /= numberOfTestExamples;
			testError = Math.sqrt(testError);
			
			// Add them to the running list of errors.
			cvEnsembleTrainingErrors.add(trainingError);
			cvEnsembleTestErrors.add(testError);
		}
	}
	
	private void updateRawPredictorInfluencesAndAllDataPredictionsAtONOT(int correctedONOTIndex) {
		for (int i = 0; i < numberOfPredictors; i++) {
			allDataRawPredictorInfluencesAtONOT[i] = allDataGbmDataset.rawPredictorInfluences[correctedONOTIndex][i];
			cvEnsembleRawPredictorInfluencesAtONOT[i] = cvGbmDatasets[0].rawPredictorInfluences[correctedONOTIndex][i];
			
		}

		for (int j = 1; j < numOfFolds; j++) {
			for (int i = 0; i < numberOfPredictors; i++) {
				cvEnsembleRawPredictorInfluencesAtONOT[i] += cvGbmDatasets[0].rawPredictorInfluences[correctedONOTIndex][i];
			}
		}
		
		for (int i = 0; i < numberOfPredictors; i++) {
			cvEnsembleRawPredictorInfluencesAtONOT[i] /= numOfFolds;
		}
		
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			allDataTrainingPredictionsAtONOT[i] = allDataGbmDataset.trainingPredictions[correctedONOTIndex][i];

		}
		for (int i = 0; i < numberOfTestExamples; i++) {
			allDataTestPredictionsAtONOT[i] = allDataGbmDataset.testPredictions[correctedONOTIndex][i];

		}
	}
	
	public String getSummary() {
		return String.format("Time In Seconds: %f \n"
						+ "Step Size: %d \n"
						+ "Number Of Folds: %d \n"
						+ "TotalNumberOfTrees: %d \n"
						+ "OptimalNumberOfTrees (ONOT): %d \n"
						+ "Avg CV Validation RMSE @ ONOT: %f \n" 
						+ "Avg CV Training RMSE @ ONOT: %f \n"
						+ "All Data Training RMSE @ ONOT: %f \n"
						+ "Avg CV Test RMSE @ ONOT: %f \n" 
						+ "All Data Test RMSE @ ONOT: %f \n" 
						+ "CV Ensemble Training RMSE @ ONOT: %f \n" 
						+ "CV Ensemble Test RMSE @ ONOT: %f\n"
						+ "All Data Avg Number Of Splits (All Trees): %f\n"
						+ "All Data Number Of Splits Std Dev (All Trees): %f\n",
				timeInSeconds,
				stepSize,
				numOfFolds,
				totalNumberOfTrees,
				optimalNumberOfTrees, 
				avgCvValidationErrors.get(optimalNumberOfTrees-1),
				avgCvTrainingErrors.get(optimalNumberOfTrees-1),
				allDataTrainingErrors.get(optimalNumberOfTrees-1),
				avgCvTestErrors.get(optimalNumberOfTrees-1),
				allDataTestErrors.get(optimalNumberOfTrees-1),
				cvEnsembleTrainingErrors.get(optimalNumberOfTrees-1),
				cvEnsembleTestErrors.get(optimalNumberOfTrees-1),
				allDataGbmDataset.avgNumberOfSplitsAcrossAllTrees.getMean(),
				allDataGbmDataset.avgNumberOfSplitsAcrossAllTrees.getRootMeanSquaredError());
	}
	

	
	public void saveRunDataToFile(String directory, RunFileType runFileType) throws IOException {
		new File(directory).mkdirs();
		String normalFileName = directory + parameters.getFileNamePrefix(runFileType) + "--runData.txt";
		BufferedWriter normal = new BufferedWriter(new PrintWriter(new File(normalFileName)));
		
		normal.write(getSummary());
		normal.write(allDataGbmDataset.getPerExamplePrintOut(allDataTrainingPredictionsAtONOT, allDataTestPredictionsAtONOT));
		normal.write(getRelativeInfluencesSummary());
		printPerTreeInformation(normal);
		normal.flush();
		normal.close();
	}
	
	public void printPerTreeInformation(BufferedWriter bw) {
		try {
			bw.write("TreeNumber\t"
					+ "AvgCvTrainingError\t"
					+ "AvgCvValidationError\t"
					+ "AvgCvTestError\t"
					+ "AllDataTrainingError\t"
					+ "AllDataTestError\t"
					+ "CvEnsembleTrainingError\t"
					+ "CvEnsembleTestError\t"
					+ "ExamplesInNodeMean\t"
					+ "ExamplesInNodeStdDev\t"
					+ "LearningRateMean\t"
					+ "LearningRateStdDev\t"
					+ "ActualNumberOfSplits\n");
			for (int i = 0; i < totalNumberOfTrees; i++) {
				bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.8f\t%.8f\t%d\n", 
						i+1,
						avgCvTrainingErrors.get(i),
						avgCvValidationErrors.get(i),
						avgCvTestErrors.get(i),
						allDataTrainingErrors.get(i),
						allDataTestErrors.get(i),
						cvEnsembleTrainingErrors.get(i),
						cvEnsembleTestErrors.get(i),
						allDataAvgExamplesInNodeForEachTree.get(i),
						allDataStdDevExamplesInNodeForEachTree.get(i),
						allDataAvgLearningRatesForEachTree.get(i),
						allDataStdDevLearningRatesForEachTree.get(i),
						allDataActualNumberOfSplitsForEachTree.get(i)));
			}
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//-----------------------------------RELATIVE INFLUENCES---------------------------------------------

	public String getRelativeInfluencesString(String header, double[] relativeInf) {
		StringBuffer s = new StringBuffer();
		s.append(header);
		PriorityQueue<Map.Entry<String, Double>> sortedRelativeInfluences = 
				new PriorityQueue<Map.Entry<String, Double>>(new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
						return Double.compare(arg1.getValue(), arg0.getValue());
					}
				});
		for (int i = 0; i < relativeInf.length; i++) {
			sortedRelativeInfluences.add(new SimpleEntry<String, Double>(predictorNames[i], relativeInf[i]));
		}
		while (!sortedRelativeInfluences.isEmpty()) {
			Map.Entry<String, Double> next = sortedRelativeInfluences.poll();
			s.append(next.getKey() + ": " + next.getValue() + "\n");
		}
		return s.toString();
	}

	public double[] convertRawInfluenceSumsIntoRelativeInfluences(double[] relativeInfluences) {
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] /= (optimalNumberOfTrees * cvGbmDatasets.length);
		}
		double sum = 0;
		for (int i = 0; i < relativeInfluences.length; i++) {
			sum += relativeInfluences[i];
		}
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] *= 100;
			relativeInfluences[i] /= sum;
		}
		return relativeInfluences;
	}
	
	
	public String getRelativeInfluencesSummary() {
		return 	getRelativeInfluencesString("---------CV Ensemble Relative Influences-----------\n", convertRawInfluenceSumsIntoRelativeInfluences(cvEnsembleRawPredictorInfluencesAtONOT)) +
				getRelativeInfluencesString("---------All Data Function Relative Influences-----------\n", convertRawInfluenceSumsIntoRelativeInfluences(allDataRawPredictorInfluencesAtONOT));
	}
}
