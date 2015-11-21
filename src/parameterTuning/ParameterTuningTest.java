package parameterTuning;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.cv.CrossValidatedResultFunctionEnsemble;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import parameterTuning.plotting.ErrorCurveScriptExecutor;
import parameterTuning.plotting.ErrorCurveScriptGenerator;
import parameterTuning.plotting.PredictionGraphGenerator;
import parameterTuning.plotting.RunDataSummaryRecordGraphGenerator;
import utilities.CommandLineExecutor;
import utilities.CompressedTarBallCreator;
import utilities.RecursiveFileDeleter;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import dataset.Dataset;
import dataset.DatasetParameters;


public class ParameterTuningTest {
	private ParameterTuningParameters tuningParameters = ParameterTuningParameters.getRangesForTest3();
	
	private ParameterTuningTest(){}
	
	public static void runOnAllDatasets(ParameterTuningParameters parameters) {
		ParameterTuningTest test = new ParameterTuningTest();
		test.tuningParameters = parameters;
		
		GradientBoostingTree.executor = Executors.newCachedThreadPool();
		for (int runNumber = 0; runNumber < test.tuningParameters.NUMBER_OF_RUNS; runNumber++) {
			for (DatasetParameters datasetParams : test.tuningParameters.datasets) {
				Dataset dataset = new Dataset(datasetParams, ParameterTuningParameters.TRAINING_SAMPLE_FRACTION);
				boolean runComplete = test.tryDifferentParameters(dataset, runNumber);
				if (runComplete) {
					if (compressRunData(datasetParams, test.tuningParameters, runNumber)) {
						if (scpCompressedRunData(datasetParams, test.tuningParameters, runNumber)) {
							extractCompressedRunDataOnRemoteServer(datasetParams, test.tuningParameters, runNumber);
						}
					}
				}
			}
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static boolean compressRunData(DatasetParameters datasetParams, ParameterTuningParameters tuningParameters, int runNumber) {
		String runDataDir = tuningParameters.runDataOutputDirectory + datasetParams.minimalName; 
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + String.format("/Run%d/", runNumber);
		
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "compressRunData--doneLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already completed compressing run data for run number %d.", datasetParams.minimalName, runNumber));
			return true;
		}
		
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + "compressRunData--hostLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Another host has already claimed compressing run data for run number %d.", datasetParams.minimalName, runNumber));
			return false;
		}
		
		File source = new File(runDataDir + String.format("/Run%d/", runNumber));
		File destination = new File(runDataDir + String.format("/%sRun%d.tar.gz", datasetParams.minimalName, runNumber));
		
		if (!source.exists()) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Run Data doesn't exist! Failed to compress run data for run number %d. Marking as done.", datasetParams.minimalName, runNumber));
			SimpleHostLock.writeDoneLock(locksDir + "compressRunData--doneLock.txt");
			return true;
		}

		StopWatch timer = new StopWatch().start();
		// DO task
		try {
			timer.printMessageWithTime(String.format("[%s] Beginning to compress run data for run number %d.", datasetParams.minimalName, runNumber));
			CompressedTarBallCreator.compressFile(source, destination);
			timer.printMessageWithTime(String.format("[%s] Finished compressing run data for run number %d.", datasetParams.minimalName, runNumber));
			
			SimpleHostLock.writeDoneLock(locksDir + "compressRunData--doneLock.txt");
			return true;
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			RecursiveFileDeleter.deleteDirectory(new File(locksDir +  "compressRunData--hostLock.txt"));
			timer.printMessageWithTime(String.format("[%s] Unexpectedly failed to compress run data for run number %d. Removed host lock so someone else can try.", datasetParams.minimalName, runNumber));
		}
		return false;
	}
	
	public static boolean scpCompressedRunData(DatasetParameters datasetParams, ParameterTuningParameters tuningParameters, int runNumber) {
		String runDataDir = tuningParameters.runDataOutputDirectory + datasetParams.minimalName; 
		String remoteDataDir = tuningParameters.runDataFreenasDirectory + datasetParams.minimalName + "/"; 
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + String.format("/Run%d/", runNumber);
		
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "scpRunData--doneLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already completed scp run data for run number %d.", datasetParams.minimalName, runNumber));
			return true;
		}
		
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + "scpRunData--hostLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Another host has already claimed scp run data for run number %d.", datasetParams.minimalName, runNumber));
			return false;
		}
		
		File source = new File(runDataDir + String.format("/%sRun%d.tar.gz", datasetParams.minimalName, runNumber));
		
		if (!source.exists()) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Compressed Run Data doesn't exist! Failed to scp run data for run number %d. Marking as done.", datasetParams.minimalName, runNumber));
			SimpleHostLock.writeDoneLock(locksDir + "scpRunData--doneLock.txt");
			return true;
		}
		
		StopWatch timer = new StopWatch().start();
		// DO task
		try {
			String stdOutAndError = CommandLineExecutor.runProgramAndWaitForItToComplete(runDataDir, "scp", String.format("%sRun%d.tar.gz", datasetParams.minimalName, runNumber), "ambarket.info:" + remoteDataDir);
			if (stdOutAndError.length() > 0) {
				System.err.println(StopWatch.getDateTimeStamp() + "\n" + stdOutAndError);
				timer.printMessageWithTime(String.format("[%s] Failed to scp run data for run number %d on remote host.", datasetParams.minimalName, runNumber));
				System.exit(1);
			}
			timer.printMessageWithTime(String.format("[%s] Finished scp'ing run data for run number %d.", datasetParams.minimalName, runNumber));
			
			SimpleHostLock.writeDoneLock(locksDir + "scpRunData--doneLock.txt");
			return true;
		} catch (IOException | InterruptedException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			RecursiveFileDeleter.deleteDirectory(new File(locksDir +  "scpRunData--hostLock.txt"));
			timer.printMessageWithTime(String.format("[%s] Unexpectedly failed to scp run data for run number %d. Removed host lock so someone else can try.", datasetParams.minimalName, runNumber));
			System.exit(1);
		}
		return false;
	}
	
	public static void extractCompressedRunDataOnRemoteServer(DatasetParameters datasetParams, ParameterTuningParameters tuningParameters, int runNumber) {
		String runDataDir = tuningParameters.runDataOutputDirectory + datasetParams.minimalName; 
		String remoteDataDir = tuningParameters.runDataFreenasDirectory + datasetParams.minimalName; 
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + String.format("/Run%d/", runNumber);
		
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "extractRunData--doneLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already completed extracting run data for run number %d.", datasetParams.minimalName, runNumber));
			return;
		}
		
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + "extractRunData--hostLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Another host has already claimed extracting run data for run number %d.", datasetParams.minimalName, runNumber));
			return;
		}
		
		File source = new File(runDataDir + String.format("/%sRun%d.tar.gz", datasetParams.minimalName, runNumber));
		
		if (!source.exists()) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Compressed Run Data doesn't exist! (TODO, do this check on remote host instead) Failed to extract run data for run number %d. Marking as done.", datasetParams.minimalName, runNumber));
			SimpleHostLock.writeDoneLock(locksDir + "extractRunData--doneLock.txt");
			return;
		}
		
		StopWatch timer = new StopWatch().start();
		// DO task
		try {
			//String stdOutAndError = CommandLineExecutor.runProgramAndWaitForItToComplete(runDataDir, "ssh", "ambarket.info", "\"cd " + remoteDataDir + "; " + "tar -xzf " + String.format("%sRun%d.tar.gz\"", datasetParams.minimalName, runNumber));
			String stdOutAndError = CommandLineExecutor.runProgramAndWaitForItToComplete(System.getProperty("user.dir") + "/scripts/", "./extractRunDataOnRemoteHost.sh", datasetParams.minimalName, String.format("%sRun%d.tar.gz", datasetParams.minimalName, runNumber));
			if (stdOutAndError.length() > 0) {
				System.err.println(StopWatch.getDateTimeStamp() + "\n" + stdOutAndError);
				timer.printMessageWithTime(String.format("[%s] Failed to extracting run data for run number %d on remote host.", datasetParams.minimalName, runNumber));
				System.exit(1);
			}
			timer.printMessageWithTime(String.format("[%s] Finished extracting run data for run number %d on remote host.", datasetParams.minimalName, runNumber));
			SimpleHostLock.writeDoneLock(locksDir + "extractRunData--doneLock.txt");
		} catch (IOException | InterruptedException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			RecursiveFileDeleter.deleteDirectory(new File(locksDir +  "extractRunData--hostLock.txt"));
			timer.printMessageWithTime(String.format("[%s] Unexpectedly failed to extracting run data for run number %d. Removed host lock so someone else can try.", datasetParams.minimalName, runNumber));
			System.exit(1);
		}
	}

	
	public static void processAllDatasets(ParameterTuningParameters parameters) {
		ParameterTuningTest test = new ParameterTuningTest();
		test.tuningParameters = parameters;
		
		for (DatasetParameters datasetParams : test.tuningParameters.datasets) {

			test.averageAllRunData(datasetParams);
			
			test.readSortAndSaveRunDataSummaryRecordsFromAverageRunData(datasetParams, "/Averages/");
			RunDataSummaryRecordGraphGenerator.generateAndSaveAllGraphs(datasetParams, test.tuningParameters, "/Averages/");
			test.generateErrorCurveScriptsForAllRunData(datasetParams, "/Averages/");

			test.executeErrorCurveAndPerExampleScriptsForBestAndWorstRunData(datasetParams, "/Averages/", 50);
		}
	}

	/**
	 * Return true if this host just realized all tests are done for this dataset
	 * @param dataset
	 * @param runNumber
	 * @return
	 */
	public boolean tryDifferentParameters(Dataset dataset, int runNumber) {
		boolean[] doneList = new boolean[tuningParameters.totalNumberOfTests];

		StopWatch timer = new StopWatch().start(), globalTimer = new StopWatch().start();
		for (int testNum = 0; testNum < tuningParameters.parametersList.length; testNum++) {
			GbmParameters parameters = tuningParameters.parametersList[testNum];
			timer.start();
			String resultMessage = performCrossValidationUsingParameters(parameters, dataset, runNumber);
			doneList[testNum] = resultMessage.startsWith("Already completed") || resultMessage.startsWith("Finished");
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] " + resultMessage + "\n\t This " + dataset.parameters.minimalName + " test in %s. Have been running for %s total.", 
					dataset.parameters.minimalName, parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber, testNum, tuningParameters.totalNumberOfTests, 
					timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
		}
		for (int i = 0; i < doneList.length; i++) {
			if (doneList[i] == false) {
				String lockPath = tuningParameters.locksDirectory 
						+ dataset.parameters.minimalName
						+ "/Run" + runNumber + "/" 
						+ tuningParameters.parametersList[i].getRunDataSubDirectory(tuningParameters.runFileType)
						+ tuningParameters.parametersList[i].getFileNamePrefix(tuningParameters.runFileType) + "--doneLock.txt";
				
				if (!SimpleHostLock.checkDoneLock(lockPath)) {
					return false;
				}
			}
		}
		// All the ones we thought still needed to be done are done -> all are done.
		return true;
	}
	
	public void averageAllRunData(DatasetParameters datasetParams) {
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/Averages/";
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "averageAllDataLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already averaged all data for ", datasetParams.minimalName));
			return;
		}
		
		String paramTuningDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/";
		int submissionNumber = 0;
		StopWatch globalTimer = new StopWatch().start() ;
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();
		for (GbmParameters parameters : tuningParameters.parametersList) {
			futureQueue.add(GradientBoostingTree.executor.submit(
					new AverageRunDataForParameters(datasetParams, parameters, paramTuningDirectory, tuningParameters, ++submissionNumber, globalTimer)));
			
			if (futureQueue.size() >= 50) {
				System.out.println(StopWatch.getDateTimeStamp() + "Reached 50 threads, waiting for some to finish");
				while (futureQueue.size() > 20) {
					try {
						futureQueue.poll().get();

					} catch (InterruptedException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					} catch (ExecutionException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Submitted the last of them, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		SimpleHostLock.writeDoneLock(locksDir + "averageAllDataLock.txt");
		System.out.println(StopWatch.getDateTimeStamp() + "Finished averaging all run data.");
	}
	
	/**
	 * 
	 * @param runDataSubDirectory Should wither be run0/ or Averages/
	 */
	public void readSortAndSaveRunDataSummaryRecordsFromAverageRunData(DatasetParameters datasetParams, String runDataSubDirectory) {
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/RunDataSummaryRecords/";
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "runDataSummaryLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already read sorted and saved all RunDataSummaryRecords", datasetParams.minimalName));
			return;
		}
		
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + runDataSubDirectory;
		PriorityQueue<RunDataSummaryRecord> sortedByCvValidationError = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.CvValidationErrorComparator());
		PriorityQueue<RunDataSummaryRecord> sortedByAllDataTestError = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.AllDataTestErrorComparator());
		PriorityQueue<RunDataSummaryRecord> sortedByTimeInSeconds = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.TimeInSecondsComparator());
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (GbmParameters parameters : tuningParameters.parametersList) {
			timer.start();

			RunDataSummaryRecord record = RunDataSummaryRecord.readRunDataSummaryRecordFromRunDataFile(runDataDirectory, parameters, tuningParameters.runFileType);
			if (record != null) {
				sortedByCvValidationError.add(record);
				sortedByAllDataTestError.add(record);
				sortedByTimeInSeconds.add(record);
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Created RunDataSummaryRecord for %s (%d out of %d) in %s. Have been running for %s total.", 
						datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			} else {
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Failed to create RunDataSummaryRecord for %s because runData was not found (%d out of %d) in %s. Have been running for %s total.", 
						datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			}
		}
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByCvValidationError", sortedByCvValidationError);
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByAllDataTestError", sortedByAllDataTestError);
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByTimeInSeconds", sortedByTimeInSeconds);
		SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryLock.txt");
	}
	
	public void generateErrorCurveScriptsForAllRunData(DatasetParameters datasetParams, String runDataSubDirectory) {
		String overallLocksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/ErrorCurves/";
		new File(overallLocksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(overallLocksDir + "generatedAllErrorCurvesLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already generated all error curves for ", datasetParams.minimalName));
			return;
		}
		ExecutorService executor = Executors.newCachedThreadPool();
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + runDataSubDirectory;
		int submissionNumber = 0;
		StopWatch globalTimer = new StopWatch().start() ;
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();
		for (GbmParameters parameters : tuningParameters.parametersList) {
			futureQueue.add(executor.submit(
					new ErrorCurveScriptGenerator(datasetParams, parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer)));
			
			if (futureQueue.size() >= 30) {
				System.out.println(StopWatch.getDateTimeStamp() + "Reached 30 error curve threads, waiting for some to finish");
				while (futureQueue.size() > 20) {
					try {
						futureQueue.poll().get();

					} catch (InterruptedException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					} catch (ExecutionException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Submitted the last of the error curve jobs, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		SimpleHostLock.writeDoneLock(overallLocksDir + "generatedAllErrorCurvesLock.txt");
		System.out.println(StopWatch.getDateTimeStamp() + "Finished generating error curves for all run data.");
	}
	
	public void executeErrorCurveScriptsForAllRunData(DatasetParameters datasetParameters, String runDataSubDirectory) {
		String overallLocksDir = tuningParameters.locksDirectory + datasetParameters.minimalName + "/ErrorCurveExecutor/";
		new File(overallLocksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(overallLocksDir + "executedAllErrorCurvesLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already executed all error curve scripts for ", datasetParameters.minimalName));
			return;
		}
		ExecutorService executor = Executors.newFixedThreadPool(3);
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		int submissionNumber = 0;
		StopWatch globalTimer = new StopWatch().start() ;
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();
		for (GbmParameters parameters : tuningParameters.parametersList) {
			futureQueue.add(executor.submit(
					new ErrorCurveScriptExecutor(datasetParameters, parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer)));
			
			if (futureQueue.size() >= 30) {
				System.out.println(StopWatch.getDateTimeStamp() + "Reached 30 error curve executor threads, waiting for some to finish");
				while (futureQueue.size() > 20) {
					try {
						futureQueue.poll().get();
	
					} catch (InterruptedException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					} catch (ExecutionException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Submitted the last of the error curve jobs, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		SimpleHostLock.writeDoneLock(overallLocksDir + "generatedAllErrorCurvesLock.txt");
		System.out.println(StopWatch.getDateTimeStamp() + "Finished executing error curves for all run data.");
	}
	
	public void executeErrorCurveAndPerExampleScriptsForBestAndWorstRunData(DatasetParameters datasetParameters, String runDataSubDirectory, int numberOfBestAndWorst) {
		ExecutorService executor = Executors.newFixedThreadPool(3);
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		ArrayList<RunDataSummaryRecord> allRecords = RunDataSummaryRecord.readRunDataSummaryRecords(datasetParameters.minimalName, runDataDirectory);
		
		int submissionNumber = 0;
		StopWatch globalTimer = new StopWatch().start() ;
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();

		Dataset dataset = new Dataset(datasetParameters);
		
		for (int i = 0; i < numberOfBestAndWorst; i++) {					
			futureQueue.add(executor.submit(
					new ErrorCurveScriptExecutor(datasetParameters, allRecords.get(i).parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer)));
			futureQueue.add(executor.submit(
					new ErrorCurveScriptExecutor(datasetParameters, allRecords.get(allRecords.size()-1-i).parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer)));
			
			// Don't have per example run data for these due to bug.
			if (dataset.parameters.minimalName.equals("nasa") || dataset.parameters.minimalName.equals("powerPlant")) {
				continue;
			}
			futureQueue.add(executor.submit(
					new PredictionGraphGenerator(dataset, allRecords.get(i).parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer, ParameterTuningParameters.interestingPredictorGraphsByDataset.get(datasetParameters.minimalName))));
			futureQueue.add(executor.submit(
					new PredictionGraphGenerator(dataset, allRecords.get(allRecords.size()-1-i).parameters, runDataDirectory, tuningParameters, ++submissionNumber, globalTimer, ParameterTuningParameters.interestingPredictorGraphsByDataset.get(datasetParameters.minimalName))));
			
			if (futureQueue.size() >= 30) {
				System.out.println(StopWatch.getDateTimeStamp() + "Reached 30 error curve executor threads, waiting for some to finish");
				while (futureQueue.size() > 20) {
					try {
						futureQueue.poll().get();

					} catch (InterruptedException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					} catch (ExecutionException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					}
				}
			}
		}

		System.out.println(StopWatch.getDateTimeStamp() + "Submitted the last of the error curve jobs, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Finished executing error curves for all run data.");
		executor.shutdownNow();
	}

	
	//----------------------------------------------Private Per Parameter Helpers-----------------------------------------------------------------------
	
	private String performCrossValidationUsingParameters(GbmParameters parameters, Dataset dataset, int runNumber) {
		String runDataDir = tuningParameters.runDataOutputDirectory + dataset.parameters.minimalName + String.format("/Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber);
		String locksDir = tuningParameters.locksDirectory + dataset.parameters.minimalName + String.format("/Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber);
		
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--doneLock.txt")) {
			return "Already completed %s on run number %d. (%d out of %d)";
		}
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--hostLock.txt")) {
			return "Another host has already claimed %s on run number %d. (%d out of %d)";
		}
		CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, dataset, tuningParameters.CV_NUMBER_OF_FOLDS, tuningParameters.CV_STEP_SIZE);
		if (ensemble != null) {
			try {
				ensemble.saveRunDataToFile(runDataDir, tuningParameters.runFileType);
			} catch (IOException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
			SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--doneLock.txt");
			return "Finished %s on run number %d. (%d out of %d)";
		} else {
			SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--doneLock.txt");
			return "Failed to build %s on run number %d due to impossible parameters. (%d out of %d)";
		}
	}	
}
