package parameterTuning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dataset.Dataset;
import dataset.DatasetParameters;
import parameterTuning.RunDataSummaryRecord.AllDataTestErrorComparator;
import parameterTuning.RunDataSummaryRecord.CvEnsembleErrorComparator;
import parameterTuning.RunDataSummaryRecord.CvValidationErrorComparator;
import parameterTuning.plotting.AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator;
import parameterTuning.plotting.AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.GraphableProperty;
import parameterTuning.plotting.ErrorCurveScriptExecutor;
import parameterTuning.plotting.ErrorCurveScriptGenerator;
import parameterTuning.plotting.SortedResponsePredictionGraphGenerator;
import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.StopWatch;

public class LatexResultsGenerator {

	public static void writeEntireResultsSection(ParameterTuningParameters tuningParameters) {
		//tuningParameters.datasets = new DatasetParameters[] {ParameterTuningParameters.crimeCommunitiesParameters};
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(tuningParameters.runDataProcessingDirectory + "entireResultsSection.tex"));
			System.out.println("Writing directories comment");
			// Write all the directories comment
			for (DatasetParameters datasetParameters : tuningParameters.datasets) {
				writeRunDataDirectoryForNBestParametersForCV_ATD_and_ABT(bw, tuningParameters, datasetParameters, 5);
			}
			System.out.println("Writing tables");
			// Write all the tables
			//bw.write("\\clearpage\n");
			for (DatasetParameters datasetParameters : tuningParameters.datasets) {
				//writeBestAndWorstConstantAndVariableLatexTable(bw, datasetParameters, tuningParameters);
				writeBestConstantAndVariableLatexTable(bw, datasetParameters, tuningParameters, 1);
			}
			System.out.println("Writing plotLegends");
			//bw.write("\\clearpage\n");
			// Write the plot legends only once
			writeAllPlotLegends(bw, tuningParameters);
			
			/*
			System.out.println("Writing error curves");
			// Write all the plots
			for (DatasetParameters datasetParameters : tuningParameters.datasets) {
				writeBestErrorCurves(bw, tuningParameters, datasetParameters);
			}
			System.out.println("Writing prediction curves");
			for (DatasetParameters datasetParameters : tuningParameters.datasets) {
				writeBestPredictionAndResidualCurves(bw, tuningParameters, datasetParameters);
			}
			*/
			System.out.println("Writing run data summary curves");
			writeAverageRunDataSummaryCurves(bw, tuningParameters);
			
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	//-----------------------------------------DIRECTORIES COMMENT OF BEST PARAMETERS------------------------------------------------------------------
	private static void writeRunDataDirectoryForNBestParametersForCV_ATD_and_ABT(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams, int n) throws IOException {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName +  "/Averages/";
		ArrayList<RunDataSummaryRecord> allRecordsCV = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir);
		ArrayList<RunDataSummaryRecord> allRecordsADTE = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByAllDataTestError");
		ArrayList<RunDataSummaryRecord> allRecordsCvEnsemble = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByCvEnsembleTestError");
		
