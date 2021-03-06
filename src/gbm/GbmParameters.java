package gbm;

import parameterTuning.RunDataSummaryRecord.RunFileType;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.Logger;

public class GbmParameters {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(bagFraction);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((learningRatePolicy == null) ? 0 : learningRatePolicy.hashCode());
		temp = Double.doubleToLongBits(maxLearningRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + maxNumberOfSplits;
		result = prime * result + minExamplesInNode;
		temp = Double.doubleToLongBits(minLearningRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + numOfTrees;
		result = prime * result + ((splitsPolicy == null) ? 0 : splitsPolicy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GbmParameters other = (GbmParameters) obj;
		if (Double.doubleToLongBits(bagFraction) != Double.doubleToLongBits(other.bagFraction))
			return false;
		if (learningRatePolicy != other.learningRatePolicy)
			return false;
		if (Double.doubleToLongBits(maxLearningRate) != Double.doubleToLongBits(other.maxLearningRate))
			return false;
		if (maxNumberOfSplits != other.maxNumberOfSplits)
			return false;
		if (minExamplesInNode != other.minExamplesInNode)
			return false;
		if (Double.doubleToLongBits(minLearningRate) != Double.doubleToLongBits(other.minLearningRate))
			return false;
		if (numOfTrees != other.numOfTrees)
			return false;
		if (splitsPolicy != other.splitsPolicy)
			return false;
		return true;
	}

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
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			setMinLearningRate(minLearningRate);
		} else {
			minLearningRate = -1;
		}
		setBagFraction(bagFraction);
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinExamplesInNode(minExamplesInNode);
		setmaxNumberOfSplits(maxNumberOfSplits);
		this.learningRatePolicy = learningRatePolicy;
		this.splitsPolicy = splitsPolicy;
	}
	
