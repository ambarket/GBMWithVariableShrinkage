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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.StopWatch;
import dataset.Dataset;


public class ParameterTuningTest3 {
	public static final String powerPlantParamTune = System.getProperty("user.dir") + "/data/paramTuning3/powerPlantParameterTuning/";
	public static final String nasaParamTune = System.getProperty("user.dir") + "/data/paramTuning3/nasaParameterTuning/";
	public static final String bikeSharingParamTune = System.getProperty("user.dir") + "/data/paramTuning3/bikeSharingParameterTuning/";
	
	public static final String bikeSharingFiles = System.getProperty("user.dir") + "/data/BikeSharing/";
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	public static final String nasaFiles = System.getProperty("user.dir") + "/data/NASAAirFoild/";
	
	public static final int NUMBER_OF_TREES = 150000;
	public static final double BAG_FRACTION = 0.75;
	public static final int MIN_EXAMPLES_IN_NODE = 1;
	public static final int CV_NUMBER_OF_FOLDS = 4;
	public static final int CV_STEP_SIZE = 5000;
	public static final double TRAINING_SAMPLE_FRACTION = 0.8;
	
	public static final LearningRatePolicy LEARNING_RATE_POLICY = LearningRatePolicy.CONSTANT;
	public static final SplitsPolicy SPLITS_POLICY = SplitsPolicy.CONSTANT;
	
	public static final double[] constantLearningRates = {1, 0.7, 0.4, 0.1, 0.01, 0.001, 0.0001};
	public static final int[] splits = {128, 64, 32, 16, 8, 4, 2, 1};
	public static final double[] minLearningRates = {0.01, 0.001, 0.0001};
	public static final double[] maxLearningRates = {1, 0.7, 0.4, 0.1};
	public static final int NUMBER_OF_RUNS = 10;
	
	public static void runNASA() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset2 = new Dataset(nasaFiles + "data.txt", true, true, 5, TRAINING_SAMPLE_FRACTION);
			tryDifferentRevisedVariableParameters(trainingDataset2, nasaParamTune, i);
			tryDifferentConstantParameters(trainingDataset2, nasaParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
		/*
		readSortAndSaveResultsAsSortedOptimalParameterRecords(nasaParamTune);
		readOptimalParameterRecordsOldFormat("AirFoil", nasaParamTune);
		*/
	}
	
