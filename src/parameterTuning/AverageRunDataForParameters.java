package parameterTuning;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import parameterTuning.RunDataSummaryRecord.RunFileType;
import utilities.DoubleCompare;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import utilities.SumCountAverage;
import dataset.DatasetParameters;

/**
 * ONLY works for multithreading on a single host. Locking isn't set up to support multiple hosts working on this.
 * @author ambar_000
 *
 */
public class AverageRunDataForParameters implements Callable<Void>{
	DatasetParameters datasetParams;
	GbmParameters parameters;
	String paramTuneDir;
	ParameterTuningParameters tuningParameters;
	int submissionNumber;
	StopWatch globalTimer;
	public AverageRunDataForParameters(DatasetParameters datasetParams, GbmParameters parameters, String paramTuneDir, ParameterTuningParameters tuningParameters, int submissionNumber, StopWatch globalTimer) {
		this.datasetParams = datasetParams;
		this.parameters = parameters;
		this.paramTuneDir = paramTuneDir;
		this.tuningParameters = tuningParameters;
		this.submissionNumber = submissionNumber;
		this.globalTimer = globalTimer;
	}

	public Void call() {
		StopWatch timer = new StopWatch().start();
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/Averages/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--averagesLock.txt")) {
			System.out.println(String.format("[%s] Already averaged runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}
		
		double timeInSeconds = 0, 
				cvTestError = 0, cvValidationError = 0, cvTrainingError = 0, 
				allDataTrainingError = 0, allDataTestError = 0,
				cvEnsembleTrainingError = 0, cvEnsembleTestError = 0,
				avgNumberOfSplits = 0, stdDevNumberOfSplits = 0,
				avgLearningRate = 0, stdDevLearningRate = 0,
				optimalNumberOfTrees = 0, totalNumberOfTrees= 0, stepSize = 0, numberOfFolds = 0;
		
		HashMap<String, SumCountAverage> cvEnsembleReltiveInfluences = new HashMap<>();
		HashMap<String, SumCountAverage> allDataFunctionReltiveInfluences = new HashMap<>();
		HashMap<Integer, PerExampleRunDataEntry> perExampleRunData = new HashMap<>();
		
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTestErrorByIteration = new ArrayList<Double>();
		
		ArrayList<Double> examplesInNodeMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> examplesInNodeStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> actualNumberOfSplitsByIteration = new ArrayList<Double>();

		
		ArrayList<Double> totalNumberOfTreesByRunNumber = new ArrayList<Double>();
		ArrayList<Double> optimalNumberOfTreesByRunNumber = new ArrayList<Double>();
		
		int numberOfTreesFound = 0;
		int numberOfRunsFound = 0;
		RunFileType runFileType = null;
		// Read through all the files cooresponding to these parameters and average the data.
		for (int runNumber = 0; runNumber < tuningParameters.NUMBER_OF_RUNS; runNumber++) {
			
			// Get the summary info at the top of the file.
			RunDataSummaryRecord summaryInfo = RunDataSummaryRecord.readRunDataSummaryRecordFromRunDataFile(paramTuneDir + "Run" + runNumber + "/", parameters, tuningParameters.runFileType);
			if (summaryInfo == null) {
				// Run data file wasn't found. Continue to next iteration.
				continue;
			} else {
				numberOfRunsFound++;
			}
			
			// Assume the runFileType of the first run. They could be mixed and basically want to revert to the most basic form.
			runFileType = (runFileType == null) ? summaryInfo.runFileType : runFileType; 

			// We have these attributes for all versions of the run files.
			stepSize += summaryInfo.stepSize;
			numberOfFolds += summaryInfo.numberOfFolds;
			totalNumberOfTrees += summaryInfo.totalNumberOfTrees;
			totalNumberOfTreesByRunNumber.add(summaryInfo.totalNumberOfTrees);
			optimalNumberOfTrees += summaryInfo.optimalNumberOfTrees;
			optimalNumberOfTreesByRunNumber.add(summaryInfo.optimalNumberOfTrees);
			cvValidationError += summaryInfo.cvValidationError;
			cvTrainingError += summaryInfo.cvTrainingError;
			allDataTrainingError += summaryInfo.allDataTrainingError;
			cvTestError += summaryInfo.cvTestError;
			allDataTestError += summaryInfo.allDataTestError;
			// Don't have timeInSeconds for the original runFiles
			if (runFileType != RunFileType.Original) {
				timeInSeconds += summaryInfo.timeInSeconds;
			}
			// Changed things in the middle of ParamTuning3
			if (runFileType == RunFileType.ParamTuning3) {
				avgNumberOfSplits += summaryInfo.avgNumberOfSplits;
			}
			if (runFileType == RunFileType.ParamTuning4) {
				cvEnsembleTrainingError += summaryInfo.cvEnsembleTrainingError;
				cvEnsembleTestError += summaryInfo.cvEnsembleTestError;
				avgNumberOfSplits += summaryInfo.avgNumberOfSplits;
				stdDevNumberOfSplits += summaryInfo.stdDevNumberOfSplits;
				avgLearningRate += summaryInfo.avgLearningRate;
				stdDevLearningRate += summaryInfo.stdDevLearningRate;
			}

			try {
				// Already know it exists based on successful creation of RunDataSummaryRecord
				String runDataFilePath = paramTuneDir + String.format("Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber) + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--runData.txt";
				String line = null;
				BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
				
				if (runFileType != RunFileType.ParamTuning4) {
					// Skip straight to per tree info because per example/relative influence data isn't available.
					while (!(line = br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
				} else {
					// skip summary info because we already read it in as RunDataSummaryRecord above.
					while (!(line = br.readLine()).startsWith("Training Data [OriginalFileLineNum"));
					
					// Average training data info
					while (!(line = br.readLine()).startsWith("Test Data [OriginalFileLineNum")) {
						String[] components = line.split("\t");
						int originalFileLineNumber = Integer.parseInt(components[0]);
						PerExampleRunDataEntry entry = perExampleRunData.get(originalFileLineNumber);
						
						if (entry == null) {
							entry = new PerExampleRunDataEntry(originalFileLineNumber, Double.parseDouble(components[1]));
							perExampleRunData.put(originalFileLineNumber, entry);
						}
						entry.predictionAtOptimalNOTAsTrainingData.addData(Double.parseDouble(components[2]));
						entry.residualAsTrainingData.addData(Double.parseDouble(components[3]));
						entry.avgLearningRateAsTrainingData.addData(Double.parseDouble(components[4]));
					}
					// Average test data info
					while (!(line = br.readLine()).startsWith("---------CV Ensemble Relative Influences")) {
						String[] components = line.split("\t");
						int originalFileLineNumber = Integer.parseInt(components[0]);
						PerExampleRunDataEntry entry = perExampleRunData.get(originalFileLineNumber);
						
						if (entry == null) {
							entry = new PerExampleRunDataEntry(originalFileLineNumber, Double.parseDouble(components[1]));
							perExampleRunData.put(originalFileLineNumber, entry);
						}
						entry.predictionAtOptimalNOTAsTestData.addData(Double.parseDouble(components[2]));
						entry.residualAsTestData.addData(Double.parseDouble(components[3]));
						entry.avgLearningRateAsTestData.addData(Double.parseDouble(components[4]));
					}
					// Average CV Ensemble Relative Influences
					while (!(line = br.readLine()).startsWith("---------All Data Function Relative Influences")) {
						String[] components = line.split(": ");
						SumCountAverage avg = cvEnsembleReltiveInfluences.get(components[0]);
						if (avg == null) {
							avg = new SumCountAverage();
							cvEnsembleReltiveInfluences.put(components[0], avg);
						}
						avg.addData(Double.parseDouble(components[1]));
					}
					// Average All Data Function Relative Influences
					while (!(line = br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError")) {
						String[] components = line.split(": ");
						SumCountAverage avg = allDataFunctionReltiveInfluences.get(components[0]);
						if (avg == null) {
							avg = new SumCountAverage();
							allDataFunctionReltiveInfluences.put(components[0], avg);
						}
						avg.addData(Double.parseDouble(components[1]));
					}
				}

				// Avg Per Tree RunData
				int index = 0;
				while ((line = br.readLine()) != null /*&& !line.isEmpty()*/) {
					String[] components = line.split("\t");
					if (numberOfTreesFound <= index) {
						avgCvTrainingErrorByIteration.add(Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.add(Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.add(Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.add(Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.add(Double.parseDouble(components[5].trim()));
						if (runFileType == RunFileType.ParamTuning4) {
							cvEnsembleTrainingErrorByIteration.add(Double.parseDouble(components[6].trim()));
							cvEnsembleTestErrorByIteration.add(Double.parseDouble(components[7].trim()));
							examplesInNodeMeanByIteration.add(Double.parseDouble(components[8].trim()));
							examplesInNodeStdDevByIteration.add(Double.parseDouble(components[9].trim()));
							learningRateMeanByIteration.add(Double.parseDouble(components[10].trim()));
							learningRateStdDevByIteration.add(Double.parseDouble(components[11].trim()));
							actualNumberOfSplitsByIteration.add(Double.parseDouble(components[12].trim()));
						}
						numberOfTreesFound++;
						index++;
					} else {
						avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) + Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index) + Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) + Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) + Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) + Double.parseDouble(components[5].trim()));
						if (runFileType == RunFileType.ParamTuning4) {
							cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) + Double.parseDouble(components[6].trim()));
							cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) + Double.parseDouble(components[7].trim()));
							examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) + Double.parseDouble(components[8].trim()));
							examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) + Double.parseDouble(components[9].trim()));
							learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) + Double.parseDouble(components[10].trim()));
							learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) + Double.parseDouble(components[11].trim()));
							actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) + Double.parseDouble(components[12].trim()));
						}
						index++;
					}
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} // End runNumber Loop
		if (numberOfRunsFound == 0) {
			SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--averagesLock.txt");
			System.out.println(String.format("[%s] No run data was found for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}
		// Compute the averages. Don't need to worry about runFileTypes b/c/ they will just be 0 if they aren't present.
		timeInSeconds /= numberOfRunsFound;
		stepSize  /= numberOfRunsFound;
		numberOfFolds  /= numberOfRunsFound;
		totalNumberOfTrees  /= numberOfRunsFound;
		optimalNumberOfTrees  /= numberOfRunsFound;
		cvValidationError  /= numberOfRunsFound;
		cvTrainingError  /= numberOfRunsFound;
		allDataTrainingError  /= numberOfRunsFound;
		cvTestError  /= numberOfRunsFound;
		allDataTestError  /= numberOfRunsFound;
		cvEnsembleTrainingError /= numberOfRunsFound;
		cvEnsembleTestError /= numberOfRunsFound;
		avgNumberOfSplits  /= numberOfRunsFound;
		stdDevNumberOfSplits /= numberOfRunsFound;
		avgLearningRate /= numberOfRunsFound;
		stdDevLearningRate /= numberOfRunsFound;
		
		double minNumberOfTreesAllRunsHave = Double.MAX_VALUE;
		for (double i : totalNumberOfTreesByRunNumber) {
			if (i < minNumberOfTreesAllRunsHave) { minNumberOfTreesAllRunsHave = i;}
		}
		for (int index = 0; index < numberOfTreesFound; index++) {
			if (index < minNumberOfTreesAllRunsHave) {
				avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) / numberOfRunsFound);
				avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index)  / numberOfRunsFound);
				avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) / numberOfRunsFound);
				allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) / numberOfRunsFound);
				allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) / numberOfRunsFound);
				if (runFileType == RunFileType.ParamTuning4) {
					cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) / numberOfRunsFound);
					cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) / numberOfRunsFound);
					examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) / numberOfRunsFound);
					examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) / numberOfRunsFound);
					learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) / numberOfRunsFound);
					learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) / numberOfRunsFound);
					actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) / numberOfRunsFound);
				}
			} else {
				int numberOfRunsWithThisTree = 0;
				for (double i : totalNumberOfTreesByRunNumber) {
					if (index < i) { numberOfRunsWithThisTree++; }
				}
				avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
				avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index)  / numberOfRunsWithThisTree);
				avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
				allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
				allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
				if (runFileType == RunFileType.ParamTuning4) {
					cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
					cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
					examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) / numberOfRunsWithThisTree);
					examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) / numberOfRunsWithThisTree);
					learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) / numberOfRunsWithThisTree);
					learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) / numberOfRunsWithThisTree);
					actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) / numberOfRunsWithThisTree);
				}
			}
		}
		
		// Save them to a new file to be processed later.
		try {
			String averageRunDataDirectory = paramTuneDir + "Averages/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
			new File(averageRunDataDirectory).mkdirs();
			BufferedWriter bw = new BufferedWriter(new PrintWriter(averageRunDataDirectory + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--runData.txt"));
			BufferedWriter perExampleRunDataWriter = new BufferedWriter(new PrintWriter(averageRunDataDirectory + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--averagePerExampleRunData.txt"));
			BufferedWriter relativeInfluencesWriter = new BufferedWriter(new PrintWriter(averageRunDataDirectory + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--averageRelativeInfluences.txt"));

			bw.write(String.format("Time In Seconds: %f \n"
					+ "Step Size: %.1f \n"
					+ "Number Of Folds: %.1f \n"
					+ "Avg TotalNumberOfTrees: %.2f \n"
					+ "Avg OptimalNumberOfTrees (ONOT): %.2f \n"
					+ "Avg CV Validation RMSE @ ONOT: %f \n" 
					+ "Avg CV Training RMSE @ ONOT: %f \n"
					+ "All Data Training RMSE @ ONOT: %f \n"
					+ "Avg CV Test RMSE @ ONOT: %f \n" 
					+ "All Data Test RMSE @ ONOT: %f \n" 
					+ "CV Ensemble Training RMSE @ ONOT: %f \n" 
					+ "CV Ensemble Test RMSE @ ONOT: %f\n"
					+ "All Data Avg Number Of Splits (All Trees): %f\n"
					+ "All Data Number Of Splits Std Dev (All Trees): %f\n"
					+ "Learning Rate Avg of Per Example Averages: %.8f\n"
					+ "Learning Rate Std Dev of Per Example Averages: %.8f\n"
					+ "Number of runs found: %d\n"
					+ "Total number of trees found: %d\n"
					+ "Number of trees found in each run: %s\n"
					+ "Optimal number of trees of each run: %s\n",
			timeInSeconds,
			stepSize,
			numberOfFolds,
			totalNumberOfTrees,
			optimalNumberOfTrees, 
			cvValidationError,
			cvTrainingError,
			allDataTrainingError,
			cvTestError,
			allDataTestError,
			cvEnsembleTrainingError,
			cvEnsembleTestError,
			avgNumberOfSplits,
			stdDevNumberOfSplits,
			avgLearningRate,
			stdDevLearningRate,
			numberOfRunsFound,
			numberOfTreesFound,
			convertDoubleArrayListToCommaSeparatedString(totalNumberOfTreesByRunNumber),
			convertDoubleArrayListToCommaSeparatedString(optimalNumberOfTreesByRunNumber)));

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
			
			if (runFileType == RunFileType.ParamTuning4) {
				for (int i = 0; i < numberOfTreesFound; i++) {
					bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.8f\t%.8f\t%.2f\n", 
							i+1,
							avgCvTrainingErrorByIteration.get(i),
							avgCvValidationErrorByIteration.get(i),
							avgCvTestErrorByIteration.get(i),
							allDataTrainingErrorByIteration.get(i),
							allDataTestErrorByIteration.get(i),
							cvEnsembleTrainingErrorByIteration.get(i),
							cvEnsembleTestErrorByIteration.get(i),
							examplesInNodeMeanByIteration.get(i),
							examplesInNodeStdDevByIteration.get(i),
							learningRateMeanByIteration.get(i),
							learningRateStdDevByIteration.get(i),
							actualNumberOfSplitsByIteration.get(i)));
				}
			} else {
				// THis is an old run data file, still want to print out 0's so that methods that process this data can assume
				//	something is present.
				for (int i = 0; i < numberOfTreesFound; i++) {
					bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.8f\t%.8f\t%d\n", 
							i+1,
							avgCvTrainingErrorByIteration.get(i),
							avgCvValidationErrorByIteration.get(i),
							avgCvTestErrorByIteration.get(i),
							allDataTrainingErrorByIteration.get(i),
							allDataTestErrorByIteration.get(i),
							0.0,
							0.0,
							0.0,
							0.0,
							0.0,
							0.0,
							0));
				}
			}
			
			bw.flush();
			bw.close();
			
			perExampleRunDataWriter.write("OriginalFileLineNum\t"
					+ "TargetResponse\t"
					+ "AsTrainingData_Count"
					+ "AsTrainingData_PredictionAtOptimalNOT\t"
					+ "AsTrainingData_Residual\t"
					+ "AsTrainingData_AvgLearningRate\t"
					+ "AsTestData_Count"
					+ "AsTestData_PredictionAtOptimalNOT\t"
					+ "AsTestData_Residual\t"
					+ "AsTestData_AvgLearningRate\n");
			for (int i = 0; i < perExampleRunData.size(); i++) {
				PerExampleRunDataEntry entry = perExampleRunData.get(i);
				if (entry == null) {
					System.out.println("WARNING: No per example data found for original file line number " + i + ". "
							+ "This is possible due to a bug that has since been fixed. "
							+ "Watch out and make sure this isn't happening on new data."
							+ "Breaking early because this per example run data is worthless due to that bug.");
					break;
				}
				perExampleRunDataWriter.write(String.format("%d\t%f\t%d\t%f\t%f\t%f\t%d\t%f\t%f\t%f\n",
						entry.originalFileLineNum,
						entry.targetResponse,
						entry.predictionAtOptimalNOTAsTrainingData.getCount(),
						entry.predictionAtOptimalNOTAsTrainingData.getMean(),
						entry.residualAsTrainingData.getMean(),
						entry.avgLearningRateAsTrainingData.getMean(),
						entry.predictionAtOptimalNOTAsTestData.getCount(),
						entry.predictionAtOptimalNOTAsTestData.getMean(),
						entry.residualAsTestData.getMean(),
						entry.avgLearningRateAsTestData.getMean()));
			}
			perExampleRunDataWriter.flush();
			perExampleRunDataWriter.close();
			
			relativeInfluencesWriter.write("---------CV Ensemble Relative Influences-----------\n");
			ArrayList<Map.Entry<String,SumCountAverage>> sortedCvRelativeInfluences = new ArrayList<>(cvEnsembleReltiveInfluences.entrySet());
			sortedCvRelativeInfluences.sort(new MapEntryDescendingSumCountAverageComparator());
			for (Map.Entry<String, SumCountAverage> relativeInfluence : sortedCvRelativeInfluences) {
				relativeInfluencesWriter.write(relativeInfluence.getKey() + ": " + relativeInfluence.getValue().getMean() + "\n");
			}
			
			relativeInfluencesWriter.write("\n---------All Data Function Relative Influences-----------\n");
			ArrayList<Map.Entry<String,SumCountAverage>> sortedAllDataRelativeInfluences = new ArrayList<>(allDataFunctionReltiveInfluences.entrySet());
			sortedAllDataRelativeInfluences.sort(new MapEntryDescendingSumCountAverageComparator());
			for (Map.Entry<String, SumCountAverage> relativeInfluence : sortedAllDataRelativeInfluences) {
				relativeInfluencesWriter.write(relativeInfluence.getKey() + ": " + relativeInfluence.getValue().getMean() + "\n");
			}
			relativeInfluencesWriter.flush();
			relativeInfluencesWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--averagesLock.txt");
		
		System.out.println(String.format("[%s] Successfully averaged run data for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
				datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
		return null;
	}
	
	private static String convertDoubleArrayListToCommaSeparatedString(ArrayList<Double> totalNumberOfTreesByRunNumber) {
		StringBuffer s = new StringBuffer();
		boolean first = true;
		for (double i : totalNumberOfTreesByRunNumber) {
			if (first) {
				s.append(i);
				first = false;
			} else {
				s.append(", " + i);
			}
			
		}
		return s.toString();
	}

	private static class PerExampleRunDataEntry {
		public int originalFileLineNum;
		public double targetResponse;
		public SumCountAverage predictionAtOptimalNOTAsTrainingData = new SumCountAverage();
		public SumCountAverage predictionAtOptimalNOTAsTestData = new SumCountAverage();
		public SumCountAverage residualAsTrainingData = new SumCountAverage();
		public SumCountAverage residualAsTestData = new SumCountAverage();
		public SumCountAverage avgLearningRateAsTrainingData = new SumCountAverage();
		public SumCountAverage avgLearningRateAsTestData = new SumCountAverage();
		
		public PerExampleRunDataEntry(int originalFileLineNum, double targetResponse) {
			this.originalFileLineNum = originalFileLineNum;
			this.targetResponse = targetResponse;
		}
	}
	
	
	private static class MapEntryDescendingSumCountAverageComparator implements Comparator<Map.Entry<String,SumCountAverage>> {
		@Override
		public int compare(Entry<String, SumCountAverage> arg0, Entry<String, SumCountAverage> arg1) {
			return DoubleCompare.compare(arg1.getValue().getMean(), arg0.getValue().getMean());
		}
	}
}
