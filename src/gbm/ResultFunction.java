package gbm;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
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
	public ArrayList<Double> testError;
	
	public GbmParameters parameters;
	
	// Needed for relative Influence Calculation
	public int numberOfPredictors;
	public String[] predictorNames;
	
	int sumOfSplits = 0;
	
	// construction function
	ResultFunction(GbmParameters parameters, double intialValue, String[] predictorNames) {
		this.numberOfPredictors = predictorNames.length;
		this.predictorNames = predictorNames;
		this.initialValue = intialValue;
		this.parameters = parameters;
		trees = new ArrayList<RegressionTree> ();
		trainingError = new ArrayList<Double>();
		validationError = new ArrayList<Double>();
		testError = new ArrayList<Double>();
	}
	
	public void addTree(RegressionTree newTree, double trainingError, double validationError, double testError) {
		this.trees.add(newTree);
		this.trainingError.add(trainingError);
		this.validationError.add(validationError);
		this.testError.add(testError);
		this.sumOfSplits += newTree.actualNumberOfSplits;
	}
	
	public double getAvgNumberOfSplits() {
		return sumOfSplits / trees.size();
	}
	
	// the following function is used to estimate the function
	public double getLearnedValue(Attribute[] instance_x) {
		double result = initialValue;
		
		if (trees.size() == 0) {
			return result;
		}
		
		for (RegressionTree tree : trees){
			// Learning rate is accounted for in the tree itself.
			result += tree.getLearnedValue(instance_x);
		}
		
		return result;
	}
	
	public String getRelativeInfluencesString() {
		return getRelativeInfluencesString(trees.size());
	}
	
	public String getRelativeInfluencesString(int numberOfTrees) {
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
		return calcRelativeInfluences(trees.size());
	}

	public double[] calcRelativeInfluences(int numberOfTrees) {
		double[] relativeInfluences = new double[numberOfPredictors];
		for (int i = 0 ; i < numberOfTrees; i++) {
			RegressionTree tree = trees.get(i);
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
		if (node.splitPredictorIndex != -1) {
			relativeInfluences[node.splitPredictorIndex] += Math.round(((node.getSquaredErrorImprovement())) * 10) / 10.0;
		}
		calcRelativeInfluenceHelper(relativeInfluences, node.leftChild);
		calcRelativeInfluenceHelper(relativeInfluences, node.rightChild);
	}
	
	public String getSummary() {
		return String.format("\nTotalNumberOfTrees: %d \n"
						+ "Training RMSE: %f \n"
						+ "Validation RMSE: %f \n"
						+ "Test RMSE: %f \n\n" 
						+ getRelativeInfluencesString(),
				trees.size(),
				trainingError.get(trees.size()-1),
				validationError.get(trees.size()-1),
				testError.get(trees.size()-1));
	}
	
	public void allowUserToPrintTrees() {
		String userInput = "";
		Scanner sc = new Scanner(System.in);
		while(!userInput.equalsIgnoreCase("n")) {
			System.out.println("Would you like to print an individual tree? Enter a number between 0 and " + (trees.size()-1) + " or type 'N'");
			userInput = sc.nextLine();
			int value = 0;
			try {
				value = Integer.parseInt(userInput);
			} catch(Exception e) {
				System.out.println("Try again :)");
				continue;
			}
			
			if (value > (trees.size()-1)) {
				continue;
			}
			
			try {
				this.trees.get(value).root.printTree(new OutputStreamWriter(System.out));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sc.close();
	}
}