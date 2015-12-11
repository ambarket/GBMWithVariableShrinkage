package parameterTuning;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import dataset.DatasetParameters;
import gbm.GbmParameters;
import parameterTuning.RunDataSummaryRecord.RunFileType;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;


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
		
	public static ImmutableMap<String, String[][]> interestingPredictorGraphsByDataset = new ImmutableMap.Builder<String, String[][]>()
			.put("nasa", new String[][] {
					{"suctionSideDisplacment"},
					{"frequency"},
					{"suctionSideDisplacment", "Frequency"}
				})
			.put("powerPlant", new String[][] {
					{"PEAT"},
					{"RH"},
					{"PEAT", "RH"}
				})
			.put("bikeSharingDay", new String[][] {
					{"mnth"},
					{"weekday"},
					{"mnth", "weekday"}
				})
			.put("crimeCommunities", new String[][] {
					{"PctIlleg"},
					{"racePctWhite"},
					{"PctKids2Par"},
					{"PctHousLess3BR"},
					{"PctKids2Par", "PctIlleg"},
					{"PctKids2Par", "PctHousLess3BR"},
					{"PctKids2Par", "racePctWhite"},
					{"PctKids2Par", "racePctBlack"},
				})
			.put("bikeSharingHourly", new String[][] {
					{"hr"},
					{"hum"},
					{"temp"},
					{"mnth"},
					{"hr", "hum"},
					{"hr", "temp"},
					{"hum", "temp"}
				})
			.build();
	
	public int NUMBER_OF_TREES, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE, NUMBER_OF_RUNS;
	public static double TRAINING_SAMPLE_FRACTION = 0.8;
	public double[] constantLearningRates;
	public double[] minLearningRates;
	public double[] maxLearningRates;
	public double[] bagFractions;
	public int[] maxNumberOfSplts;
	public int[] minExamplesInNode;
	public ImmutableSet<LearningRatePolicy> learningRatePolicies;
	public SplitsPolicy[] splitPolicies;
	public int totalNumberOfTests;
	public DatasetParameters[] datasets;
	public String runDataOutputDirectory;
	public String runDataProcessingDirectory;
	public String locksDirectory;
	public RunFileType runFileType;
	
	public GbmParameters[] parametersList;
	public String runDataFreenasDirectory;
	public String remoteLocksDirectory;
	public String hostsThatShouldShutdownFile;
	
	private static ParameterTuningParameters test5Parameters;
	private static ParameterTuningParameters test6Parameters;
	
	private ParameterTuningParameters() {
		
	}
	
	public static ParameterTuningParameters getRangesForTest5() {
		if (test5Parameters == null) {
			test5Parameters = new ParameterTuningParameters();
			test5Parameters.NUMBER_OF_RUNS = 4;
			test5Parameters.NUMBER_OF_TREES = 150000;
			test5Parameters.CV_NUMBER_OF_FOLDS = 5;
			test5Parameters.CV_STEP_SIZE = 500;
			
			test5Parameters.constantLearningRates = new double[] {0.5, 0.2, 0.1, 0.01, 0.001, 0.0001};
			test5Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test5Parameters.maxLearningRates = new double[] {1, 0.7, 0.4, 0.1};
			test5Parameters.maxNumberOfSplts = new int[] {128, 64, 32, 16, 8, 4, 2, 1};

			test5Parameters.bagFractions = new double[] {0.25, 0.5, 0.75, 1};
			test5Parameters.minExamplesInNode = new int[] {1, 10, 75, 150};	
			test5Parameters.learningRatePolicies = new ImmutableSet.Builder<LearningRatePolicy>()
					.add(LearningRatePolicy.REVISED_VARIABLE)
					.add(LearningRatePolicy.CONSTANT)
					.build();
			test5Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test5Parameters.totalNumberOfTests = 
					((test5Parameters.minLearningRates.length * test5Parameters.maxLearningRates.length) + (test5Parameters.constantLearningRates.length))
					 * test5Parameters.maxNumberOfSplts.length * test5Parameters.bagFractions.length * test5Parameters.minExamplesInNode.length;
			
			test5Parameters.runDataOutputDirectory = (System.getProperty("user.dir") + "/parameterTuning/5/");
			test5Parameters.locksDirectory = System.getProperty("user.dir") + "/locks/5/";
			
			test5Parameters.remoteLocksDirectory = "/mnt/nfs/Austin/GBMWithVariableShrinkage/locks/5/";
			
			test5Parameters.runDataProcessingDirectory = "/mnt/nfs/Austin/GBMWithVariableShrinkage/parameterTuning/5/";
			test5Parameters.runDataFreenasDirectory = "/mnt/raidZ_6TB/Austin/GBMWithVariableShrinkage/parameterTuning/5/";

			test5Parameters.hostsThatShouldShutdownFile = System.getProperty("user.dir") + "/scripts/hostsToShutdownAfterCurrentTest.txt";
			
			test5Parameters.datasets = new DatasetParameters[] {powerPlantParameters, nasaParameters, bikeSharingDayParameters, crimeCommunitiesParameters /*,bikeSharingHourlyParameters*/};
			test5Parameters.runFileType = RunFileType.ParamTuning4;
			
			test5Parameters.parametersList = new GbmParameters[test5Parameters.totalNumberOfTests];
			
			int done = 0;
			for (LearningRatePolicy learningRatePolicy : test5Parameters.learningRatePolicies) {
				for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? test5Parameters.minLearningRates : new double[] {-1}) {
					for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? test5Parameters.maxLearningRates : test5Parameters.constantLearningRates) {
						for (int numberOfSplits : test5Parameters.maxNumberOfSplts) {
							for (double bagFraction : test5Parameters.bagFractions) {
								for (int minExamplesInNode : test5Parameters.minExamplesInNode) {
									for (SplitsPolicy splitPolicy : test5Parameters.splitPolicies) {
										// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
										test5Parameters.parametersList[done] = new GbmParameters(minLR, maxLR, numberOfSplits, 
													bagFraction, minExamplesInNode, test5Parameters.NUMBER_OF_TREES, 
													learningRatePolicy, splitPolicy);
										if (test5Parameters.parametersList[done] == null) {
											System.out.println();
										}
										done++;
									}
								}
							}
						}
					}
				}
			}
		}
		return test5Parameters;
	}
	
	public static ParameterTuningParameters getRangesForTest6() {
		if (test6Parameters == null) {
			test6Parameters = new ParameterTuningParameters();
			test6Parameters.NUMBER_OF_RUNS = 10;
			test6Parameters.NUMBER_OF_TREES = 150000;
			test6Parameters.CV_NUMBER_OF_FOLDS = 5;
			test6Parameters.CV_STEP_SIZE = 500;
			


			
			test6Parameters.constantLearningRates = new double[] {/*0.5, 0.2, 0.1, 0.01, 0.001, */0.0001};
			test6Parameters.minLearningRates = new double[] {0.01, 0.001, 0.0001};
			test6Parameters.maxLearningRates = new double[] {1, 0.7, 0.4, 0.1};
			test6Parameters.maxNumberOfSplts = new int[] {128 /*, 64, 32, 16, 8, 4, 2, 1*/};
			test6Parameters.bagFractions = new double[] {/*0.25, 0.5, 0.75, */1};
			test6Parameters.minExamplesInNode = new int[] {1, 10, 75, 150};	
			test6Parameters.learningRatePolicies = new ImmutableSet.Builder<LearningRatePolicy>()
					.add(LearningRatePolicy.CONSTANT)
					.build();
			test6Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test6Parameters.constantLearningRates = new double[] {/*0.5, 0.2, 0.1, 0.01, 0.001, 0.0001*/};
			test6Parameters.minLearningRates = new double[] {0.01 /*, 0.001, 0.0001*/};
			test6Parameters.maxLearningRates = new double[] {1, /*0.7, 0.4, 0.1*/};
			test6Parameters.maxNumberOfSplts = new int[] {/*128 , 64, 32, 16, 8, 4, 2,*/ 1};
			test6Parameters.bagFractions = new double[] {0.25 /*, 0.5, 0.75, 1*/};
			test6Parameters.minExamplesInNode = new int[] {1, 10, 75, 150};	
			test6Parameters.learningRatePolicies = new ImmutableSet.Builder<LearningRatePolicy>()
					.add(LearningRatePolicy.REVISED_VARIABLE)
					.build();
			test6Parameters.splitPolicies = new SplitsPolicy[] {SplitsPolicy.CONSTANT};
			
			test6Parameters.totalNumberOfTests = 0;
			if (test6Parameters.learningRatePolicies.contains(LearningRatePolicy.CONSTANT)) {
				test6Parameters.totalNumberOfTests += test6Parameters.constantLearningRates.length * test6Parameters.maxNumberOfSplts.length * test6Parameters.bagFractions.length * test6Parameters.minExamplesInNode.length;
			}
			if (test6Parameters.learningRatePolicies.contains(LearningRatePolicy.REVISED_VARIABLE)) {
				test6Parameters.totalNumberOfTests += test6Parameters.minLearningRates.length * test6Parameters.maxLearningRates.length * test6Parameters.maxNumberOfSplts.length * test6Parameters.bagFractions.length * test6Parameters.minExamplesInNode.length;
			}
			
			test6Parameters.runDataOutputDirectory = (System.getProperty("user.dir") + "/parameterTuning/6/");
			test6Parameters.runDataProcessingDirectory = "/mnt/nfs/Austin/GBMWithVariableShrinkage/parameterTuning/6/";
			test6Parameters.runDataFreenasDirectory = "/mnt/raidZ_6TB/Austin/GBMWithVariableShrinkage/parameterTuning/6/";
			
			test6Parameters.locksDirectory = System.getProperty("user.dir") + "/locks/6/";
			
			test6Parameters.datasets = new DatasetParameters[] {powerPlantParameters, nasaParameters, bikeSharingDayParameters, crimeCommunitiesParameters /*,bikeSharingHourlyParameters*/};
			test6Parameters.runFileType = RunFileType.ParamTuning4;
			
			test6Parameters.parametersList = new GbmParameters[test6Parameters.totalNumberOfTests];
			
			int done = 0;
			for (LearningRatePolicy learningRatePolicy : test6Parameters.learningRatePolicies) {
				for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? test6Parameters.minLearningRates : new double[] {-1}) {
					for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? test6Parameters.maxLearningRates : test6Parameters.constantLearningRates) {
						for (int numberOfSplits : test6Parameters.maxNumberOfSplts) {
							for (double bagFraction : test6Parameters.bagFractions) {
								for (int minExamplesInNode : test6Parameters.minExamplesInNode) {
									for (SplitsPolicy splitPolicy : test6Parameters.splitPolicies) {
										// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
										test6Parameters.parametersList[done] = new GbmParameters(minLR, maxLR, numberOfSplits, 
													bagFraction, minExamplesInNode, test6Parameters.NUMBER_OF_TREES, 
													learningRatePolicy, splitPolicy);
										if (test6Parameters.parametersList[done] == null) {
											System.out.println();
										}
										done++;
									}
								}
							}
						}
					}
				}
			}
		}
		return test6Parameters;
	}
}
