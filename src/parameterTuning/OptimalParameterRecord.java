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
import java.util.PriorityQueue;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.DoubleCompare;


public class OptimalParameterRecord {
	public GbmParameters parameters;
	public double timeInSeconds;
	public int totalNumberOfTrees;
	public int optimalNumberOfTrees;
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
	
	
	public void inferTimeInSecondsFromPartialRun(int numberOfTreesInPartialRun, double timeInSeconds) {
		this.timeInSeconds = (timeInSeconds / numberOfTreesInPartialRun) * totalNumberOfTrees;
	}

	public static class CvValidationErrorComparator implements Comparator<OptimalParameterRecord> {
		@Override
		public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
			return DoubleCompare.compare(o1.cvValidationError, o2.cvValidationError);
		}
	}
	
	public static class CvTestErrorComparator implements Comparator<OptimalParameterRecord> {
		@Override
		public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
			return DoubleCompare.compare(o1.cvTestError, o2.cvTestError);
		}
	}
	
	public static class TimeInSecondsComparator implements Comparator<OptimalParameterRecord> {
		@Override
		public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
			return DoubleCompare.compare(o1.timeInSeconds, o2.timeInSeconds);
		}
	}
	
	public static class AllDataTestErrorComparator implements Comparator<OptimalParameterRecord> {
		@Override
		public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
			return DoubleCompare.compare(o1.allDataTestError, o2.allDataTestError);
		}
	}
	
	public static class AllDataTrainingErrorComparator implements Comparator<OptimalParameterRecord> {
		@Override
		public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
			return DoubleCompare.compare(o1.allDataTrainingError, o2.allDataTrainingError);
		}
	}
	
	public static void saveOptimalParameterRecordsOldFormat(String paramTuneDir, String fileNamePrefix, PriorityQueue<OptimalParameterRecord> sortedEnsembles) {
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "All_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter constant = new BufferedWriter(new PrintWriter(new File( paramTuneDir + "Constant_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter variable = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "Variable_" + fileNamePrefix + "Parameters.txt")));
			bw.append("TimeInSeconds\tCvValidation\tCvTest\tAllDataTraining\tAllDataTest\tTotalNumberOfTreesTrained\tOptimalNumberOfTrees\t" + GbmParameters.getOldTabSeparatedHeader() + "\n");
			while (!sortedEnsembles.isEmpty()) {
				OptimalParameterRecord record = sortedEnsembles.poll();
				
				bw.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
						record.timeInSeconds, record.cvValidationError, 
						record.cvTestError, record.allDataTrainingError, 
						record.allDataTestError,
						record.totalNumberOfTrees, record.optimalNumberOfTrees) 
						+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
				if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
					constant.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
							record.timeInSeconds, record.cvValidationError, 
							record.cvTestError, record.allDataTrainingError, 
							record.allDataTestError,
							record.totalNumberOfTrees, record.optimalNumberOfTrees) 
							+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
				} else {
					variable.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
							record.timeInSeconds, record.cvValidationError, 
							record.cvTestError, record.allDataTrainingError, 
							record.allDataTestError,
							record.totalNumberOfTrees, record.optimalNumberOfTrees) 
							+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
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
	
	public static ArrayList<OptimalParameterRecord> readOptimalParameterRecordsOldFormat(String datasetName, String paramTuneDir) {
		ArrayList<OptimalParameterRecord> records = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paramTuneDir + "All_SortedByCvValidationErrorParameters.txt")));
			br.readLine(); // discard header
			String text;
			while ((text = br.readLine()) != null) {
				String[] columns = text.split("\t");
				OptimalParameterRecord record = new OptimalParameterRecord();
				record.timeInSeconds = Double.parseDouble(columns[0].trim());
				record.cvValidationError = Double.parseDouble(columns[1].trim());
				record.cvTestError = Double.parseDouble(columns[2].trim());
				record.allDataTrainingError = Double.parseDouble(columns[3].trim());
				record.allDataTestError = Double.parseDouble(columns[4].trim());
				record.totalNumberOfTrees = Integer.parseInt(columns[5].trim());
				record.optimalNumberOfTrees = Integer.parseInt(columns[6].trim());
				record.parameters = new GbmParameters(
						Double.parseDouble(columns[8].trim()), // LR
						Integer.parseInt(columns[10].trim()), //NOS
						Double.parseDouble(columns[9].trim()), //BF
						Integer.parseInt(columns[11].trim()), //MEIN
						Integer.parseInt(columns[12].trim()), //NOT
						LearningRatePolicy.valueOf(columns[7].trim()), // LearningRatePolicy
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
	
	public static void saveOptimalParameterRecords(String paramTuneDir, String fileNamePrefix, PriorityQueue<OptimalParameterRecord> sortedEnsembles) {
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
				OptimalParameterRecord record = sortedEnsembles.poll();
				
				String recordString = String.format("%.4f\t%.4f\t%.4f\t%.4f\t%d\t%.4f\t%.4f\t%.8f\t%.8f\t", 
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
	
	public static ArrayList<OptimalParameterRecord> readOptimalParameterRecords(String datasetName, String paramTuneDir) {
		ArrayList<OptimalParameterRecord> records = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paramTuneDir + "All_SortedByCvValidationErrorParameters.txt")));
			br.readLine(); // discard header
			String text;
			while ((text = br.readLine()) != null) {
				String[] columns = text.split("\t");
				OptimalParameterRecord record = new OptimalParameterRecord();
				record.timeInSeconds = Double.parseDouble(columns[0].trim());
				record.allDataTestError = Double.parseDouble(columns[1].trim());
				record.cvEnsembleTestError = Double.parseDouble(columns[2].trim());
				record.cvValidationError = Double.parseDouble(columns[3].trim());
				record.optimalNumberOfTrees = Integer.parseInt(columns[4].trim());
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
	
	
	public static OptimalParameterRecord readOptimalParameterRecordFromRunDataFile(String runDataDirectory, GbmParameters parameters) {
		String runDataFilePath = runDataDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--runData.txt";
		OptimalParameterRecord record = new OptimalParameterRecord();
		
		if (!new File(runDataFilePath).exists()) {
			System.out.println(String.format("Couldn't find " + runDataFilePath));
			return null;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(runDataFilePath)));
			record.timeInSeconds = Double.parseDouble(br.readLine().split(": ")[1].trim());
			br.readLine(); // Skip Step Size
			br.readLine(); // Skip number of Folds
			record.totalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
			record.optimalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
			record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
			try {
				record.cvEnsembleTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.cvEnsembleTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.avgNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.stdDevNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.avgLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
				record.stdDevLearningRate = Double.parseDouble(br.readLine().split(": ")[1].trim());
			} catch(Exception e) {
				System.out.println("Failed to read avgNumberOfSplits, cvEnsembleErrors, avgLearningRate, or stdDevLearningRate. "
						+ "This must be an old run data file, setting these to -1 and moving on");
				record.cvEnsembleTrainingError = -1;
				record.cvEnsembleTestError = -1;
				record.stdDevNumberOfSplits = -1;
				record.avgNumberOfSplits = -1;
				record.avgLearningRate = -1;
				record.stdDevLearningRate = -1;
			}
			record.parameters = parameters;
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return record;
	}
}
