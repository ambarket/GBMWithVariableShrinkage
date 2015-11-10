package parameterTuning;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import dataset.DatasetParameters;


public class ParameterTuningParameters {
	
	/*
	public static Map<String, DatasetParameters> datasetParametersMap = ImmutableMap.<String, DatasetParameters>builder()
			.put("powerPlant", new DatasetParameters("powerPlant", "Power Plant", "data/PowerPlant/", "Folds5x2_pp.txt",4))
			.put("nasa", new DatasetParameters("nasa", "Nasa Air Foil", "data/NASAAirFoild/", "data.txt",5))
			.put("bikeSharingDay", new DatasetParameters("bikeSharingDay", "Bike Sharing By Day", "data/BikeSharing/", "bikeSharing.txt",11))
			.put("crimeCommunities", new DatasetParameters("crimeCommunities", "Crime Communities", "data/CrimeCommunities/", "communitiesOnlyPredictive.txt",11)).build();
	*/
	public static DatasetParameters powerPlantParameters = new DatasetParameters("powerPlant", "Power Plant", "data/PowerPlant/", "Folds5x2_pp.txt",4);
	public static DatasetParameters nasaParameters = new DatasetParameters("nasa", "Nasa Air Foil", "data/NASAAirFoild/", "data.txt",5);
	public static DatasetParameters bikeSharingDayParameters = new DatasetParameters("bikeSharingDay", "Bike Sharing By Day", "data/BikeSharing/", "bikeSharing.txt",11);
	public static DatasetParameters crimeCommunitiesParameters = new DatasetParameters("crimeCommunities", "Crime Communities", "data/CrimeCommunities/", "communitiesOnlyPredictive.txt",122);
			
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
	public DatasetParameters[] datasets;
	public String parameterTuningDirectory;
	
	private static ParameterTuningParameters test3Parameters;
	private static ParameterTuningParameters test4Parameters;
	
	private ParameterTuningParameters() {
		
	}
	
	public static ParameterTuningParameters getRangesForTest3() {
		if (test3Parameters == null) {
			test3Parameters = new ParameterTuningParameters();
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
			
			test3Parameters.parameterTuningDirectory = (System.getProperty("user.dir") + "/parameterTuning/3/");
			
			test3Parameters.datasets = new DatasetParameters[] {powerPlantParameters, nasaParameters, bikeSharingDayParameters, crimeCommunitiesParameters};
		}
		return test3Parameters;
	}
	
	synchronized public static ParameterTuningParameters getRangesForTest4() {
		if (test3Parameters == null) {
			test3Parameters = new ParameterTuningParameters();
			test3Parameters.NUMBER_OF_RUNS = 10;
			test3Parameters.NUMBER_OF_TREES = 150000;
			test3Parameters.CV_NUMBER_OF_FOLDS = 4;
			test3Parameters.CV_STEP_SIZE = 5000;
			test3Parameters.TRAINING_SAMPLE_FRACTION = 0.8;
			
			test3Parameters.constantLearningRates = new double[] {1, 0.7, 0.4, 0.1, 0.01, 0.001, 0.0001};
			test3Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test3Parameters.maxLearningRates = new double[] {0.4, 0.1};
			test3Parameters.maxNumberOfSplts = new int[] {16, 8, 4, 2, 1};

			test3Parameters.bagFractions = new double[] {0.75};
			test3Parameters.minExamplesInNode = new int[] {1};	
			test3Parameters.learningRatePolicies = new LearningRatePolicy[] {LearningRatePolicy.REVISED_VARIABLE, LearningRatePolicy.CONSTANT};
			test3Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.RANDOM};
			
			test3Parameters.totalNumberOfTests = 
					((test3Parameters.minLearningRates.length * test3Parameters.maxLearningRates.length) + (test3Parameters.constantLearningRates.length))
					 * test3Parameters.maxNumberOfSplts.length * test3Parameters.bagFractions.length * test3Parameters.minExamplesInNode.length;
		}
		return test3Parameters;
	}
}
