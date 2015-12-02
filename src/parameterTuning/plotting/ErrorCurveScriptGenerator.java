package parameterTuning.plotting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import com.google.common.primitives.Doubles;

import dataset.DatasetParameters;
import gbm.GbmParameters;
import parameterTuning.ParameterTuningParameters;
import parameterTuning.RunDataSummaryRecord;
import utilities.CommandLineExecutor;
import utilities.SimpleHostLock;
import utilities.StopWatch;

public class ErrorCurveScriptGenerator implements Callable<Void>{
	DatasetParameters datasetParams;
	GbmParameters parameters;
	String runDataFullDirectory;
	ParameterTuningParameters tuningParameters;
	int submissionNumber;
	StopWatch globalTimer;
	
	public ErrorCurveScriptGenerator(DatasetParameters datasetParams, GbmParameters parameters, String runDataFullDirectory, ParameterTuningParameters tuningParameters, int submissionNumber, StopWatch globalTimer) {
		this.datasetParams = datasetParams;
		this.parameters = parameters;
		this.runDataFullDirectory = runDataFullDirectory;
		this.tuningParameters = tuningParameters;
		this.submissionNumber = submissionNumber;
		this.globalTimer = globalTimer;
	}
	/**
	 * Return path to mathematica file containing learning curve code.
	 * @param datasetParams
	 * @param runDataFullDirectory
	 * @param parameters
	 * @param expectedRunFileType
	 * @return
	 */
	public Void call() {
		StopWatch timer = new StopWatch().start(), perTaskTimer = new StopWatch().start();
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/ErrorCurveGenerator/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "errorCurveGenerator--doneLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already generated error curve runData for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + "errorCurveGenerator--hostLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Another host claimed generation of error curve script for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		
		double maxRMSE = Double.MIN_VALUE, maxExamplesInNode = Double.MIN_VALUE;

		//StringBuilder CvTrainingErrorByIteration = new StringBuilder();
		StringBuilder CvValidationErrorByIteration = new StringBuilder();
		//StringBuilder CvTestErrorByIteration = new StringBuilder();
		StringBuilder allDataTrainingErrorByIteration  = new StringBuilder();
		StringBuilder allDataTestErrorByIteration  = new StringBuilder();
		StringBuilder CvEnsembleTrainingErrorByIteration  = new StringBuilder();
		StringBuilder CvEnsembleTestErrorByIteration  = new StringBuilder();
		
		StringBuilder exampleInNodeMeanByIteration  = new StringBuilder();
		StringBuilder exampleInNodeStdDevByIteration  = new StringBuilder();
		StringBuilder learningRateMeanByIteration  = new StringBuilder();
		StringBuilder learningRateStdDevByIteration  = new StringBuilder();
		StringBuilder actualNumberOfSplitsByIteration  = new StringBuilder();
		
		// Read through all the files cooresponding to these parameters and average the data.
		RunDataSummaryRecord record = RunDataSummaryRecord.readRunDataSummaryRecordFromRunDataFile(runDataFullDirectory, parameters, tuningParameters.runFileType);
		if (record == null) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Run Data Not Found! Failed to generate error curve runData for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			SimpleHostLock.writeDoneLock(locksDir + "errorCurveGenerator--doneLock.txt");
			return null;
		}
		String runDataFilePath = runDataFullDirectory + parameters.getRunDataSubDirectory(record.runFileType) + parameters.getFileNamePrefix(record.runFileType)  + "--runData.txt";
		int treeNum = 1;
		try {
			BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
			// skip summary info, relative influences, and header
			while (!(br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
			// read in error data 
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] components = line.split("\t");
				double[] tmpErrors = {
						Double.parseDouble(components[1].trim()),
						Double.parseDouble(components[2].trim()),
						Double.parseDouble(components[3].trim()),
						Double.parseDouble(components[4].trim()),
						Double.parseDouble(components[5].trim()),
						Double.parseDouble(components[6].trim()),
						Double.parseDouble(components[7].trim()),
				};
				double[] tmpExamplesInNode = {
						Double.parseDouble(components[8].trim()),
						Double.parseDouble(components[9].trim())
				};
				maxRMSE = Math.max(maxRMSE, Doubles.max(tmpErrors));
				maxExamplesInNode = Math.max(maxExamplesInNode, Doubles.max(tmpExamplesInNode));

				//MathematicaListCreator.addToMathematicaList(treeNum, components[1], CvTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[2], CvValidationErrorByIteration);
				//MathematicaListCreator.addToMathematicaList(treeNum, components[3], CvTestErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[4], allDataTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[5], allDataTestErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[6], CvEnsembleTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[7], CvEnsembleTestErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[8], exampleInNodeMeanByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[9], exampleInNodeStdDevByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[10], learningRateMeanByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[11], learningRateStdDevByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[12], actualNumberOfSplitsByIteration);
				
				treeNum++;
			}
			br.close();
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Reading of per tree run data failed! Failed to generate error curve for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		perTaskTimer.printMessageWithTime(String.format("[%s] Finished reading error/metadata in for %s (%d out of %d)", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests));
		perTaskTimer.start();
		
		String baseFileDirectory = (runDataFullDirectory + parameters.getRunDataSubDirectory( tuningParameters.runFileType)).replace("\\", "/");

		String generatedGraphsFullPathNoExtension = baseFileDirectory + "/PerTreeRunDataGraphs/";
		new File(generatedGraphsFullPathNoExtension).mkdirs();
		String mathematicaFileName = "mathematica.m";
		String mathematicaFileFullPath = baseFileDirectory + mathematicaFileName;
		
		String latexFileNameFullPath = generatedGraphsFullPathNoExtension + "latexCode.txt";
		
		StringBuffer saveToFile = new StringBuffer();
		StringBuffer latexCode = new StringBuffer();
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "allDataAndCvEnsembleErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", allDataAndCvEnsembleErrorCurve, ImageResolution -> 300]\n\n");
		