		bw.write("\\begin{comment}\n");
		writeRunDataDirectoryForNBestParameters(bw, tuningParameters, datasetParams, allRecordsCV, "CV", n);
		writeRunDataDirectoryForNBestParameters(bw, tuningParameters, datasetParams, allRecordsADTE, "ADTE", n);
		writeRunDataDirectoryForNBestParameters(bw, tuningParameters, datasetParams, allRecordsCvEnsemble, "ADTE", n);
		bw.write("\\end{comment}\n");
	}
	
	private static void writeRunDataDirectoryForNBestParameters(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams, List<RunDataSummaryRecord> records, String RMSEType, int n) throws IOException {
		StringBuilder best = new StringBuilder(datasetParams.minimalName + ": " + RMSEType + " Best Parameter Directories\n");
		for (int i = 0; i < n; i++) {
			String directory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/" + records.get(i).parameters.getRunDataSubDirectory();
			if (i < n) {
				best.append(directory + "\n");
			}
		}
		bw.write(best.toString());
	}
	
	//-----------------------------------------TABLES------------------------------------------------------------------
	private static void writeBestColumnWiseLatexTable(BufferedWriter bw, DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, int n) throws IOException {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + "/Averages/";
		ArrayList<RunDataSummaryRecord> allRecordsCV = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir);
		ArrayList<RunDataSummaryRecord> allRecordsADTE = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByAllDataTestError");
		ArrayList<RunDataSummaryRecord> allRecordsCvEnsemble = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByCvEnsembleTestError");

		bw.write("\\begin{table}[!t]\n");
		bw.write("\\resizebox{\\linewidth}{!}{\n");
		bw.write("\t\\begin{tabular}{ | c || c  | c |c | c | c  || c | }\n");
		
		writeBestOverallNRunDataTables(bw, "Parameters with lowest CV RMSE", allRecordsCV, n);
		
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		writeBestOverallNRunDataTables(bw, "Parameters with lowest ATD Test RMSE", allRecordsADTE, n);
		
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		writeBestOverallNRunDataTables(bw, "Parameters with lowest ABT Test RMSE", allRecordsCvEnsemble, n);

		bw.write("\t\\end{tabular}\n");
		bw.write("\t}\n");
		bw.write("\t\\caption{" + datasetParameters.fullName + ": Parameters resulting in GBMs with the lowest CV RMSE (Top), ATD Test RMSE (Middle), and ABT Test RMSE (Bottom)" + "}\n");
		bw.write("\t\\label{tab:" + datasetParameters.minimalName + "bestParameters" + "}\n");
		bw.write("\\end{table}\n\n\n");
	}
	
	public static void writeBestOverallNRunDataTables(BufferedWriter bw, String titlePrefix, ArrayList<RunDataSummaryRecord> allRecords, int n) throws IOException {
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(allRecords);
		constantRecords.sort(new CvValidationErrorComparator());
		
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(allRecords);
		variableRecords.sort(new CvValidationErrorComparator());
		
		bw.write("\t\t\\multicolumn{1}{c}{\\textbf{Property}} & \\multicolumn{5}{c}{\\textbf{" + titlePrefix + " Overall}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\ \n"
				+ "\\hline");
		bw.write(getNBestOverallColumns(allRecords, n));
		/*
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		bw.write("\t\t\\multicolumn{1}{c}{\\textbf{Property}} & \\multicolumn{5}{c}{\\textbf{" + titlePrefix + " (Constant Shrinkage)}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\\n "
				+ "\\hline");
		bw.write(getNBestColumns(constantRecords, 5, LearningRatePolicy.CONSTANT));
		
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		bw.write("\t\t\\multicolumn{1}{c}{\\textbf{}} & \\multicolumn{5}{c}{\\textbf{" + titlePrefix + " (Variable Shrinkage)}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\ \\hline\n");
		bw.write(getNBestColumns(variableRecords, 5, LearningRatePolicy.REVISED_VARIABLE));
		*/
	}
	
	public enum ErrorType {CV, ATD, ABT};
	private static void writeBestConstantAndVariableLatexTable(BufferedWriter bw, DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, int n) throws IOException {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + "/Averages/";
		ArrayList<RunDataSummaryRecord> allRecordsCV = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir);
		ArrayList<RunDataSummaryRecord> allRecordsADTE = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByAllDataTestError");
		ArrayList<RunDataSummaryRecord> allRecordsCvEnsemble = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByCvEnsembleTestError");

		bw.write("\\begin{table}[!t]\n");
		bw.write("\\centering\n");
		bw.write("\\resizebox*{!}{0.47\\textheight}{\n");
		bw.write("\t\\begin{tabular}{  l | c  c  |c  }\n");
		
		writeNBestConstantAndVariable(bw, "Parameters with lowest CV RMSE", ErrorType.CV, allRecordsCV, n);
		
		bw.write("\t\t\\multicolumn{4}{c}{} \\\\\n");
		writeNBestConstantAndVariable(bw, "Parameters with lowest ATD Test RMSE", ErrorType.ATD, allRecordsADTE, n);
		
		bw.write("\t\t\\multicolumn{4}{c}{} \\\\\n");
		writeNBestConstantAndVariable(bw, "Parameters with lowest ABT Test RMSE", ErrorType.ABT, allRecordsCvEnsemble, n);

		bw.write("\t\\end{tabular}\n");
		bw.write("\t}\n");
		bw.write("\t\\caption{" + datasetParameters.fullName + ": Optimal Parameters}\n");
		bw.write("\t\\label{tab:" + datasetParameters.minimalName + "bestParameters" + "}\n");
		bw.write("\\end{table}\n\n\n");
	}
	
	public static void writeNBestConstantAndVariable(BufferedWriter bw, String titlePrefix, ErrorType errorType, ArrayList<RunDataSummaryRecord> allRecords, int n) throws IOException {
	//bw.write("\t\t\\multicolumn{3}{c}{\\textbf{" + titlePrefix + "}} & \\multicolumn{1}{c}{\\textbf{Difference}} \\\\ \n" + "\\hline \n");
		bw.write("\t\t\\multicolumn{3}{c}{\\textbf{" + titlePrefix + "}} & \\multicolumn{1}{c}{\\textbf{\\% Decrease}} \\\\ \n" + "\\hline \n");
		bw.write(getNBestConstantAndVariableColumns(errorType, allRecords, n));
		/*
		bw.write("\t\t\\multicolumn{6}{c}{} \\\\\n");
		bw.write("\t\t\\multicolumn{1}{c}{\\textbf{Property}} & \\multicolumn{5}{c}{\\textbf{" + titlePrefix + " (Constant Shrinkage)}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\\n "
				+ "\\hline");
		bw.write(getNBestColumns(constantRecords, 5, LearningRatePolicy.CONSTANT));
		
		bw.write("\t\t\\multicolumn{6}{c}{} \\\\\n");
		bw.write("\t\t\\multicolumn{1}{c}{\\textbf{}} & \\multicolumn{5}{c}{\\textbf{" + titlePrefix + " (Variable Shrinkage)}} & \\multicolumn{1}{c}{\\textbf{Averages}} \\\\ \\hline\n");
		bw.write(getNBestColumns(variableRecords, 5, LearningRatePolicy.REVISED_VARIABLE));
		*/
	}
	
	private static void writeBestAndWorstConstantAndVariableLatexTable(BufferedWriter bw, DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters) throws IOException {
		String paramTuneDir = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + "/Averages/";
		ArrayList<RunDataSummaryRecord> allRecordsCV = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir);
		ArrayList<RunDataSummaryRecord> allRecordsADTE = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByAllDataTestError");
		ArrayList<RunDataSummaryRecord> allRecordsCvEnsemble = RunDataSummaryRecord.readRunDataSummaryRecords(paramTuneDir, "SortedByCvEnsembleTestError");

		bw.write("\\begin{table}[!t]\n");
		bw.write("\\resizebox{\\linewidth}{!}{\n");
		bw.write("\t\\begin{tabular}{ | c || c | c | c || c | c | c |}\n");
		
		writeBestAndWorstConstantAndVariableTable(bw, ErrorType.CV, allRecordsCV);
		
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		writeBestAndWorstConstantAndVariableTable(bw, ErrorType.ATD, allRecordsADTE);
		
		bw.write("\t\t\\multicolumn{7}{c}{} \\\\\n");
		writeBestAndWorstConstantAndVariableTable(bw, ErrorType.ABT, allRecordsCvEnsemble);

		bw.write("\t\\end{tabular}\n");
		bw.write("\t}\n");
		bw.write("\t\\caption{" + datasetParameters.fullName + ": Parameters resulting in GBMs with the lowest CV RMSE (Top), ATD Test RMSE (Middle), and ABT Test RMSE (Bottom)" + "}\n");
		bw.write("\t\\label{tab:" + datasetParameters.minimalName + "bestParameters" + "}\n");
		bw.write("\\end{table}\n\n\n");
	}
	
	public static void writeBestAndWorstConstantAndVariableTable(BufferedWriter bw, ErrorType errorType, ArrayList<RunDataSummaryRecord> allRecords) throws IOException {
		bw.write("\t\t\\multicolumn{1}{c}{} & \\multicolumn{2}{c}{\\textbf{Best Parameters}} & \\multicolumn{1}{c}{\\textbf{Diff}} &\\multicolumn{2}{c}{\\textbf{Worst Parameters}} & \\multicolumn{1}{c}{\\textbf{Diff}} \\\\ \n"
				+ "\\hline");
		bw.write(getBestAndWorstConstantAndVariableColumns(errorType, allRecords));
	}
	
	//-----------------------------------------PLOT LEGENDS------------------------------------------------------------------
	private static void writeAllPlotLegends(BufferedWriter bw, ParameterTuningParameters tuningParameters) throws IOException {
		bw.append("\\begin{figure}[!htb]\\centering\n");
		//writeErrorCurveLegend(bw, tuningParameters);
		//bw.append("\t\\unskip\\ \\vrule\\ \n");
		//writePredicationAndResidualCurveLegend(bw, tuningParameters);
		writeRunDataSummaryCurveLegend(bw, tuningParameters);
		//bw.append("\t\\caption{Plot Legends for Error Curves (Left), Residual Curves (Middle), Aggregated Results Curves (Right)" + "}\n");
		bw.append("\t\\caption{Plot Legend for Pairwise Parameter Influence Plots" + "}\n");
		bw.append("\t\\label{fig:legends}\n");
		bw.append("\\end{figure}\n\n");
	}
	private static void writeErrorCurveLegend(BufferedWriter bw, ParameterTuningParameters tuningParameters) throws IOException {
		String directory = tuningParameters.runDataProcessingDirectory;
		//StringBuilder errorCurveLatexCode = new StringBuilder("\\begin{figure}[!htb]\\centering\n");
		bw.append("\t\\resizebox{0.65\\linewidth}{!}{\n");
		bw.append("\t\t\\includegraphics{{" + (directory + "errorCurveLegend")  + "}.png}\n");
		bw.append("\t}\n");
		//errorCurveLatexCode.append("\t\\caption{Error Curve Legend" + "}\n");
		//errorCurveLatexCode.append("\t\\label{tab:errorCurveLegend}\n");
		//errorCurveLatexCode.append("\\end{figure}\n\n");
		//bw.write(errorCurveLatexCode.toString() + "\n\n");
	}
	
	private static void writePredicationAndResidualCurveLegend(BufferedWriter bw, ParameterTuningParameters tuningParameters) throws IOException {
		String directory = tuningParameters.runDataProcessingDirectory;
		//StringBuilder errorCurveLatexCode = new StringBuilder("\\begin{figure}[!htb]\\centering\n");
		bw.append("\t\\resizebox{0.24\\linewidth}{!}{\n");
		bw.append("\t\t\\includegraphics{{" + (directory + "predictionAndResidualCurveLegend")  + "}.png}\n");
		bw.append("\t}\n");
		//errorCurveLatexCode.append("\t\\caption{Residual Plot Legend" + "}\n");
		//errorCurveLatexCode.append("\t\\label{tab:predictionAndResidualLegend}\n");
		//errorCurveLatexCode.append("\\end{figure}\n\n");
		//bw.write(errorCurveLatexCode.toString() + "\n\n");
	}
	
	private static void writeRunDataSummaryCurveLegend(BufferedWriter bw, ParameterTuningParameters tuningParameters) throws IOException {
		String directory = tuningParameters.runDataProcessingDirectory;
		//StringBuilder errorCurveLatexCode = new StringBuilder("\\begin{figure}[!htb]\\centering\n");
		bw.append("\t\\resizebox{0.5\\linewidth}{!}{\n");
		bw.append("\t\t\\includegraphics{{" + (directory + "runDataSummaryGraphLegend")  + "}.png}\n");
		bw.append("\t}\n");
		//errorCurveLatexCode.append("\t\\caption{Summary Curve Legend" + "}\n");
		//errorCurveLatexCode.append("\t\\label{tab:predictionAndResidualLegend}\n");
		//errorCurveLatexCode.append("\\end{figure}\n\n");
		//errorCurveLatexCode.append("\\clearpage");
		//bw.write(errorCurveLatexCode.toString() + "\n\n");
	}
	
	//-----------------------------------------PLOTS------------------------------------------------------------------
	private static void writeBestErrorCurves(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams) throws IOException {
		String directory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName +  "/Averages/";
		ArrayList<RunDataSummaryRecord> allRecordsCV = RunDataSummaryRecord.readRunDataSummaryRecords(directory);
		ArrayList<RunDataSummaryRecord> allRecordsADTE = RunDataSummaryRecord.readRunDataSummaryRecords(directory, "SortedByAllDataTestError");
		ArrayList<RunDataSummaryRecord> allRecordsCvEnsemble = RunDataSummaryRecord.readRunDataSummaryRecords(directory, "SortedByCvEnsembleTestError");
		
		bw.append("\\begin{figure}[!htb]\\centering\n");
		writeBestErrorCurvesForProvidedRecords(bw, tuningParameters, datasetParams, allRecordsCV);
		writeBestErrorCurvesForProvidedRecords(bw, tuningParameters, datasetParams, allRecordsADTE);
		writeBestErrorCurvesForProvidedRecords(bw, tuningParameters, datasetParams, allRecordsCvEnsemble);
		bw.write("\t\\caption{" + datasetParams.fullName + ": Error Curves for best Constant (Left) and Variable (Right) GBMs with the lowest CV RMSE (Top), ATD Test RMSE (Middle), and ABT Test RMSE (Bottom)" + "}\n");
		bw.write("\t\\label{tab:" + datasetParams.minimalName + "errorCurves" + "}\n");
		bw.append("\\end{figure}\n\n");
	}
	
	private static void writeBestErrorCurvesForProvidedRecords(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams, ArrayList<RunDataSummaryRecord> records ) throws IOException {
		boolean constantErrorCurveDone = false, variableErrorCurveDone = false;
		StringBuilder constant = new StringBuilder(), variable = new StringBuilder();
		for (int i = 0; i < records.size(); i++) {
			String directory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/" + records.get(i).parameters.getRunDataSubDirectory();
			if (!constantErrorCurveDone && records.get(i).parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
				constant.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				constant.append("\t\t\\includegraphics{{" + (directory + "PerTreeRunDataGraphs/everyErrorCurve")  + "}.png}\n");
				constant.append("\t}\n");
				constantErrorCurveDone = true;
				// Ensure they have been generated
				new ErrorCurveScriptGenerator(datasetParams, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();		
				new ErrorCurveScriptExecutor(datasetParams, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();		
			}
			if (!variableErrorCurveDone && records.get(i).parameters.learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
				variable.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				variable.append("\t\t\\includegraphics{{" + (directory + "PerTreeRunDataGraphs/everyErrorCurve")  + "}.png}\n");
				variable.append("\t}\n");
				variableErrorCurveDone = true;
				// Ensure they have been generated
				new ErrorCurveScriptGenerator(datasetParams, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();		
				new ErrorCurveScriptExecutor(datasetParams, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();		
			}
			if (constantErrorCurveDone && variableErrorCurveDone) {
				break;
			}
		}		
		bw.append(constant.toString() + variable.toString());
	}
	
	private static void writeBestPredictionAndResidualCurves(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams) throws IOException {
		ArrayList<RunDataSummaryRecord> records = RunDataSummaryRecord.readRunDataSummaryRecords(tuningParameters.runDataProcessingDirectory + datasetParams.minimalName +  "/Averages/");
		Dataset ds = new Dataset(datasetParams);
		boolean constantErrorCurveDone = false, variableErrorCurveDone = false;
		StringBuilder constantPrediction = new StringBuilder(), variablePrediction = new StringBuilder();
		StringBuilder constantResidual = new StringBuilder(), variableResidual = new StringBuilder();
		bw.append("\\begin{figure}[!htb]\\centering\n");
		for (int i = 0; i < records.size(); i++) {
			String directory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/" + records.get(i).parameters.getRunDataSubDirectory();
			if (!constantErrorCurveDone && records.get(i).parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
				constantPrediction.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				constantPrediction.append("\t\t\\includegraphics{{" + (directory + "SortedPerExampleRunDataGraphs/ExampleNumberToPredictionGraphs/allPredictionCurves")  + "}.png}\n");
				constantPrediction.append("\t}\n");
				
				constantResidual.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				constantResidual.append("\t\t\\includegraphics{{" + (directory + "SortedPerExampleRunDataGraphs/ExampleNumberToPredictionGraphs/allResidualCurves")  + "}.png}\n");
				constantResidual.append("\t}\n");
				constantErrorCurveDone = true;
				//new PredictionGraphGenerator(ds, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch(), new String[][] {}).call();
				new SortedResponsePredictionGraphGenerator(ds, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();
			}
			if (!variableErrorCurveDone && records.get(i).parameters.learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
				variablePrediction.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				variablePrediction.append("\t\t\\includegraphics{{" + (directory + "SortedPerExampleRunDataGraphs/ExampleNumberToPredictionGraphs/allPredictionCurves")  + "}.png}\n");
				variablePrediction.append("\t}\n");
				
				variableResidual.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				variableResidual.append("\t\t\\includegraphics{{" + (directory + "SortedPerExampleRunDataGraphs/ExampleNumberToPredictionGraphs/allResidualCurves")  + "}.png}\n");
				variableResidual.append("\t}\n");
				variableErrorCurveDone = true;
				//new PredictionGraphGenerator(ds, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch(), new String[][] {}).call();
				new SortedResponsePredictionGraphGenerator(ds, records.get(i).parameters, tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/Averages/", tuningParameters, 0, new StopWatch()).call();

			}
			if (constantErrorCurveDone && variableErrorCurveDone) {
				break;
			}
		}		
		bw.append(constantPrediction.toString() + variablePrediction.toString() + constantResidual.toString() + variableResidual.toString());
		bw.write("\t\\caption{" + datasetParams.fullName + ": Predictions and Residuals for best Constant (Left) and Variable (Right) GBMs with the lowest CV RMSE (Top), ATD Test RMSE (Middle), and ABT Test RMSE (Bottom)" + "}\n");
		bw.write("\t\\label{tab:" + datasetParams.minimalName + "errorCurves" + "}\n");
		bw.append("\\end{figure}\n\n");
	}
	
	private static void writeRunDataSummaryCurves(BufferedWriter bw, ParameterTuningParameters tuningParameters, DatasetParameters datasetParams) throws IOException {
		String topDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName +  "/Averages/graphs/NoFilter/";
		GraphableProperty[] xAxes = AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.getXAxes();
		GraphableProperty[] yAxes = AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.getYAxes();

		for (GraphableProperty y : yAxes) {
			bw.append("\\begin{figure}[!htb]\\centering\n");
			for (GraphableProperty x : xAxes) {
				String directory = topDirectory + AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.convertGraphablePropertyAxesArrayToMinimalString(new GraphableProperty[] {x, y}) + "/";
				String filePrefix = null;
				
				if (new File(directory + "AllAllPoints.png").exists()) {
					filePrefix = "All";
				} else if (new File(directory + "ConstAllPoints.png").exists()) {
					filePrefix = "Const";
				} else if (new File(directory + "VarAllPoints.png").exists()) {
					filePrefix = "Var";
				} else {
					System.err.println("No graphs exists for directory " + directory);
					continue;
				}
				bw.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				bw.append("\t\t\\includegraphics{{" + (directory + filePrefix + "UniquePoints")  + "}.png}\n");
				bw.append("\t}\n");
				//bw.append("\t\\unskip\\ \\vrule\\");
				bw.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				bw.append("\t\t\\includegraphics{{" + (directory + filePrefix + "AllPoints")  + "}.png}\n");
				bw.append("\t}\n");
				//bw.append("\t\\unskip\\ \\hrule\\");
			}		
			bw.write("\t\\caption{" + datasetParams.fullName + ": Parameters vs. " + y.toString()  + "}\n");
			bw.write("\t\\label{tab:" + datasetParams.minimalName + "parametersVs" + y.name() + "}\n");
			bw.append("\\end{figure}\n\n");
		}
	}
	
	private static void writeAverageRunDataSummaryCurves(BufferedWriter bw, ParameterTuningParameters tuningParameters) throws IOException {
		String topDirectory = tuningParameters.runDataProcessingDirectory + "avgSummaryGraphs/NoFilter/";
		GraphableProperty[] xAxes = AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.getXAxes();
		GraphableProperty[] yAxes = AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.getYAxes();
		
		ArrayList<GraphableProperty[]> additionalAxes = AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.getAdditionalAxes();

		bw.append("\\begin{figure}[!htb]\\centering\n");
		for (GraphableProperty[] add : additionalAxes) {
			String directory = topDirectory + AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.convertGraphablePropertyAxesArrayToMinimalString(add) + "/";
			String filePrefix = null;
			
			if (new File(directory + "AllAllPoints.png").exists()) {
				filePrefix = "All";
			} else if (new File(directory + "ConstAllPoints.png").exists()) {
				filePrefix = "Const";
			} else if (new File(directory + "VarAllPoints.png").exists()) {
				filePrefix = "Var";
			} else {
				System.err.println("No graphs exists for directory " + directory);
				continue;
			}
			bw.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
			bw.append("\t\t\\includegraphics{{" + (directory + filePrefix + "AllPoints")  + "}.png}\n");
			bw.append("\t}\n");
		}
		bw.write("\t\\caption{Cross Validation Error vs. Generalization Error}\n");
		bw.write("\t\\label{fig:cvVsGeneralization}\n");
		bw.append("\\end{figure}\n\n");
		
		for (GraphableProperty y : yAxes) {
			bw.append("\\begin{figure}[!htb]\\centering\n");
			for (GraphableProperty x : xAxes) {
				String directory = topDirectory + AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator.convertGraphablePropertyAxesArrayToMinimalString(new GraphableProperty[] {x, y}) + "/";
				String filePrefix = null;
				
				if (new File(directory + "AllAllPoints.png").exists()) {
					filePrefix = "All";
				} else if (new File(directory + "ConstAllPoints.png").exists()) {
					filePrefix = "Const";
				} else if (new File(directory + "VarAllPoints.png").exists()) {
					filePrefix = "Var";
				} else {
					System.err.println("No graphs exists for directory " + directory);
					continue;
				}
				bw.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				bw.append("\t\t\\includegraphics{{" + (directory + filePrefix + "AllPoints")  + "}.png}\n");
				bw.append("\t}\n");
				//bw.append("\t\\unskip\\ \\vrule\\");
				bw.append("\t\\resizebox{0.49\\linewidth}{!}{\n");
				bw.append("\t\t\\includegraphics{{" + (directory + filePrefix + "UniquePoints")  + "}.png}\n");
				bw.append("\t}\n");
				//bw.append("\t\\unskip\\ \\hrule\\");
			}		
			bw.write("\t\\caption{Parameters vs. " + y.toString()  + "}\n");
			bw.write("\t\\label{fig:parametersVs" + y.name() + "}\n");
			bw.append("\\end{figure}\n\n");
		}

	}
	
	//-----------------------------------------HELPERS------------------------------------------------------------------
	private static String getNBestOverallColumns(List<RunDataSummaryRecord> records, int n) {
		StringBuilder bestRows = new StringBuilder();

		bestRows.append(getRowFromBestNRecords(RowType.LearningRatePolicy, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MinShrinkage, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MaxShrinkage, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.BagFraction, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MinExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MaxSplits, records, 5));

		bestRows.append("\t\t \\hline \n");
		
		bestRows.append(getRowFromBestNRecords(RowType.TimeInSeconds, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.Trees, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.CV_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ATD_Test_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ABT_Test_Error, records, 5));
	
		/*
		bestRows.append("\t\t \\hline \n");
		bestRows.append(getRowFromBestNRecords(RowType.AvgExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.AvgSplits, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevSplits, records, 5));
		if (lrPolicy == LearningRatePolicy.REVISED_VARIABLE) {
			bestRows.append(getRowFromBestNRecords(RowType.AvgShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.StdDevShrinkage, records, 5));
		}
		*/

		return bestRows.toString();
	}
	
	private static String getNBestConstantAndVariableColumns(ErrorType errorType, List<RunDataSummaryRecord> records, int n) {
		StringBuilder bestRows = new StringBuilder();

		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.LearningRatePolicy, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.MinShrinkage, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.MaxShrinkage, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.BagFraction, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.MinExamplesInNode, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.MaxSplits, records, n));

		//bestRows.append("\t\t \\hline \n");
		//bestRows.append("\t\t \\multicolumn{4}{c}{} \\\\ \n");
		
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.TimeInSeconds, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.Trees, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.CV_Error, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.ATD_Test_Error, records, n));
		bestRows.append(getRowFromBestNConstantAndVariableRecords(errorType, RowType.ABT_Test_Error, records, n));
	
		/*
		bestRows.append("\t\t \\hline \n");
		bestRows.append(getRowFromBestNRecords(RowType.AvgExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.AvgSplits, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevSplits, records, 5));
		if (lrPolicy == LearningRatePolicy.REVISED_VARIABLE) {
			bestRows.append(getRowFromBestNRecords(RowType.AvgShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.StdDevShrinkage, records, 5));
		}
		*/

		return bestRows.toString();
	}
	
	private static String getBestAndWorstConstantAndVariableColumns(ErrorType errorType, List<RunDataSummaryRecord> records) {
		StringBuilder bestRows = new StringBuilder();

		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.LearningRatePolicy, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.MinShrinkage, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.MaxShrinkage, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.BagFraction, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.MinExamplesInNode, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.MaxSplits, records));

		bestRows.append("\t\t \\hline \n");
		
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.TimeInSeconds, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.Trees, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.CV_Error, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.ATD_Test_Error, records));
		bestRows.append(getRowFromBestAndWorstConstantAndVariableRecords(errorType, RowType.ABT_Test_Error, records));
	
		/*
		bestRows.append("\t\t \\hline \n");
		bestRows.append(getRowFromBestNRecords(RowType.AvgExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.AvgSplits, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevSplits, records, 5));
		if (lrPolicy == LearningRatePolicy.REVISED_VARIABLE) {
			bestRows.append(getRowFromBestNRecords(RowType.AvgShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.StdDevShrinkage, records, 5));
		}
		*/

		return bestRows.toString();
	}

	
	
	private static String getNBestColumns(List<RunDataSummaryRecord> records, int n, LearningRatePolicy lrPolicy) {
		StringBuilder bestRows = new StringBuilder();
		bestRows.append(getRowFromBestNRecords(RowType.LearningRatePolicy, records, 5));
		if (lrPolicy == LearningRatePolicy.CONSTANT) {
			bestRows.append(getRowFromBestNRecords(RowType.Shrinkage, records, 5));
		} else {
			bestRows.append(getRowFromBestNRecords(RowType.MinShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.MaxShrinkage, records, 5));
		}
		bestRows.append(getRowFromBestNRecords(RowType.BagFraction, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MinExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.MaxSplits, records, 5));
		

		bestRows.append("\t\t \\hline \n");
		
		bestRows.append(getRowFromBestNRecords(RowType.TimeInSeconds, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.Trees, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.CV_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ATD_Test_Error, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.ABT_Test_Error, records, 5));
	
		/*
		bestRows.append("\t\t \\hline \n");
		bestRows.append(getRowFromBestNRecords(RowType.AvgExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevExamplesInNode, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.AvgSplits, records, 5));
		bestRows.append(getRowFromBestNRecords(RowType.StdDevSplits, records, 5));
		if (lrPolicy == LearningRatePolicy.REVISED_VARIABLE) {
			bestRows.append(getRowFromBestNRecords(RowType.AvgShrinkage, records, 5));
			bestRows.append(getRowFromBestNRecords(RowType.StdDevShrinkage, records, 5));
		}
		*/

		return bestRows.toString();
	}
	
	private enum RowType {
		TimeInSeconds("RunningTime (seconds)"), Trees("Optimal Num. of Trees"), Shrinkage, 
		MinShrinkage("Min Shrinkage"), MaxShrinkage ("Max Shrinkage"), 
		BagFraction("Bag Fraction"), MinExamplesInNode ("Min Leaf Size"), 
		AvgExamplesInNode("Avg. Leaf Size"), StdDevExamplesInNode("Std. Dev. Leaf Size"), 
		MaxSplits("MaxSplits"), AvgSplits("Avg. Splits"), StdDevSplits("Std. Dev. Splits"), 
		AvgShrinkage("Avg. Shrinkage"), StdDevShrinkage("Std. Dev. Shrinkage"), 
		ATD_Test_Error("ATD Test RMSE"), ABT_Test_Error("ABT Test RMSE"), CV_Error("Cross Validation RMSE"),
		LearningRatePolicy("Shrinkage Type");
		
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
	
	private static double getAverageOfFiniteElements(double[] array) {
		double avg = 0;
		int count = array.length;
		for (int i = 0; i < array.length; i++){
			if (Double.isInfinite(array[i])) {
				count--;
			} else {
				avg += array[i];
			}
		} 
		if (count > 0) {
			return avg / count;
		} else {
			return Double.POSITIVE_INFINITY;
		}
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
					if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
						values[i] = Double.POSITIVE_INFINITY;
						formattedValues[i] = "-";
					} else {
						values[i] = record.parameters.minLearningRate;
					}
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
				case LearningRatePolicy:
					highlightMinAndMaxValue = false;
					values[i] = Double.POSITIVE_INFINITY; 
					formattedValues[i] = record.parameters.learningRatePolicy.toString();
				default:
					break;
			}
			if (Double.isNaN(values[i]) && rowType.name().contains("StdDev") || values[i] * 10000 < 1) {
				values[i] = 0; // StdDev formula breaks down when value is practically zero but not quite due to rounding
			}
			// Ugly way of handling LR Policy
			if (Double.isFinite(values[i])) {
				formattedValues[i] = (isInteger) ? String.valueOf(values[i]) : (String.format("%f", values[i])).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			}
			
		
		}
		double avg = getAverageOfFiniteElements(values);
		if (Double.isInfinite(avg)) {
			formattedValues[n] = "-";
		} else {
			formattedValues[n] = (String.format("%f", avg)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
		}
		int minIndex = getIndexWithMin(values), maxIndex = getIndexWithMax(values);
		for (int j = 0; j < formattedValues.length; j++) {
			if (j == minIndex && highlightMinAndMaxValue) {
				//retval.append("\\cellcolor{gray!25}" + formattedValues[j]);
				retval.append("\\textbf{" + formattedValues[j] + "}");
			/*
			} else if (j == maxIndex && highlightMinAndMaxValue){
				retval.append("\\cellcolor{gray!60}" + formattedValues[j]);
			*/
			} else {
				retval.append(formattedValues[j]);
			}
			if (j != formattedValues.length-1) {
				retval.append(" & ");
			}
		}
		return retval.toString() + "\\\\ \\hline \n ";
	}
	
	/**
	 * Find n best constant and n best variable and % improvement using variable
	 * @param rowType
	 * @param records
	 * @param n
	 * @return
	 */
	private static String getRowFromBestNConstantAndVariableRecords(ErrorType errorType, RowType rowType, List<RunDataSummaryRecord> records, int n) {
		StringBuilder retval = new StringBuilder();
		retval.append("\t\t\\rule{0pt}{2ex} " + rowType + " & ");
		double[] values = new double[n*2];
		String[] formattedValues = new String[(n*2)+1];
		boolean highlightMinAndMaxValue = true;
		
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(records);
		
		
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(records);

		switch (errorType) {
			case ABT:
				constantRecords.sort(new CvEnsembleErrorComparator());
				variableRecords.sort(new CvEnsembleErrorComparator());
				break;
			case ATD:
				constantRecords.sort(new AllDataTestErrorComparator());
				variableRecords.sort(new AllDataTestErrorComparator());
				break;
			case CV:
				constantRecords.sort(new CvValidationErrorComparator());
				variableRecords.sort(new CvValidationErrorComparator());
				break;
		}
		
		for (int i = 0; i < n*2; i++) {
			RunDataSummaryRecord record;
			if (i < n) {
				record = constantRecords.get(i);
			} else {
				record = variableRecords.get(i);
			}
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
					if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
						values[i] = Double.POSITIVE_INFINITY;
						formattedValues[i] = "-";
					} else {
						values[i] = record.parameters.minLearningRate;
					}
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
				case LearningRatePolicy:
					highlightMinAndMaxValue = false;
					values[i] = Double.POSITIVE_INFINITY; 
					formattedValues[i] = record.parameters.learningRatePolicy.toString();
				default:
					break;
			}
			if (Double.isNaN(values[i]) && rowType.name().contains("StdDev") || values[i] * 10000 < 1) {
				values[i] = 0; // StdDev formula breaks down when value is practically zero but not quite due to rounding
			}
			// Ugly way of handling LR Policy
			if (Double.isFinite(values[i])) {
				formattedValues[i] = (isInteger) ? String.valueOf(values[i]) : (String.format("%f", values[i])).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			}
		}
		HashSet<RowType> percentImprRowTypes = new HashSet<RowType>();
		percentImprRowTypes.add(RowType.ABT_Test_Error);
		percentImprRowTypes.add(RowType.ATD_Test_Error);
		percentImprRowTypes.add(RowType.CV_Error);
		percentImprRowTypes.add(RowType.TimeInSeconds);
		percentImprRowTypes.add(RowType.Trees);

		if (percentImprRowTypes.contains(rowType)) {
			double impr = getPercentImprovementUsingVariable(values);
			if (impr >= 0) {
				formattedValues[n*2] = (String.format("\\textbf{%.2f}", impr)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			} else {
				formattedValues[n*2] = (String.format("%.2f", impr)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			}
		} else {
			formattedValues[n*2] = "-";
		}
		for (int j = 0; j < formattedValues.length; j++) {
			retval.append(formattedValues[j]);
			if (j != formattedValues.length-1) {
				retval.append(" & ");
			}
		}
		return retval.toString() + "\\\\ \\hline \n ";
	}
	
	/**
	 * Find n best constant and n best variable and % improvement using variable
	 * @param rowType
	 * @param records
	 * @param n
	 * @return
	 */
	private static String getRowFromBestAndWorstConstantAndVariableRecords(ErrorType errorType, RowType rowType, List<RunDataSummaryRecord> records) {
		StringBuilder retval = new StringBuilder();
		retval.append("\t\t\\rule{0pt}{2ex} " + rowType + " & ");
		double[] values = new double[4];
		String[] formattedValues = new String[6];
		boolean highlightMinAndMaxValue = true;
		
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(records);
		
		
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(records);

		switch (errorType) {
			case ABT:
				constantRecords.sort(new CvEnsembleErrorComparator());
				variableRecords.sort(new CvEnsembleErrorComparator());
				break;
			case ATD:
				constantRecords.sort(new AllDataTestErrorComparator());
				variableRecords.sort(new AllDataTestErrorComparator());
				break;
			case CV:
				constantRecords.sort(new CvValidationErrorComparator());
				variableRecords.sort(new CvValidationErrorComparator());
				break;
		}
		
		for (int i = 0; i < 4; i++) {
			RunDataSummaryRecord record = null;
			switch (i) {
				case 0:
					record = constantRecords.get(0);
					break;
				case 1:
					record = variableRecords.get(0);
					break;
				case 2:
					record = constantRecords.get(constantRecords.size()-1);
					break;
				case 3:
					record = variableRecords.get(variableRecords.size()-1);
					break;
			}
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
					if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
						values[i] = Double.POSITIVE_INFINITY;
						if (i < 2) {
							formattedValues[i] = "-";
						} else {
							formattedValues[i+1] = "-";
						}
					} else {
						values[i] = record.parameters.minLearningRate;
					}
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
				case LearningRatePolicy:
					highlightMinAndMaxValue = false;
					values[i] = Double.POSITIVE_INFINITY; 
					if (i < 2) {
						formattedValues[i] = record.parameters.learningRatePolicy.toString();
					} else {
						formattedValues[i+1] = record.parameters.learningRatePolicy.toString();
					}
				default:
					break;
			}
			if (Double.isNaN(values[i]) && rowType.name().contains("StdDev") || values[i] * 10000 < 1) {
				values[i] = 0; // StdDev formula breaks down when value is practically zero but not quite due to rounding
			}
			// Ugly way of handling LR Policy
			if (Double.isFinite(values[i])) {
				if (i < 2) {
					formattedValues[i] = (isInteger) ? String.valueOf(values[i]) : (String.format("%f", values[i])).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
				} else {
					formattedValues[i+1] = (isInteger) ? String.valueOf(values[i]) : (String.format("%f", values[i])).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
				}
			}
		}
		HashSet<RowType> percentImprRowTypes = new HashSet<RowType>();
		percentImprRowTypes.add(RowType.ABT_Test_Error);
		percentImprRowTypes.add(RowType.ATD_Test_Error);
		percentImprRowTypes.add(RowType.CV_Error);
		percentImprRowTypes.add(RowType.TimeInSeconds);
		percentImprRowTypes.add(RowType.Trees);

		if (percentImprRowTypes.contains(rowType)) {
			double bestDiff = values[1] - values[0];
			double worstDiff = values[3] - values[2];
			formattedValues[2] = (String.format("%f", bestDiff)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			formattedValues[5] = (String.format("%f", worstDiff)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
			if (bestDiff <= 0)
			{
				formattedValues[1] = "\\cellcolor{gray!25}" + formattedValues[1];
			} else {
				formattedValues[0] = "\\cellcolor{gray!25}" + formattedValues[0];
			}
			if (worstDiff <= 0)
			{
				formattedValues[4] = "\\cellcolor{gray!25}" + formattedValues[4];
			} else {
				formattedValues[3] = "\\cellcolor{gray!25}" + formattedValues[3];
			}
		} else {
			formattedValues[2] = "-";
			formattedValues[5] = "-";
		}
		
		for (int j = 0; j < formattedValues.length; j++) {
			if (formattedValues[j] != null) {
				retval.append(formattedValues[j]);
			} else {
				retval.append("-");
			}
			
			if (j != formattedValues.length-1) {
				retval.append(" & ");
			}
		}
		return retval.toString() + "\\\\ \\hline \n ";
	}
	
	private static double getPercentImprovementUsingVariable(double[] values) {
		// First half are constant, second is variable
		double impr = 0;
		double avgConst = 0, avgVar = 0;
		for (int i = 0; i < values.length; i++) {
			if ( i < values.length / 2) {
				avgConst += values[i];
			} else {
				avgVar += values[i];
			}
		}
		avgConst /= values.length / 2;
		avgVar /= values.length / 2;
		
		impr = ((avgConst - avgVar) / avgConst) * 100;
		return impr;
	}
}
