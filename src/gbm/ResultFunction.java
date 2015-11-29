package gbm;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

import regressionTree.LearningRateTerminalValuePair;
import regressionTree.RegressionTree;
import regressionTree.TreeNode;
import utilities.StopWatch;
import utilities.SumCountAverage;
import dataset.Attribute;

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
	
	public SumCountAverage numberOfSplits = new SumCountAverage();

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
		this.numberOfSplits.addData(newTree.actualNumberOfSplits);
	}
	
	// the following function is used to estimate the function
	public double getLearnedValue(Attribute[] instance_x) {
		double result = initialValue;
		
		if (trees.size() == 0) {
			return result;
		}
		
		for (RegressionTree tree : trees){
			LearningRateTerminalValuePair pair = tree.getLearningRateTerminalValuePair(instance_x);
			result += pair.learningRate * pair.terminalValue;
		}
		
		return result;
	}
	
	public double getLearnedValue(Attribute[] instance_x, int numberOfTrees) {
		double result = initialValue;
		
		if (trees.size() == 0) {
			return result;
		}
		if ( numberOfTrees > trees.size()) {
			System.err.println(StopWatch.getDateTimeStamp() + "ERROR: Cannot predict using " + numberOfTrees + " when only " + trees.size() + " trees exists.");
		}
		
		for (int i = 0; i < numberOfTrees; i++) {
			result += trees.get(i).getLearnedValueWithLearningRateApplied(instance_x);
		}
		
		return result;
	}
	
	public String getRelativeInfluencesString( String header) {
		return getRelativeInfluencesString(trees.size(), header);
	}
	
	public String getRelativeInfluencesString(int numberOfTrees, String header) {
		double[] relativeInf = calcRelativeInfluences();
		StringBuffer s = new StringBuffer();
		s.append(header);
		PriorityQueue<Map.Entry<String, Double>> sortedRelativeInfluences = 
				new PriorityQueue<Map.Entry<String, Double>>(new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
						return Double.compare(arg1.getValue(), arg0.getValue());
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
						+ getRelativeInfluencesString("--------------Relative Influences-------------"),
				trees.size(),
				trainingError.get(trees.size()-1),
				validationError.get(trees.size()-1),
				testError.get(trees.size()-1));
	}
	
	public void allowUserToPrintTrees() {
		String userInput = "";
		Scanner sc = new Scanner(System.in);
		while(!userInput.equalsIgnoreCase("n")) {
			System.out.println(StopWatch.getDateTimeStamp() + "Would you like to print an individual tree? Enter a number between 0 and " + (trees.size()-1) + " or type 'N'");
			userInput = sc.nextLine();
			int value = 0;
			try {
				value = Integer.parseInt(userInput);
			} catch(Exception e) {
				System.out.println(StopWatch.getDateTimeStamp() + "Try again :)");
				continue;
			}
			
			if (value > (trees.size()-1)) {
				continue;
			}
			
			try {
				this.trees.get(value).root.printTree(new OutputStreamWriter(System.out));
			} catch (IOException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		sc.close();
	}
}