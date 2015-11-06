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
	
	public static final String bikeSharingFiles = System.getProperty("user.dir") + "/data/BikeSharing/";
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	public static final String nasaFiles = System.getProperty("user.dir") + "/data/NASAAirFoild/";
	
	public static final ParameterTuningParameterRanges ranges = ParameterTuningParameterRanges.getRangesForTest3();
	
	public static void runNASA() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset2 = new Dataset(nasaFiles + "data.txt", true, true, 5, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters(trainingDataset2, nasaParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
		/*
		readSortAndSaveResultsAsSortedOptimalParameterRecords(nasaParamTune);
		readOptimalParameterRecordsOldFormat("AirFoil", nasaParamTune);
		*/
	}
	
	public static void runBikeSharing() {
		
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset = new Dataset(bikeSharingFiles + "bikeSharing.txt", true, true, 11, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters(trainingDataset, bikeSharingParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
		
		
		/*
		averageAllRunData(bikeSharingParamTune);
		generateMathematicaLearningCurvesForAllRunData(bikeSharingParamTune + "Averages/");
		readSortAndSaveOptimalParameterRecordsFromRunData(bikeSharingParamTune + "Averages/");
		ArrayList<OptimalParameterRecord> records = OptimalParameterRecord.readOptimalParameterRecords("BikeSharing", bikeSharingParamTune + "Averages/");
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("BikeSharing", bikeSharingParamTune + "Averages/", records);
		*/
	}
	
	public static void runPowerPlant() {
		
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < ranges.NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset3 = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, ranges.TRAINING_SAMPLE_FRACTION);
			tryDifferentParameters(trainingDataset3, powerPlantParamTune, i);
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

	public static void tryDifferentParameters(Dataset dataset, String paramTuneDir, int runNumber) {
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
									System.out.println(String.format(resultMessage + "\n This test took %.4f minutes. Have been runnung for %.4f minutes total.", 
											parameters.getFileNamePrefix(), runNumber, ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
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
									sortedByCvValidationError.add(record);
									sortedByAllDataTestError.add(record);
									sortedByTimeInSeconds.add(record);
									System.out.println(String.format("Averaged runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
											parameters.getFileNamePrefix(), ++done, ranges.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
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
	
	public static void generateMathematicaLearningCurvesForAllRunData(String runDataDirectory) {
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
									MathematicaLearningCurveCreator.createLearningCurveForParameters(runDataDirectory, parameters);
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
		// Temporary to be backwards compatible.
		if (new File(runDataDir + parameters.getFileNamePrefix() + "--runData.txt").exists()) {
			SimpleHostLock.writeDoneLock(runDataDir + parameters.getFileNamePrefix() + "--doneLock.txt");
			return "I've already completed %s on run number %d. (%d out of %d)";
        }
		CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, dataset, ranges.CV_NUMBER_OF_FOLDS, ranges.CV_STEP_SIZE);
		if (ensemble != null) {
			try {
				ensemble.saveDataToFile(runDataDir);
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
		double timeInSeconds = 0, cvTestError = 0, cvValidationError = 0, cvTrainingError = 0, allDataTrainingError = 0, allDataTestError = 0, allDataAvgNumberOfSplits = 0;
		int optimalNumberOfTrees = 0, totalNumberOfTrees= 0, stepSize = 0, numberOfFolds = 0;
		ArrayList<Double> avgCvTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvValidationErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> avgCvTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTrainingErrorByIteration = new ArrayList<Double>();
		ArrayList<Double> allDataTestErrorByIteration = new ArrayList<Double>();
		ArrayList<Integer> avgCvTrainingErrorByIterationCount = new ArrayList<Integer>();
		ArrayList<Integer> avgCvValidationErrorByIterationCount = new ArrayList<Integer>();
		ArrayList<Integer> avgCvTestErrorByIterationCount = new ArrayList<Integer>();
		ArrayList<Integer> allDataTrainingErrorByIterationCount = new ArrayList<Integer>();
		ArrayList<Integer> allDataTestErrorByIterationCount = new ArrayList<Integer>();
		int numberOfTreesFound = 0;
		int numberOfRunsFound = 0;
		// Read through all the files cooresponding to these parameters and average the data.
		for (int runNumber = 0; runNumber < ranges.NUMBER_OF_RUNS; runNumber++) {
			numberOfRunsFound++;
			String runDataFilePath = paramTuneDir + String.format("Run%d/" + parameters.getRunDataSubDirectory(), runNumber) + parameters.getFileNamePrefix() + "--runData.txt";
			if (!new File(runDataFilePath).exists()) {
				System.out.println(String.format("Couldn't find Run%d/" + parameters.getRunDataSubDirectory(), runNumber) + parameters.getFileNamePrefix() + "--runData.txt");
				continue;
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
				
				timeInSeconds += Double.parseDouble(br.readLine().split(": ")[1].trim());
				stepSize = Integer.parseInt(br.readLine().split(": ")[1].trim());
				numberOfFolds = Integer.parseInt(br.readLine().split(": ")[1].trim());
				totalNumberOfTrees += Integer.parseInt(br.readLine().split(": ")[1].trim());
				optimalNumberOfTrees += Integer.parseInt(br.readLine().split(": ")[1].trim());
				cvValidationError += Double.parseDouble(br.readLine().split(": ")[1].trim());
				cvTrainingError += Double.parseDouble(br.readLine().split(": ")[1].trim());
				allDataTrainingError += Double.parseDouble(br.readLine().split(": ")[1].trim());
				cvTestError += Double.parseDouble(br.readLine().split(": ")[1].trim());
				allDataTestError += Double.parseDouble(br.readLine().split(": ")[1].trim());
				allDataAvgNumberOfSplits += Double.parseDouble(br.readLine().split(": ")[1].trim());
				
				String line = null;
			
				// skip relative influences and header
				while (!(line = br.readLine()).startsWith("TreeNumber\tAvgCvTrainingError"));
				// read in error data 
				int index = 0;
				while ((line = br.readLine()) != null) {
					String[] components = line.split("\t");
					if (numberOfTreesFound <= index) {
						avgCvTrainingErrorByIteration.add(Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.add(Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.add(Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.add(Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.add(Double.parseDouble(components[5].trim()));
						avgCvTrainingErrorByIterationCount.add(1);
						avgCvValidationErrorByIterationCount.add(1);
						avgCvTestErrorByIterationCount.add(1);
						allDataTrainingErrorByIterationCount.add(1);
						allDataTestErrorByIterationCount.add(1);
						numberOfTreesFound++;
						index++;
					} else {
						avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) + Double.parseDouble(components[1].trim()));
						avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index) + Double.parseDouble(components[2].trim()));
						avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) + Double.parseDouble(components[3].trim()));
						allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) + Double.parseDouble(components[4].trim()));
						allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) + Double.parseDouble(components[5].trim()));
						avgCvTrainingErrorByIterationCount.set(index, avgCvTrainingErrorByIterationCount.get(index) + 1);
						avgCvValidationErrorByIterationCount.set(index, avgCvValidationErrorByIterationCount.get(index) + 1);
						avgCvTestErrorByIterationCount.set(index, avgCvTestErrorByIterationCount.get(index) + 1);
						allDataTrainingErrorByIterationCount.set(index, allDataTrainingErrorByIterationCount.get(index) + 1);
						allDataTestErrorByIterationCount.set(index, allDataTestErrorByIterationCount.get(index) + 1);
						index++;
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				numberOfRunsFound--;
			}
			// Compute the averages
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
			allDataAvgNumberOfSplits  /= numberOfRunsFound;
			for (int index = 0; index < numberOfTreesFound; index++) {
				avgCvTrainingErrorByIteration.set(index, avgCvTrainingErrorByIteration.get(index) / avgCvTrainingErrorByIterationCount.get(index));
				avgCvValidationErrorByIteration.set(index, avgCvValidationErrorByIteration.get(index)  / avgCvValidationErrorByIterationCount.get(index));
				avgCvTestErrorByIteration.set(index, avgCvTestErrorByIteration.get(index) / avgCvTestErrorByIterationCount.get(index));
				allDataTrainingErrorByIteration.set(index, allDataTrainingErrorByIteration.get(index) / allDataTrainingErrorByIterationCount.get(index));
				allDataTestErrorByIteration.set(index, allDataTestErrorByIteration.get(index) / allDataTestErrorByIterationCount.get(index));
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
						+ "OptimalNumberOfTrees: %d \n"
						+ "CV Validation RMSE: %f \n" 
						+ "CV Training RMSE: %f \n"
						+ "All Data Training RMSE: %f \n"
						+ "CV Test RMSE: %f \n" 
						+ "All Data Test RMSE: %f \n" 
						+ "All Data Avg Number Of Splits: %f\n",
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
						allDataAvgNumberOfSplits));

				bw.write("TreeNumber\tAvgCvTrainingError\tAvgCvValidationError\tAvgCvTestError\tAllDataTrainingError\tAllDataTestError\n");
				for (int i = 0; i < totalNumberOfTrees; i++) {
					bw.write(String.format("%d\t%.5f\t%.5f\t%.5f\t%.5f\t%.5f\n", 
							i+1,
							avgCvTrainingErrorByIteration.get(i),
							avgCvValidationErrorByIteration.get(i),
							avgCvTestErrorByIteration.get(i),
							allDataTrainingErrorByIteration.get(i),
							allDataTestErrorByIteration.get(i)));
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
