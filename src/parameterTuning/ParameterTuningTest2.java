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

import parameterTuning.OptimalParameterRecord.RunFileType;
import parameterTuning.plotting.PairwiseOptimalParameterRecordPlots;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.StopWatch;
import dataset.Dataset;


public class ParameterTuningTest2 {
	public static final String powerPlantParamTune = System.getProperty("user.dir") + "/data/powerPlantParameterTuning/";
	public static final String nasaParamTune = System.getProperty("user.dir") + "/data/nasaParameterTuning/";
	public static final String bikeSharingParamTune = System.getProperty("user.dir") + "/data/bikeSharingParameterTuning/";
	
	public static final String bikeSharingFiles = System.getProperty("user.dir") + "/data/BikeSharing/";
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	public static final String nasaFiles = System.getProperty("user.dir") + "/data/NASAAirFoild/";
	
	public static final int NUMBER_OF_TREES = 250000;
	public static final int CV_NUMBER_OF_FOLDS = 4;
	public static final int CV_STEP_SIZE = 1000;
	public static final double TRAINING_SAMPLE_FRACTION = 0.8;
	
	public static final LearningRatePolicy LEARNING_RATE_POLICY = LearningRatePolicy.CONSTANT;
	public static final SplitsPolicy SPLITS_POLICY = SplitsPolicy.CONSTANT;
	
	public static void runNASA() {
		/*
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		Dataset trainingDataset2 = new Dataset(nasaFiles + "data.txt", true, true, 5, TRAINING_SAMPLE_FRACTION);
		tryDifferentParameters(trainingDataset2, nasaParamTune);
		*/
		readSortAndSaveResultsAsSortedOptimalParameterRecords(nasaParamTune);
		readOptimalParameterRecordsOldFormat("AirFoil", nasaParamTune);
	}
	
	public static void runBikeSharing() {
		/*
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		Dataset trainingDataset = new Dataset(bikeSharingFiles + "bikeSharing.txt", true, true, 11, TRAINING_SAMPLE_FRACTION);
		tryDifferentParameters(trainingDataset, bikeSharingParamTune);
		*/
		readSortAndSaveResultsAsSortedOptimalParameterRecords(bikeSharingParamTune);
		readOptimalParameterRecordsOldFormat("BikeSharing", bikeSharingParamTune);
	}
	
	public static void runPowerPlant() {
		/*
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		Dataset trainingDataset3 = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true,4, TRAINING_SAMPLE_FRACTION);
		tryDifferentParameters(trainingDataset3, powerPlantParamTune);
		 */
		readSortAndSaveResultsAsSortedOptimalParameterRecords(powerPlantParamTune);
		ArrayList<OptimalParameterRecord> records = readOptimalParameterRecordsOldFormat("PowerPlant", powerPlantParamTune);
		PairwiseOptimalParameterRecordPlots.generatePairwiseOptimalParameterRecordPlots("PowerPlant", powerPlantParamTune, records);
	}
	
