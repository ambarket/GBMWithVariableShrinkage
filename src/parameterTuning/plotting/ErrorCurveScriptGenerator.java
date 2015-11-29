package parameterTuning.plotting;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import parameterTuning.ParameterTuningParameters;
import parameterTuning.RunDataSummaryRecord;
import utilities.SimpleHostLock;
import utilities.StopWatch;

import com.google.common.primitives.Doubles;

import dataset.DatasetParameters;

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
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/ErrorCurves/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "errorCurveLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already generated error curve runData for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		
		double maxRMSE = Double.MIN_VALUE, maxExamplesInNode = Double.MIN_VALUE;

		StringBuilder avgCvTrainingErrorByIteration = new StringBuilder();
		StringBuilder avgCvValidationErrorByIteration = new StringBuilder();
		StringBuilder avgCvTestErrorByIteration = new StringBuilder();
		StringBuilder allDataTrainingErrorByIteration  = new StringBuilder();
		StringBuilder allDataTestErrorByIteration  = new StringBuilder();
		StringBuilder cvEnsembleTrainingErrorByIteration  = new StringBuilder();
		StringBuilder cvEnsembleTestErrorByIteration  = new StringBuilder();
		
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
			SimpleHostLock.writeDoneLock(locksDir + "errorCurveLock.txt");
			return null;
		}
		String runDataFilePath = runDataFullDirectory + parameters.getRunDataSubDirectory(record.runFileType) + parameters.getFileNamePrefix(record.runFileType)  + "--runData.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
			// skip summary info, relative influences, and header
			while (!(br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
			// read in error data 
			String line = null;
			int treeNum = 1;
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

				MathematicaListCreator.addToMathematicaList(treeNum, components[1], avgCvTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[2], avgCvValidationErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[3], avgCvTestErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[4], allDataTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[5], allDataTestErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[6], cvEnsembleTrainingErrorByIteration);
				MathematicaListCreator.addToMathematicaList(treeNum, components[7], cvEnsembleTestErrorByIteration);
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
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "cvEnsembleErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", cvEnsembleErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "cvErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", cvErrorCurve, ImageResolution -> 300]\n\n");
		//saveToFile.append("*)\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "examplesInNodeCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", examplesInNodeCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "learningRateCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", learningRateCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName = \"" + (generatedGraphsFullPathNoExtension + "splitsCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", splitsCurve, ImageResolution -> 300]\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "allDataAndCvEnsembleErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Training Data And CvEnsemble") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllTrainingDataAndCvEnsemble")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "everyErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Plots") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllPlots")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "allDataErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("All Training Data") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AllTrainingData")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "cvEnsembleErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("CvEnsemble") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("CvEnsemble")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "cvErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getErrorCurveLatexCaption("Avg Cross Validation") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference("AvgCrossValidation")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "examplesInNodeCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Examples In Node") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualExamplesInNode")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "learningRateCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Learning Rates") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualLearningRates")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=\\lineWidth]{{" + (generatedGraphsFullPathNoExtension + "splitsCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fullName + " " + parameters.getMetaDataCurveLatexCaption("Actual Number Of Splits") + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference("ActualNumberOfSplits")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		try {
			BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicaFileFullPath)));
			BufferedWriter latex = new BufferedWriter(new PrintWriter(new File(latexFileNameFullPath)));
			mathematica.write("avgCvTrainingError = " + MathematicaListCreator.closeOutMathematicaList(avgCvTrainingErrorByIteration) + "\n");
			mathematica.write("avgCvValidationError = " + MathematicaListCreator.closeOutMathematicaList(avgCvValidationErrorByIteration) + "\n");
			mathematica.write("avgCvTestError = " + MathematicaListCreator.closeOutMathematicaList(avgCvTestErrorByIteration) + "\n");
			mathematica.write("allDataTrainingError = " + MathematicaListCreator.closeOutMathematicaList(allDataTrainingErrorByIteration) + "\n");
			mathematica.write("allDataTestError = " + MathematicaListCreator.closeOutMathematicaList(allDataTestErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTrainingError = " + MathematicaListCreator.closeOutMathematicaList(cvEnsembleTrainingErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTestError = " + MathematicaListCreator.closeOutMathematicaList(cvEnsembleTestErrorByIteration) + "\n");
			
			mathematica.write("exampleInNodeMean = " + MathematicaListCreator.closeOutMathematicaList(exampleInNodeMeanByIteration) + "\n");
			mathematica.write("exampleInNodeStdDev = " + MathematicaListCreator.closeOutMathematicaList(exampleInNodeStdDevByIteration) + "\n");
			mathematica.write("learningRateMean = " + MathematicaListCreator.closeOutMathematicaList(learningRateMeanByIteration) + "\n");
			mathematica.write("learningRateStdDev = " + MathematicaListCreator.closeOutMathematicaList(learningRateStdDevByIteration) + "\n");
			mathematica.write("actualNumberOfSplits = " + MathematicaListCreator.closeOutMathematicaList(actualNumberOfSplitsByIteration) + "\n");
			
			double minONOT = Doubles.min(record.optimalNumberOfTreesFoundinEachRun);
			double maxONOT = Doubles.max(record.optimalNumberOfTreesFoundinEachRun);
			if (minONOT == maxONOT) { minONOT-=0.1; }
			mathematica.write("optimalNumberOfTreesUpperBound = {{" + minONOT + ", " + (maxRMSE + 1) +"}, {" + maxONOT + ", " + (maxRMSE+1) + "}}\n");
			mathematica.write("avgOptimalNumberOfTrees = {{" + record.optimalNumberOfTrees + ", " + 0 +"}, {" + record.optimalNumberOfTrees + ", " + maxRMSE + "}}\n");
			
			
			mathematica.write("cvValidation = ListLinePlot[{avgCvValidationError}"
					+ ", PlotLegends -> {\"avgCvValidationError\"}"
					+ ", PlotStyle -> {{Black}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("cvTraining = ListLinePlot[{avgCvTrainingError}"
					+ ", PlotLegends -> {\"avgCvTrainingError\"}"
					+ ", PlotStyle -> {{Lighter[Orange]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("cvTest = ListLinePlot[{avgCvTestError}"
					+ ", PlotLegends -> {\"avgCvTrainingError\"}"
					+ ", PlotStyle -> {{Darker[Orange]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("allDataTraining = ListLinePlot[{allDataTrainingError}"
					+ ", PlotLegends -> {\"allDataTrainingError\"}"
					+ ", PlotStyle -> {{Lighter[Red]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("allDataTest = ListLinePlot[{allDataTestError}"
					+ ", PlotLegends -> {\"allDataTestError\"}"
					+ ", PlotStyle -> {{Darker[Red]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("cvEnsembleTraining = ListLinePlot[{cvEnsembleTrainingError}"
					+ ", PlotLegends -> {\"cvEnsembleTrainingError\"}"
					+ ", PlotStyle -> {{Cyan}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("cvEnsembleTest = ListLinePlot[{cvEnsembleTestError}"
					+ ", PlotLegends -> {\"cvEnsembleTestError\"}"
					+ ", PlotStyle -> {{Darker[Blue]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("optimalNumberOfTrees = ListLinePlot[{avgOptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					+ ", PlotLegends -> {\"avgOptimalNumberOfTrees\"}"
					+ ", PlotStyle -> {Green, Green}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {2 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
			

			mathematica.write("allDataErrorCurve = Show[cvValidation, allDataTraining, allDataTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("cvEnsembleErrorCurve = Show[cvValidation, cvEnsembleTraining, cvEnsembleTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("cvErrorCurve = Show[cvValidation, cvTraining, cvTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("allDataAndCvEnsembleErrorCurve = Show[cvValidation, allDataTraining, allDataTest, cvEnsembleTraining, cvEnsembleTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			mathematica.write("everyErrorCurve = Show[cvValidation, cvTraining, cvTest, allDataTraining, allDataTest, cvEnsembleTraining, cvEnsembleTest, optimalNumberOfTrees, PlotRange -> All]\n\n");
			
			mathematica.write("examplesInNodeCurve = ListLinePlot[{exampleInNodeMean,exampleInNodeStdDev}"
					+ ", PlotLegends -> {\"exampleInNodeStdDev\", \"exampleInNodeMean\"}"
					+ ", PlotStyle -> {{Darker[Green]}, {Darker[Blue]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"ExamplesInLeafNodes\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxExamplesInNode + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("learningRateCurve = ListLinePlot[{learningRateMean, learningRateStdDev}"
					+ ", PlotLegends -> {\"learningRateStdDev\", \"learningRateMean\"}"
					+ ", PlotStyle -> {{Darker[Green]}, {Darker[Blue]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"LearningRate\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + (parameters.maxLearningRate + 0.1) + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			mathematica.write("splitsCurve = ListLinePlot[{actualNumberOfSplits}"
					+ ", PlotLegends -> {\"actualNumberOfSplits\"}"
					+ ", PlotStyle -> Darker[Blue]"
					+ ", AxesLabel->{\"Number Of Trees\", \"ActualNumberOfSplits\"}"
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
		SimpleHostLock.writeDoneLock(locksDir + "errorCurveLock.txt");
		return null;
	}
}
