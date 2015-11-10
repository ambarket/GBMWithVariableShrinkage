package parameterTuning;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.cv.CrossValidatedResultFunctionEnsemble;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;

import parameterTuning.OptimalParameterRecord.RunFileType;
import parameterTuning.plotting.MathematicaLearningCurveCreator;
import parameterTuning.plotting.PairwiseOptimalParameterRecordPlots;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import dataset.Dataset;


public class ParameterTuningTest3 {
	public static final String powerPlantParamTune = System.getProperty("user.dir") + "/data/paramTuning3/powerPlantParameterTuning/";
	public static final String nasaParamTune = System.getProperty("user.dir") + "/data/paramTuning3/nasaParameterTuning/";
	public static final String bikeSharingParamTune = System.getProperty("user.dir") + "/data/paramTuning3/bikeSharingParameterTuning/";
	public static final String crimeCommunitiesParamTune = System.getProperty("user.dir") + "/data/paramTuning3/crimeCommunitiesParameterTuning/";
	
	public static final String bikeSharingFiles = System.getProperty("user.dir") + "/data/BikeSharing/";
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	public static final String nasaFiles = System.getProperty("user.dir") + "/data/NASAAirFoild/";
	public static final String crimeFiles = System.getProperty("user.dir") + "/data/CrimeCommunities/";
	
	public static final ParameterTuningParameterRanges ranges = ParameterTuningParameterRanges.getRangesForTest3();
	