	public static void tryDifferentParameters(Dataset trainingDataset, String paramTuneDir) {
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (double learningRate = 2.5; learningRate >= 0.001; learningRate /=2) {
			for (double bagFraction = 1; bagFraction >= 0.5; bagFraction -= 0.25) {
				for (int numberOfSplits = 20; numberOfSplits > 0; numberOfSplits -= 4) {
					String lrbfsDirectory = paramTuneDir + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, bagFraction, numberOfSplits);
					new File(lrbfsDirectory).mkdirs();
					int minExamplesInNode = 1;
					for (int i = 0; i <= 1; i++) {
						GbmParameters parameters;
						if (i == 0) {
							if (learningRate <= 1) {
								parameters = new GbmParameters(learningRate, numberOfSplits, bagFraction, minExamplesInNode, NUMBER_OF_TREES, LearningRatePolicy.CONSTANT, SplitsPolicy.CONSTANT);
							} else {
								continue;
							}
						} else {
							parameters = new GbmParameters(learningRate, numberOfSplits, bagFraction, minExamplesInNode, NUMBER_OF_TREES, LearningRatePolicy.VARIABLE, SplitsPolicy.CONSTANT);
						}
						timer.start();
						CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
						if (ensemble != null) {
							try {
								ensemble.saveRunDataToFile(lrbfsDirectory, RunFileType.ParamTuning2);
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println("Finished " + parameters.getOldFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
						} else {
							System.out.println("Failed to build because of inpossible parameters " + parameters.getOldFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
						}
					}
					done+=2;
					System.out.println("Have been running for " + globalTimer.getElapsedMinutes() + " minutes. Completed " + done + " out of 180");
				}
			}
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static void readSortAndSaveResultsAsSortedOptimalParameterRecords(String paramTuneDir) {
		try {
			PriorityQueue<OptimalParameterRecord> sortedByCvValidationError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvValidationErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByCvTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTrainingError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTrainingErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByTimeInSeconds = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.TimeInSecondsComparator());
			for (double learningRate = 2.5; learningRate >= 0.001; learningRate /=2) {
				for (double bagFraction = 1; bagFraction >= 0.5; bagFraction -= 0.25) {
					for (int numberOfSplits = 20; numberOfSplits > 0; numberOfSplits -= 4) {
						String lrbfsDirectory = paramTuneDir + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, bagFraction, numberOfSplits);
						new File(lrbfsDirectory).mkdirs();
						int minExamplesInNode = 1;
						for (int i = 0; i <= 1; i++) {
							OptimalParameterRecord record = new OptimalParameterRecord();
						
							GbmParameters parameters;
							if (i == 0) {
								parameters = new GbmParameters(learningRate, numberOfSplits, bagFraction, minExamplesInNode, 250000, LearningRatePolicy.CONSTANT, SplitsPolicy.CONSTANT);
							} else {
								parameters = new GbmParameters(learningRate, numberOfSplits, bagFraction, minExamplesInNode, 250000, LearningRatePolicy.VARIABLE, SplitsPolicy.CONSTANT);
							}
							try {
								// System.out.println(lrbfsDirectory + "normal/" + parameters.getFileNamePrefix() + "--normal.txt");
								BufferedReader br = new BufferedReader(new FileReader(new File(lrbfsDirectory + "normal/" + parameters.getOldFileNamePrefix() + "--normal.txt")));
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
								record.parameters = parameters;
								br.close();
							} catch (FileNotFoundException e) {
								System.out.println("File for " + parameters.getOldTabSeparatedPrintOut() + " not found");
								continue;
							}
							sortedByCvValidationError.add(record);
							sortedByAllDataTestError.add(record);
							sortedByAllDataTrainingError.add(record);
							sortedByTimeInSeconds.add(record);
							sortedByCvTestError.add(record);
						}
					}
				}
				
			}
			saveOptimalParameterRecordsOldFormat(paramTuneDir, "SortedByCvValidationError", sortedByCvValidationError);
			saveOptimalParameterRecordsOldFormat(paramTuneDir, "SortedByCvTestError", sortedByCvTestError);
			saveOptimalParameterRecordsOldFormat(paramTuneDir, "SortedByAllDataTestError", sortedByAllDataTestError);
			saveOptimalParameterRecordsOldFormat(paramTuneDir, "SortedByAllDataTrainingError", sortedByAllDataTrainingError);
			saveOptimalParameterRecordsOldFormat(paramTuneDir, "SortedByTimeInSeconds", sortedByTimeInSeconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void saveOptimalParameterRecordsOldFormat(String paramTuneDir, String fileNamePrefix, PriorityQueue<OptimalParameterRecord> sortedEnsembles) {
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "All_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter constant = new BufferedWriter(new PrintWriter(new File( paramTuneDir + "Constant_" + fileNamePrefix + "Parameters.txt")));
			BufferedWriter variable = new BufferedWriter(new PrintWriter(new File(paramTuneDir + "Variable_" + fileNamePrefix + "Parameters.txt")));
			bw.append("TimeInSeconds\tCvValidation\tCvTest\tAllDataTraining\tAllDataTest\tTotalNumberOfTreesTrained\tOptimalNumberOfTrees\t" + GbmParameters.getOldTabSeparatedHeader() + "\n");
			while (!sortedEnsembles.isEmpty()) {
				OptimalParameterRecord record = sortedEnsembles.poll();
				
				bw.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
						record.timeInSeconds, record.cvValidationError, 
						record.cvTestError, record.allDataTrainingError, 
						record.allDataTestError,
						record.totalNumberOfTrees, record.optimalNumberOfTrees) 
						+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
				if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
					constant.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
							record.timeInSeconds, record.cvValidationError, 
							record.cvTestError, record.allDataTrainingError, 
							record.allDataTestError,
							record.totalNumberOfTrees, record.optimalNumberOfTrees) 
							+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
				} else {
					variable.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
							record.timeInSeconds, record.cvValidationError, 
							record.cvTestError, record.allDataTrainingError, 
							record.allDataTestError,
							record.totalNumberOfTrees, record.optimalNumberOfTrees) 
							+ record.parameters.getOldTabSeparatedPrintOut() + "\n");
				}
			}
			constant.flush();
			constant.close();
			variable.flush();
			variable.close();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<OptimalParameterRecord> readOptimalParameterRecordsOldFormat(String datasetName, String paramTuneDir) {
		ArrayList<OptimalParameterRecord> records = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(paramTuneDir + "All_SortedByCvValidationErrorParameters.txt")));
			br.readLine(); // discard header
			String text;
			while ((text = br.readLine()) != null) {
				String[] columns = text.split("\t");
				OptimalParameterRecord record = new OptimalParameterRecord();
				record.timeInSeconds = Double.parseDouble(columns[0].trim());
				record.cvValidationError = Double.parseDouble(columns[1].trim());
				record.cvTestError = Double.parseDouble(columns[2].trim());
				record.allDataTrainingError = Double.parseDouble(columns[3].trim());
				record.allDataTestError = Double.parseDouble(columns[4].trim());
				record.totalNumberOfTrees = Integer.parseInt(columns[5].trim());
				record.optimalNumberOfTrees = Integer.parseInt(columns[6].trim());
				record.parameters = new GbmParameters(
						Double.parseDouble(columns[8].trim()), // LR
						Integer.parseInt(columns[10].trim()), //NOS
						Double.parseDouble(columns[9].trim()), //BF
						Integer.parseInt(columns[11].trim()), //MEIN
						Integer.parseInt(columns[12].trim()), //NOT
						LearningRatePolicy.valueOf(columns[7].trim()), // LearningRatePolicy
						SplitsPolicy.CONSTANT);
				records.add(record);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return records;
	}
}
