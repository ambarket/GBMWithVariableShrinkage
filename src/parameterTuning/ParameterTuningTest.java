package parameterTuning;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.cv.CrossValidatedResultFunctionEnsemble;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import parameterTuning.plotting.MathematicaLearningCurveCreator;
import parameterTuning.plotting.RunDataSummaryRecordGraphGenerator;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;
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
		for (DatasetParameters datasetParams : test.tuningParameters.datasets) {
			for (int runNumber = 0; runNumber < test.tuningParameters.NUMBER_OF_RUNS; runNumber++) {
				Dataset dataset = new Dataset(datasetParams, ParameterTuningParameters.TRAINING_SAMPLE_FRACTION);
				test.tryDifferentParameters(dataset, runNumber);
			}
		}
		GradientBoostingTree.executor.shutdownNow();
	}
	
	public static void processAllDatasets(ParameterTuningParameters parameters) {
		ParameterTuningTest test = new ParameterTuningTest();
		test.tuningParameters = parameters;
		
		for (DatasetParameters datasetParams : test.tuningParameters.datasets) {

			test.averageAllRunData(datasetParams);
			test.generateMathematicaLearningCurvesForAllRunData(datasetParams, "/Averages/");
			test.readSortAndSaveRunDataSummaryRecordsFromAverageRunData(datasetParams, "/Averages/");
			RunDataSummaryRecordGraphGenerator.generateAndSaveAllGraphs(datasetParams, test.tuningParameters, "/Averages/");
		}
	}

	public void tryDifferentParameters(Dataset dataset, int runNumber) {
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : tuningParameters.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.maxLearningRates : tuningParameters.constantLearningRates) {
					for (int numberOfSplits : tuningParameters.maxNumberOfSplts) {
						for (double bagFraction : tuningParameters.bagFractions) {
							for (int minExamplesInNode : tuningParameters.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : tuningParameters.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, tuningParameters.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();
									String resultMessage = performCrossValidationUsingParameters(parameters, dataset, runNumber);
									System.out.println(String.format("[%s]" + resultMessage + "\n This " + dataset.parameters.minimalName + " test took %.4f minutes. Have been runnung for %.4f minutes total.", 
											dataset.parameters.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), runNumber, ++done, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
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
	
	public void averageAllRunData(DatasetParameters datasetParams) {
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/Averages/";
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "averageAllDataLock.txt")) {
			System.out.println(String.format("[%s] Already averages all data for ", datasetParams.minimalName));
			return;
		}
		
		String paramTuningDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + "/";
		int submissionNumber = 0;
		StopWatch globalTimer = new StopWatch().start() ;
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();
		for (LearningRatePolicy learningRatePolicy : tuningParameters.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.maxLearningRates : tuningParameters.constantLearningRates) {
					for (int numberOfSplits : tuningParameters.maxNumberOfSplts) {
						for (double bagFraction : tuningParameters.bagFractions) {
							for (int minExamplesInNode : tuningParameters.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : tuningParameters.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, tuningParameters.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
		
									futureQueue.add(GradientBoostingTree.executor.submit(
											new AverageRunDataForParameters(datasetParams, parameters, paramTuningDirectory, tuningParameters, ++submissionNumber, globalTimer)));
									
									if (futureQueue.size() >= 50) {
										System.out.println("Reached 50 threads, waiting for some to finish");
										while (futureQueue.size() > 20) {
											try {
												futureQueue.poll().get();

											} catch (InterruptedException e) {
												e.printStackTrace();
											} catch (ExecutionException e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println("Submitted the last of them, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		SimpleHostLock.writeDoneLock(locksDir + "averageAllDataLock.txt");
		System.out.println("Finished averaging all run data.");
	}
	
	/**
	 * 
	 * @param runDataSubDirectory Should wither be run0/ or Averages/
	 */
	public void readSortAndSaveRunDataSummaryRecordsFromAverageRunData(DatasetParameters datasetParams, String runDataSubDirectory) {
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/RunDataSummaryRecords/";
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "runDataSummaryLock.txt")) {
			System.out.println(String.format("[%s] Already read sorted and saved all RunDataSummaryRecords", datasetParams.minimalName));
			return;
		}
		
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + runDataSubDirectory;
		PriorityQueue<RunDataSummaryRecord> sortedByCvValidationError = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.CvValidationErrorComparator());
		PriorityQueue<RunDataSummaryRecord> sortedByAllDataTestError = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.AllDataTestErrorComparator());
		PriorityQueue<RunDataSummaryRecord> sortedByTimeInSeconds = new PriorityQueue<RunDataSummaryRecord>(new RunDataSummaryRecord.TimeInSecondsComparator());
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : tuningParameters.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.maxLearningRates : tuningParameters.constantLearningRates) {
					for (int numberOfSplits : tuningParameters.maxNumberOfSplts) {
						for (double bagFraction : tuningParameters.bagFractions) {
							for (int minExamplesInNode : tuningParameters.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : tuningParameters.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, tuningParameters.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();

									RunDataSummaryRecord record = RunDataSummaryRecord.readRunDataSummaryRecordFromRunDataFile(runDataDirectory, parameters, tuningParameters.runFileType);
									if (record != null) {
										sortedByCvValidationError.add(record);
										sortedByAllDataTestError.add(record);
										sortedByTimeInSeconds.add(record);
										System.out.println(String.format("[%s] Created RunDataSummaryRecord for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
												datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
									} else {
										System.out.println(String.format("[%s] Failed to create RunDataSummaryRecord for %s because runData was not found (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
												datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
									}
								}
							}
						}
					}
				}
			}
		}
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByCvValidationError", sortedByCvValidationError);
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByAllDataTestError", sortedByAllDataTestError);
		RunDataSummaryRecord.saveRunDataSummaryRecords(runDataDirectory, "SortedByTimeInSeconds", sortedByTimeInSeconds);
		SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryLock.txt");
	}
	
	public void generateMathematicaLearningCurvesForAllRunData(DatasetParameters datasetParams, String runDataSubDirectory) {
		String overallLocksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/Averages/";
		new File(overallLocksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(overallLocksDir + "generatedAllErrorCurvesLock.txt")) {
			System.out.println(String.format("[%s] Already generated all error curves for ", datasetParams.minimalName));
			return;
		}
		
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParams.minimalName + runDataSubDirectory;
		int done = 0;
		StopWatch timer = (new StopWatch()), globalTimer = new StopWatch().start() ;
		for (LearningRatePolicy learningRatePolicy : tuningParameters.learningRatePolicies) {
			for (double minLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.minLearningRates : new double[] {-1}) {
				for (double maxLR : (learningRatePolicy == LearningRatePolicy.REVISED_VARIABLE) ? tuningParameters.maxLearningRates : tuningParameters.constantLearningRates) {
					for (int numberOfSplits : tuningParameters.maxNumberOfSplts) {
						for (double bagFraction : tuningParameters.bagFractions) {
							for (int minExamplesInNode : tuningParameters.minExamplesInNode) {
								for (SplitsPolicy splitPolicy : tuningParameters.splitPolicies) {
									// Note minLearningRate will be ignored unless LearningRatePolicy == REVISED_VARIABLE
									GbmParameters parameters = new GbmParameters(minLR, maxLR, numberOfSplits, 
												bagFraction, minExamplesInNode, tuningParameters.NUMBER_OF_TREES, 
												learningRatePolicy, splitPolicy);
									timer.start();
									String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/Averages/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
									new File(locksDir).mkdirs();
									if (SimpleHostLock.checkDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--errorCurveLock.txt")) {
										System.out.println(String.format("[%s] Already Created error curve runData for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
												datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
										continue;
									}
									MathematicaLearningCurveCreator.createLearningCurveForParameters(datasetParams, runDataDirectory, parameters, tuningParameters.runFileType);
									SimpleHostLock.writeDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--errorCurveLock.txt");
									System.out.println(String.format("[%s] Created error curve for %s (%d out of %d) in %.4f minutes. Have been runnung for %.4f minutes total.", 
											datasetParams.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), ++done, tuningParameters.totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
								}
							}
						}
					}
				}
			}
		}
		SimpleHostLock.writeDoneLock(overallLocksDir + "generatedAllErrorCurvesLock.txt");
	}
	
	
	//----------------------------------------------Private Per Parameter Helpers-----------------------------------------------------------------------
	
	private String performCrossValidationUsingParameters(GbmParameters parameters, Dataset dataset, int runNumber) {
		String runDataDir = tuningParameters.runDataOutputDirectory + dataset.parameters.minimalName + String.format("/Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber);
		String locksDir = tuningParameters.locksDirectory + dataset.parameters.minimalName + String.format("/Run%d/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType), runNumber);
		
		new File(locksDir).mkdirs();
		if (!SimpleHostLock.checkAndClaimHostLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--hostLock.txt")) {
			return "Another host has already claimed %s on run number %d. (%d out of %d)";
		}
		if (SimpleHostLock.checkDoneLock(locksDir + parameters.getFileNamePrefix(tuningParameters.runFileType) + "--doneLock.txt")) {
			return "Already completed %s on run number %d. (%d out of %d)";
		}
		CrossValidatedResultFunctionEnsemble ensemble = GradientBoostingTree.crossValidate(parameters, dataset, tuningParameters.CV_NUMBER_OF_FOLDS, tuningParameters.CV_STEP_SIZE);
		if (ensemble != null) {
			try {
				ensemble.saveRunDataToFile(runDataDir, tuningParameters.runFileType);
			} catch (IOException e) {
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
