import gbm.GbmDataset;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.ResultFunction;
import gbm.cv.CrossValidatedResultFunctionEnsemble;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
import utilities.DoubleCompare;
import utilities.Logger;
import utilities.PlotGenerator;
import utilities.StopWatch;
import dataset.Dataset;


public class Main {
	
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	
	public static final int NUMBER_OF_TREES = 500000;
	public static final int CV_NUMBER_OF_FOLDS = 5;
	public static final int CV_STEP_SIZE = 1000;
	public static final double TRAINING_SAMPLE_FRACTION = 0.8;
	
	
	public static final double LEARNING_RATE = .001;
	public static final double BAG_FRACTION = 1;
	public static final int MAX_NUMBER_OF_SPLITS = 3;
	public static final int MIN_EXAMPLES_IN_NODE = 10;
	public static final LearningRatePolicy LEARNING_RATE_POLICY = LearningRatePolicy.CONSTANT;
	public static final SplitsPolicy SPLITS_POLICY = SplitsPolicy.CONSTANT;
	
	
	public static void main(String[] args) {
		//recreateRExperiment();
		//experiment2();
		//crossVal1();
		tryDifferentParameters();
		//readAndSortParameters();
		/*
		try {
			//DataSetGen.gen();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	

	public static void crossVal1() {
		StopWatch timer = (new StopWatch()).start();
		Dataset trainingDataset = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
		GbmParameters parameters = new GbmParameters(BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS, LEARNING_RATE_POLICY, SplitsPolicy.CONSTANT);
		CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
		System.out.println(ensemble.getSummary());
		
		//PlotGenerator.plotCVEnsemble("CrossValidation.png", "CV Training, Validation, Test RMSE", ensemble);
		//PlotGenerator.plotTrainingAndValidationErrors("CrossValidation.png", "CV Training, Validation, Test RMSE", null, ensemble.avgCvTrainingErrors, ensemble.avgCvValidationErrors, ensemble.avgCvTestErrors);

		//PlotGenerator.plotTrainingAndValidationErrors("sdfsdfsdf", "asdfasd", ensemble.allDataFunction.trainingError, ensemble.allDataFunction.testError);

		GradientBoostingTree.executor.shutdownNow();
		System.out.println("Finished " + CV_NUMBER_OF_FOLDS + " in " + timer.getElapsedSeconds() + " seconds");
	}
	
	public static void tryDifferentParameters() {
		String root = System.getProperty("user.dir") + "/data/parameterTuning/";
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		Dataset trainingDataset = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
		//PriorityQueue<Map.Entry<Double, GbmParameters>> sortedEnsembles = new PriorityQueue<Map.Entry<Double, GbmParameters>>(new EnsembleComparator());
		int done = 0;
		for (double learningRate = 2; learningRate >= 1; learningRate-=0.5) {
			for (double bagFraction = 1; bagFraction >= 0.1; bagFraction -= 0.3) {
				for (int numberOfSplits = 1; numberOfSplits > 0; numberOfSplits -= 100) {
					String lrbfsDirectory = root + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, bagFraction, numberOfSplits);
					new File(lrbfsDirectory).mkdirs();
					//for (int minExamplesInNode = 1; minExamplesInNode <= 1000; minExamplesInNode *= 10) {
					int minExamplesInNode = 1;
						//for (int i = 0; i <= 1; i++) {
							GbmParameters parameters;
							//if (i == 0) {
								//parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.CONSTANT);
							//} else {
								parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.VARIABLE, SplitsPolicy.RANDOM);
							//}
							timer.start();
							CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
							if (ensemble != null) {
								try {
									ensemble.saveDataToFile("", lrbfsDirectory);
								} catch (IOException e) {
									e.printStackTrace();
								}
								//PlotGenerator.plotCVEnsemble(lrbfsDirectory, parameters.getFileNamePrefix(), parameters.getFileNamePrefix(), ensemble);
								System.out.println("Finished " + parameters.getFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
							} else {
								System.out.println("Failed to build because of inpossible parameters " + parameters.getFileNamePrefix() + " in " + timer.getElapsedSeconds() + " seconds");
							}
						//}
						done+=2;
						System.out.println("Have been running for " + globalTimer.getElapsedMinutes() + " minutes. Completed " + done + " out of 384");
					}
				//}
			}
		}
		// crecordSortedParameters(sortedEnsembles);
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static class OptimalParameterRecord  {
		public GbmParameters parameters;
		public double timeInSeconds;
		public int totalNumberOfTrees;
		public int optimalNumberOfTrees;
		public double cvValidationError;
		public double cvTrainingError;
		public double allDataTrainingError;
		public double cvTestError;
		public double allDataTestError;
		
		
		public void inferTimeInSecondsFromPartialRun(int numberOfTreesInPartialRun, double timeInSeconds) {
			this.timeInSeconds = (timeInSeconds / numberOfTreesInPartialRun) * totalNumberOfTrees;
		}

		public static class CvValidationErrorComparator implements Comparator<OptimalParameterRecord> {
			@Override
			public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
				return DoubleCompare.compare(o1.cvValidationError, o2.cvValidationError);
			}
		}
		
		public static class CvTestErrorComparator implements Comparator<OptimalParameterRecord> {
			@Override
			public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
				return DoubleCompare.compare(o1.cvTestError, o2.cvTestError);
			}
		}
		
		public static class TimeInSecondsComparator implements Comparator<OptimalParameterRecord> {
			@Override
			public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
				return DoubleCompare.compare(o1.timeInSeconds, o2.timeInSeconds);
			}
		}
		
		public static class AllDataTestErrorComparator implements Comparator<OptimalParameterRecord> {
			@Override
			public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
				return DoubleCompare.compare(o1.allDataTestError, o2.allDataTestError);
			}
		}
		
		public static class AllDataTrainingErrorComparator implements Comparator<OptimalParameterRecord> {
			@Override
			public int compare(OptimalParameterRecord o1, OptimalParameterRecord o2) {
				return DoubleCompare.compare(o1.allDataTrainingError, o2.allDataTrainingError);
			}
		}
	}
	public static void readAndSortParameters() {
		try {
			Dataset trainingDataset = new Dataset(powerPlantFiles + "Folds5x2_pp.txt", true, true, 4, TRAINING_SAMPLE_FRACTION);
			StopWatch timer = new StopWatch();
			PriorityQueue<OptimalParameterRecord> sortedByCvValidationError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvValidationErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByCvTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.CvTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTestError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTestErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByAllDataTrainingError = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.AllDataTrainingErrorComparator());
			PriorityQueue<OptimalParameterRecord> sortedByTimeInSeconds = new PriorityQueue<OptimalParameterRecord>(new OptimalParameterRecord.TimeInSecondsComparator());
			String root = System.getProperty("user.dir") + "/data/parameterTuning/";
			for (double learningRate = 3; learningRate >= .001; learningRate = (learningRate > 1) ? learningRate-1 : learningRate / 10) {
				for (double bagFraction = 1; bagFraction >= 0.1; bagFraction -= 0.3) {
					for (int numberOfSplits = 10; numberOfSplits > 0; numberOfSplits -= 3) {
						String lrbfsDirectory = root + String.format("%.5fLR/%.5fBF/%dSplits/", learningRate, bagFraction, numberOfSplits);
						new File(lrbfsDirectory).mkdirs();
						for (int minExamplesInNode = 1; minExamplesInNode <= 1000; minExamplesInNode *= 10) {
							for (int i = 0; i <= 1; i++) {
								OptimalParameterRecord record = new OptimalParameterRecord();
							
								GbmParameters parameters;
								if (i == 0) {
									parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.CONSTANT, SPLITS_POLICY);
								} else {
									parameters = new GbmParameters(bagFraction, learningRate, NUMBER_OF_TREES, minExamplesInNode, numberOfSplits, LearningRatePolicy.VARIABLE, SPLITS_POLICY);
								}
								try {
									// System.out.println(lrbfsDirectory + "normal/" + parameters.getFileNamePrefix() + "--normal.txt");
									BufferedReader br = new BufferedReader(new FileReader(new File(lrbfsDirectory + "normal/" + parameters.getFileNamePrefix() + "--normal.txt")));
									String mightBeTimeInSeconds = br.readLine(); // Added this later. For backwards compatability with prior data need extra checks
									double timeInSeconds = -1;
									if (mightBeTimeInSeconds.contains("TimeInSeconds")) {
										timeInSeconds = Double.parseDouble(mightBeTimeInSeconds.split(": ")[1].trim());
										br.readLine(); // Skip Step Size
									}
									br.readLine(); // Skip number of Folds
									record.totalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
									record.optimalNumberOfTrees = Integer.parseInt(br.readLine().split(": ")[1].trim());
									record.cvValidationError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.cvTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.allDataTrainingError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.cvTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.allDataTestError = Double.parseDouble(br.readLine().split(": ")[1].trim());
									record.parameters = parameters;
									record.timeInSeconds = timeInSeconds;
									br.close();
								} catch (FileNotFoundException e) {
									System.out.println("File for " + parameters.getTabSeparatedPrintOut() + " not found");
									continue;
								}
								// If its an old run, extrapolate time in seconds form a short run.
								if (record.timeInSeconds < 0) {
									timer.start();
									parameters.numOfTrees = 500;
									GradientBoostingTree.crossValidate(parameters, trainingDataset, CV_NUMBER_OF_FOLDS, CV_STEP_SIZE);
									double timeInSeconds =timer.getElapsedSeconds();
									System.out.println(parameters.getTabSeparatedPrintOut() + " took " + timeInSeconds);
									record.inferTimeInSecondsFromPartialRun(500, timeInSeconds);
									parameters.numOfTrees = NUMBER_OF_TREES; // Set back so its correct int he file.
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
			}
			recordSortedParameters("SortedByCvValidationError", sortedByCvValidationError);
			recordSortedParameters("SortedByCvTestError", sortedByCvTestError);
			recordSortedParameters("SortedByAllDataTestError", sortedByAllDataTestError);
			recordSortedParameters("SortedByAllDataTrainingError", sortedByAllDataTrainingError);
			recordSortedParameters("SortedByTimeInSeconds", sortedByTimeInSeconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void recordSortedParameters(String fileNamePrefix, PriorityQueue<OptimalParameterRecord> sortedEnsembles) {
		String directory = System.getProperty("user.dir") + "/data/parameterTuning/";
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(directory + fileNamePrefix + "Parameters.txt")));
			bw.append("TimeInSeconds\tCvValidation\tCvTest\tAllDataTraining\tAllDataTest\tTotalNumberOfTreesTrained\tOptimalNumberOfTrees\t" + GbmParameters.getTabSeparatedHeader() + "\n");
			while (!sortedEnsembles.isEmpty()) {
				OptimalParameterRecord record = sortedEnsembles.poll();
				
				bw.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%d\t", 
						record.timeInSeconds, record.cvValidationError, 
						record.cvTestError, record.allDataTrainingError, 
						record.allDataTestError,
						record.totalNumberOfTrees, record.optimalNumberOfTrees) 
						+ record.parameters.getTabSeparatedPrintOut() + "\n");
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void allowUserToPrintTrees(ResultFunction function) {
		String userInput = "";
		Scanner sc = new Scanner(System.in);
		while(!userInput.equalsIgnoreCase("n")) {
			System.out.println("Would you like to print an individual tree? Enter a number between 0 and " + (NUMBER_OF_TREES-1) + " or type 'N'");
			userInput = sc.nextLine();
			int value = 0;
			try {
				value = Integer.parseInt(userInput);
			} catch(Exception e) {
				System.out.println("Try again :)");
				continue;
			}
			
			if (value > (NUMBER_OF_TREES-1)) {
				continue;
			}
			
			try {
				function.trees.get(value).root.printTree(new OutputStreamWriter(System.out));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sc.close();
	}
	
	/*
	public static void experiment2() {
		StopWatch timer = (new StopWatch());
		
		//Dataset trainingDataset = new Dataset( System.getProperty("user.dir") + "/data/ServoMotor/" + "TRAINING.txt", true, true, 4);
		//Dataset testDataset = new Dataset(System.getProperty("user.dir") + "/data/ServoMotor/" + "TEST.txt", true, true, 4);
		
		Dataset trainingDataset = new Dataset( System.getProperty("user.dir") + "/data/gen2/" + "TRAINING.txt", true, true, 9, 1);
		Dataset validationDataset = new Dataset(System.getProperty("user.dir") + "/data/gen2/" + "TEST.txt", true, true, 9, 1);
		
		timer.start();
		GbmParameters parameters = new GbmParameters(BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS, LEARNING_RATE_POLICY);
		ResultFunction function = GradientBoostingTree.buildGradientBoostingMachine(parameters, trainingDataset, validationDataset);
		Logger.println("Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();
		double[] relativeInf = function.calcRelativeInfluences();
		Logger.println("Relative Influences\n--------------------");
		for (int i = 0; i < relativeInf.length; i++) {
			Logger.println(i + ": " + relativeInf[i] + "%");
		}
		Logger.println("Calc Rel Inf in " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();

		Logger.println("Training RMSE: " + function.trainingError.get(NUMBER_OF_TREES-1) + "\nValidation RMSE: " + function.validationError.get(NUMBER_OF_TREES-1) + "\nTime in Seconds: " + timer.getElapsedSeconds());
		

		String userInput = "";
		Scanner sc = new Scanner(System.in);
		while(!userInput.equalsIgnoreCase("n")) {
			System.out.println("Would you like to print an individual tree? Enter a number between 0 and " + (NUMBER_OF_TREES-1) + " or type 'N'");
			userInput = sc.nextLine();
			int value = 0;
			try {
				value = Integer.parseInt(userInput);
			} catch(Exception e) {
				System.out.println("Try again :)");
				continue;
			}
			
			if (value > (NUMBER_OF_TREES-1)) {
				continue;
			}
			
			try {
				function.trees.get(value).root.printTree(new OutputStreamWriter(System.out));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sc.close();
	}
	
	*/
	public static void recreateRExperiment() {
		StopWatch timer = (new StopWatch());
		
		Dataset trainingDataset = new Dataset(powerPlantFiles + "TRAINING.txt", true, true, 4, 1);
		Dataset validationDataset = new Dataset(powerPlantFiles + "TEST.txt", true, true, 4, 1);
		
		timer.start();
		GbmParameters parameters = new GbmParameters(BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS, LEARNING_RATE_POLICY.CONSTANT, SPLITS_POLICY.CONSTANT);
		ResultFunction function = GradientBoostingTree.buildGradientBoostingMachine(parameters, trainingDataset);
		Logger.println("Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		System.out.println(function.getRelativeInfluencesString());
		
		
		timer.start();
		GbmDataset v = new GbmDataset(validationDataset);
		
		Logger.println("Training RMSE: " + function.trainingError.get(NUMBER_OF_TREES-1) + "\nValidation RMSE: " + v.calcTrainingRMSE(function) + "\nTime in Seconds: " + timer.getElapsedSeconds());
	
		allowUserToPrintTrees(function);
	}
	

}
