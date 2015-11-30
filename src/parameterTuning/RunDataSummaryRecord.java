package parameterTuning;
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

import dataset.DatasetParameters;
import gbm.GbmParameters;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
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
	
	
	public void inferTimeInSecondsFromPartialRun(int numberOfTreesInPartialRun, double timeInSeconds) {
		this.timeInSeconds = (timeInSeconds / numberOfTreesInPartialRun) * totalNumberOfTrees;
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
				
				String recordString = String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.8f\t%.8f\t%.8f\t%.8f\t", 
						record.timeInSeconds, 
						record.allDataTestError, 
						record.cvEnsembleTestError, 
						record.cvValidationError, 
						record.optimalNumberOfTrees, 
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
				record.avgExamplesInNode = Double.parseDouble(columns[9].trim());
				record.stdDevExamplesInNode = Double.parseDouble(columns[10].trim());
				record.parameters = new GbmParameters(
						Double.parseDouble(columns[12].trim()), // MinLR
						Double.parseDouble(columns[13].trim()), // MaxLR
						Integer.parseInt(columns[14].trim()), // NOS
						Double.parseDouble(columns[15].trim()), //BF
						Integer.parseInt(columns[16].trim()), //MEIN
						Integer.parseInt(columns[17].trim()), //NOT
						LearningRatePolicy.valueOf(columns[11].trim()), // LearningRatePolicy
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
	
	public static RunDataSummaryRecord readRunDataSummaryRecordFromRunDataFile(String runDataDirectory, GbmParameters parameters, RunFileType expectedRunFileType) {
		String runDataFilePath = null;
		try {
		 runDataFilePath = runDataDirectory + parameters.getRunDataSubDirectory(expectedRunFileType) + parameters.getFileNamePrefix(expectedRunFileType)  + "--runData.txt";
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println();
		}
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
			if (line.startsWith("All Data Avg Examples In Node")) {
				record.avgExamplesInNode = Double.parseDouble(line.split(": ")[1].trim());
				record.stdDevExamplesInNode = Double.parseDouble(br.readLine().split(": ")[1].trim());
				line = br.readLine();
			}
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
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
		} catch (Exception e) {
			System.exit(1);
		}
		return record;
	}
	
	//-----------------------------------------------------------------
	/*
	public static void writeBestAndWorstADTELatexTable(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, String runDataSubDirectory) {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		ArrayList<RunDataSummaryRecord> allRecords = readRunDataSummaryRecords(paramTuneDir);
		
		PriorityQueue<RunDataSummaryRecord> constantRecords = new PriorityQueue<>(new AllDataTestErrorComparator());
		constantRecords.addAll(RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(allRecords));
		
		PriorityQueue<RunDataSummaryRecord> variableRecords = new PriorityQueue<>(new AllDataTestErrorComparator());
		variableRecords.addAll(RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(allRecords));
		
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "BestAndWorstTables.txt")));
			bw.write("\\begin{table}[!t]\n");
			bw.write("\t\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c | c | c |  c | c || c |}\n");
			bw.write("\t\t\\hline\n");
			bw.write("\t\t\\rule{0pt}{2ex} Time & Trees & ConstShr & BF & MEIN & MaxSplits & RMSE \\\\ \\hline \n");
			bw.write(getNBestAndWorstRows(constantRecords, 5, LearningRatePolicy.CONSTANT));
			bw.write("\t\\end{tabular}}\n");
			bw.write("\t\\caption{" + datasetParameters.fullName + ": Constant Shrinkage Parameters with Best and Worst RMSE on Test Set.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName  + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n\n\n");
			
			bw.write("\\begin{table}[!t]\n");
			bw.write("\t\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c | c | c | c | c | c || c | }\n");
			bw.write("\t\t\\hline\n");
			bw.write("\t\t\\rule{0pt}{2ex} Time & Trees & MinShr & MaxShr & BF & MEIN & MaxSplits & RMSE \\\\ \\hline \n");
			bw.write(getNBestAndWorstRows(variableRecords, 5, LearningRatePolicy.REVISED_VARIABLE));
			bw.write("\t\\end{tabular}}\n");
			bw.write("\t\\caption{" + datasetParameters.fullName + ": Variable Shrinkage Parameters with Best and Worst RMSE on Test Set.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static String getNBestAndWorstRows(PriorityQueue<RunDataSummaryRecord> records, int n, LearningRatePolicy lrPolicy) {
		StringBuilder bestRows = new StringBuilder();
		StringBuilder worstRows = new StringBuilder();
		int recordCount = records.size() - 1;

		List<RunDataSummaryRecord> sortedRecords = new ArrayList<RunDataSummaryRecord>(records);
		for (int i = 0; i < n; i++) {
			RunDataSummaryRecord best = sortedRecords.get(i);
			RunDataSummaryRecord worst = sortedRecords.get(recordCount-i);
			
			if (lrPolicy == LearningRatePolicy.CONSTANT) {
				bestRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						best.timeInSeconds, 
						(int)best.optimalNumberOfTrees,
						best.parameters.maxLearningRate,
						best.parameters.bagFraction,
						best.parameters.minExamplesInNode,
						best.parameters.maxNumberOfSplits,
						best.allDataTestError));
				worstRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						worst.timeInSeconds, 
						(int)worst.optimalNumberOfTrees,
						worst.parameters.maxLearningRate,
						worst.parameters.bagFraction,
						worst.parameters.minExamplesInNode,
						worst.parameters.maxNumberOfSplits,
						worst.allDataTestError));
			} else {
				bestRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.1f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						best.timeInSeconds, 
						(int)best.optimalNumberOfTrees,
						best.parameters.minLearningRate,
						best.parameters.maxLearningRate,
						best.parameters.bagFraction,
						best.parameters.minExamplesInNode,
						best.parameters.maxNumberOfSplits,
						best.allDataTestError));
				worstRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.1f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						worst.timeInSeconds, 
						(int)worst.optimalNumberOfTrees,
						worst.parameters.minLearningRate,
						worst.parameters.maxLearningRate,
						worst.parameters.bagFraction,
						worst.parameters.minExamplesInNode,
						worst.parameters.maxNumberOfSplits,
						worst.allDataTestError));
			}
		}
		return bestRows.toString() + "\\hline" + worstRows.toString();
	}
	
	public static void writeBestCVErrorLatexTable(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, String runDataSubDirectory) {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		ArrayList<RunDataSummaryRecord> allRecords = readRunDataSummaryRecords(paramTuneDir);
		PriorityQueue<RunDataSummaryRecord> constantRecords = new PriorityQueue<>(new CvValidationErrorComparator());
		constantRecords.addAll(RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(allRecords));
		
		PriorityQueue<RunDataSummaryRecord> variableRecords = new PriorityQueue<>(new CvValidationErrorComparator());
		variableRecords.addAll(RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(allRecords));
		
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "BestCVTables.txt")));
			bw.write("\\begin{table}[!t]\n");
			bw.write("\t\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c | c | c | c | c || c |}\n");
			bw.write("\t\t\\hline\n");
			bw.write("\t\t\\rule{0pt}{2ex} Time & Trees & ConstShr & BF & MEIN & MaxSplits & RMSE \\\\ \\hline \n");
			bw.write(getNBestRows(constantRecords, 5, LearningRatePolicy.CONSTANT));
			bw.write("\t\\end{tabular}}\n");
			bw.write("\t\\caption{Constant Shrinkage: " + datasetParameters.fullName + " Parameters with Best Cross Validation RMSE.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName  + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n\n\n");
			
			bw.write("\\begin{table}[!t]\n");
			bw.write("\t\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c | c | c | c | c | c || c | }\n");
			bw.write("\t\t\\hline\n");
			bw.write("\t\t\\rule{0pt}{2ex} Time & Trees & MinShr & MaxShr & BF & MEIN & MaxSplits & RMSE \\\\ \\hline \n");
			bw.write(getNBestRows(variableRecords, 5, LearningRatePolicy.REVISED_VARIABLE));
			bw.write("\t\\end{tabular}}\n");
			bw.write("\t\\caption{Variable Shrinkage: " + datasetParameters.fullName + " Parameters with Best Cross Validation RMSE.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static String getNBestRows(PriorityQueue<RunDataSummaryRecord> records, int n, LearningRatePolicy lrPolicy) {
		StringBuilder bestRows = new StringBuilder();

		for (int i = 0; i < n; i++) {
			RunDataSummaryRecord best = records.poll();
			
			if (lrPolicy == LearningRatePolicy.CONSTANT) {
				bestRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						best.timeInSeconds, 
						(int)best.optimalNumberOfTrees,
						best.parameters.maxLearningRate,
						best.parameters.bagFraction,
						best.parameters.minExamplesInNode,
						best.parameters.maxNumberOfSplits,
						best.cvValidationError));
			} else {
				bestRows.append(String.format("\t\t\\rule{0pt}{2ex} %.2f & %d & %.4f & %.1f & %.2f & %d & %d & %.4f \\\\ \\hline\n",
						best.timeInSeconds, 
						(int)best.optimalNumberOfTrees,
						best.parameters.minLearningRate,
						best.parameters.maxLearningRate,
						best.parameters.bagFraction,
						best.parameters.minExamplesInNode,
						best.parameters.maxNumberOfSplits,
						best.cvValidationError));
			}
		}
		return bestRows.toString();
	}
	*/
	public static void writeBestColumnWiseLatexTable(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, String runDataSubDirectory) {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		ArrayList<RunDataSummaryRecord> allRecords = readRunDataSummaryRecords(paramTuneDir);
		
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(allRecords);
		constantRecords.sort(new CvValidationErrorComparator());
		
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(allRecords);
		variableRecords.sort(new CvValidationErrorComparator());
		
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "BestCVTables.txt")));
			bw.write("\\begin{table}[!t]\n");
			bw.write("\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c  c  c  c  c  | c | }\n");
			//bw.write("\t\t\\hline\n");
			bw.write("\t\t\\multicolumn{1}{c}{\\textbf{Property}} & \\multicolumn{5}{c}{\\textbf{Best 5 Parameters with Constant Shrinkage}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\ "
					+ "\\hline");
			bw.write(getNBestColumns(constantRecords, 5, LearningRatePolicy.CONSTANT));
			bw.write("\t\\end{tabular}\n");
			bw.write("\t}\n");
			bw.write("\t\\caption{Constant Shrinkage: " + datasetParameters.fullName + " Parameters with Best Cross Validation RMSE.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName  + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n\n\n");
			
			bw.write("\\begin{table}[!t]\n");
			bw.write("\\resizebox{\\linewidth}{!}{\n");
			bw.write("\t\\begin{tabular}{ | c | c  c  c  c  c  | c | }\n");
			//bw.write("\t\t\\hline\n");
			bw.write("\t\t\\multicolumn{1}{c}{\\textbf{}} & \\multicolumn{5}{c}{\\textbf{Best 5 Parameters with Variable Shrinkage}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\ \\hline");
			bw.write(getNBestColumns(variableRecords, 5, LearningRatePolicy.REVISED_VARIABLE));
			bw.write("\t\\end{tabular}\n");
			bw.write("\t}\n");
			bw.write("\t\\caption{Variable Shrinkage: " + datasetParameters.fullName + " Parameters with Best Cross Validation RMSE.}\n");
			bw.write("\t\\label{tab:" + datasetParameters.minimalName + "constantBestAndWorstParameters}\n");
			bw.write("\\end{table}\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static String getNBestColumns(List<RunDataSummaryRecord> records, int n, LearningRatePolicy lrPolicy) {
		StringBuilder bestRows = new StringBuilder();

		bestRows.append(getRowFromBestNRecords(RowType.BagFraction, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MinExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MaxSplits, records, 5));
		
		if (lrPolicy == LearningRatePolicy.CONSTANT) {
			bestRows.append(getRowFromBestNRecords(RowType.Shrinkage, records, 5));
		} else {
			bestRows.append(getRowFromBestNRecords(RowType.MinShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.MaxShrinkage, records, 5));
		}
		bestRows.append("\t\t \\hline \n");
		
		bestRows.append(getRowFromBestNRecords(RowType.TimeInSeconds, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ATD_Test_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ABT_Test_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.CV_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.Trees, records, 5));

		bestRows.append("\t\t \\hline \n");
		bestRows.append(getRowFromBestNRecords(RowType.AvgExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.AvgSplits, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevSplits, records, 5));
		if (lrPolicy == LearningRatePolicy.REVISED_VARIABLE) {
			bestRows.append(getRowFromBestNRecords(RowType.AvgShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.StdDevShrinkage, records, 5));
		}


		return bestRows.toString();
	}
	
	private enum RowType {
		TimeInSeconds("RunningTime (seconds)"), Trees("Optimal Num. of Trees"), Shrinkage, 
		MinShrinkage("Min Shrinkage"), MaxShrinkage ("Max Shrinkage"), 
		BagFraction("Bag Fraction"), MinExamplesInNode ("Min Leaf Size"), 
		AvgExamplesInNode("Avg. Leaf Size"), StdDevExamplesInNode("Std. Dev. Leaf Size"), 
		MaxSplits("MaxSplits"), AvgSplits("Avg. Splits"), StdDevSplits("Std. Dev. Splits"), 
		AvgShrinkage("Avg. Shrinkage"), StdDevShrinkage("Std. Dev. Shrinkage"), 
		ATD_Test_Error("ATD Test RMSE"), ABT_Test_Error("ABT Test RMSE"), CV_Error("Cross Validation RMSE");
		
	    private final String fieldDescription;

	    private RowType() {
	        fieldDescription = null;
	    }
	    
	    private RowType(String value) {
	        fieldDescription = value;
	    }

	    @Override
	    public String toString() {
	        return (fieldDescription == null) ? name() : fieldDescription;
	    }
	    
	}
	
	private static int getIndexWithMin(double[] array) {
		int minIndex = 0;
		for (int i = 1; i < array.length; i++){
		   double newnumber = array[i];
		   if ((newnumber < array[minIndex])){
			   minIndex = i;
		  }
		} 
		return minIndex;
	}
	
	private static int getIndexWithMax(double[] array) {
		int minIndex = 0;
		for (int i = 1; i < array.length; i++){
		   double newnumber = array[i];
		   if ((newnumber > array[minIndex])){
			   minIndex = i;
		  }
		} 
		return minIndex;
	}
	
	private static double getAverage(double[] array) {
		double avg = 0;
		for (int i = 0; i < array.length; i++){
			avg += array[i];
		} 
		return avg / array.length;
	}
	
	private static String getRowFromBestNRecords(RowType rowType, List<RunDataSummaryRecord> records, int n) {
		StringBuilder retval = new StringBuilder();
		retval.append("\t\t\\rule{0pt}{2ex} " + rowType + " & ");
		double[] values = new double[n];
		String[] formattedValues = new String[n+1];
		boolean highlightMinAndMaxValue = true;
		for (int i = 0; i < n; i++) {
			RunDataSummaryRecord record = records.get(i);
			boolean isInteger = false;
			switch (rowType) {
				case ABT_Test_Error:
					values[i] = record.cvEnsembleTestError;
					break;
				case ATD_Test_Error:
					values[i] = record.allDataTestError;
					break;
				case AvgExamplesInNode:
					values[i] = record.avgExamplesInNode;
					break;
				case AvgSplits:
					values[i] = record.avgNumberOfSplits;
					break;
				case AvgShrinkage:
					values[i] = record.avgLearningRate;
					break;
				case BagFraction:
					values[i] = record.parameters.bagFraction;
					highlightMinAndMaxValue = false;
					break;
				case CV_Error:
					values[i] = record.cvValidationError;
					break;
				case MaxSplits:
					values[i] = record.parameters.maxNumberOfSplits;
					isInteger = true;
					highlightMinAndMaxValue = false;
					break;
				case MaxShrinkage:
					values[i] = record.parameters.maxLearningRate;
					highlightMinAndMaxValue = false;
					break;
				case MinShrinkage:
					values[i] = record.parameters.minLearningRate;
					highlightMinAndMaxValue = false;
					break;
				case MinExamplesInNode:
					values[i] = record.parameters.minExamplesInNode;
					isInteger = true;
					highlightMinAndMaxValue = false;
					break;
				case Shrinkage:
					values[i] = record.parameters.maxLearningRate;
					highlightMinAndMaxValue = false;
					break;
				case StdDevExamplesInNode:
					values[i] = record.stdDevExamplesInNode;
					break;
				case StdDevShrinkage:
					values[i] = record.stdDevLearningRate;
					break;
				case StdDevSplits:
					values[i] = record.stdDevNumberOfSplits;
					break;
				case TimeInSeconds:
					values[i] = record.timeInSeconds;
					break;
				case Trees:
					values[i] = record.optimalNumberOfTrees;
					break;
				default:
					break;
			}
			if (Double.isNaN(values[i]) && rowType.name().contains("StdDev") || values[i] * 10000 < 1) {
				values[i] = 0; // StdDev formula breaks down when value is practically zero but not quite due to rounding
			}
			formattedValues[i] = (isInteger) ? String.valueOf(values[i]) : (String.format("%f", values[i])).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
		
		}
		formattedValues[n] = (String.format("%f", getAverage(values))).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
		int minIndex = getIndexWithMin(values), maxIndex = getIndexWithMax(values);
		for (int j = 0; j < formattedValues.length; j++) {
			if (j == minIndex && highlightMinAndMaxValue) {
				retval.append("\\cellcolor{gray!10}" + formattedValues[j]);
			} else if (j == maxIndex && highlightMinAndMaxValue){
				retval.append("\\cellcolor{gray!30}" + formattedValues[j]);
			} else {
				retval.append(formattedValues[j]);
			}
			if (j != formattedValues.length-1) {
				retval.append(" & ");
			}
		}
		return retval.toString() + "\\\\ \\hline \n ";
	}
}
