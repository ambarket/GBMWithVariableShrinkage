package gbm;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.Logger;

public class GbmParameters {
	// class members
	public double bagFraction; 
	// In the case of Constant learning rate, maxLearningRate is used as the learningRate
	public double maxLearningRate;
	public double minLearningRate;
	public int numOfTrees;
	public LearningRatePolicy learningRatePolicy;
	public SplitsPolicy splitsPolicy;
	
	// tree related parameters
	public int minExamplesInNode;
	public int maxNumberOfSplits;
	public GbmParameters(double minLearningRate, double maxLearningRate, int maxNumberOfSplits, double bagFraction, int minExamplesInNode, int numOfTrees, LearningRatePolicy learningRatePolicy, SplitsPolicy splitsPolicy) {
		setBagFraction(bagFraction);
		setMinLearningRate(minLearningRate);
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinObsInNode(minExamplesInNode);
		setmaxNumberOfSplits(maxNumberOfSplits);
		this.learningRatePolicy = learningRatePolicy;
		this.splitsPolicy = splitsPolicy;
	}
	
	// Backwards compatability with old data that didnt have minimum learning rate
	public GbmParameters(double maxLearningRate, int maxNumberOfSplits, double bagFraction, int minExamplesInNode, int numOfTrees, LearningRatePolicy learningRatePolicy, SplitsPolicy splitsPolicy) {
		setBagFraction(bagFraction);
		minLearningRate = -1;	
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinObsInNode(minExamplesInNode);
		setmaxNumberOfSplits(maxNumberOfSplits);
		this.learningRatePolicy = learningRatePolicy;
		this.splitsPolicy = splitsPolicy;
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
	
	private void setMinLearningRate(double minLearningRate) {
		if (minLearningRate <= 0) {
			Logger.println(Logger.LEVELS.DEBUG, "Learning rate must be >= 0");
			System.exit(0);
		}
		this.minLearningRate = minLearningRate;
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
	
	public String getOldFileNamePrefix() {
		return String.format(learningRatePolicy.name() 
				+ "_MLR-%.4f" 
				+ "_BF-%.4f"
				+ "_SPLITS-%d"
				+ "_MEIN-%d"
				+ "_TREES-%d",
				maxLearningRate, bagFraction, maxNumberOfSplits, minExamplesInNode, numOfTrees);
	}
	
	public static String getOldTabSeparatedHeader() {
		return 
				"LearningRatePolicy\t"
				+ "MaxLearningRate\t" 
				+ "BagFraction\t"
				+ "MaxNumberOfSplits\t"
				+ "MinExamplesInNode\t"
				+ "NumberOfTrees\t";
	}
	public String getOldTabSeparatedPrintOut() {
		return String.format(
				learningRatePolicy.name() + "\t" 
				+ "%.4f\t" 
				+ "%.4f\t"
				+ "%d\t"
				+ "%d\t"
				+ "%d\t",
				maxLearningRate, bagFraction, maxNumberOfSplits, minExamplesInNode, numOfTrees);
	}
	
	public String getFileNamePrefix() {
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			return String.format(learningRatePolicy.name() 
					+ "_MinLR-%.4f" 
					+ "_MaxLR-%.4f" 
					+ "_SPLITS-%d"
					+ "_BF-%.4f"
					+ "_MEIN-%d"
					+ "_TREES-%d",
					minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
		} else {
			return String.format(learningRatePolicy.name() 
					+ "_LR-%.4f" 
					+ "_SPLITS-%d"
					+ "_BF-%.4f"
					+ "_MEIN-%d"
					+ "_TREES-%d",
					maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
		}
	}
	
	public static String getTabSeparatedHeader() {
		return 
				"LearningRatePolicy\t"
				+ "MinLearningRate\t"
				+ "MaxLearningRate\t" 
				+ "MaxNumberOfSplits\t"
				+ "BagFraction\t"
				+ "MinExamplesInNode\t"
				+ "NumberOfTrees\t";
	}
	public String getTabSeparatedPrintOut() {
		return String.format(
				learningRatePolicy.name() + "\t" 
				+ "%.4f\t" 
				+ "%.4f\t"
				+ "%d\t"
				+ "%.4f\t"
				+ "%d\t"
				+ "%d\t",
				minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
	}
}