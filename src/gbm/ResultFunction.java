package gbm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

import dataset.Attribute;
import regressionTree.LearningRateTerminalValuePair;
import regressionTree.RegressionTree;
import utilities.StopWatch;

public class ResultFunction {
	public double initialValue;
	public ArrayList<RegressionTree> trees;
	
	public GbmParameters parameters;
	
	// Needed for relative Influence Calculation
	public int numberOfPredictors;
	public String[] predictorNames;
	
	/**
	 * For use by subclasses with their own constructors
	 */
	protected ResultFunction() {
		
	}

	public ResultFunction(GbmParameters parameters, double intialValue, String[] predictorNames) {
		this.numberOfPredictors = predictorNames.length;
		this.predictorNames = predictorNames;
		this.initialValue = intialValue;
		this.parameters = parameters;
		trees = new ArrayList<RegressionTree>();
	}
	
	public void addTree(RegressionTree tree) {
		this.trees.add(tree);
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
	
	public double getLearnedValueString(Attribute[] instance_x, int numberOfTrees, StringBuilder str) {
		double result = initialValue;
		str.append(utilities.Logger.formatNice(initialValue) + " ");
		if (trees.size() == 0) {
			return result;
		}
		if ( numberOfTrees > trees.size()) {
			System.err.println(StopWatch.getDateTimeStamp() + "ERROR: Cannot predict using " + numberOfTrees + " when only " + trees.size() + " trees exists.");
		}
		
		for (int i = 0; i < numberOfTrees; i++) {
			str.append("+ ( ");
			result += trees.get(i).getLearnedValueWithLearningRateAppliedString(instance_x, str);
			str.append(" ) ");
		}
		str.append("= " + utilities.Logger.formatNice(result));
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
			trees.get(i).root.calcRelativeInfluences(relativeInfluences);
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
	
	public String getSummary() {
		return String.format("\nTotalNumberOfTrees: %d \n"
						+ getRelativeInfluencesString("--------------Relative Influences-------------"),
				trees.size());
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
				//System.out.println(this.trees.get(value).printOneSplitLatexTree());
			} catch (IOException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		sc.close();
	}
	
	public String formatNice(double d) {
		return String.format("%f", d).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1");
	}
	
	public void printFirstTwoTrees(String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(
		"\\begin{frame}\n"
		+ "\\frametitle{" + prefix + "Example: Nasa Air Foil Dataset}\n"
		+ "\\Wider[4.5em]{\n"
		);
		sb.append(
		"\\resizebox*{\\textwidth}{!}{\n"
		+	"\\begin{tabular} {|l c |}\n"
		+		"\t\t\\hline\n"
		+		"\t\t\\tiny Bag Fraction & \\tiny " + (String.format("%f", parameters.bagFraction)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + " \\\\ \\hline\n"
		+		"\t\t\\tiny Min Shrinkage & \\tiny " + (String.format("%f", parameters.minLearningRate)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\tiny Max Shrinkage & \\tiny " + (String.format("%f", parameters.maxLearningRate)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\tiny Number of Splits & \\tiny " + (String.format("%d", parameters.maxNumberOfSplits)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\tiny Min Leaf Size & \\tiny " + (String.format("%d", parameters.minExamplesInNode)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\tiny Per Tree Sample Size & \\tiny " + (String.format("%d", this.trees.get(0).sampleSize)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\tiny Avg Training Response & \\tiny " + (String.format("%f", initialValue)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "  \\\\ \\hline\n"
		+		"\t\t\\end{tabular}\n"
		);
		
		int[][] map = this.trees.get(0).gbmDataset.getNumericalPredictorSortedIndexMap();
		Attribute[] firstInstance =  this.trees.get(0).gbmDataset.getTrainingInstances()[map[this.trees.get(0).root.splitPredictorIndex][0]];
		Attribute firstResponse = this.trees.get(0).gbmDataset.getTrainingResponses()[map[this.trees.get(0).root.splitPredictorIndex][0]];;
		Attribute[] secondInstance =  this.trees.get(0).gbmDataset.getTrainingInstances()[map[this.trees.get(0).root.splitPredictorIndex][map[this.trees.get(0).root.splitPredictorIndex].length-1]];;
		Attribute secondResponse =  this.trees.get(0).gbmDataset.getTrainingResponses()[map[this.trees.get(0).root.splitPredictorIndex][map[this.trees.get(0).root.splitPredictorIndex].length-1]];;
		
		StringBuilder firstFunction = new StringBuilder("$ \\hat{F} = ");
		double firstPred = getLearnedValueString(firstInstance, 2, firstFunction);
		double firstResidual = firstResponse.getNumericValue() - firstPred;
		firstFunction.append("$");
		StringBuilder secondFunction = new StringBuilder("$ \\hat{F} = ");
		double secondPred = getLearnedValueString(secondInstance, 2, secondFunction);
		double secondResidual = secondResponse.getNumericValue() - secondPred;
		secondFunction.append("$");
		
		
		sb.append(
			"\\begin{tabular} {|c c c c c c c c c|}\n"
		+		"\t\t\\hline\n"
		+		"\t\t \\tiny Example & \\tiny Frequency & \\tiny Angle & \\tiny ChordLength & \\tiny FreeStreamVelocity & \\tiny SuctionSideDisplacment & \\tiny SoundPressureLevel & \\tiny Prediction & \\tiny Squared Error \\\\ \\hline\n"
		);
		sb.append("\t\t \\tiny 1 & ");
		for (int i = 0; i < firstInstance.length; i++) {
			sb.append("\\tiny " + formatNice(firstInstance[i].getNumericValue()) + " & ");
		}
		sb.append("\\tiny " + formatNice(firstResponse.getNumericValue()) + " & ");
		sb.append("\\tiny " + formatNice(firstPred) + " & ");
		sb.append("\\tiny " + formatNice(firstResidual * firstResidual) + "  \\\\ \\hline\n");
		
		sb.append("\t\t \\tiny 2 & ");
		for (int i = 0; i < secondInstance.length; i++) {
			sb.append("\\tiny " + formatNice(secondInstance[i].getNumericValue()) + " & ");
		}
		sb.append("\\tiny " + formatNice(secondResponse.getNumericValue()) + " & ");
		sb.append("\\tiny " + formatNice(secondPred) + " & ");
		sb.append("\\tiny " + formatNice(secondResidual * secondResidual) + "  \\\\ \\hline\n");
		
		sb.append("\\multicolumn{9}{c}{}");
		sb.append(
					"\t\t \\\\ \\hline\n"
			+		"\t\t \\tiny Example & \\multicolumn{6}{c}{\\tiny Function} & & \\\\ \\hline\n"
			+		"\t\t \\tiny 1 & \\multicolumn{6}{c}{\\tiny" + firstFunction.toString() + "} & & \\\\ \\hline\n"
			+		"\t\t \\tiny 2 & \\multicolumn{6}{c}{\\tiny" + secondFunction.toString() + "} & & \\\\ \\hline\n"
			);
		sb.append("\t\t\\end{tabular}\n");
		sb.append("}\n");
		
		sb.append("\\begin{tabular} {c c}\n");
		sb.append(this.trees.get(0).printOneSplitLatexTree());
		sb.append("&\n");
		sb.append(this.trees.get(1).printOneSplitLatexTree());
		sb.append("\n\\end{tabular}\n");
		sb.append("}\n");
		sb.append("\\end{frame}\n");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("/home/amb6470/git/GBMWithVariableShrinkageWrittenWork/Final Report/" + prefix + "example.tex"));
			bw.write(sb.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}