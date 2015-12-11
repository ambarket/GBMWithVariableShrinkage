package parameterTuning;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import dataset.DatasetParameters;
import gbm.GbmParameters;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.MaxAndMin;
import utilities.StopWatch;


public class RunDataSummaryRecord {
	// An unfortunate consequence of continuously wanting to add more data.
	public enum RunFileType {Original, ParamTuning2, ParamTuning3, ParamTuning4};
	
	public GbmParameters parameters;
	public double timeInSeconds;
	public double stepSize;
	public double numberOfFolds;
	public double totalNumberOfTrees;
	public double optimalNumberOfTrees;
	public double cvValidationError;
	public double cvTrainingError;
	public double allDataTrainingError;
	public double cvTestError;
	public double allDataTestError;
	public double cvEnsembleTrainingError;
	public double cvEnsembleTestError;
	public double avgExamplesInNode;
	public double stdDevExamplesInNode;
	public double avgNumberOfSplits;
	public double stdDevNumberOfSplits;
	public double avgLearningRate;
	public double stdDevLearningRate;
	
	public RunFileType runFileType; 
	
	
	// Average RunData only fields
	public double numberOfRunsRound;
	public double totalNumberOfTreesFound;
	public double[] numberOfTreesFoundInEachRun;
	public double[] optimalNumberOfTreesFoundinEachRun;
	public double totalNumberOfInteractionsAtONOT;
	public double minNumberOfTreesAllRunsHave;
	
	public static ArrayList<RunDataSummaryRecord> getAverageRecordsAcrossDatasets(ParameterTuningParameters tuningParameters, String runDataSubDirectory) {
		HashMap<GbmParameters, ArrayList<RunDataSummaryRecord>> recordsByParametersMap = new HashMap<>();
		
		for (DatasetParameters datasetParameters : tuningParameters.datasets) {
			String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
			ArrayList<RunDataSummaryRecord> allRecords = RunDataSummaryRecord.readRunDataSummaryRecords(runDataDirectory);

			// First need to 0-1 normalize the comparison attributes 
			MaxAndMin error = new MaxAndMin();
			//MaxAndMin cvError = new MaxAndMin();
			//MaxAndMin atdError = new MaxAndMin();
			//MaxAndMin abtError = new MaxAndMin();
			MaxAndMin time = new MaxAndMin();
			MaxAndMin optTrees = new MaxAndMin();
			for (RunDataSummaryRecord record : allRecords) {
				error.updateMaxAndMinIfNecessary(record.cvValidationError);
				error.updateMaxAndMinIfNecessary(record.allDataTestError);
				error.updateMaxAndMinIfNecessary(record.cvEnsembleTestError);
				//cvError.updateMaxAndMinIfNecessary(record.cvValidationError);
				//atdError.updateMaxAndMinIfNecessary(record.allDataTestError);
				//abtError.updateMaxAndMinIfNecessary(record.cvEnsembleTestError);
				time.updateMaxAndMinIfNecessary(record.timeInSeconds);
				optTrees.updateMaxAndMinIfNecessary(record.optimalNumberOfTrees);
			}
			for (RunDataSummaryRecord record : allRecords) {
				//record.cvValidationError = (record.cvValidationError - cvError.min) / (cvError.max - cvError.min);
				//record.allDataTestError = (record.cvValidationError - atdError.min) / (atdError.max - atdError.min);
				//record.cvEnsembleTestError = (record.cvValidationError - abtError.min) / (abtError.max - abtError.min);
				record.cvValidationError = (record.cvValidationError - error.min) / (error.max - error.min);
				record.allDataTestError = (record.allDataTestError - error.min) / (error.max - error.min);
				record.cvEnsembleTestError = (record.cvEnsembleTestError - error.min) / (error.max - error.min);
				record.timeInSeconds = (record.timeInSeconds - time.min) / (time.max - time.min);
				record.optimalNumberOfTrees = (record.optimalNumberOfTrees - optTrees.min) / (optTrees.max - optTrees.min);
				if (!recordsByParametersMap.containsKey(record.parameters)) {
					recordsByParametersMap.put(record.parameters, new ArrayList<>());
				}
				recordsByParametersMap.get(record.parameters).add(record);
			}
		}
		
		ArrayList<RunDataSummaryRecord> retval = new ArrayList<RunDataSummaryRecord>();
		for (Map.Entry<GbmParameters, ArrayList<RunDataSummaryRecord>> entry : recordsByParametersMap.entrySet()) {
			retval.add(averageRecords(entry.getValue()));
		}
		return retval;
	}
	
	
	public static RunDataSummaryRecord averageRecords(List<RunDataSummaryRecord> records) {
		RunDataSummaryRecord retval = new RunDataSummaryRecord();
		for (RunDataSummaryRecord record : records) {
			retval.timeInSeconds += record.timeInSeconds;
			retval.totalNumberOfTrees += record.totalNumberOfTrees;
			retval.optimalNumberOfTrees += record.optimalNumberOfTrees;
			retval.cvValidationError += record.cvValidationError;
			retval.cvTrainingError += record.cvTrainingError;
			retval.allDataTrainingError += record.allDataTrainingError;
			retval.cvTestError += record.cvTestError;
			retval.allDataTestError += record.allDataTestError;
			retval.cvEnsembleTrainingError += record.cvEnsembleTrainingError;
			retval.cvEnsembleTestError += record.cvEnsembleTestError;
			retval.avgExamplesInNode += record.avgExamplesInNode;
			retval.stdDevExamplesInNode += record.stdDevExamplesInNode;
			retval.avgNumberOfSplits += record.avgNumberOfSplits;
			retval.stdDevNumberOfSplits += record.stdDevNumberOfSplits;
			retval.avgLearningRate += record.avgLearningRate;
			retval.stdDevLearningRate += record.stdDevLearningRate;
		}
		
		retval.timeInSeconds /= records.size();
		retval.totalNumberOfTrees  /= records.size();
		retval.optimalNumberOfTrees  /= records.size();
		retval.cvValidationError  /= records.size();
		retval.cvTrainingError  /= records.size();
		retval.allDataTrainingError  /= records.size();
		retval.cvTestError  /= records.size();
		retval.allDataTestError  /= records.size();
		retval.cvEnsembleTrainingError  /= records.size();
		retval.cvEnsembleTestError  /= records.size();
		retval.avgExamplesInNode  /= records.size();
		retval.stdDevExamplesInNode  /= records.size();
		retval.avgNumberOfSplits  /= records.size();
		retval.stdDevNumberOfSplits /= records.size();
		retval.avgLearningRate  /= records.size();
		retval.stdDevLearningRate /= records.size();
		
		retval.parameters = records.get(0).parameters;
		return retval;
	}
	public void inferTimeInSecondsFromPartialRun(int numberOfTreesInPartialRun, double timeInSeconds) {
		this.timeInSeconds = (timeInSeconds / numberOfTreesInPartialRun) * totalNumberOfTrees;
	}

