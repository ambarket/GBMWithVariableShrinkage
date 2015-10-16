import java.util.ArrayList;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;


public class RegressionTreeTest {

	static ArrayList<ArrayList<Double>>  randomInstances = new ArrayList<ArrayList<Double>>();
	static ArrayList<Double> randomLabels = new ArrayList<Double>();
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
		System.out.println("Done generating Data: " + timer.getElapsedSeconds());
	}
	
	// 		minObsInNode = 10;
	// maxDepth = 3;

	
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
		RegressionTree tree = new RegressionTree();
		
		RegressionTree.BestSplit bestSplit = tree.get_optimal_split(randomInstances, randomLabels, Double.MAX_VALUE);
		System.out.println("Found Best Split " + timer.getElapsedSeconds());
		System.out.println(bestSplit);
	}

	@Test
	public void build_regression_treeTest() {
		timer.start();
		RegressionTree tree = new RegressionTree();
		
		tree.build_regression_tree(randomInstances, randomLabels);
		tree.print_nodes();
	}
}
