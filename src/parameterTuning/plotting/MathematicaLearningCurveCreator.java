package parameterTuning.plotting;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import parameterTuning.OptimalParameterRecord;

public class MathematicaLearningCurveCreator {
	public static void createLearningCurveForParameters(String runDataDirectory, GbmParameters parameters) {
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();

		// Read through all the files cooresponding to these parameters and average the data.
		String runDataFilePath = runDataDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--runData.txt";

		OptimalParameterRecord record = OptimalParameterRecord.readOptimalParameterRecordFromRunDataFile(runDataDirectory, parameters);
		try {
			BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
			// skip summary info, relative influences, and header
			while (!(br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
			// read in error data 
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] components = line.split("\t");
				avgCvTrainingErrorByIteration.add(Double.parseDouble(components[1].trim()));
				avgCvValidationErrorByIteration.add(Double.parseDouble(components[2].trim()));
				avgCvTestErrorByIteration.add(Double.parseDouble(components[3].trim()));
				allDataTrainingErrorByIteration.add(Double.parseDouble(components[4].trim()));
				allDataTestErrorByIteration.add(Double.parseDouble(components[5].trim()));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String mathematicFileName = runDataDirectory + parameters.getRunDataSubDirectory() + parameters.getFileNamePrefix()  + "--learningCurve.txt";
		
		try {
			BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicFileName)));
			mathematica.write("avgCvTrainingError := " + MathematicaListCreator.convertToMathematicaList(avgCvTrainingErrorByIteration) + "\n");
			mathematica.write("avgCvValidationError := " + MathematicaListCreator.convertToMathematicaList(avgCvValidationErrorByIteration) + "\n");
			mathematica.write("avgCvTestError := " + MathematicaListCreator.convertToMathematicaList(avgCvTestErrorByIteration) + "\n");
			mathematica.write("allDataTrainingError := " + MathematicaListCreator.convertToMathematicaList(allDataTrainingErrorByIteration) + "\n");
			mathematica.write("allDataTestError := " + MathematicaListCreator.convertToMathematicaList(allDataTestErrorByIteration) + "\n");
			mathematica.write("optimalNumberOfTrees := {{" + record.optimalNumberOfTrees + ", 0}, {" + record.optimalNumberOfTrees + ", 15}}\n");
			mathematica.write("ListLinePlot[{avgCvTrainingError,avgCvValidationError,avgCvTestError, allDataTrainingError, allDataTestError, optimalNumberOfTrees}"
					+ ", PlotLegends -> {\"avgCvTrainingError\", \"avgCvValidationError\", \"avgCvTestError\", \"allDataTrainingError\", \"allDataTestError\", \"optimalNumberOfTrees\"}"
					+ ", PlotStyle -> {{Orange, Dashed, Thick}, {Blue, Dashed, Thick}, {Red, Dashed, Thick}, Cyan, Magenta, {Green, Thin}}"
					+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
					+ ", PlotRange -> {{Automatic, Automatic}, {0, 15}}"
					+ "] \n");
			mathematica.flush();
			mathematica.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
