package gbm.cv;

import gbm.GbmDataset;
import gbm.GbmParameters;
import gbm.ResultFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import parameterTuning.RunDataSummaryRecord.RunFileType;
import regressionTree.RegressionTree;
import utilities.DoubleCompare;
import utilities.Matrix;
import utilities.StopWatch;
import dataset.Attribute;

public class CrossValidatedResultFunctionEnsemble {
	public GbmParameters parameters;
	public ResultFunction[] cvFunctions;
	public ResultFunction allDataFunction;
	public GbmDataset[] cvGbmDatasets;
	public GbmDataset allDataGbmDataset;
	public double avgInitialValue;
	public int optimalNumberOfTrees, totalNumberOfTrees;
	public int stepSize, numOfFolds;
	public double[] avgCvValidationErrors;
	public double[] avgCvTrainingErrors;
	public double[] avgCvTestErrors;
	public String[] predictorNames;
	public double timeInSeconds;
	
	// Implement De'ath (2007) aggregated boosted trees.
	public double[] cvEnsembleTrainingPredictions;
	public double[] cvEnsembleTestPredictions;
	public double[] cvEnsembleTrainingErrors;
	public double[] cvEnsembleTestErrors;
	
	public double[] trainingPredictionsAtOptimalNumberOfTrees;
	public double[] testPredictionsAtOptimalNumberOfTrees;
	public int numberOfTrainingExamples;
	public int numberOfTestExamples;
	
	public CrossValidatedResultFunctionEnsemble(GbmParameters parameters, CrossValidationStepper[] steppers, int totalNumberOfTrees, double timeInSeconds) {
		this.predictorNames = steppers[0].dataset.getPredictorNames();
		this.parameters = parameters;
		this.totalNumberOfTrees = totalNumberOfTrees;
		this.stepSize = steppers[0].stepSize;
		this.numOfFolds = steppers.length-1;
		this.timeInSeconds = timeInSeconds;
		
		this.cvFunctions = new ResultFunction[steppers.length-1];
		this.cvGbmDatasets = new GbmDataset[steppers.length-1];
		this.avgInitialValue = 0.0;
		for (int i = 0; i < steppers.length-1; i++) {
			cvFunctions[i] = steppers[i].function;
			cvGbmDatasets[i] = steppers[i].dataset;
			avgInitialValue += cvFunctions[i].initialValue;
		}
		allDataFunction = steppers[steppers.length-1].function;
		allDataGbmDataset = steppers[steppers.length-1].dataset;
		this.avgInitialValue /= cvFunctions.length;

		// Optimal number of trees is the point where average cross validation error is minimized.
		double minAvgValidationError = Double.MAX_VALUE;
		this.avgCvTrainingErrors = new double[totalNumberOfTrees];
		this.avgCvValidationErrors = new double[totalNumberOfTrees];
		this.avgCvTestErrors = new double[totalNumberOfTrees];
		for (int i = 0; i < totalNumberOfTrees; i++) {
			for (int functionIndex = 0; functionIndex < cvFunctions.length; functionIndex++) {
				avgCvTrainingErrors[i] += cvFunctions[functionIndex].trainingError.get(i);
				avgCvValidationErrors[i] += cvFunctions[functionIndex].validationError.get(i);
				avgCvTestErrors[i] += cvFunctions[functionIndex].testError.get(i);
			}
			if (DoubleCompare.lessThan(avgCvValidationErrors[i], minAvgValidationError)) {
				minAvgValidationError = avgCvValidationErrors[i];
				this.optimalNumberOfTrees = i+1;
			}
		}
		for (int i = 0; i < totalNumberOfTrees; i++) {
			avgCvTrainingErrors[i] /= cvFunctions.length;
			avgCvValidationErrors[i] /= cvFunctions.length;
			avgCvTestErrors[i] /= cvFunctions.length;
		}
	
		if (optimalNumberOfTrees == totalNumberOfTrees) {
			System.out.println(StopWatch.getDateTimeStamp() + "Warning: The optimal number was trees was equivalent to the number of trees grown. Consider running longer");
		}
		
		StopWatch timer = new StopWatch().start();
		// Compute the training and test predictions using the optimal number of trees so that they can be output.
		//	Could be interesting to graph these along side the most influencial predictors to visually see the fit.
		this.numberOfTrainingExamples = allDataGbmDataset.getNumberOfTrainingExamples();
		this.numberOfTestExamples = allDataGbmDataset.getNumberOfTestExamples();
		trainingPredictionsAtOptimalNumberOfTrees = new double[numberOfTrainingExamples];
		testPredictionsAtOptimalNumberOfTrees = new double[numberOfTestExamples];
		Attribute[][] trainingInstances = allDataGbmDataset.getTrainingInstances();
		Attribute[][] testInstances = allDataGbmDataset.getTestInstances();
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			trainingPredictionsAtOptimalNumberOfTrees[i] = allDataFunction.getLearnedValue(trainingInstances[i], optimalNumberOfTrees);	
		}
		for (int i = 0; i < numberOfTestExamples; i++) {
			testPredictionsAtOptimalNumberOfTrees[i] = allDataFunction.getLearnedValue(testInstances[i], optimalNumberOfTrees);	
		}
		System.out.println(StopWatch.getDateTimeStamp() + String.format("Took %.4f seconds to compute predictions at optimal number of trees", timer.getElapsedSeconds()));
		
