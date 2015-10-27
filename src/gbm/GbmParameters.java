package gbm;

import utilities.Logger;

public class GbmParameters {
	// class members
	public double bagFraction; 
	public double maxLearningRate;
	public int numOfTrees;
	
	// tree related parameters
	public int minExamplesInNode;
	public int maxNumberOfSplits;
	
	public GbmParameters(double bagFraction, double maxLearningRate, int numOfTrees, int minExamplesInNode, int maxNumberOfSplits) {
		setBagFraction(bagFraction);
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinObsInNode(minExamplesInNode);
		setmaxNumberOfSplits(maxNumberOfSplits);
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
}