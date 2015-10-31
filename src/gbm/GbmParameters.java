package gbm;

import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.Logger;

public class GbmParameters {
	// class members
	public double bagFraction; 
	public double maxLearningRate;
	public int numOfTrees;
	public LearningRatePolicy learningRatePolicy;
	
	// tree related parameters
	public int minExamplesInNode;
	public int maxNumberOfSplits;
	public double timeInSeconds;
	public int optimalNumberOfTrees;
	public GbmParameters(double bagFraction, double maxLearningRate, int numOfTrees, int minExamplesInNode, int maxNumberOfSplits, LearningRatePolicy learningRatePolicy) {
		setBagFraction(bagFraction);
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinObsInNode(minExamplesInNode);
		setmaxNumberOfSplits(maxNumberOfSplits);
		this.learningRatePolicy = learningRatePolicy;
	}

	private void setBagFraction(double bagFraction) {
		if (bagFraction <= 0 || bagFraction > 1) {
			Logger.println(Logger.LEVELS.DEBUG, "BagFraction must be in range (0, 1]");
			System.exit(0);
		}
		this.bagFraction = bagFraction;
	}
	
	private void setMaxLearningRate(double maxLearningRate) {
		if (maxLearningRate <= 0) {
			Logger.println(Logger.LEVELS.DEBUG, "Learning rate must be >= 0");
			System.exit(0);
		}
		this.maxLearningRate = maxLearningRate;
	}
	
	private void setNumOfTrees(int numOfTrees) {
		if (numOfTrees < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "Number of trees must be >= 1");
			System.exit(0);
		}
		this.numOfTrees = numOfTrees;
	}
	
	private void setmaxNumberOfSplits(int maxNumberOfSplits) {
		if (maxNumberOfSplits < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "The tree's maxNumberOfSplits must be >= 1");
			System.exit(0);	
		}
		this.maxNumberOfSplits = maxNumberOfSplits;
	}
	
	private void setMinObsInNode(int minExamplesInNode) {
		if (minExamplesInNode < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "MinObsInNode must be >= 1");
			System.exit(0);
		}
	
		this.minExamplesInNode = minExamplesInNode;
	}
	
	// get parameters
	public double getBagFraction() {
		return bagFraction;
	}
	
	public double getMaxLearningRate() {
		return maxLearningRate;
	}
	
	public int getNumOfTrees() {
		return numOfTrees;
	}
	
	public int getMinObsInNode() {
		return minExamplesInNode;
	}
	
	public int getMaxNumberOfSplits() {
		return maxNumberOfSplits;
	}
	
	public String getFileNamePrefix() {
		return String.format(learningRatePolicy.name() 
				+ "_MLR-%.4f" 
				+ "_BF-%.4f"
				+ "_SPLITS-%d"
				+ "_MEIN-%d"
				+ "_TREES-%d",
				maxLearningRate, bagFraction, maxNumberOfSplits, minExamplesInNode, numOfTrees);
	}
	
	public static String getTabSeparatedHeader() {
		return "TimeInSeconds\t"
				+ "OptimalNumberOfTrees\t"
				+ "LearningRatePolicy\t"
				+ "MaxLearningRate\t" 
				+ "BagFraction\t"
				+ "MaxNumberOfSplits\t"
				+ "MinExamplesInNode\t"
				+ "NumberOfTrees\t";
	}
	public String getTabSeparatedPrintOut() {
		return String.format(
				"%.4f\t"
				+ "%d\t"
				+ learningRatePolicy.name() + "\t" 
				+ "%.4f\t" 
				+ "%.4f\t"
				+ "%d\t"
				+ "%d\t"
				+ "%d\t",
				timeInSeconds, optimalNumberOfTrees, maxLearningRate, bagFraction, maxNumberOfSplits, minExamplesInNode, numOfTrees);
	}
}