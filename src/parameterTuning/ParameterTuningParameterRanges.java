package parameterTuning;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;


public class ParameterTuningParameterRanges {
	public int NUMBER_OF_TREES, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE, NUMBER_OF_RUNS;
	public double TRAINING_SAMPLE_FRACTION;
	public double[] constantLearningRates;
	public double[] minLearningRates;
	public double[] maxLearningRates;
	public double[] bagFractions;
	public int[] maxNumberOfSplts;
	public int[] minExamplesInNode;
	public LearningRatePolicy[] learningRatePolicies;
	public SplitsPolicy[] splitPolicies;
	public int totalNumberOfTests;
	
	private static ParameterTuningParameterRanges  test3Parameters = null; 
	
	private ParameterTuningParameterRanges() {
		
	}
	
	synchronized public static ParameterTuningParameterRanges getRangesForTest3() {
		if (test3Parameters == null) {
			test3Parameters = new ParameterTuningParameterRanges();
			test3Parameters.NUMBER_OF_RUNS = 10;
			test3Parameters.NUMBER_OF_TREES = 150000;
			test3Parameters.CV_NUMBER_OF_FOLDS = 4;
			test3Parameters.CV_STEP_SIZE = 5000;
			test3Parameters.TRAINING_SAMPLE_FRACTION = 0.8;
			
			test3Parameters.constantLearningRates = new double[] {1, 0.7, 0.4, 0.1, 0.01, 0.001, 0.0001};
			test3Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test3Parameters.maxLearningRates = new double[] {1, 0.7, 0.4, 0.1};
			test3Parameters.maxNumberOfSplts = new int[] {128, 64, 32, 16, 8, 4, 2, 1};

			test3Parameters.bagFractions = new double[] {0.75};
			test3Parameters.minExamplesInNode = new int[] {1};	
			test3Parameters.learningRatePolicies = new LearningRatePolicy[] {LearningRatePolicy.REVISED_VARIABLE, LearningRatePolicy.CONSTANT};
			test3Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test3Parameters.totalNumberOfTests = 
					((test3Parameters.minLearningRates.length * test3Parameters.maxLearningRates.length) + (test3Parameters.constantLearningRates.length))
					 * test3Parameters.maxNumberOfSplts.length * test3Parameters.bagFractions.length * test3Parameters.minExamplesInNode.length;
		}
		return test3Parameters;
	}
}
