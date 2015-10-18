package gbm.test;
import gbm.RegressionTree;
import gbm.RegressionTree.TerminalType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import utilities.Logger;
import utilities.StopWatch;


public class RegressionTreeTest {

	static ArrayList<ArrayList<Double>>  randomInstances = new ArrayList<ArrayList<Double>>();
	static ArrayList<Double> randomLabels = new ArrayList<Double>();
	static StopWatch timer = (new StopWatch());
	static Random rand = new Random();
	
	@BeforeClass
	public static void setup() {
		timer.start();
		for (int example = 0; example < 10000; example++) {
			ArrayList<Double> instance = new ArrayList<Double>();
			for (int attribute = 0; attribute < 2; attribute++) {
				instance.add(rand.nextDouble());
			}
			randomLabels.add(rand.nextDouble());
			randomInstances.add(instance);
			
		}
		Logger.println(Logger.LEVELS.DEBUG, "Done generating Data: " + timer.getElapsedSeconds());
	}

	@Test
	public void build_regression_treeTest() {
		timer.start();
		RegressionTree tree = new RegressionTree(1, 100, TerminalType.AVERAGE);
		
		tree.build(randomInstances, randomLabels);
		try {
			tree.print_nodes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.println(Logger.LEVELS.DEBUG, "Done building tree: " + timer.getElapsedSeconds());
	}
}
