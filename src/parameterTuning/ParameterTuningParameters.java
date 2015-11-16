package parameterTuning;

import parameterTuning.RunDataSummaryRecord.RunFileType;
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
	public static DatasetParameters bikeSharingHourlyParameters = new DatasetParameters("bikeSharingHourly", "Bike Sharing By Hour", "data/BikeSharing/", "bikeSharingHourly_OnlyFirstYear.txt",12);
	public static DatasetParameters crimeCommunitiesParameters = new DatasetParameters("crimeCommunities", "Crime Communities", "data/CrimeCommunities/", "communitiesOnlyPredictive.txt",122);
			
	public int NUMBER_OF_TREES, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE, NUMBER_OF_RUNS;
	public static double TRAINING_SAMPLE_FRACTION = 0.8;
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
	public String runDataOutputDirectory;
	public String runDataProcessingDirectory;
	public String locksDirectory;
	public RunFileType runFileType;
	
	private static ParameterTuningParameters test3Parameters;
	private static ParameterTuningParameters test4Parameters;
	private static ParameterTuningParameters test5Parameters;
	
	private ParameterTuningParameters() {
		
	}
	
	public static ParameterTuningParameters getRangesForTest3() {
		if (test3Parameters == null) {
			test3Parameters = new ParameterTuningParameters();
			test3Parameters.NUMBER_OF_RUNS = 10; 
			test3Parameters.NUMBER_OF_TREES = 150000;
			test3Parameters.CV_NUMBER_OF_FOLDS = 4;
			test3Parameters.CV_STEP_SIZE = 5000;
			
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
			
			test3Parameters.runDataOutputDirectory = (System.getProperty("user.dir") + "/parameterTuning/3/");
			
			test3Parameters.datasets = new DatasetParameters[] {powerPlantParameters, nasaParameters, bikeSharingHourlyParameters, crimeCommunitiesParameters, bikeSharingDayParameters};
			test3Parameters.runFileType = RunFileType.ParamTuning3;
		}
		return test3Parameters;
	}
	
	public static ParameterTuningParameters getRangesForTest4() {
		if (test4Parameters == null) {
			test4Parameters = new ParameterTuningParameters();
			test4Parameters.NUMBER_OF_RUNS = 10;
			test4Parameters.NUMBER_OF_TREES = 150000;
			test4Parameters.CV_NUMBER_OF_FOLDS = 4;
			test4Parameters.CV_STEP_SIZE = 5000;
			
			test4Parameters.constantLearningRates = new double[] {0.1, 0.01, 0.001, 0.0001};
			test4Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test4Parameters.maxLearningRates = new double[] {1, 0.7, 0.4, 0.1};
			test4Parameters.maxNumberOfSplts = new int[] {128, 64, 32, 16, 8, 4, 2, 1};

			test4Parameters.bagFractions = new double[] {0.25, 0.5, 0.75, 1};
			test4Parameters.minExamplesInNode = new int[] {1, 50, 100, 250};	
			test4Parameters.learningRatePolicies = new LearningRatePolicy[] {LearningRatePolicy.REVISED_VARIABLE, LearningRatePolicy.CONSTANT};
			test4Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test4Parameters.totalNumberOfTests = 
					((test4Parameters.minLearningRates.length * test4Parameters.maxLearningRates.length) + (test4Parameters.constantLearningRates.length))
					 * test4Parameters.maxNumberOfSplts.length * test4Parameters.bagFractions.length * test4Parameters.minExamplesInNode.length;
			
			test4Parameters.runDataOutputDirectory = (System.getProperty("user.dir") + "/parameterTuning/4/");
			test4Parameters.runDataProcessingDirectory = "/mnt/freenas/Austin/GBMWithVariableShrinkage/parameterTuning/4/";
			test4Parameters.locksDirectory = (System.getProperty("user.dir") + "/locks/4/");
			
			test4Parameters.datasets = new DatasetParameters[] {/*nasaParameters,*/ bikeSharingDayParameters, powerPlantParameters, crimeCommunitiesParameters, bikeSharingHourlyParameters};
			test4Parameters.runFileType = RunFileType.ParamTuning4;
		}
		return test4Parameters;
	}
	
	public static ParameterTuningParameters getRangesForTest5() {
		if (test5Parameters == null) {
			test5Parameters = new ParameterTuningParameters();
			test5Parameters.NUMBER_OF_RUNS = 5;
			test5Parameters.NUMBER_OF_TREES = 2;
			test5Parameters.CV_NUMBER_OF_FOLDS = 4;
			test5Parameters.CV_STEP_SIZE = 2;
			
			test5Parameters.constantLearningRates = new double[] {0.1, 0.01, 0.001, 0.0001};
			test5Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test5Parameters.maxLearningRates = new double[] {0.4, 0.1};
			test5Parameters.maxNumberOfSplts = new int[] {4, 2, 1};

			test5Parameters.bagFractions = new double[] {1};
			test5Parameters.minExamplesInNode = new int[] {250};	
			test5Parameters.learningRatePolicies = new LearningRatePolicy[] {LearningRatePolicy.REVISED_VARIABLE, LearningRatePolicy.CONSTANT};
			test5Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test5Parameters.totalNumberOfTests = 
					((test5Parameters.minLearningRates.length * test5Parameters.maxLearningRates.length) + (test5Parameters.constantLearningRates.length))
					 * test5Parameters.maxNumberOfSplts.length * test5Parameters.bagFractions.length * test5Parameters.minExamplesInNode.length;
			
			test5Parameters.runDataOutputDirectory = (System.getProperty("user.dir") + "/parameterTuning/5/");
			test5Parameters.runDataProcessingDirectory = (System.getProperty("user.dir") + "/parameterTuning/5/");
			test5Parameters.locksDirectory = (System.getProperty("user.dir") + "/locks/5/");
			
			test5Parameters.datasets = new DatasetParameters[] {bikeSharingHourlyParameters};
			test5Parameters.runFileType = RunFileType.ParamTuning4;
		}
		return test5Parameters;
	}
}
