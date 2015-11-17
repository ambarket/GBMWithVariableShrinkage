package parameterTuning.plotting;

import gbm.GbmParameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.google.common.primitives.Doubles;

import parameterTuning.ParameterTuningParameters;
import utilities.DoubleCompare;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import utilities.SumCountAverage;
import dataset.Attribute;
import dataset.Attribute.Type;
import dataset.Dataset;

public class PredictionGraphGenerator implements Callable<Void> {
	Dataset dataset;
	GbmParameters parameters;
	String runDataFullDirectory;
	ParameterTuningParameters tuningParameters;
	int submissionNumber;
	StopWatch globalTimer;
	String[] predictorNames;
	int[] predictorIndices;
	
	int numberOfExamples;
	
	int[] trainingDataCount;
	double[] trainingDataPrediction;
	double[] trainingDataResidual;
	double[] trainingDataAvgLearningRate;
	
	int[] testDataCount;
	double[] testDataPrediction;
	double[] testDataResidual;
	double[] testDataAvgLearningRate;
	
	double[] targetResponses;
	
	public PredictionGraphGenerator(Dataset dataset, GbmParameters parameters, String runDataFullDirectory, ParameterTuningParameters tuningParameters, int submissionNumber, StopWatch globalTimer, String...predictorNames) {
		this.dataset = dataset;
		this.parameters = parameters;
		this.runDataFullDirectory = runDataFullDirectory;
		this.tuningParameters = tuningParameters;
		this.submissionNumber = submissionNumber;
		this.globalTimer = globalTimer;
		this.predictorNames = predictorNames;
		if (predictorNames.length < 1 || predictorNames.length > 2) {
			throw new IllegalArgumentException();
		}
		String[] dsPredictorNames = dataset.getPredictorNames();
		for (int i = 0; i < predictorNames.length; i++) {
			predictorIndices[i] = Arrays.binarySearch(dsPredictorNames, predictorNames[i]);
		}
		
		numberOfExamples = dataset.getNumberOfTrainingExamples();
		
		trainingDataCount = new int[numberOfExamples];
		trainingDataPrediction = new double[numberOfExamples];
		trainingDataResidual = new double[numberOfExamples];
		trainingDataAvgLearningRate = new double[numberOfExamples];
		
		testDataCount = new int[numberOfExamples];
		testDataPrediction = new double[numberOfExamples];
		testDataResidual = new double[numberOfExamples];
		testDataAvgLearningRate = new double[numberOfExamples];
		
		targetResponses = new double[numberOfExamples];
	}
	
	public Void call() {
		StopWatch timer = new StopWatch().start();
		String locksDir = tuningParameters.locksDirectory + dataset.parameters.minimalName + "/PredictionGraphs/" + parameters.getRunDataSubDirectory(tuningParameters.runFileType);
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "predictionGraphLock.txt")) {
			System.out.println(String.format("[%s] Already generated prediction graphs for %s (%d out of %d) in %s. Have been runnung for %s total.", 
					dataset.parameters.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}

		
		String runDataFilePath = runDataFullDirectory + parameters.getRunDataSubDirectory(tuningParameters.runFileType) + parameters.getFileNamePrefix(tuningParameters.runFileType)  + "--averagePerExampleRunData.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(runDataFilePath));
			br.readLine(); // skip header
			// read in error data 
			String line = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				String[] components = line.split("\t");

				if (Integer.parseInt(components[0]) != i) {
					throw new IllegalStateException();
				}
				
				targetResponses[i] = Double.parseDouble(components[1]);
				trainingDataCount[i] = Integer.parseInt(components[2]);
				trainingDataPrediction[i] = Double.parseDouble(components[3]);
				trainingDataResidual[i] = Double.parseDouble(components[4]);
				trainingDataAvgLearningRate[i] = Double.parseDouble(components[5]);
				testDataCount[i] = Integer.parseInt(components[6]);
				testDataPrediction[i] = Double.parseDouble(components[7]);
				testDataResidual[i] = Double.parseDouble(components[8]);
				testDataAvgLearningRate[i] = Double.parseDouble(components[9]);

				i++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(String.format("[%s] Reading of per example run data failed! Failed to generate prediction graphs for %s (%d out of %d) in %s. Have been runnung for %s total.", 
					dataset.parameters.minimalName, parameters.getFileNamePrefix(tuningParameters.runFileType), submissionNumber, tuningParameters.totalNumberOfTests, timer.getTimeInMostAppropriateUnit(), globalTimer.getTimeInMostAppropriateUnit()));
			return null;
		}
		TreeSet<Point> points = getPoints();
		
		SimpleHostLock.writeDoneLock(locksDir + "predictionGraphLock.txt");
		return null;
	}
	
	private TreeSet<Point> getPoints() {
		HashMap<UniqueXYPointKey, Point> uniqueXYPointKeyToAvgZValueMap = new HashMap<>();
		Attribute[][] instances = dataset.getTrainingInstances();
		
		for (int i = 0; i < numberOfExamples; i++) {
			Attribute[] instance = instances[i];
			UniqueXYPointKey key = new UniqueXYPointKey(instance, predictorIndices);
			
			Point avg = uniqueXYPointKeyToAvgZValueMap.get(key);
			if (avg == null) {
				avg = new Point(key);
				uniqueXYPointKeyToAvgZValueMap.put(key, avg);
			}
			avg.response.addData(targetResponses[i]);
			avg.predictionAsTrainingData.addData(trainingDataPrediction[i]);
			avg.predictionAsTestData.addData(testDataPrediction[i]);
		}
		
		return new TreeSet<Point>(uniqueXYPointKeyToAvgZValueMap.values());
	}
	
	private static class UniqueXYPointKey {
		public Attribute[] XYvalues;
		public UniqueXYPointKey(Attribute[] instance, int[] predictorIndices) {
			XYvalues = new Attribute[predictorIndices.length];
			
			for (int i = 0; i < XYvalues.length; i++) {
				XYvalues[i] = instance[i];
			}
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(XYvalues);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UniqueXYPointKey other = (UniqueXYPointKey) obj;
			if (!Arrays.equals(XYvalues, other.XYvalues))
				return false;
			return true;
		}
	}
	
	private static class Point implements Comparable<Point> {
		public SumCountAverage response= new SumCountAverage();
		public SumCountAverage predictionAsTrainingData = new SumCountAverage();
		public SumCountAverage predictionAsTestData = new SumCountAverage();
		public UniqueXYPointKey key;
		public Point(UniqueXYPointKey keu) {
			this.key = key;
		}
		
		@Override
		public int compareTo(Point that) {
			int comp = -1;
			if ((comp = DoubleCompare.compare(response.getMean(), that.response.getMean())) != 0) {
				return comp;
			}
			if ((comp = DoubleCompare.compare(predictionAsTrainingData.getMean(), that.predictionAsTrainingData.getMean())) != 0) {
				return comp;
			}
			return DoubleCompare.compare(predictionAsTestData.getMean(), that.predictionAsTestData.getMean());
		}
	}
}
