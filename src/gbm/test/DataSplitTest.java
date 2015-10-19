package gbm.test;
import gbm.DataSplit;
import gbm.Dataset;
import gbm.RegressionTree;

import java.util.ArrayList;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class DataSplitTest {

	static Dataset dataset;
	static ArrayList<ArrayList<Double>>  randomInstances = new ArrayList<ArrayList<Double>>();
	static ArrayList<Double> randomLabels = new ArrayList<Double>();
	static int minExamplesInNode = 10;
	static StopWatch timer = (new StopWatch());
	static Random rand = new Random();
	static boolean[] inSample;
	static int[] shuffledIndices;
	@BeforeClass
	public static void setup() {
		for (int example = 0; example < 10000; example++) {
			ArrayList<Double> instance = new ArrayList<Double>();
			for (int attribute = 0; attribute < 2; attribute++) {
				instance.add(rand.nextDouble());
			}
			randomLabels.add(rand.nextDouble());
			randomInstances.add(instance);
			
		}
		dataset = new Dataset(randomInstances, randomLabels);
		// data for growing trees
	    inSample = new boolean[dataset.numOfExamples];
		shuffledIndices = RandomSample.fisherYatesShuffle(dataset.numOfExamples);
		int sampleSize = (int)(1 * shuffledIndices.length);
		for (int i = 0; i < sampleSize; i++ ) {
			inSample[shuffledIndices[i]] = true;
		}
		
		Logger.println(Logger.LEVELS.DEBUG, "Done generating Data: " + timer.getElapsedSeconds());
	}
	
	// 		minExamplesInNode = 10;
	// maxNumberOfSplits = 3;

	
	/**
	 *  BenchMark for get_optimal_split with 10000 random instances
		Done generating Data: 0.007396846
		Found Best Split 0.028187385
		SplitAttribute: 0
		splitValue: 0.1534743635124522
		splitIndex: 1543
		leftSquaredError: 130.78544215112169
		rightSquaredError: 707.4522880073323
		status: true
	 */
	@Test
	public void get_optimal_splitTest() {
		timer.start();
		// we need to sample randomly without replacement
		DataSplit split = DataSplit.splitDataIntoChildren(dataset, inSample, minExamplesInNode, dataset.calcMeanY(), Double.MAX_VALUE, RegressionTree.TerminalType.AVERAGE);
		Logger.println(Logger.LEVELS.DEBUG, "Found Best Split " + timer.getElapsedSeconds());
		Logger.println(Logger.LEVELS.DEBUG, split);
	}

}
