package gbm.cv;

import gbm.GbmParameters;
import gbm.ResultFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import parameterTuning.plotting.MathematicaListCreator;
import utilities.DoubleCompare;
import utilities.Matrix;
import dataset.Attribute;
import dataset.Dataset;

public class CrossValidatedResultFunctionEnsemble {
	public GbmParameters parameters;
	public ResultFunction[] functions;
	public ResultFunction allDataFunction;
	public double avgInitialValue;
	public int optimalNumberOfTrees, totalNumberOfTrees;
	public int stepSize, numOfFolds;
	public double[] avgCvValidationErrors;
	public double[] avgCvTrainingErrors;
	public double[] avgCvTestErrors;
	public String[] predictorNames;
	public double timeInSeconds;
	
	public CrossValidatedResultFunctionEnsemble(GbmParameters parameters, CrossValidationStepper[] steppers, int totalNumberOfTrees, double timeInSeconds) {
		this.predictorNames = steppers[0].dataset.getPredictorNames();
		this.parameters = parameters;
		this.totalNumberOfTrees = totalNumberOfTrees;
		this.stepSize = steppers[0].stepSize;
		this.numOfFolds = steppers.length-1;
		this.timeInSeconds = timeInSeconds;
		
		this.functions = new ResultFunction[steppers.length-1];
		this.avgInitialValue = 0.0;
		for (int i = 0; i < steppers.length-1; i++) {
			functions[i] = steppers[i].function;
			avgInitialValue += functions[i].initialValue;
		}
		allDataFunction = steppers[steppers.length-1].function;
		this.avgInitialValue /= functions.length;

		// Optimal number of trees is the point where average cross validation error is minimized.
		double minAvgValidationError = Double.MAX_VALUE;
		this.avgCvTrainingErrors = new double[totalNumberOfTrees];
		this.avgCvValidationErrors = new double[totalNumberOfTrees];
		this.avgCvTestErrors = new double[totalNumberOfTrees];
		for (int i = 0; i < totalNumberOfTrees; i++) {
			for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
				avgCvTrainingErrors[i] += functions[functionIndex].trainingError.get(i);
				avgCvValidationErrors[i] += functions[functionIndex].validationError.get(i);
				avgCvTestErrors[i] += functions[functionIndex].testError.get(i);
			}
			if (DoubleCompare.lessThan(avgCvValidationErrors[i], minAvgValidationError)) {
				minAvgValidationError = avgCvValidationErrors[i];
				this.optimalNumberOfTrees = i+1;
			}
		}
		for (int i = 0; i < totalNumberOfTrees; i++) {
			avgCvTrainingErrors[i] /= functions.length;
			avgCvValidationErrors[i] /= functions.length;
			avgCvTestErrors[i] /= functions.length;
		}
		
