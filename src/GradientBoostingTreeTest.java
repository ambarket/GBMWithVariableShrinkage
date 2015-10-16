import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;




public class GradientBoostingTreeTest {
	GradientBoostingTree boostedTree = null;


	static ArrayList<ArrayList<Double>>  randomInstances = new ArrayList<ArrayList<Double>>();
	static ArrayList<Double> randomLabels = new ArrayList<Double>();
	static StopWatch timer = (new StopWatch());
	static Random rand = new Random();
	
	@BeforeClass
	public static void setUpBeforeClass() {
		timer.start();
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
		boostedTree = new GradientBoostingTree(0.5, 0.01, 10000, 10, 3);
		GradientBoostingTree.ResultFunction result = boostedTree.gradient_boosting_tree(randomInstances, randomLabels);
		System.out.println("Done boosting trees: " + timer.getElapsedSeconds());
		
		System.out.println("Error: " + calcSquaredError(result));
		
	}
	
	private double calcSquaredError(GradientBoostingTree.ResultFunction result) {
		double error = 0.0;
		for (int i = 0; i < randomInstances.size(); i++) {
			double tmp = result.get_value(randomInstances.get(i)) - randomLabels.get(i);
			error += tmp*tmp;
		}
		return error;
	}

}