	public static class CvEnsembleErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.cvEnsembleTestError, o2.cvEnsembleTestError);
		}
	}
	
	public static class CvValidationErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.cvValidationError, o2.cvValidationError);
		}
	}
	
	public static class CvTestErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.cvTestError, o2.cvTestError);
		}
	}
	
	public static class TimeInSecondsComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.timeInSeconds, o2.timeInSeconds);
		}
	}
	
	public static class AllDataTestErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.allDataTestError, o2.allDataTestError);
		}
	}
	
	public static class AllDataTrainingErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return Double.compare(o1.allDataTrainingError, o2.allDataTrainingError);
		}
	}

	public static void saveRunDataSummaryRecords(String paramTuneDir, String fileNamePrefix, PriorityQueue<RunDataSummaryRecord> sortedEnsembles) {
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "All_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter constant = new BufferedWriter(new PrintWriter(new File( paramTuneDir + "Constant_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter variable = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "Variable_" + fileNamePrefix + "Parameters.txt")));
			
			bw.append("TimeInSeconds\t"
					+ "AllDataTest\t"
					+ "CvEnsembleTest\t"
					+ "CvValidation\t"
					+ "OptimalNumberOfTrees\t"
					+ "TotoalNumberOfIteractionsAtONOT"
					+ "AvgNumberOfSplits\t"
					+ "StdDevNumberOfSplits\t"
					+ "AvgLearningRate\t"
					+ "LearningRateStdDev\t" 
					+ "AvgExamplesInNode\t"
					+ "StdDevExamplesInNode\t" 
					+ GbmParameters.getTabSeparatedHeader() 
					+ "\n");
			while (!sortedEnsembles.isEmpty()) {
				RunDataSummaryRecord record = sortedEnsembles.poll();
				
				String recordString = String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t.4f\t%.4f\t%.8f\t%.8f\t%.8f\t%.8f\t", 
						record.timeInSeconds, 
						record.allDataTestError, 
						record.cvEnsembleTestError, 
						record.cvValidationError, 
						record.optimalNumberOfTrees, 
						record.totalNumberOfInteractionsAtONOT,
						record.avgNumberOfSplits, 
						record.stdDevNumberOfSplits, 
						record.avgLearningRate, 
						record.stdDevLearningRate,
						record.avgExamplesInNode,
						record.stdDevExamplesInNode)
						+ record.parameters.getTabSeparatedPrintOut() + "\n";
				bw.append(recordString);
				if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
					constant.append(recordString);
				} else {
					variable.append(recordString);
				}
			}
			constant.flush();
			constant.close();
			variable.flush();
			variable.close();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
		}
	}

	public static ArrayList<RunDataSummaryRecord> readRunDataSummaryRecords(String paramTuneDir) {
		return readRunDataSummaryRecords(paramTuneDir, "SortedByCvValidationError");
	}
	public static ArrayList<RunDataSummaryRecord> readRunDataSummaryRecords(String paramTuneDir, String prefix) {
		ArrayList<RunDataSummaryRecord> records = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paramTuneDir + "All_" + prefix + "Parameters.txt")));
			br.readLine(); // discard header
			String text;
			while ((text = br.readLine()) != null) {
				String[] columns = text.split("\t");
				RunDataSummaryRecord record = new RunDataSummaryRecord();
				record.timeInSeconds = Double.parseDouble(columns[0].trim());
				record.allDataTestError = Double.parseDouble(columns[1].trim());
				record.cvEnsembleTestError = Double.parseDouble(columns[2].trim());
				record.cvValidationError = Double.parseDouble(columns[3].trim());
				record.optimalNumberOfTrees = Double.parseDouble(columns[4].trim());
				record.totalNumberOfInteractionsAtONOT = Double.parseDouble(columns[5].trim());
				record.avgNumberOfSplits = Double.parseDouble(columns[6].trim());
				record.stdDevNumberOfSplits = Double.parseDouble(columns[7].trim());
				record.avgLearningRate = Double.parseDouble(columns[8].trim());
				record.stdDevLearningRate = Double.parseDouble(columns[9].trim());
				record.avgExamplesInNode = Double.parseDouble(columns[10].trim());
				record.stdDevExamplesInNode = Double.parseDouble(columns[11].trim());
				record.parameters = new GbmParameters(
						Double.parseDouble(columns[13].trim()), // MinLR
						Double.parseDouble(columns[14].trim()), // MaxLR
						Integer.parseInt(columns[15].trim()), // NOS
						Double.parseDouble(columns[16].trim()), //BF
						Integer.parseInt(columns[17].trim()), //MEIN
						Integer.parseInt(columns[18].trim()), //NOT
						LearningRatePolicy.valueOf(columns[12].trim()), // LearningRatePolicy
						SplitsPolicy.CONSTANT);
				records.add(record);
			}
			br.close();
		} catch (Exception e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		}
		return records;
	}
	
	public static RunDataSummaryRecord readRunDataSummaryRecordFromRunDataFile(String runDataDirectory, GbmParameters parameters) {
		String runDataFilePath = runDataDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--runData.txt";
		RunDataSummaryRecord record = new RunDataSummaryRecord();
		
		if (!new File(runDataFilePath).exists()) {
			System.out.println(StopWatch.getDateTimeStamp() + "Couldn't find " + runDataFilePath);
			return null;
		}
		
		String line = null;
		record.runFileType = null;
		record.parameters = parameters;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(runDataFilePath)));
			line = br.readLine();
			if (line == null) {
				System.err.println("Weird case of empty run data file. Hoepfully this isn't common. FilePath: \n" + runDataFilePath);
				br.close();
				return null;
			}
			record.timeInSeconds = Double.parseDouble(line.split(": ")[1].trim());
			record.stepSize = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.numberOfFolds = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.totalNumberOfTrees = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.optimalNumberOfTrees = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvEnsembleTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvEnsembleTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.avgNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.stdDevNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.avgLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.stdDevLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
			
			// Check if its an averaged run data file
			line = br.readLine();
			if (line.contains("All Data Avg Examples In Node")) {
				record.avgExamplesInNode = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.stdDevExamplesInNode = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.numberOfRunsRound = Double.parseDouble(line.split(": ")[1].trim());
				record.totalNumberOfTreesFound = Double.parseDouble(br.readLine().split(": ")[1].trim());
				String[] numberOfTreesInEachRun = br.readLine().split(": ")[1].split(", ");
				String[] optimalNumberOfTreesInEachRun = br.readLine().split(": ")[1].split(", ");
				record.numberOfTreesFoundInEachRun = new double[(int)record.numberOfRunsRound];
				record.optimalNumberOfTreesFoundinEachRun = new double[(int)record.numberOfRunsRound];
				for (int i = 0; i < numberOfTreesInEachRun.length; i++) {
					record.numberOfTreesFoundInEachRun[i] = Double.parseDouble(numberOfTreesInEachRun[i]);
					record.optimalNumberOfTreesFoundinEachRun[i] = Double.parseDouble(optimalNumberOfTreesInEachRun[i]);
				}
				record.totalNumberOfInteractionsAtONOT = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.minNumberOfTreesAllRunsHave = Double.parseDouble(br.readLine().split(": ")[1].trim());
			}
			
			br.close();
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		}
		return record;
	}
}
