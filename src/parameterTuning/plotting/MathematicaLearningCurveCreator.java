package parameterTuning.plotting;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import com.google.common.primitives.Doubles;

import parameterTuning.ParameterTuningParameters;
import parameterTuning.RunDataSummaryRecord;
import utilities.CommandLineExecutor;
import utilities.DoubleCompare;
import utilities.RecursiveFileDeleter;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import dataset.DatasetParameters;

public class MathematicaLearningCurveCreator implements Callable<Void>{
	DatasetParameters datasetParams;
	GbmParameters parameters;
	String runDataFullDirectory;
	ParameterTuningParameters tuningParameters;
	int submissionNumber;
	StopWatch globalTimer;
	
	public MathematicaLearningCurveCreator(DatasetParameters datasetParams, GbmParameters parameters, String runDataFullDirectory, ParameterTuningParameters tuningParameters, int submissionNumber, StopWatch globalTimer) {
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
		StopWatch timer = new StopWatch().start();
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/ErrorCurves/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--errorCurveLock.txt")) {
			System.out.println(String.format("[%s] Already generated error curve runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}
		
		double maxRMSE = Double.MIN_VALUE, maxMetaData = Double.MIN_VALUE;
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTestErrorByIteration = new ArrayList<Double>();
		
		ArrayList<Double> exampleInNodeMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> exampleInNodeStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> actualNumberOfSplitsByIteration = new ArrayList<Double>();
		
		// Read through all the files cooresponding to these parameters and average the data.
		RunDataSummaryRecord record = RunDataSummaryRecord.readRunDataSummaryRecordFromRunDataFile(runDataFullDirectory, parameters, tuningParameters.runFileType);
		if (record == null) {
			System.out.println(String.format("[%s] Run Data Not Found! Failed to generate error curve runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--errorCurveLock.txt");
			return null;
		}
		String runDataFilePath = runDataFullDirectory + parameters.getRunDataSubDirectory(record.runFileType) + parameters.getFileNamePrefix(record.runFileType)  + "--runData.txt";
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
				double[] tmpMetaData = {
						Double.parseDouble(components[8].trim()),
						Double.parseDouble(components[9].trim()),
						Double.parseDouble(components[10].trim()) * 100,
						Double.parseDouble(components[11].trim()) * 100,
						Double.parseDouble(components[12].trim()) * 10,
				};
				maxRMSE = Math.max(maxRMSE, Doubles.max(tmpErrors));
				maxMetaData = Math.max(maxMetaData, Doubles.max(tmpMetaData));
				

				avgCvTrainingErrorByIteration.add(tmpErrors[0]);
				avgCvValidationErrorByIteration.add(tmpErrors[1]);
				avgCvTestErrorByIteration.add(tmpErrors[2]);
				allDataTrainingErrorByIteration.add(tmpErrors[3]);
				allDataTestErrorByIteration.add(tmpErrors[4]);
				cvEnsembleTrainingErrorByIteration.add(tmpErrors[5]);
				cvEnsembleTestErrorByIteration.add(tmpErrors[6]);
				
				exampleInNodeMeanByIteration.add(tmpMetaData[0]);
				exampleInNodeStdDevByIteration.add(tmpMetaData[1]);
				learningRateMeanByIteration.add(tmpMetaData[2]);
				learningRateStdDevByIteration.add(tmpMetaData[3]);
				actualNumberOfSplitsByIteration.add(tmpMetaData[4]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(String.format("[%s] Reading of per tree run data failed! Failed to generate error curve for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}

		String baseFileDirectory = (runDataFullDirectory + parameters.getRunDataSubDirectory( tuningParameters.runFileType)).replace("\\", "/");
		String baseFileNameNoExtension = parameters.getFileNamePrefix(tuningParameters.runFileType).replace("\\", "/");
		String baseFileFullPathNoExtension = baseFileDirectory + baseFileNameNoExtension;
		
		String mathematicaFileName = baseFileNameNoExtension + "--mathematica.m";
		String mathematicaFileFullPath = baseFileDirectory + mathematicaFileName;
		
		String latexFileNameFullPath = baseFileFullPathNoExtension + "--latexCode.txt";
		
		
		StringBuffer saveToFile = new StringBuffer();
		StringBuffer latexCode = new StringBuffer();
		
		saveToFile.append("fileName := \"" + (baseFileFullPathNoExtension + "--allErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", allErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName := \"" + (baseFileFullPathNoExtension + "--allDataErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", allDataErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName := \"" + (baseFileFullPathNoExtension + "--cvEnsembleErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", cvEnsembleErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName := \"" + (baseFileFullPathNoExtension + "--cvErrorCurve") + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", cvErrorCurve, ImageResolution -> 300]\n\n");
		
		saveToFile.append("fileName := \"" + (baseFileFullPathNoExtension + "--metaDataCurve")  + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", metaDataCurve, ImageResolution -> 300]\n\n");

		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + (baseFileFullPathNoExtension + "--allErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getErrorCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + (baseFileFullPathNoExtension + "--allDataErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getErrorCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + (baseFileFullPathNoExtension + "--cvEnsembleErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getErrorCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + (baseFileFullPathNoExtension + "--cvErrorCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getErrorCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getErrorCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + (baseFileFullPathNoExtension + "--metaDataCurve")  + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getMetaDataCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getMetaDataCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		try {
			BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicaFileFullPath)));
			BufferedWriter latex = new BufferedWriter(new PrintWriter(new File(latexFileNameFullPath)));
			mathematica.write("avgCvTrainingError := " + MathematicaListCreator.convertToMathematicaList(avgCvTrainingErrorByIteration) + "\n");
			mathematica.write("avgCvValidationError := " + MathematicaListCreator.convertToMathematicaList(avgCvValidationErrorByIteration) + "\n");
			mathematica.write("avgCvTestError := " + MathematicaListCreator.convertToMathematicaList(avgCvTestErrorByIteration) + "\n");
			mathematica.write("allDataTrainingError := " + MathematicaListCreator.convertToMathematicaList(allDataTrainingErrorByIteration) + "\n");
			mathematica.write("allDataTestError := " + MathematicaListCreator.convertToMathematicaList(allDataTestErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTrainingError := " + MathematicaListCreator.convertToMathematicaList(cvEnsembleTrainingErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTestError := " + MathematicaListCreator.convertToMathematicaList(cvEnsembleTestErrorByIteration) + "\n");
			
			mathematica.write("exampleInNodeMean := " + MathematicaListCreator.convertToMathematicaList(exampleInNodeMeanByIteration) + "\n");
			mathematica.write("exampleInNodeStdDev := " + MathematicaListCreator.convertToMathematicaList(exampleInNodeStdDevByIteration) + "\n");
			mathematica.write("learningRateMean := " + MathematicaListCreator.convertToMathematicaList(learningRateMeanByIteration) + "\n");
			mathematica.write("learningRateStdDev := " + MathematicaListCreator.convertToMathematicaList(learningRateStdDevByIteration) + "\n");
			mathematica.write("actualNumberOfSplits := " + MathematicaListCreator.convertToMathematicaList(actualNumberOfSplitsByIteration) + "\n");
			
			double minONOT = Doubles.min(record.optimalNumberOfTreesFoundinEachRun);
			double maxONOT = Doubles.max(record.optimalNumberOfTreesFoundinEachRun);
			if (minONOT == maxONOT) { minONOT-=0.1; }
			mathematica.write("optimalNumberOfTreesUpperBound := {{" + minONOT + ", " + (maxRMSE + 1) +"}, {" + maxONOT + ", " + (maxRMSE+1) + "}}\n");
			mathematica.write("avgOptimalNumberOfTrees := {{" + record.optimalNumberOfTrees + ", " + 0 +"}, {" + record.optimalNumberOfTrees + ", " + maxRMSE + "}}\n");
			
			
			mathematica.write("allErrorCurve := ListLinePlot[{avgCvValidationError, avgCvTrainingError, avgCvTestError, allDataTrainingError, allDataTestError, cvEnsembleTrainingError, cvEnsembleTestError, avgOptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					+ ", PlotLegends -> {\"avgCvValidationError\", \"avgCvTrainingError\", \"avgCvTestError\", \"allDataTrainingError\", \"allDataTestError\", \"cvEnsembleTrainingError\", \"cvEnsembleTestError\", \"avgOptimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Lighter[Blue], Opacity[0.85]}, {Lighter[Cyan], Opacity[0.85]}, {Darker[Cyan], Opacity[0.85]}, {Lighter[Red], Opacity[0.85]}, {Darker[Red], Opacity[0.85]}, {Lighter[Orange], Opacity[0.85]}, {Darker[Orange], Opacity[0.85]}, Green, Green}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {9 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
	
			mathematica.write("allDataErrorCurve := ListLinePlot[{avgCvValidationError, allDataTrainingError, allDataTestError, avgOptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					+ ", PlotLegends -> {\"avgCvValidationError\", \"allDataTrainingError\", \"allDataTestError\", \"avgOptimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Lighter[Blue], Opacity[0.85]}, {Lighter[Red], Opacity[0.85]}, {Orange, Opacity[0.85]}, Green, Green}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {5 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
			
			mathematica.write("cvEnsembleErrorCurve := ListLinePlot[{avgCvValidationError, cvEnsembleTrainingError, cvEnsembleTestError, avgOptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					+ ", PlotLegends -> {\"avgCvValidationError\", \"cvEnsembleTrainingError\", \"cvEnsembleTestError\", \"avgOptimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Lighter[Blue], Opacity[0.85]}, {Lighter[Red], Opacity[0.85]}, {Orange, Opacity[0.85]}, Green, Green}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {5 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
			
			mathematica.write("cvErrorCurve := ListLinePlot[{avgCvValidationError, avgCvTrainingError, avgCvTestError, avgOptimalNumberOfTrees, optimalNumberOfTreesUpperBound}"
					+ ", PlotLegends -> {\"avgCvValidationError\", \"avgCvTrainingError\", \"avgCvTestError\", \"avgOptimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Lighter[Blue], Opacity[0.85]}, {Lighter[Red], Opacity[0.85]}, {Orange, Opacity[0.85]}, Green, Green}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ ", ImageSize -> Large"
					+ ", Filling -> {5 -> {Axis, RGBColor[0, 1, 0, .3]}}" 
					+ "]\n\n");
					
			mathematica.write("metaDataCurve := ListLinePlot[{exampleInNodeMean,exampleInNodeStdDev,learningRateMean, learningRateStdDev, actualNumberOfSplits}"
					+ ", PlotLegends -> {\"exampleInNodeMean\", \"exampleInNodeStdDev\", \"learningRateMean (X 100)\", \"learningRateStdDev (X 100)\", \"actualNumberOfSplits (X 10)\"}"
					+ ", PlotStyle -> {{Lighter[Red], Opacity[0.85]}, {Darker[Red], Opacity[0.85]}, {Lighter[Green], Opacity[0.85]}, {Darker[Green], Opacity[0.85]}, {Lighter[Blue], Opacity[0.85]}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"Units\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxMetaData + "}}"
					+ ", ImageSize -> Large"
					+ "]\n\n");
			
			
			mathematica.write(saveToFile.toString());
			
			latex.write(latexCode.toString());
			mathematica.flush();
			mathematica.close();
			latex.flush();
			latex.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(String.format("[%s] Writing of error curve to file Failed! Failed to generate error curve for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}
		
		try {
			StopWatch errorCurveTimer = new StopWatch().start();
			errorCurveTimer.printMessageWithTime("Starting execution of " + mathematicaFileFullPath);
			CommandLineExecutor.runProgramAndWaitForItToComplete(baseFileDirectory, new String[] {"math", "-script", mathematicaFileName});
			RecursiveFileDeleter.deleteDirectory(new File(mathematicaFileFullPath));
			errorCurveTimer.printMessageWithTime("Finished execution of " + mathematicaFileFullPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(String.format("[%s] Call to mathematica script failed! Failed to generate error curve for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
					datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			return null;
		}
		
		
		SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--errorCurveLock.txt");
		
		System.out.println(String.format("[%s] Successfully generated error curve for run data for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
				datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
		return null;
	}
}
