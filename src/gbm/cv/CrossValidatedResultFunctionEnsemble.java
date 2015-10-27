package gbm.cv;

import gbm.GbmParameters;
import gbm.ResultFunction;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import dataset.Attribute;
import dataset.Dataset;
import regressionTree.RegressionTree;
import utilities.DoubleCompare;

public class CrossValidatedResultFunctionEnsemble {
	GbmParameters parameters;
	public ResultFunction[] functions;
	public double avgInitialValue;
	public int optimalNumberOfTrees, totalNumberOfTrees;
	public int stepSize, numOfFolds;
	public double[] avgCvValidationErrors;
	public double[] avgCvTrainingErrors;
	public String[] predictorNames;
	
	public CrossValidatedResultFunctionEnsemble(GbmParameters parameters, CrossValidationStepper[] steppers, int totalNumberOfTrees) {
		this.predictorNames = steppers[0].dataset.getPredictorNames();
		this.parameters = parameters;
		this.totalNumberOfTrees = totalNumberOfTrees;
		this.stepSize = steppers[0].stepSize;
		this.numOfFolds = steppers.length;
		
		this.functions = new ResultFunction[steppers.length];
		this.avgInitialValue = 0.0;
		for (int i = 0; i < steppers.length; i++) {
			functions[i] = steppers[i].function;
			avgInitialValue += functions[i].initialValue;
		}
		this.avgInitialValue /= functions.length;

		// Optimal number of trees is the point where average cross validation error is minimized.
		double minAvgValidationError = Double.MAX_VALUE;
		this.avgCvTrainingErrors = new double[totalNumberOfTrees];
		this.avgCvValidationErrors = new double[totalNumberOfTrees];
		for (int i = 0; i < totalNumberOfTrees; i++) {
			for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
				avgCvTrainingErrors[i] += functions[functionIndex].trainingError.get(i);
				avgCvValidationErrors[i] += functions[functionIndex].validationError.get(i);
			}
			if (DoubleCompare.lessThan(avgCvValidationErrors[i], minAvgValidationError)) {
				minAvgValidationError = avgCvValidationErrors[i];
				this.optimalNumberOfTrees = i;
			}
		}
		for (int i = 0; i < totalNumberOfTrees; i++) {
			avgCvTrainingErrors[i] /= functions.length;
			avgCvValidationErrors[i] /= functions.length;
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
		double[] relativeInf = calcRelativeInfluences();
		StringBuffer s = new StringBuffer();
		s.append("Relative Influences\n--------------------\n");
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
	
	
	private double[] calcRelativeInfluences() {
		double[] relativeInfluences = new double[predictorNames.length];
		int totalNumOfTrees = 0;
		for (int functionIndex = 0; functionIndex < functions.length; functionIndex++) {
			for (RegressionTree tree : functions[functionIndex].trees) {
				ResultFunction.calcRelativeInfluenceHelper(relativeInfluences, tree.root);
			}
			totalNumOfTrees += functions[functionIndex].trees.size();
		}
		
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] /= totalNumOfTrees;
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
		Attribute[] responses = dataset.getResponses();
		Attribute[][] instances = dataset.getInstances();
		double rmse = 0.0;
		for (int i = 0; i < dataset.getNumberOfExamples(); i++) {
			double tmp = (getLearnedValue(instances[i]) - responses[i].getNumericValue());
			rmse += tmp * tmp;
		}
		rmse /= dataset.getNumberOfExamples();
		rmse = Math.sqrt(rmse);
		return rmse;
	}
	
	public String getSummary() {
		return String.format("\nTotalNumberOfTrees: %d \n"
						+ "OptimalNumberOfTrees: %d \n"
						+ "CV Training RMSE: %f \n"
						+ "CV Validation RMSE: %f \n" 
						+ "Step Size: %d \n"
						+ "Number Of Folds: %d \n\n"
						+ getRelativeInfluencesString(),
				totalNumberOfTrees,
				optimalNumberOfTrees, 
				avgCvTrainingErrors[optimalNumberOfTrees],
				avgCvValidationErrors[optimalNumberOfTrees],
				stepSize,
				numOfFolds);
	}
}