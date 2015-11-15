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
import java.util.List;
import java.util.PriorityQueue;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.DoubleCompare;


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
	
	
	public void inferTimeInSecondsFromPartialRun(int numberOfTreesInPartialRun, double timeInSeconds) {
		this.timeInSeconds = (timeInSeconds / numberOfTreesInPartialRun) * totalNumberOfTrees;
	}

	public static class CvValidationErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return DoubleCompare.compare(o1.cvValidationError, o2.cvValidationError);
		}
	}
	
	public static class CvTestErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return DoubleCompare.compare(o1.cvTestError, o2.cvTestError);
		}
	}
	
	public static class TimeInSecondsComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return DoubleCompare.compare(o1.timeInSeconds, o2.timeInSeconds);
		}
	}
	
	public static class AllDataTestErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return DoubleCompare.compare(o1.allDataTestError, o2.allDataTestError);
		}
	}
	
	public static class AllDataTrainingErrorComparator implements Comparator<RunDataSummaryRecord> {
		@Override
		public int compare(RunDataSummaryRecord o1, RunDataSummaryRecord o2) {
			return DoubleCompare.compare(o1.allDataTrainingError, o2.allDataTrainingError);
		}
	}
	
	public static void saveRunDataSummaryRecords(String paramTuneDir, String fileNamePrefix, List<RunDataSummaryRecord> list) {
		PriorityQueue<RunDataSummaryRecord> queue = new PriorityQueue<>(new AllDataTestErrorComparator());
		queue.addAll(list);
		saveRunDataSummaryRecords(paramTuneDir, fileNamePrefix, queue);
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
					+ "AvgNumberOfSplits\t"
					+ "StdDevNumberOfSplits\t"
					+ "AvgLearningRate\t"
					+ "LearningRateStdDev\t" 
					+ GbmParameters.getTabSeparatedHeader() 
					+ "\n");
			while (!sortedEnsembles.isEmpty()) {
				RunDataSummaryRecord record = sortedEnsembles.poll();
				
				String recordString = String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.8f\t%.8f\t", 
						record.timeInSeconds, 
						record.allDataTestError, 
						record.cvEnsembleTestError, 
						record.cvValidationError, 
						record.optimalNumberOfTrees, 
						record.avgNumberOfSplits, 
						record.stdDevNumberOfSplits, 
						record.avgLearningRate, 
						record.stdDevLearningRate)
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
			e.printStackTrace();
		}
	}
	
	public static ArrayList<RunDataSummaryRecord> readRunDataSummaryRecords(String datasetName, String paramTuneDir) {
		ArrayList<RunDataSummaryRecord> records = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paramTuneDir + "All_SortedByCvValidationErrorParameters.txt")));
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
				record.avgNumberOfSplits = Double.parseDouble(columns[5].trim());
				record.stdDevNumberOfSplits = Double.parseDouble(columns[6].trim());
				record.avgLearningRate = Double.parseDouble(columns[7].trim());
				record.stdDevLearningRate = Double.parseDouble(columns[8].trim());
				record.parameters = new GbmParameters(
						Double.parseDouble(columns[10].trim()), // MinLR
						Double.parseDouble(columns[11].trim()), // MaxLR
						Integer.parseInt(columns[12].trim()), // NOS
						Double.parseDouble(columns[13].trim()), //BF
						Integer.parseInt(columns[14].trim()), //MEIN
						Integer.parseInt(columns[15].trim()), //NOT
						LearningRatePolicy.valueOf(columns[9].trim()), // LearningRatePolicy
						SplitsPolicy.CONSTANT);
				records.add(record);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return records;
	}
	
	public static RunDataSummaryRecord readRunDataSummaryRecordFromRunDataFile(String runDataDirectory, GbmParameters parameters, RunFileType expectedRunFileType) {
		String runDataFilePath = runDataDirectory + parameters.getRunDataSubDirectory(expectedRunFileType) + parameters.getFileNamePrefix(expectedRunFileType)  + "--runData.txt";
		RunDataSummaryRecord record = new RunDataSummaryRecord();
		
		if (!new File(runDataFilePath).exists()) {
			System.out.println("Couldn't find " + runDataFilePath);
			return null;
		}
		
		String line = null;
		record.runFileType = null;
		record.parameters = parameters;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(runDataFilePath)));
			line = br.readLine();
			// Added time in seconds after the original parameter tuning test.
			if (line.contains("Step Size")) {
				record.runFileType = RunFileType.Original;
				record.stepSize = Double.parseDouble(line.split(": ")[1].trim());
			} else {
				record.timeInSeconds = Double.parseDouble(line.split(": ")[1].trim());
				record.stepSize = Double.parseDouble(br.readLine().split(": ")[1].trim());
			}
			// Always have been the same
			record.numberOfFolds = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.totalNumberOfTrees = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.optimalNumberOfTrees = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			
			// Changed multiple times
			line = br.readLine();
			if (line.contains("CV Relative Influences") && record.runFileType == null) {
				record.runFileType = RunFileType.ParamTuning2;
			} else if (line.contains("All Data Avg Number Of Splits")) {
				record.runFileType = RunFileType.ParamTuning3;
				record.avgNumberOfSplits = Double.parseDouble(line.split(": ")[1].trim());
			} else if (line.contains("CV Ensemble")) {
				record.runFileType = RunFileType.ParamTuning4;
				record.cvEnsembleTrainingError = Double.parseDouble(line.split(": ")[1].trim());
			}
			if (record.runFileType == RunFileType.ParamTuning4) {
				record.cvEnsembleTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.avgNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.stdDevNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.avgLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.stdDevLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
			} 
			// Check if this is an averaged run data file, if so read the extra data in.
			line = br.readLine();
			if (line.contains("Number of runs found")) {
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
			}
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return record;
	}
}
