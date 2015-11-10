package parameterTuning.plotting;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import dataset.DatasetParameters;
import parameterTuning.OptimalParameterRecord;
import utilities.DoubleCompare;

public class MathematicaLearningCurveCreator {
	public static void createLearningCurveForParameters(DatasetParameters datasetParams, String runDataFullDirectory, GbmParameters parameters) {
		double maxRMSE = Double.MIN_VALUE;
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTestErrorByIteration = new ArrayList<Double>();
		
		/* TODO
		ArrayList<Double> exampleInNodeStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> numberOfSplitsByIteration = new ArrayList<Double>();
		*/
		
		// Read through all the files cooresponding to these parameters and average the data.
		String runDataFilePath = runDataFullDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--runData.txt";

		OptimalParameterRecord record = OptimalParameterRecord.readOptimalParameterRecordFromRunDataFile(runDataFullDirectory, parameters);
		if (record == null) {
			System.out.println("Couldn't create learning curve for " + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix() + " because runData not found.");
			return;
		}
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
				for (double error : tmpErrors) {
					if (DoubleCompare.greaterThan(error, maxRMSE)) {
						maxRMSE = error;
					}
				}
				avgCvTrainingErrorByIteration.add(tmpErrors[0]);
				avgCvValidationErrorByIteration.add(tmpErrors[1]);
				avgCvTestErrorByIteration.add(tmpErrors[2]);
				allDataTrainingErrorByIteration.add(tmpErrors[3]);
				allDataTestErrorByIteration.add(tmpErrors[4]);
				cvEnsembleTrainingErrorByIteration.add(tmpErrors[5]);
				cvEnsembleTestErrorByIteration.add(tmpErrors[6]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String mathematicFileName = runDataFullDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--learningCurve.txt";
		mathematicFileName = mathematicFileName.replace("\\", "/");
		String imageFileNameNoExtension = (runDataFullDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()).replace("\\", "/");
		StringBuffer saveToFile = new StringBuffer();
		StringBuffer latexCode = new StringBuffer();
		
		saveToFile.append("fileName := \"" + imageFileNameNoExtension + "\"\n");
		saveToFile.append("Export[fileName <> \".png\", learningCurve]\n\n");

		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{{" + imageFileNameNoExtension + "}.png}\n");
		latexCode.append("\\caption{" + datasetParams.fileName + " " + parameters.getLearningCurveLatexCaption() + "}\n");
		latexCode.append("\\label{fig:" +  datasetParams.minimalName + parameters.getLearningCurveLatexFigureReference()  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		try {
			BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicFileName)));
			mathematica.write("avgCvTrainingError := " + MathematicaListCreator.convertToMathematicaList(avgCvTrainingErrorByIteration) + "\n");
			mathematica.write("avgCvValidationError := " + MathematicaListCreator.convertToMathematicaList(avgCvValidationErrorByIteration) + "\n");
			mathematica.write("avgCvTestError := " + MathematicaListCreator.convertToMathematicaList(avgCvTestErrorByIteration) + "\n");
			mathematica.write("allDataTrainingError := " + MathematicaListCreator.convertToMathematicaList(allDataTrainingErrorByIteration) + "\n");
			mathematica.write("allDataTestError := " + MathematicaListCreator.convertToMathematicaList(allDataTestErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTrainingError := " + MathematicaListCreator.convertToMathematicaList(cvEnsembleTrainingErrorByIteration) + "\n");
			mathematica.write("cvEnsembleTestError := " + MathematicaListCreator.convertToMathematicaList(cvEnsembleTestErrorByIteration) + "\n");
			mathematica.write("optimalNumberOfTrees := {{" + record.optimalNumberOfTrees + ", 0}, {" + record.optimalNumberOfTrees + ", " + maxRMSE + "}}\n");
			mathematica.write("learningCurve := ListLinePlot[{avgCvTrainingError,avgCvValidationError,avgCvTestError, allDataTrainingError, allDataTestError, cvEnsembleTrainingError, cvEnsembleTestError, optimalNumberOfTrees}"
					+ ", PlotLegends -> {\"avgCvTrainingError\", \"avgCvValidationError\", \"avgCvTestError\", \"allDataTrainingError\", \"allDataTestError\", \"cvEnsembleTrainingError\", \"cvEnsembleTestError\", \"optimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Magenta, Dashed}, {Cyan, Dashed}, {Red, Dashed}, Blue, Orange, Pink, Brown, {Green, Thin}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, " + maxRMSE + "}}"
					+ "] \nlearningCurve\n\n");
			mathematica.write(saveToFile.toString());
			mathematica.write(latexCode.toString());
			mathematica.flush();
			mathematica.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