	public static void runBikeSharing() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset = new Dataset(bikeSharingFiles + "bikeSharing.txt", true, true, 11, TRAINING_SAMPLE_FRACTION);
			tryDifferentRevisedVariableParameters(trainingDataset, bikeSharingParamTune, i);
			tryDifferentConstantParameters(trainingDataset, bikeSharingParamTune, i);
		}
		GradientBoostingTree.executor.shutdownNow();
		
		/*
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		Dataset trainingDataset = new Dataset(bikeSharingFiles + "bikeSharing.txt", true, true, 11, TRAINING_SAMPLE_FRACTION);
		tryDifferentParameters(trainingDataset, bikeSharingParamTune);
		*/
		/*
		readSortAndSaveResultsAsSortedOptimalParameterRecords(bikeSharingParamTune);
		readOptimalParameterRecordsOldFormat("BikeSharing", bikeSharingParamTune);
		*/
	}
	
	public static void runPowerPlant() {
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			Dataset trainingDataset3 = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
			tryDifferentRevisedVariableParameters(trainingDataset3, powerPlantParamTune, i);
			tryDifferentConstantParameters(trainingDataset3, powerPlantParamTune, i);
		}
		
		GradientBoostingTree.executor.shutdownNow();
		
		/*
		readSortAndSaveResultsAsSortedOptimalParameterRecords(powerPlantParamTune, 0);
		ArrayList<OptimalParameterRecord> records = readOptimalParameterRecordsOldFormat("PowerPlant", powerPlantParamTune);
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("PowerPlant", powerPlantParamTune, records);
		*/
	}
	
	public static boolean checkAndClaimHostLock(String hostLockFilePath) {
		File hostLock;
		if ((hostLock = new File(hostLockFilePath)).exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(hostLock));
				String hostName = br.readLine();
				br.close();
				return (hostName).equals(InetAddress.getLocalHost().getHostName());
			} catch (FileNotFoundException e) {
				System.out.println("Host lock file exists but file not found. Makes no sense.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new PrintWriter(hostLock));
				bw.write(InetAddress.getLocalHost().getHostName() + "\n");
				bw.flush();
				bw.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		System.out.println("ERROR Shouldnt reach here in checkAndClaimHostLock");
		return false;
	}
	
	public static void tryDifferentRevisedVariableParameters(Dataset trainingDataset, String paramTuneDir, int runNumber) {
		int done = 0, total = minLearningRates.length * maxLearningRates.length * splits.length;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (double minLR : minLearningRates) {
			for (double maxLR : maxLearningRates) {
				for (int numberOfSplits : splits) {
					String lrbfsDirectory = paramTuneDir + String.format("Run%d/RevisedVariable/%.5fMinLR/%.5fMaxLR/%dSplits/", runNumber, minLR, maxLR, numberOfSplits);
					new File(lrbfsDirectory).mkdirs();
					GbmParameters parameters = new GbmParameters(
							minLR, maxLR, numberOfSplits, 
							BAG_FRACTION, MIN_EXAMPLES_IN_NODE, NUMBER_OF_TREES, 
							LearningRatePolicy.REVISED_VARIABLE, SplitsPolicy.CONSTANT);
					if (!checkAndClaimHostLock(lrbfsDirectory + parameters.getFileNamePrefix() + "--hostLock.txt")) {
						System.out.println(String.format("Another host has already claimed %s on run number %d. (%d out of %d)\n "
								+ "Have been runnung for %.4f minutes total.", 
								parameters.getFileNamePrefix(), runNumber, ++done, total, globalTimer.getElapsedMinutes()));
						continue;
					}
					if (new File(lrbfsDirectory + parameters.getFileNamePrefix() + "--runData.txt").exists()) {
						System.out.println(String.format("Skipping %s. (%d out of %d)\n "
								+ "Have been runnung for %.4f minutes total.", 
								parameters.getFileNamePrefix(), ++done, total, globalTimer.getElapsedMinutes()));
						continue;
                    }
					timer.start();
					CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
					if (ensemble != null) {
						try {
							ensemble.saveDataToFile(lrbfsDirectory);
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println(String.format("Finished %s in %.4f minutes. (%d out of %d)\n "
								+ "Have been runnung for %.4f minutes total.", 
								parameters.getFileNamePrefix(), timer.getElapsedMinutes(), ++done, total, globalTimer.getElapsedMinutes()));
					} else {
						System.out.println(String.format("Failed to build due to impossible parameters %s in %.4f minutes. (%d out of %d)\n "
								+ "Have been runnung for %.4f minutes total.", 
								parameters.getFileNamePrefix(), timer.getElapsedMinutes(), ++done, total, globalTimer.getElapsedMinutes()));
					}
				}
			}
		}
	}
	
	public static void tryDifferentConstantParameters(Dataset trainingDataset, String paramTuneDir, int runNumber) {
		int done = 0, total = constantLearningRates.length * splits.length;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (double LR : constantLearningRates) {
			for (int numberOfSplits : splits) {
				String lrbfsDirectory = paramTuneDir + String.format("Run%d/Constant/%.5fLR/%dSplits/", runNumber, LR, numberOfSplits);
				new File(lrbfsDirectory).mkdirs();
				GbmParameters parameters = new GbmParameters(
						LR, numberOfSplits, 
						BAG_FRACTION, MIN_EXAMPLES_IN_NODE, NUMBER_OF_TREES, 
						LearningRatePolicy.CONSTANT, SplitsPolicy.CONSTANT);
				if (!checkAndClaimHostLock(lrbfsDirectory + parameters.getFileNamePrefix() + "--hostLock.txt")) {
					System.out.println(String.format("Another host has already claimed %s on run number %d. (%d out of %d)\n "
							+ "Have been runnung for %.4f minutes total.", 
							parameters.getFileNamePrefix(), runNumber, ++done, total, globalTimer.getElapsedMinutes()));
					continue;
				}
				if (new File(lrbfsDirectory + parameters.getFileNamePrefix() + "--runData.txt").exists()) {
					System.out.println(String.format("Skipping %s. (%d out of %d)\n "
							+ "Have been runnung for %.4f minutes total.", 
							parameters.getFileNamePrefix(), ++done, total, globalTimer.getElapsedMinutes()));
					continue;
                }
				timer.start();
				CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
				if (ensemble != null) {
					try {
						ensemble.saveDataToFile(lrbfsDirectory);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println(String.format("Finished %s in %.4f minutes. (%d out of %d)\n "
							+ "Have been runnung for %.4f minutes total.", 
							parameters.getFileNamePrefix(), timer.getElapsedMinutes(), ++done, total, globalTimer.getElapsedMinutes()));
				} else {
					System.out.println(String.format("Failed to build due to impossible parameters %s in %.4f minutes. (%d out of %d)\n "
							+ "Have been runnung for %.4f minutes total.", 
							parameters.getFileNamePrefix(), timer.getElapsedMinutes(), ++done, total, globalTimer.getElapsedMinutes()));
				}
			}
		}
	}
	
	public static void readSortAndSaveResultsAsSortedOptimalParameterRecords(String paramTuneDir, int runNumber) {
		PriorityQueue<OptimalParameterRecord> sortedByCvValidationError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvValidationErrorComparator());
		PriorityQueue<OptimalParameterRecord> sortedByAllDataTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTestErrorComparator());
		PriorityQueue<OptimalParameterRecord> sortedByTimeInSeconds = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.TimeInSecondsComparator());
		// Grab the variable LR results
		for (double minLR : minLearningRates) {
			for (double maxLR : maxLearningRates) {
				for (int numberOfSplits : splits) {
					String lrbfsDirectory = paramTuneDir + String.format("Run%d/RevisedVariable/%.5fMinLR/%.5fMaxLR/%dSplits/", runNumber, minLR, maxLR, numberOfSplits);
					new File(lrbfsDirectory).mkdirs();
					GbmParameters parameters = new GbmParameters(
							minLR, maxLR, numberOfSplits, 
							BAG_FRACTION, MIN_EXAMPLES_IN_NODE, NUMBER_OF_TREES, 
							LearningRatePolicy.REVISED_VARIABLE, SplitsPolicy.CONSTANT);
					OptimalParameterRecord record = new OptimalParameterRecord();
					try {
						BufferedReader br = new BufferedReader(new FileReader(new File(lrbfsDirectory + parameters.getFileNamePrefix() + "--runData.txt")));
						record.timeInSeconds = Double.parseDouble(br.readLine().split(": ")[1].trim());
						br.readLine(); // Skip Step Size
						br.readLine(); // Skip number of Folds
						record.totalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
						record.optimalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
						record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.avgNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.parameters = parameters;
						br.close();
					} catch (FileNotFoundException e) {
						System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
						continue;
					} catch (IOException e2) {
						System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
						continue;
					}
					sortedByCvValidationError.add(record);
					sortedByAllDataTestError.add(record);
					sortedByTimeInSeconds.add(record);
				}
			}
		}
		// Grab the constant LR results
		for (double minLR : minLearningRates) {
			for (double maxLR : maxLearningRates) {
				for (int numberOfSplits : splits) {
					String lrbfsDirectory = paramTuneDir + String.format("Run%d/RevisedVariable/%.5fMinLR/%.5fMaxLR/%dSplits/", 0, minLR, maxLR, numberOfSplits);
					new File(lrbfsDirectory).mkdirs();
					GbmParameters parameters = new GbmParameters(
							minLR, maxLR, numberOfSplits, 
							BAG_FRACTION, MIN_EXAMPLES_IN_NODE, NUMBER_OF_TREES, 
							LearningRatePolicy.REVISED_VARIABLE, SplitsPolicy.CONSTANT);
					OptimalParameterRecord record = new OptimalParameterRecord();
					try {
						BufferedReader br = new BufferedReader(new FileReader(new File(lrbfsDirectory + parameters.getFileNamePrefix() + "--runData.txt")));
						record.timeInSeconds = Double.parseDouble(br.readLine().split(": ")[1].trim());
						br.readLine(); // Skip Step Size
						br.readLine(); // Skip number of Folds
						record.totalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
						record.optimalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
						record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.avgNumberOfSplits = Double.parseDouble(br.readLine().split(": ")[1].trim());
						record.parameters = parameters;
						br.close();
					} catch (FileNotFoundException e) {
						System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
						continue;
					} catch (IOException e2) {
						System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
						continue;
					}
					sortedByCvValidationError.add(record);
					sortedByAllDataTestError.add(record);
					sortedByTimeInSeconds.add(record);
				}
			}
		}
		String runFolder = paramTuneDir + String.format("Run%d/", runNumber);
		OptimalParameterRecord.saveOptimalParameterRecords(runFolder, "SortedByCvValidationError", sortedByCvValidationError);
		OptimalParameterRecord.saveOptimalParameterRecords(runFolder, "SortedByAllDataTestError", sortedByAllDataTestError);
		OptimalParameterRecord.saveOptimalParameterRecords(runFolder, "SortedByTimeInSeconds", sortedByTimeInSeconds);
	}
}
