package gbm.test;
import gbm.Dataset;
import gbm.GradientBoostingTree;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import utilities.Logger;
import utilities.StopWatch;




public class GradientBoostingTreeTest {
	GradientBoostingTree boostedTree = null;


	static Dataset dataset;
	static ArrayList<ArrayList<Double>>  randomInstances = new ArrayList<ArrayList<Double>>();
	static ArrayList<Double> randomLabels = new ArrayList<Double>();
	static int minObsInNode = 10;
	static StopWatch timer = (new StopWatch());
	static Random rand = new Random();
	
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
		Logger.println(Logger.LEVELS.DEBUG, "Done generating Data: " + timer.getElapsedSeconds());
	}
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		timer.start();
		boostedTree = new GradientBoostingTree(dataset, 0.5, 0.01, 1000, 10, 3);
		GradientBoostingTree.ResultFunction result = boostedTree.buildGradientBoostingMachine();
		Logger.println(Logger.LEVELS.DEBUG, "Done boosting trees: " + timer.getElapsedSeconds());
		
		Logger.println(Logger.LEVELS.DEBUG, "Error: " + calcSquaredError(result));
		
		double[] relativeInf = result.calcRelativeInfluences();
		Logger.println(Logger.LEVELS.DEBUG, "Relative Influences\n--------------------");
		for (int i = 0; i < relativeInf.length; i++) {
			Logger.println(Logger.LEVELS.DEBUG, i + ": " + relativeInf[i] + "%");
		}
		
	}
	
	private double calcSquaredError(GradientBoostingTree.ResultFunction result) {
		double error = 0.0;
		for (int i = 0; i < randomInstances.size(); i++) {
			double tmp = result.predictLabel(randomInstances.get(i)) - randomLabels.get(i);
			error += tmp*tmp;
		}
		return error;
	}

}