	public static void runCrimeCommunities() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset2 = new Dataset(crimeFiles + "communitiesOnlyPredictive.txt", true, true, 122, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters("Crime Communities", trainingDataset2, crimeCommunitiesParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static void runNASA() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset2 = new Dataset(nasaFiles + "data.txt", true, true, 5, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters("NASA", trainingDataset2, nasaParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static void processNASA() {
		averageAllRunData(nasaParamTune);
		generateMathematicaLearningCurvesForAllRunData("NASA Air Foil", nasaParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(nasaParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("NASA Air Foil", nasaParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("NASA Air Foil", nasaParamTune + "Averages/", records);
	}
	
	
	public static void runBikeSharing() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset = new Dataset(bikeSharingFiles + "bikeSharing.txt", true, true, 11, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters("Bike Sharing", trainingDataset, bikeSharingParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static void processBikeSharing() {
		averageAllRunData(bikeSharingParamTune);
		generateMathematicaLearningCurvesForAllRunData("Bike Sharing", bikeSharingParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(bikeSharingParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("BikeSharing", bikeSharingParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("BikeSharing", bikeSharingParamTune + "Averages/", records);
	}
	
	public static void processPowerPlant() {
		averageAllRunData(powerPlantParamTune);
		generateMathematicaLearningCurvesForAllRunData("PowerPlant", powerPlantParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(powerPlantParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("PowerPlant", powerPlantParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("PowerPlant", powerPlantParamTune + "Averages/", records);
	}
	
	public static void processCrimeCommunities() {
		averageAllRunData(crimeCommunitiesParamTune);
		generateMathematicaLearningCurvesForAllRunData("CrimeCommunities", crimeCommunitiesParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(crimeCommunitiesParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("CrimeCommunities", crimeCommunitiesParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("CrimeCommunities", crimeCommunitiesParamTune + "Averages/", records);
	}
	
	public static void runPowerPlant() {
		
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset3 = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters("Power Plant", trainingDataset3, powerPlantParamTune, i);
		}
		
		GradientBoostingTree.executor.shutdownNow();
		
		/*
		averageAllRunData(powerPlantParamTune);
		generateMathematicaLearningCurvesForAllRunData(powerPlantParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(powerPlantParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("PowerPlant", powerPlantParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("PowerPlant", powerPlantParamTune + "Averages/", records);
		*/
		
	}

	public static void tryDifferentParameters(String datasetName, Dataset dataset, String paramTuneDir, int runNumber) {
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : ranges.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.maxLearningRates : ranges.constantLearningRates) {
					for (int numberOfSplits : ranges.maxNumberOfSplts) {
						for (double bagFraction : ranges.bagFractions) {
							for (int minExamplesInNode : ranges.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : ranges.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, ranges.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();
									String resultMessage = performCrossValidationUsingParameters(parameters, dataset, paramTuneDir, runNumber);
									System.out.println(String.format(resultMessage + "\n This " + datasetName + " test took %.4f minutes. Have been runnung for %.4f minutes total.", 
											parameters.getFileNamePrefix(), runNumber, ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
									//timer.start();
									//System.gc();
									//System.out.println(String.format("Spent %.4f seconds doing garabge collection", timer.getElapsedMinutes()));
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static void averageAllRunData(String paramTuneDir) {
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : ranges.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.maxLearningRates : ranges.constantLearningRates) {
					for (int numberOfSplits : ranges.maxNumberOfSplts) {
						for (double bagFraction : ranges.bagFractions) {
							for (int minExamplesInNode : ranges.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : ranges.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, ranges.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();
									averageRunDataForParameters(parameters, paramTuneDir);
									System.out.println(String.format("Averaged runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
											parameters.getFileNamePrefix(), ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param runDataDirectory Should wither be .../run0/ or .../Averages/
	 */
	public static void readSortAndSaveOptimalParameterRecordsFromRunData(String runDataDirectory) {
		PriorityQueue<OptimalParameterRecord> sortedByCvValidationError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvValidationErrorComparator());
		PriorityQueue<OptimalParameterRecord> sortedByAllDataTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTestErrorComparator());
		PriorityQueue<OptimalParameterRecord> sortedByTimeInSeconds = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.TimeInSecondsComparator());
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : ranges.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.maxLearningRates : ranges.constantLearningRates) {
					for (int numberOfSplits : ranges.maxNumberOfSplts) {
						for (double bagFraction : ranges.bagFractions) {
							for (int minExamplesInNode : ranges.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : ranges.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, ranges.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();

									OptimalParameterRecord record = OptimalParameterRecord.readOptimalParameterRecordFromRunDataFile(runDataDirectory, parameters);
									if (record != null) {
										sortedByCvValidationError.add(record);
										sortedByAllDataTestError.add(record);
										sortedByTimeInSeconds.add(record);
										System.out.println(String.format("Created optimalParameterRecord for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
												parameters.getFileNamePrefix(), ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
									} else {
										System.out.println(String.format("Failed to create optimalParameterRecord for %s because runData was not found (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
												parameters.getFileNamePrefix(), ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
									}
								}
							}
						}
					}
				}
			}
		}
		OptimalParameterRecord.saveOptimalParameterRecords(runDataDirectory, "SortedByCvValidationError", sortedByCvValidationError);
		OptimalParameterRecord.saveOptimalParameterRecords(runDataDirectory, "SortedByAllDataTestError", sortedByAllDataTestError);
		OptimalParameterRecord.saveOptimalParameterRecords(runDataDirectory, "SortedByTimeInSeconds", sortedByTimeInSeconds);
	}
	
	public static void generateMathematicaLearningCurvesForAllRunData(String datasetName, String runDataDirectory) {
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : ranges.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? ranges.maxLearningRates : ranges.constantLearningRates) {
					for (int numberOfSplits : ranges.maxNumberOfSplts) {
						for (double bagFraction : ranges.bagFractions) {
							for (int minExamplesInNode : ranges.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : ranges.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, ranges.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();
									MathematicaLearningCurveCreator.createLearningCurveForParameters(datasetName, runDataDirectory, parameters);
									System.out.println(String.format("Created learning curve for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
											parameters.getFileNamePrefix(), ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
								}
							}
						}
					}
				}
			}
		}
	}
	
	
	//----------------------------------------------Private Per Parameter Helpers-----------------------------------------------------------------------
	
	private static String performCrossValidationUsingParameters(GbmParameters parameters, Dataset dataset, String paramTuneDir, int runNumber) {
		String runDataDir = paramTuneDir + String.format("Run%d/" + parameters.getRunDataSubDirectory(), runNumber);
		new File(runDataDir).mkdirs();
		if (!SimpleHostLock.checkAndClaimHostLock(runDataDir + parameters.getFileNamePrefix() + "--hostLock.txt")) {
			return "Another host has already claimed %s on run number %d. (%d out of %d)";
		}
		if (SimpleHostLock.checkDoneLock(runDataDir + parameters.getFileNamePrefix() + "--doneLock.txt")) {
			return "Already completed %s on run number %d. (%d out of %d)";
		}
		CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, dataset, ranges.CV_NUMBER_OF_FOLDS, ranges.CV_STEP_SIZE);
		if (ensemble != null) {
			try {
				ensemble.saveRunDataToFile(runDataDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
			SimpleHostLock.writeDoneLock(runDataDir + parameters.getFileNamePrefix() + "--doneLock.txt");
			return "Finished %s on run number %d. (%d out of %d)";
		} else {
			SimpleHostLock.writeDoneLock(runDataDir + parameters.getFileNamePrefix() + "--doneLock.txt");
			return "Failed to build %s on run number %d due to impossible parameters. (%d out of %d)";
		}
	}
	
	private static void averageRunDataForParameters(GbmParameters parameters, String paramTuneDir) {
		double timeInSeconds = 0, 
				cvTestError = 0, cvValidationError = 0, cvTrainingError = 0, 
				allDataTrainingError = 0, allDataTestError = 0,
				cvEnsembleTrainingError = 0, cvEnsembleTestError = 0,
				avgNumberOfSplits = 0, stdDevNumberOfSplits = 0,
				avgLearningRate = 0, stdDevLearningRate = 0;
		int optimalNumberOfTrees = 0, totalNumberOfTrees= 0, stepSize = 0, numberOfFolds = 0;
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> cvEnsembleTestErrorByIteration = new ArrayList<Double>();
		
		ArrayList<Double> examplesInNodeMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> examplesInNodeStdDevByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateMeanByIteration = new ArrayList<Double>();
		ArrayList<Double> learningRateStdDevByIteration = new ArrayList<Double>();
		ArrayList<Integer> actualNumberOfSplitsByIteration = new ArrayList<Integer>();

		
		ArrayList<Integer> totalNumberOfTreesByRunNumber = new ArrayList<Integer>();
		
		int numberOfTreesFound = 0;
		int numberOfRunsFound = 0;
		RunFileType runFileType = null;
		// Read through all the files cooresponding to these parameters and average the data.
		for (int runNumber = 0; runNumber < ranges.NUMBER_OF_RUNS; runNumber++) {
			
			// Get the summary info at the top of the file.
			OptimalParameterRecord summaryInfo = OptimalParameterRecord.readOptimalParameterRecordFromRunDataFile(paramTuneDir + "Run" + runNumber + "/", parameters);
			if (summaryInfo == null) {
				// Run data file wasn't found. Continue to next iteration.
				continue;
			} else {
				numberOfRunsFound++;
			}
			
			// Assume the runFileType of the first run. They could be mixed and basically want to revert to the most basic form.
			runFileType = (runFileType == null) ? summaryInfo.runFileType : runFileType; 

			// We have these attributes for all versions of the run files.
			stepSize += summaryInfo.stepSize;
			numberOfFolds += summaryInfo.numberOfFolds;
			totalNumberOfTrees += summaryInfo.totalNumberOfTrees;
			optimalNumberOfTrees += summaryInfo.optimalNumberOfTrees;
			cvValidationError += summaryInfo.cvValidationError;
			cvTrainingError += summaryInfo.cvTrainingError;
			allDataTrainingError += summaryInfo.allDataTrainingError;
			cvTestError += summaryInfo.cvTestError;
			allDataTestError += summaryInfo.allDataTestError;
			// Don't have timeInSeconds for the original runFiles
			if (runFileType != RunFileType.Original) {
				timeInSeconds += summaryInfo.timeInSeconds;
			}
			// Changed things in the middle of ParamTuning3
			if (runFileType == RunFileType.ParamTuning3_OLD) {
				avgNumberOfSplits += summaryInfo.avgNumberOfSplits;
			}
			if (runFileType == RunFileType.ParamTuning3_NEW) {
				cvEnsembleTrainingError += summaryInfo.cvEnsembleTrainingError;
				cvEnsembleTestError += summaryInfo.cvEnsembleTestError;
				avgNumberOfSplits += summaryInfo.avgNumberOfSplits;
				stdDevNumberOfSplits += summaryInfo.stdDevNumberOfSplits;
				avgLearningRate += summaryInfo.avgLearningRate;
				stdDevLearningRate += summaryInfo.stdDevLearningRate;
			}

			try {
				// ALready know it exists based on successful creation of OptimalParameterRecord
				String runDataFilePath = paramTuneDir + String.format("Run%d/" + parameters.getRunDataSubDirectory(), runNumber) + parameters.getFileNamePrefix() + "--runData.txt";
				String line = null;
				BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
				
				// TODO: Average those too.
				// skip summary info, per example data, relative influences, and header
				while (!(line = br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
				// read in per tree information
				int index = 0;
				while ((line = br.readLine()) != null) {
					String[] components = line.split("\t");
					if (numberOfTreesFound <= index) {
						avgCvTrainingErrorByIteration.add(Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.add(Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.add(Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.add(Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.add(Double.parseDouble(components[5].trim()));
						if (runFileType == RunFileType.ParamTuning3_NEW) {
							cvEnsembleTrainingErrorByIteration.add(Double.parseDouble(components[6].trim()));
							cvEnsembleTestErrorByIteration.add(Double.parseDouble(components[7].trim()));
							examplesInNodeMeanByIteration.add(Double.parseDouble(components[8].trim()));
							examplesInNodeStdDevByIteration.add(Double.parseDouble(components[9].trim()));
							learningRateMeanByIteration.add(Double.parseDouble(components[10].trim()));
							learningRateStdDevByIteration.add(Double.parseDouble(components[11].trim()));
							actualNumberOfSplitsByIteration.add(Integer.parseInt(components[12].trim()));
						}
						numberOfTreesFound++;
						index++;
					} else {
						avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) + Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index) + Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) + Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) + Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) + Double.parseDouble(components[5].trim()));
						if (runFileType == RunFileType.ParamTuning3_NEW) {
							cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) + Double.parseDouble(components[6].trim()));
							cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) + Double.parseDouble(components[7].trim()));
							examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) + Double.parseDouble(components[8].trim()));
							examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) + Double.parseDouble(components[9].trim()));
							learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) + Double.parseDouble(components[10].trim()));
							learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) + Double.parseDouble(components[11].trim()));
							actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) + Integer.parseInt(components[12].trim()));
						}
						index++;
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} // End runNumber Loop
		if (numberOfRunsFound == 0) {
			return;
			
		}
		// Compute the averages. Don't need to worry about runFileTypes b/c/ they will just be 0 if they aren't present.
		timeInSeconds /= numberOfRunsFound;
		stepSize  /= numberOfRunsFound;
		numberOfFolds  /= numberOfRunsFound;
		totalNumberOfTrees  /= numberOfRunsFound;
		optimalNumberOfTrees  /= numberOfRunsFound;
		cvValidationError  /= numberOfRunsFound;
		cvTrainingError  /= numberOfRunsFound;
		allDataTrainingError  /= numberOfRunsFound;
		cvTestError  /= numberOfRunsFound;
		allDataTestError  /= numberOfRunsFound;
		cvEnsembleTrainingError /= numberOfRunsFound;
		cvEnsembleTestError /= numberOfRunsFound;
		avgNumberOfSplits  /= numberOfRunsFound;
		stdDevNumberOfSplits /= numberOfRunsFound;
		avgLearningRate /= numberOfRunsFound;
		stdDevLearningRate /= numberOfRunsFound;
		
		int minNumberOfTreesAllRunsHave = Integer.MAX_VALUE;
		for (int i : totalNumberOfTreesByRunNumber) {
			if (i < minNumberOfTreesAllRunsHave) { minNumberOfTreesAllRunsHave = i;}
		}
		for (int index = 0; index < numberOfTreesFound; index++) {
			if (index < minNumberOfTreesAllRunsHave) {
				avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) / numberOfRunsFound);
				avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index)  / numberOfRunsFound);
				avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) / numberOfRunsFound);
				allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) / numberOfRunsFound);
				allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) / numberOfRunsFound);
				if (runFileType == RunFileType.ParamTuning3_NEW) {
					cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) / numberOfRunsFound);
					cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) / numberOfRunsFound);
					examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) / numberOfRunsFound);
					examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) / numberOfRunsFound);
					learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) / numberOfRunsFound);
					learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) / numberOfRunsFound);
					actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) / numberOfRunsFound);
				}
			} else {
				int numberOfRunsWithThisTree = 0;
				for (int i : totalNumberOfTreesByRunNumber) {
					if (index < i) { numberOfRunsWithThisTree++; }
				}
				avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
				avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index)  / numberOfRunsWithThisTree);
				avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
				allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
				allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
				if (runFileType == RunFileType.ParamTuning3_NEW) {
					cvEnsembleTrainingErrorByIteration.set(index, cvEnsembleTrainingErrorByIteration.get(index) / numberOfRunsWithThisTree);
					cvEnsembleTestErrorByIteration.set(index, cvEnsembleTestErrorByIteration.get(index) / numberOfRunsWithThisTree);
					examplesInNodeMeanByIteration.set(index, examplesInNodeMeanByIteration.get(index) / numberOfRunsWithThisTree);
					examplesInNodeStdDevByIteration.set(index, examplesInNodeStdDevByIteration.get(index) / numberOfRunsWithThisTree);
					learningRateMeanByIteration.set(index, learningRateMeanByIteration.get(index) / numberOfRunsWithThisTree);
					learningRateStdDevByIteration.set(index, learningRateStdDevByIteration.get(index) / numberOfRunsWithThisTree);
					actualNumberOfSplitsByIteration.set(index, actualNumberOfSplitsByIteration.get(index) / numberOfRunsWithThisTree);
				}
			}
		}
		
		// Save them to a new file to be processed later.
		try {
			String averageRunDataDirectory = paramTuneDir + "Averages/" + parameters.getRunDataSubDirectory();
			new File(averageRunDataDirectory).mkdirs();
			BufferedWriter bw = new BufferedWriter(new PrintWriter(averageRunDataDirectory + parameters.getFileNamePrefix() + "--runData.txt"));

			bw.write(String.format("Time In Seconds: %f \n"
					+ "Step Size: %d \n"
					+ "Number Of Folds: %d \n"
					+ "TotalNumberOfTrees: %d \n"
					+ "OptimalNumberOfTrees (ONOT): %d \n"
					+ "Avg CV Validation RMSE @ ONOT: %f \n" 
					+ "Avg CV Training RMSE @ ONOT: %f \n"
					+ "All Data Training RMSE @ ONOT: %f \n"
					+ "Avg CV Test RMSE @ ONOT: %f \n" 
					+ "All Data Test RMSE @ ONOT: %f \n" 
					+ "CV Ensemble Training RMSE @ ONOT: %f \n" 
					+ "CV Ensemble Test RMSE @ ONOT: %f\n"
					+ "All Data Avg Number Of Splits (All Trees): %f\n"
					+ "All Data Number Of Splits Std Dev (All Trees): %f\n"
					+ "Learning Rate Avg of Per Example Averages: %.8f\n"
					+ "Learning Rate Std Dev of Per Example Averages: %.8f\n"
					+ "Number of runs found: %d\n"
					+ "Number of trees found: %d\n",
			timeInSeconds,
			stepSize,
			numberOfFolds,
			totalNumberOfTrees,
			optimalNumberOfTrees, 
			cvValidationError,
			cvTrainingError,
			allDataTrainingError,
			cvTestError,
			allDataTestError,
			cvEnsembleTrainingError,
			cvEnsembleTestError,
			avgNumberOfSplits,
			stdDevNumberOfSplits,
			avgLearningRate,
			stdDevLearningRate,
			numberOfRunsFound,
			numberOfTreesFound));

			bw.write("TreeNumber\t"
					+ "AvgCvTrainingError\t"
					+ "AvgCvValidationError\t"
					+ "AvgCvTestError\t"
					+ "AllDataTrainingError\t"
					+ "AllDataTestError\t"
					+ "CvEnsembleTrainingError\t"
					+ "CvEnsembleTestError\t"
					+ "ExamplesInNodeMean\t"
					+ "ExamplesInNodeStdDev\t"
					+ "LearningRateMean\t"
					+ "LearningRateStdDev\t"
					+ "ActualNumberOfSplits\n");
			
			if (runFileType == RunFileType.ParamTuning3_NEW) {
				for (int i = 0; i < numberOfTreesFound; i++) {
					bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.8f\t%.8f\t%d\n", 
							i+1,
							avgCvTrainingErrorByIteration.get(i),
							avgCvValidationErrorByIteration.get(i),
							avgCvTestErrorByIteration.get(i),
							allDataTrainingErrorByIteration.get(i),
							allDataTestErrorByIteration.get(i),
							cvEnsembleTrainingErrorByIteration.get(i),
							cvEnsembleTestErrorByIteration.get(i),
							examplesInNodeMeanByIteration.get(i),
							examplesInNodeStdDevByIteration.get(i),
							learningRateMeanByIteration.get(i),
							learningRateStdDevByIteration.get(i),
							actualNumberOfSplitsByIteration.get(i)));
				}
			} else {
				// THis is an old run data file, still want to print out 0's so that methods that process this data can assume
				//	something is present.
				for (int i = 0; i < numberOfTreesFound; i++) {
					bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\t%.8f\t%.8f\t%d\n", 
							i+1,
							avgCvTrainingErrorByIteration.get(i),
							avgCvValidationErrorByIteration.get(i),
							avgCvTestErrorByIteration.get(i),
							allDataTrainingErrorByIteration.get(i),
							allDataTestErrorByIteration.get(i),
							0.0,
							0.0,
							0.0,
							0.0,
							0.0,
							0.0,
							0));
				}
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
