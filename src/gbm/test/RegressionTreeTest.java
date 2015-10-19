package gbm.test;
import gbm.Dataset;
import gbm.RegressionTree;
import gbm.RegressionTree.TerminalType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;


public class RegressionTreeTest {
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

	@Test
	public void build_regression_treeTest() {
		timer.start();
		RegressionTree tree = new RegressionTree(1, 100, TerminalType.AVERAGE);
		
		tree.build(dataset, inSample);
		try {
			tree.print_nodes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.println(Logger.LEVELS.DEBUG, "Done building tree: " + timer.getElapsedSeconds());
	}
}