		//saveToFile.append("(*\n");
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "everyErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", everyErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "allDataErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", allDataErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "CvEnsembleErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", CvEnsembleErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "examplesInNodeCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", examplesInNodeCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "learningRateCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", learningRateCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "splitsCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", splitsCurve, ImageResolution -> 300]\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "allDataAndCvEnsembleErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Training Data And CvEnsemble") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllTrainingDataAndCvEnsemble")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "everyErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Plots") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllPlots")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "allDataErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Training Data") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllTrainingData")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "CvEnsembleErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("CvEnsemble") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("CvEnsemble")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "examplesInNodeCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Examples In Node") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualExamplesInNode")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "learningRateCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Learning Rates") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualLearningRates")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\linewidth]{{" + (generatedGraphsFullPathNoExtension + "splitsCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Number Of Splits") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualNumberOfSplits")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		try {
			BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicaFileFullPath)));
			BufferedWriter latex = new BufferedWriter(new PrintWriter(new File(latexFileNameFullPath)));
			//mathematica.write("CvTrainingError = " + MathematicaListCreator.closeOutMathematicaList(CvTrainingErrorByIteration) + "\n");
			mathematica.write("CvValidationError = " + MathematicaListCreator.closeOutMathematicaList(CvValidationErrorByIteration) + "\n");
			//mathematica.write("CvTestError = " + MathematicaListCreator.closeOutMathematicaList(CvTestErrorByIteration) + "\n");
			mathematica.write("allDataTrainingError = " + MathematicaListCreator.closeOutMathematicaList(allDataTrainingErrorByIteration) + "\n");
			mathematica.write("allDataTestError = " + MathematicaListCreator.closeOutMathematicaList(allDataTestErrorByIteration) + "\n");
			mathematica.write("CvEnsembleTrainingError = " + MathematicaListCreator.closeOutMathematicaList(CvEnsembleTrainingErrorByIteration) + "\n");
			mathematica.write("CvEnsembleTestError = " + MathematicaListCreator.closeOutMathematicaList(CvEnsembleTestErrorByIteration) + "\n");
			
			mathematica.write("exampleInNodeMean = " + MathematicaListCreator.closeOutMathematicaList(exampleInNodeMeanByIteration) + "\n");
			mathematica.write("exampleInNodeStdDev = " + MathematicaListCreator.closeOutMathematicaList(exampleInNodeStdDevByIteration) + "\n");
			mathematica.write("learningRateMean = " + MathematicaListCreator.closeOutMathematicaList(learningRateMeanByIteration) + "\n");
			mathematica.write("learningRateStdDev = " + MathematicaListCreator.closeOutMathematicaList(learningRateStdDevByIteration) + "\n");
			mathematica.write("actualNumberOfSplits = " + MathematicaListCreator.closeOutMathematicaList(actualNumberOfSplitsByIteration) + "\n");
			
			double minONOT = Doubles.min(record.optimalNumberOfTreesFoundinEachRun);
			double maxONOT = Doubles.max(record.optimalNumberOfTreesFoundinEachRun);
			if (minONOT == maxONOT) { minONOT-=0.1; }
			mathematica.write("optimalNumberOfTreesUpperBound = {{" + minONOT + ", " + (maxRMSE + 1) +"}, {" + maxONOT + ", " + (maxRMSE+1) + "}}\n");
			mathematica.write("OptimalNumberOfTrees = {{" + record.optimalNumberOfTrees + ", " + 0 +"}, {" + record.optimalNumberOfTrees + ", " + maxRMSE + "}}\n");
			
			
			mathematica.write("cvValidation = ListLinePlot[{CvValidationError}"
					//+ ", PlotLegends -> {\"Cross Validation\"}"
					+ ", PlotStyle -> {{Black}}"
					+ getFrame("Actual Number Of Splits")
					+ ", Epilog -> {Text[Style[\"" + parameters.getLineSeparatedPrintOut() + "\", TextAlignment->Left], Scaled[{" + 0.7  + ", " + 0.8 + "}]]}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");

			mathematica.write("allDataTraining = ListLinePlot[{allDataTrainingError}"
					//+ ", PlotLegends -> {\"ATD Training\"}"
					+ ", PlotStyle -> {{Lighter[Red]}}"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("allDataTest = ListLinePlot[{allDataTestError}"
					//+ ", PlotLegends -> {\"ATD Test\"}"
					+ ", PlotStyle -> {{Darker[Red]}}"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("CvEnsembleTraining = ListLinePlot[{CvEnsembleTrainingError}"
					//+ ", PlotLegends -> {\"ABT Training\"}"
					+ ", PlotStyle -> {{Cyan}}"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("CvEnsembleTest = ListLinePlot[{CvEnsembleTestError}"
					//+ ", PlotLegends -> {\"ABT Test\"}"
					+ ", PlotStyle -> {{Darker[Blue]}}"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("optimalNumberOfTrees = ListLinePlot[{OptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					//+ ", PlotLegends -> {\"Optimal Number Of Trees\"}"
					+ ", PlotStyle -> {Green, Green}"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {2 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
			

			mathematica.write("allDataErrorCurve = Show[cvValidation, allDataTraining, allDataTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("CvEnsembleErrorCurve = Show[cvValidation, CvEnsembleTraining, CvEnsembleTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("everyErrorCurve = Show[cvValidation, allDataTraining, allDataTest, CvEnsembleTraining, CvEnsembleTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
					
			mathematica.write("examplesInNodeCurve = ListLinePlot[{exampleInNodeMean,exampleInNodeStdDev}"
					+ ", PlotLegends -> {\"Leaf Size Std. Dev\", \"Avg. Leaf Size\"}"
					+ ", PlotStyle -> {{Darker[Green]}, {Darker[Blue]}}"
					+ getFrame("Leaf Size")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxExamplesInNode + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("learningRateCurve = ListLinePlot[{learningRateMean, learningRateStdDev}"
					+ ", PlotLegends -> {\"Shrinkage Std. Dev.\", \"Avg. Shrinkage\"}"
					+ ", PlotStyle -> {{Darker[Green]}, {Darker[Blue]}}"
					+ getFrame("Learning Rate")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + (parameters.maxLearningRate + 0.1) + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("splitsCurve = ListLinePlot[{actualNumberOfSplits}"
					+ ", PlotStyle -> Darker[Blue]"
					+ getFrame("Actual Number Of Splits")
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + (parameters.maxNumberOfSplits + 1) + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
						
			mathematica.write(saveToFile.toString());
			
			latex.write(latexCode.toString());
			mathematica.flush();
			mathematica.close();
			latex.flush();
			latex.close();
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Successfully generated error curve for run data for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, perTaskTimer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Writing of error curve to file Failed! Failed to generate error curve for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, perTaskTimer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		//new ErrorCurveScriptExecutor(datasetParams, parameters, runDataFullDirectory, tuningParameters, submissionNumber, globalTimer).call();
		SimpleHostLock.writeDoneLock(locksDir + "errorCurveGenerator--doneLock.txt");
		return null;
	}
	
	public static void generateAndExecutePlotLegend(ParameterTuningParameters tuningParameters) {
		String file = tuningParameters.runDataProcessingDirectory + "/errorCurveLegend";
		
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(file + ".m"));
			bw.append("errorCurveLegend = LineLegend[{Black, Lighter[Red], Darker[Red], Cyan, Darker[Blue], Green}, {\"Cross Validation\", \"ATD Training\", \"ATD Test\", \"ABT Training\", \"ABT Test\", \"Optimal Number of Trees\"}]\n\n");
			bw.append("fileName = \"" + (tuningParameters.runDataProcessingDirectory + "errorCurveLegend")  + "\"\n");
			bw.append("Export[fileName <> \".png\", errorCurveLegend, ImageResolution -> 300]\n\n");
			bw.flush();
			bw.close();
			CommandLineExecutor.executeMathematicaScript(tuningParameters.runDataProcessingDirectory, "errorCurveLegend.m");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private String getFrame(String yAxis) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(", Axes->False ");
		buffer.append(", Frame->True");
		buffer.append(", FrameStyle->Black");
		buffer.append(", FrameTicksStyle->Black");
		buffer.append(", LabelStyle->{Black, 12}");
		buffer.append(", FrameLabel->{\"Number of Trees\", \"" + yAxis +"\"}");
		buffer.append(", FrameTicks->{{Automatic, None}, {Automatic, None}} ");
		return buffer.toString();
	}
}