	// Backwards compatability with old data that didnt have minimum learning rate
	public GbmParameters(double maxLearningRate, int maxNumberOfSplits, double bagFraction, int minExamplesInNode, int numOfTrees, LearningRatePolicy learningRatePolicy, SplitsPolicy splitsPolicy) {
		setBagFraction(bagFraction);
		minLearningRate = maxLearningRate;	
		setMaxLearningRate(maxLearningRate);
		setNumOfTrees(numOfTrees);
		setMinExamplesInNode(minExamplesInNode);
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
			Logger.println(Logger.LEVELS.DEBUG, "Max Learning rate must be > 0");
			System.exit(0);
		}
		this.maxLearningRate = maxLearningRate;
	}
	
	private void setMinLearningRate(double minLearningRate) {
		if (minLearningRate <= 0) {
			Logger.println(Logger.LEVELS.DEBUG, "Min Learning rate must be > 0");
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
	
	private void setMinExamplesInNode(int minExamplesInNode) {
		if (minExamplesInNode < 1) {
			Logger.println(Logger.LEVELS.DEBUG, "minExamplesInNode must be >= 1");
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
		return getFileNamePrefix(RunFileType.ParamTuning4);
	}
	public String getFileNamePrefix(RunFileType runFileType) {
		if (runFileType != RunFileType.ParamTuning4) {
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
		} else {
			if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
				return String.format(learningRatePolicy.name() 
						+ "_MinLR-%.6f" 
						+ "_MaxLR-%.6f" 
						+ "_SPLITS-%d"
						+ "_BF-%.2f"
						+ "_MEIN-%d"
						+ "_TREES-%d",
						minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
			} else {
				return String.format(learningRatePolicy.name() 
						+ "_LR-%.6f" 
						+ "_SPLITS-%d"
						+ "_BF-%.2f"
						+ "_MEIN-%d"
						+ "_TREES-%d",
						maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
			}
		}
	}
	public String getRunDataSubDirectory() {
		return getRunDataSubDirectory(RunFileType.ParamTuning4);
	}
	public String getRunDataSubDirectory(RunFileType runFileType) {
		if (runFileType != RunFileType.ParamTuning4) {
			if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
				return String.format("RevisedVariable/%.5fMinLR/%.5fMaxLR/%dSplits/", 
						minLearningRate, maxLearningRate, maxNumberOfSplits);
			} else {
				return String.format("Constant/%.5fLR/%dSplits/", maxLearningRate, maxNumberOfSplits);
			}
		} else {
			if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
				return String.format("RevisedVariable/%.6fMinLR/%.6fMaxLR/%dSplits-%s/%.2fBF/%dMEIN/", 
						minLearningRate, maxLearningRate, maxNumberOfSplits, splitsPolicy.name(), bagFraction, minExamplesInNode);
			} else {
				return String.format("Constant/%.6fLR/%dSplits-%s/%.2fBF/%dMEIN/", 
						maxLearningRate, maxNumberOfSplits, splitsPolicy.name(), bagFraction, minExamplesInNode);
			}
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
					+ "NumberOfTrees";
	}
	public String getTabSeparatedPrintOut() {
		return String.format(
				learningRatePolicy.name() + "\t" 
				+ "%.6f\t" 
				+ "%.6f\t"
				+ "%d\t"
				+ "%.2f\t"
				+ "%d\t"
				+ "%d\t",
				minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode, numOfTrees);
	}
	
	public String getLineSeparatedPrintOut() {
		return
				learningRatePolicy.toString() + "\n" 
				+ "MinLearningRate: " + String.format("%f", minLearningRate).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "\n" 
				+ "MaxLearningRate: " +  String.format("%f", maxLearningRate).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "\n" 
				+ "MaxNumberOfSplits: " + String.valueOf(maxNumberOfSplits) + "\n" 
				+ "BagFraction: " +  String.format("%f", bagFraction).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") + "\n" 
				+ "MinLeafSize: " +  String.valueOf(minExamplesInNode) + "\n";
	}
	
	public String getErrorCurveLatexCaption(String prefix) {
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			return prefix + String.format(" Error Curve (Variable) - "
					+ " MinLearningRate = %.6f" 
					+ " MaxLearningRate = %.6f" 
					+ " MaxSplits = %d"
					+ " BagFraction = %.2f"
					+ " MinExamplesInNode = %d",
					minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		} else {
			return prefix + String.format(" Error Curve (Constant) - " 
					+ " LearningRate = %.6f" 
					+ " MaxSplits = %d"
					+ " BagFraction = %.2f"
					+ " MinExamplesInNode = %d",
					maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		}
	}
	
	public String getErrorCurveLatexFigureReference(String prefix) {
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			return prefix + String.format("ErrorCurveVariable"
					+ "MinLR%.6f" 
					+ "MaxLR%.6f" 
					+ "Splits%d"
					+ "BF%.2f"
					+ "MEIN: %d",
					minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		} else {
			return prefix + String.format("ErrorCurveConstant" 
					+ "LR%.6f" 
					+ "Splits%d"
					+ "BF%.2f"
					+ "MEIN%d",
					maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		}
	}
	
	public String getMetaDataCurveLatexCaption(String prefix) {
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			return prefix + String.format(" (Variable) - "
					+ " MinLearningRate = %.6f" 
					+ " MaxLearningRate = %.6f" 
					+ " MaxSplits = %d"
					+ " BagFraction = %.2f"
					+ " MinExamplesInNode = %d",
					minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		} else {
			return prefix + String.format(" (Constant) - " 
					+ " LearningRate = %.6f" 
					+ " MaxSplits = %d"
					+ " BagFraction = %.2f"
					+ " MinExamplesInNode = %d",
					maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		}
	}
	
	public String getMetaDataCurveLatexFigureReference(String prefix) {
		if (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) {
			return prefix + String.format("Variable"
					+ "MinLR%.6f" 
					+ "MaxLR%.6f" 
					+ "Splits%d"
					+ "BF%.2f"
					+ "MEIN: %d",
					minLearningRate, maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		} else {
			return prefix + String.format("Constant" 
					+ "LR%.6f" 
					+ "Splits%d"
					+ "BF%.2f"
					+ "MEIN%d",
					maxLearningRate, maxNumberOfSplits, bagFraction, minExamplesInNode);
		}
	}
}