		// Compute the CvEnsemble learning curve.
		timer.start();
		computeCvEnsembleLearningCurveData();
		System.out.println(StopWatch.getDateTimeStamp() + String.format("Took %.4f seconds to compute cv ensemble learning curve", timer.getElapsedSeconds()));
	}
	
	public void computeCvEnsembleLearningCurveData() {
		int numberOfTrainingExamples = allDataGbmDataset.getNumberOfTrainingExamples();
		int numberOfTestExamples = allDataGbmDataset.getNumberOfTestExamples();
		Attribute[] trainingResponses = allDataGbmDataset.getTrainingResponses();
		Attribute[] testResponses = allDataGbmDataset.getTestResponses();
		Attribute[][] trainingInstances = allDataGbmDataset.getTrainingInstances();
		Attribute[][] testInstances = allDataGbmDataset.getTestInstances();
		this.cvEnsembleTrainingPredictions = new double[numberOfTrainingExamples];
		this.cvEnsembleTestPredictions = new double[numberOfTestExamples];
		this.cvEnsembleTrainingErrors = new double[totalNumberOfTrees];
		this.cvEnsembleTestErrors = new double[totalNumberOfTrees];
				
		// Generate a learning curve for the cv ensemble (prediction = avg prediction of the cv models.
		// Need swap space to compute the average predictions at each number of trees.
		double[][] cvEnsemblePerFunctionTrainingPredictions = new double[numOfFolds][numberOfTrainingExamples];
		double[][] cvEnsemblePerFunctionTestPredictions = new double[numOfFolds][numberOfTestExamples];
		double[] trainingSwapSpace = new double[numberOfTrainingExamples];
		double[] testSwapSpace = new double[numberOfTestExamples];
		
		// Initialize all predictions to their initial values.
		for (int functionIndex = 0; functionIndex < numOfFolds; functionIndex++) {
			ResultFunction func = cvFunctions[functionIndex];
			for (int i = 0; i < numberOfTrainingExamples; i++) {
				cvEnsemblePerFunctionTrainingPredictions[functionIndex][i] = func.initialValue;
			}
			for (int i = 0; i < numberOfTestExamples; i++) {
				cvEnsemblePerFunctionTestPredictions[functionIndex][i] = func.initialValue;
			}
		}
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			cvEnsembleTrainingPredictions[i] = avgInitialValue;
			trainingSwapSpace[i] = 0;
		}
		for (int i = 0; i < numberOfTestExamples; i++) {
			cvEnsembleTestPredictions[i] = avgInitialValue;
			testSwapSpace[i] = 0;
		}
		// "Replay" each of the cv models tree by tree, average the running predictions at each step and add that
		//	average to the cv ensemble's prediction.
		for (int treeNum = 0; treeNum < totalNumberOfTrees; treeNum++) {
			// Move one tree forward in each of the cv models
			double nextPrediction = 0.0;
			for (int functionIndex = 0; functionIndex < numOfFolds; functionIndex++) {
				RegressionTree tree = cvFunctions[functionIndex].trees.get(treeNum);
				for (int i = 0; i < numberOfTrainingExamples; i++) {
					nextPrediction = tree.getLearnedValueWithLearningRateApplied(trainingInstances[i]);
					cvEnsemblePerFunctionTrainingPredictions[functionIndex][i] += nextPrediction;
					trainingSwapSpace[i] += nextPrediction;
				}
				for (int i = 0; i < numberOfTestExamples; i++) {
					nextPrediction = tree.getLearnedValueWithLearningRateApplied(testInstances[i]);
					cvEnsemblePerFunctionTestPredictions[functionIndex][i] += nextPrediction;
					testSwapSpace[i] += nextPrediction;
				}
			}
			// Add the average prediction to the cv ensemble's prediction arrays.
			for (int i = 0; i < numberOfTrainingExamples; i++) {
				cvEnsembleTrainingPredictions[i] += trainingSwapSpace[i] / cvFunctions.length;
				trainingSwapSpace[i] = 0;
			}
			for (int i = 0; i < numberOfTestExamples; i++) {
				cvEnsembleTestPredictions[i] += testSwapSpace[i] / cvFunctions.length;
				testSwapSpace[i] = 0;
			}
			
			// Compute training RMSE using TreeNum number of trees.
			cvEnsembleTrainingErrors[treeNum] = 0.0;
			for (int i = 0; i < numberOfTrainingExamples; i++) {
				double tmp = (cvEnsembleTrainingPredictions[i] - trainingResponses[i].getNumericValue());
				cvEnsembleTrainingErrors[treeNum] += tmp * tmp;
			}
			cvEnsembleTrainingErrors[treeNum] /= numberOfTrainingExamples;
			cvEnsembleTrainingErrors[treeNum] = Math.sqrt(cvEnsembleTrainingErrors[treeNum]);
			
			// Compute test RMSE using TreeNum number of trees.
			cvEnsembleTestErrors[treeNum] = 0.0;
			for (int i = 0; i < numberOfTestExamples; i++) {
				double tmp = (cvEnsembleTestPredictions[i] - testResponses[i].getNumericValue());
				cvEnsembleTestErrors[treeNum] += tmp * tmp;
			}
			cvEnsembleTestErrors[treeNum] /= numberOfTestExamples;
			cvEnsembleTestErrors[treeNum] = Math.sqrt(cvEnsembleTestErrors[treeNum]);
		}
	}
	
	public String getRelativeInfluencesString(String header) {
		return getRelativeInfluencesString(totalNumberOfTrees, header);
	}
	
	public String getRelativeInfluencesString(int numberOfTrees, String header) {
		double[] relativeInf = calcRelativeInfluences(numberOfTrees);
		StringBuffer s = new StringBuffer();
		s.append(header);
		PriorityQueue<Map.Entry<String, Double>> sortedRelativeInfluences = 
				new PriorityQueue<Map.Entry<String, Double>>(new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
						return DoubleCompare.compare(arg1.getValue(), arg0.getValue());
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
	
	public double[] calcRelativeInfluences() { 
		return calcRelativeInfluences(optimalNumberOfTrees);
	}

	public double[] calcRelativeInfluences(int numberOfTrees) {
		double[] relativeInfluences = new double[predictorNames.length];
		for (int functionIndex = 0; functionIndex < cvFunctions.length; functionIndex++) {
			Matrix.addToInPlace(relativeInfluences, cvFunctions[functionIndex].calcRelativeInfluences(numberOfTrees));
		}
		
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] /= (numberOfTrees * cvFunctions.length);
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
				avgCvValidationErrors[optimalNumberOfTrees-1],
				avgCvTrainingErrors[optimalNumberOfTrees-1],
				allDataFunction.trainingError.get(optimalNumberOfTrees-1),
				avgCvTestErrors[optimalNumberOfTrees-1],
				allDataFunction.testError.get(optimalNumberOfTrees-1),
				cvEnsembleTrainingErrors[optimalNumberOfTrees-1],
				cvEnsembleTestErrors[optimalNumberOfTrees-1],
				allDataFunction.numberOfSplits.getMean(),
				allDataFunction.numberOfSplits.getRootMeanSquaredError());
	}
	
	public String getRelativeInfluencesSummary() {
		return getRelativeInfluencesString(optimalNumberOfTrees, "---------CV Ensemble Relative Influences-----------\n")
		+ allDataFunction.getRelativeInfluencesString(optimalNumberOfTrees, "---------All Data Function Relative Influences-----------\n");
	}
	
	public void saveRunDataToFile(String directory, RunFileType runFileType) throws IOException {
		new File(directory).mkdirs();
		String normalFileName = directory + parameters.getFileNamePrefix(runFileType) + "--runData.txt";
		BufferedWriter normal = new BufferedWriter(new PrintWriter(new File(normalFileName)));
		
		normal.write(getSummary());
		normal.write(allDataGbmDataset.getPerExamplePrintOut(trainingPredictionsAtOptimalNumberOfTrees, testPredictionsAtOptimalNumberOfTrees));
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
						avgCvTrainingErrors[i],
						avgCvValidationErrors[i],
						avgCvTestErrors[i],
						allDataFunction.trainingError.get(i),
						allDataFunction.testError.get(i),
						cvEnsembleTrainingErrors[i],
						cvEnsembleTestErrors[i],
						allDataGbmDataset.avgExamplesInNodeForEachTree.get(i).getMean(),
						allDataGbmDataset.avgExamplesInNodeForEachTree.get(i).getRootMeanSquaredError(),
						allDataGbmDataset.avgLearningRatesForEachTree.get(i).getMean(),
						allDataGbmDataset.avgLearningRatesForEachTree.get(i).getRootMeanSquaredError(),
						allDataFunction.trees.get(i).actualNumberOfSplits));
			}
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		}
	}
}