package gbm;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import dataset.Attribute;
import regressionTree.RegressionTree;
import regressionTree.TreeNode;
import utilities.DoubleCompare;

public class ResultFunction {
	// class members
	public double initialValue;
	public ArrayList<RegressionTree> trees;
	
	public ArrayList<Double> trainingError;
	public ArrayList<Double> validationError;
	
	public GbmParameters parameters;
	
	// Needed for relative Influence Calculation
	public int numberOfPredictors;
	public String[] predictorNames;
	
	// construction function
	ResultFunction(GbmParameters parameters, double intialValue, String[] predictorNames) {
		this.numberOfPredictors = predictorNames.length;
		this.predictorNames = predictorNames;
		this.initialValue = intialValue;
		this.parameters = parameters;
		trees = new ArrayList<RegressionTree> ();
		trainingError = new ArrayList<Double>();
		validationError = new ArrayList<Double>();
	}
	
	
	public void addTree(RegressionTree newTree, double trainingError, double validationError) {
		this.trees.add(newTree);
		this.trainingError.add(trainingError);
		this.validationError.add(validationError);
	}
	// the following function is used to estimate the function
	public double getLearnedValue(Attribute[] instance_x) {
		double result = initialValue;
		
		if (trees.size() == 0) {
			return result;
		}
		
		Iterator<RegressionTree> iter = trees.iterator();
		while (iter.hasNext()) {
			RegressionTree tree = iter.next();
			// Learning rate is accoutned for in the tree itself.
			result += tree.getLearnedValue(instance_x);
		}
		
		return result;
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

	public double[] calcRelativeInfluences() {
		double[] relativeInfluences = new double[numberOfPredictors];
		for (RegressionTree tree : trees) {
			calcRelativeInfluenceHelper(relativeInfluences, tree.root);
		}
		for (int i = 0; i < relativeInfluences.length; i++) {
			relativeInfluences[i] /= trees.size();
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
	
	public static void calcRelativeInfluenceHelper(double[] relativeInfluences, TreeNode node) {
		if (node == null) return;
		relativeInfluences[node.splitPredictorIndex] += Math.round(((node.squaredErrorBeforeSplit - (node.leftSquaredError + node.rightSquaredError))) * 10) / 10.0;
		calcRelativeInfluenceHelper(relativeInfluences, node.leftChild);
		calcRelativeInfluenceHelper(relativeInfluences, node.rightChild);
	}
	
	public String getSummary() {
		return String.format("\nTotalNumberOfTrees: %d \n"
						+ "Training RMSE: %f \n"
						+ "Validation RMSE: %f \n\n" 
						+ getRelativeInfluencesString(),
				trees.size(),
				trainingError.get(trees.size()-1),
				validationError.get(trees.size()-1));
	}
}