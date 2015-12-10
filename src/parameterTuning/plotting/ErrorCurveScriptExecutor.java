package parameterTuning.plotting;

import java.io.File;
import java.util.concurrent.Callable;

import dataset.DatasetParameters;
import gbm.GbmParameters;
import parameterTuning.ParameterTuningParameters;
import utilities.CommandLineExecutor;
import utilities.CompressedTarBallCreator;
import utilities.RecursiveFileDeleter;
import utilities.SimpleHostLock;
import utilities.StopWatch;

public class ErrorCurveScriptExecutor implements Callable<Void>{
	DatasetParameters datasetParams;
	GbmParameters parameters;
	String runDataFullDirectory;
	ParameterTuningParameters tuningParameters;
	int submissionNumber;
	StopWatch globalTimer;
	
	public ErrorCurveScriptExecutor(DatasetParameters datasetParams, GbmParameters parameters, String runDataFullDirectory, ParameterTuningParameters tuningParameters, int submissionNumber, StopWatch globalTimer) {
		this.datasetParams = datasetParams;
		this.parameters = parameters;
		this.runDataFullDirectory = runDataFullDirectory;
		this.tuningParameters = tuningParameters;
		this.submissionNumber = submissionNumber;
		this.globalTimer = globalTimer;
	}
	/**
	 * Return path to mathematica file containing learning curve code.
	 * @param datasetParams
	 * @param runDataFullDirectory
	 * @param parameters
	 * @param expectedRunFileType
	 * @return
	 */
	public Void call() {
		StopWatch timer = new StopWatch().start();
		String locksDir = tuningParameters.locksDirectory + datasetParams.minimalName + "/ErrorCurveExecutor/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "errorCurveExecutorLock.txt")) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Already executed error curve script for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
				
		String baseFileDirectory = (runDataFullDirectory + parameters.getRunDataSubDirectory(tuningParameters.runFileType)).replace("\\", "/");
		String mathematicaFileName = "mathematica.m";
		String mathematicaFileFullPath = baseFileDirectory + mathematicaFileName;
		
		System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] About to execute error curve script for %s (%d out of %d) in %s. Have been running for %s total.", 
				datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
		
		try {
			CommandLineExecutor.runProgramAndWaitForItToComplete(baseFileDirectory, new String[] {"cmd", "/c", "math.exe", "-script", mathematicaFileName});
		
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Finished executing error curve script, about to compress and delete original script file for %s (%d out of %d) in %s. Have been running for %s total. \nDirectory: %s", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit(), baseFileDirectory));
		} catch (Exception e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Call to mathematica script failed, not writing done lock! Failed to generate error curve for %s (%d out of %d) in %s. Have been running for %s total.", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		
		try {
			CompressedTarBallCreator.compressFile(new File(mathematicaFileFullPath), new File(mathematicaFileFullPath + ".tar.gz"));
			RecursiveFileDeleter.deleteDirectory(new File(mathematicaFileFullPath));
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Finished compressing error curve script for %s (%d out of %d) in %s. Have been running for %s total. \nDirectory: %s", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit(), baseFileDirectory));
		} catch (Exception e) {
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[%s] Failed to compress error curve script, not critical so still writing done lock for %s (%d out of %d) in %s. Have been running for %s total. \nDirectory: %s", 
					datasetParams.minimalName,parameters.getRunDataSubDirectory(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit(), baseFileDirectory));
		}
	
		SimpleHostLock.writeDoneLock(locksDir + "errorCurveExecutorLock.txt");
		return null;
	}
}