		if (optimalNumberOfTrees == totalNumberOfTrees) {
			System.out.println("Warning: The optimal number was trees was equivalent to the number of trees grown. Consider running longer");
		}
	}
	
	// the following function is used to estimate the function
	public double getLearnedValue(Attribute[] instance_x) {
		double avgLearnedValue = 0.0;
		for (int j = 0; j < functions.length; j++) {
			avgLearnedValue += functions[j].getLearnedValue(instance_x);
		}
		avgLearnedValue /= functions.length;
		return avgLearnedValue;
		
		/* Not sure which way makes more sense.
		double result = avgInitialValue;
		for (int i = 0; i < optimalNumberOfTrees; i++) {
			double avgLearnedValue = 0.0;
			for (int j = 0; j < functions.length; j++) {
				avgLearnedValue += functions[j].trees.get(i).getLearnedValue(instance_x);
			}
			avgLearnedValue /= functions.length;
			result += avgLearnedValue;
		}
		return result;
		*/
	}
	public String getRelativeInfluencesString() {
		return getRelativeInfluencesString(optimalNumberOfTrees);
	}
	
	public String getRelativeInfluencesString(int numberOfTrees) {
		double[] relativeInf = calcRelativeInfluences(numberOfTrees);
		StringBuffer s = new StringBuffer();
		s.append("CV Relative Influences\n--------------------\n");
		PriorityQueue<Map.Entry<String, Double>> sortedRelativeInfluences = 
				new PriorityQueue<Map.Entry<String, Double>>(new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
						return DoubleCompare.compare(arg1.getValue(), arg0.getValue());
					}
				});
		for (int i = 0; i < relativeInf.length; i++) {
			sortedRelativeInfluences.add(new SimpleEntry<String, Double>(predictorNames[i], relativeInf[i]));
		}
		while (!sortedRelativeInfluences.isEmpty()) {
			Map.Entry<String, Double> next = sortedRelativeInfluences.poll();
			s.append(next.getKey() + ": " + next.getValue() + "\n");
		}
		return s.toString();
	}
	
	public double[] calcRelativeInfluences() { 
		return calcRelativeInfluences(optimalNumberOfTrees);
	}

	public double[] calcRelativeInfluences(int numberOfTrees) {
		double[] relativeInfluences = new double[predictorNames.length];
		for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
			Matrix.addToInPlace(relativeInfluences, functions[functionIndex].calcRelativeInfluences(numberOfTrees));
		}
		
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] /= (numberOfTrees * functions.length);
		}
		double sum = 0;
		for (int i = 0; i < relativeInfluences.length; i++) {
			sum += relativeInfluences[i];
		}
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] *= 100;
			relativeInfluences[i] /= sum;
		}
		return relativeInfluences;
	}
	
	public double calculateRootMeanSquaredError(Dataset dataset) {
		Attribute[] responses = dataset.getTrainingResponses();
		Attribute[][] instances = dataset.getTrainingInstances();
		double rmse = 0.0;
		for (int i = 0; i < dataset.getNumberOfTrainingExamples(); i++) {
			double tmp = (getLearnedValue(instances[i]) - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= dataset.getNumberOfTrainingExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public String getSummary() {
		return String.format("Time In Seconds: %f \n"
						+ "Step Size: %d \n"
						+ "Number Of Folds: %d \n"
						+ "TotalNumberOfTrees: %d \n"
						+ "OptimalNumberOfTrees: %d \n"
						+ "CV Validation RMSE: %f \n" 
						+ "CV Training RMSE: %f \n"
						+ "All Data Training RMSE: %f \n"
						+ "CV Test RMSE: %f \n" 
						+ "All Data Test RMSE: %f \n" 
						+ "All Data Avg Number Of Splits: %f\n",
						//+ getRelativeInfluencesString()
						//+ allDataFunction.getRelativeInfluencesString(optimalNumberOfTrees),
				timeInSeconds,
				stepSize,
				numOfFolds,
				totalNumberOfTrees,
				optimalNumberOfTrees, 
				avgCvValidationErrors[optimalNumberOfTrees-1],
				avgCvTrainingErrors[optimalNumberOfTrees-1],
				allDataFunction.trainingError.get(optimalNumberOfTrees-1),
				avgCvTestErrors[optimalNumberOfTrees-1],
				allDataFunction.testError.get(optimalNumberOfTrees-1),
				allDataFunction.getAvgNumberOfSplits());
	}
	
	public String getMathematicaCommentedSummary() {
		return String.format("(* Step Size: %d *) \n"
						+ "(* Number Of Folds: %d *)\n"
						+ "(* TotalNumberOfTrees: %d *)\n"
						+ "(* OptimalNumberOfTrees: %d *)\n"
						+ "(* CV Validation RMSE: %f *)\n" 
						+ "(* CV Training RMSE: %f *)\n"
						+ "(* All Data Training RMSE: %f *)\n"
						+ "(* CV Test RMSE: %f *)\n" 
						+ "(* All Data Test RMSE: %f *)\n" 
						// TODO:
						+ getRelativeInfluencesString()
						+ allDataFunction.getRelativeInfluencesString(optimalNumberOfTrees),
				stepSize,
				numOfFolds,
				totalNumberOfTrees,
				optimalNumberOfTrees, 
				avgCvValidationErrors[optimalNumberOfTrees],
				avgCvTrainingErrors[optimalNumberOfTrees],
				allDataFunction.trainingError.get(optimalNumberOfTrees),
				avgCvTestErrors[optimalNumberOfTrees],
				allDataFunction.testError.get(optimalNumberOfTrees));
	}
	
	public void saveDataToFileOld(String postFix, String directory) throws IOException {
		String mathDirectory = directory + "/mathematica/";
		String normalDirectory = directory + "/normal/";
		new File(mathDirectory).mkdirs();
		new File(normalDirectory).mkdirs();
		String mathematicFileName = mathDirectory + parameters.getOldFileNamePrefix() + ((postFix.isEmpty()) ? "" : "--" + postFix) + "--mathematica.txt";
		String normalFileName = normalDirectory + parameters.getOldFileNamePrefix() + ((postFix.isEmpty()) ? "" : "--" + postFix) + "--normal.txt";
		BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicFileName)));
		BufferedWriter normal = new BufferedWriter(new PrintWriter(new File(normalFileName)));
		
		//mathematica.write(getMathematicaCommentedSummary());
		mathematica.write("avgCvTrainingError := " + MathematicaListCreator.convertToMathematicaList(avgCvTrainingErrors) + "\n");
		mathematica.write("avgCvValidationError := " + MathematicaListCreator.convertToMathematicaList(avgCvValidationErrors) + "\n");
		mathematica.write("avgCvTestError := " + MathematicaListCreator.convertToMathematicaList(avgCvTestErrors) + "\n");
		mathematica.write("allDataTrainingError := " + MathematicaListCreator.convertToMathematicaList(allDataFunction.trainingError) + "\n");
		mathematica.write("allDataTestError := " + MathematicaListCreator.convertToMathematicaList(allDataFunction.testError) + "\n");
		mathematica.write("optimalNumberOfTrees := {{" + optimalNumberOfTrees + ", 0}, {" + optimalNumberOfTrees + ", 15}}\n");
		mathematica.write("ListLinePlot[{avgCvTrainingError,avgCvValidationError,avgCvTestError, allDataTrainingError, allDataTestError, optimalNumberOfTrees}"
				+ ", PlotLegends -> {\"avgCvTrainingError\", \"avgCvValidationError\", \"avgCvTestError\", \"allDataTrainingError\", \"allDataTestError\", \"optimalNumberOfTrees\"}"
				+ ", PlotStyle -> {{Orange, Dashed, Thick}, {Blue, Dashed, Thick}, {Red, Dashed, Thick}, Cyan, Magenta, {Green, Thin}}"
				+ ", AxesLabel->{\"Number Of Trees\", \"RMSE\"}"
				+ ", PlotRange -> {{Automatic, Automatic}, {0, 15}}"
				+ "] \n");
		mathematica.flush();
		mathematica.close();
		normal.write(getSummary());
		normal.write(convertToNormalFile());
		normal.flush();
		normal.close();
	}
	
	public void saveDataToFile(String directory) throws IOException {
		new File(directory).mkdirs();
		String normalFileName = directory + parameters.getFileNamePrefix() + "--runData.txt";
		BufferedWriter normal = new BufferedWriter(new PrintWriter(new File(normalFileName)));
		
		normal.write(getSummary());
		normal.write(convertToNormalFile());
		normal.flush();
		normal.close();
	}
	

	
	public String convertToNormalFile() {
		StringBuffer retval = new StringBuffer();
		retval.append("TreeNumber\tAvgCvTrainingError\tAvgCvValidationError\tAvgCvTestError\tAllDataTrainingError\tAllDataTestError\n");
		for (int i = 0; i < totalNumberOfTrees; i++) {
			retval.append(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\n", 
					i+1,
					avgCvTrainingErrors[i],
					avgCvValidationErrors[i],
					avgCvTestErrors[i],
					allDataFunction.trainingError.get(i),
					allDataFunction.testError.get(i)));
		}
		return retval.toString();
	}
	
	
	public String roundNicely(int precision, double variable) {
		int val = 1;
		for (int i = 1; i < precision; i++) {
			val *= 10;
		}
		return "" + Math.round(((variable)) * val) / (double)val;
	}
}