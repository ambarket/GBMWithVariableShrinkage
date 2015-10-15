/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.Iterator;
import java.util.Vector;
//import gbt.ranker.RegressionTree.TerminalType;

public class GradientBoostingTree {
	// class members
	private double m_sampling_size_ratio; 
	private double m_learning_rate;
	private int m_tree_number;
	
	// tree related parameters
	private int m_tree_min_nodes;
	private int m_tree_depth;
	
	// construction function
	GradientBoostingTree(double sample_size_ratio, double learning_rate, 
			int tree_number, int tree_min_nodes, int tree_depth)
		{
		// This will be called when initialize the class with parameters
		
		/*
		 *  Check the validity of numbers
		 */
		if (sample_size_ratio <= 0 || learning_rate <= 0 || tree_number < 1 ||
				tree_min_nodes < 3 || tree_depth < 1) {
			System.out.println(String.format("Wrong parameters:  " + 
		      "sample_size_ratio=%f, learning_rate=%f, tree_number=%d", 
		        sample_size_ratio, learning_rate, tree_number));
			
			System.exit(0);
		}
		
		// In the gradient method, the portion of "sample_size_ration" 
		// will be sampled without
		// replacement.
		m_sampling_size_ratio = sample_size_ratio; 
		
		// Set learning rate or the shrink-age factor
		m_learning_rate = learning_rate;
		
		// set the number of trees
		m_tree_number = tree_number;
		
		// set tree parameters
		m_tree_min_nodes = tree_min_nodes;
		m_tree_depth     = tree_depth;
	}
	
	// construction function
	GradientBoostingTree() {
		// use default settings
		
		// This will be called when initialize the class without parameters
		
		// In the gradient method, half of data will be sampled without
		// replacement by default.
		m_sampling_size_ratio = 0.5; 
		
		// Set default learning rate or the shrink-age factor
		m_learning_rate = 0.05;
		
		// set the default number of trees
		m_tree_number = 100;
		
		// set tree parameters: will not change the default
		// parameters of the Regression Tree method.
		m_tree_min_nodes = 10; 
		m_tree_depth = 3; 
	}
	
	// set parameters
	public void set_sample_size_ratio(double sample_size_ratio) {
		if (sample_size_ratio <= 0) {
			System.out.println("Invalid sampling ratio");
			System.exit(0);
		}
		m_sampling_size_ratio = sample_size_ratio;
	}
	
	public void set_learning_rate(double learning_rate) {
		if (learning_rate <= 0) {
			System.out.println("Invalid learning rate");
			System.exit(0);
		}
		m_learning_rate = learning_rate;
	}
	
	public void set_tree_number(int tree_number) {
		// the error in tree number will be processed
		// by the RegressionTree class
		m_tree_number = tree_number;
	}
	
	public void set_tree_depth(int depth) {
		// the error in tree number will be processed
		// by the RegressionTree class
		m_tree_depth = depth;
	}
	
	public void set_tree_min_nodes(int min_nodes) {
		m_tree_min_nodes = min_nodes;
	}
	
	// get parameters
	public double get_sample_size_ratio() {
		return m_sampling_size_ratio;
	}
	
	public double get_learning_rate() {
		return m_learning_rate;
	}
	
	public int get_tree_number() {
		return m_tree_number;
	}
	
	public class ResultFunction {
		// class members
		public double m_init_value;
		public Vector<RegressionTree> m_trees;
		public double m_combine_weight;
		
		// construction function
		ResultFunction(double learning_rate) {
			m_init_value = 0.0;
			m_combine_weight = learning_rate;
			m_trees = new Vector<RegressionTree> ();
		}
		
		
		// the following function is used to estimate the function
		public double get_value(Vector<Double> feature_x) {
			double re_res = m_init_value;
			
			if (m_trees.size() == 0) {
				return re_res;
			}
			
			Iterator<RegressionTree> iter = m_trees.iterator();
			while (iter.hasNext()) {
				RegressionTree tree = iter.next();
				
				re_res += m_combine_weight * tree.get_single_value(feature_x);
			}
			
			return re_res;
		}
	}
	
	/*
	 *  fit a regression function using the Gradient Boosting Tree method.
	 *  On success, return function; otherwise, return null. 
	 */
	public ResultFunction gradient_boosting_tree(Vector<Vector<Double>> input_x, 
			Vector<Double> input_y)
	{
		// initialize the final result
		ResultFunction res_fun = new ResultFunction(m_learning_rate);
		
		// get the feature dimension
		int feature_num = input_y.size();
		
		if (feature_num != input_x.size() || feature_num == 0 ) {
			System.out.println("Error: The input_x size should not be zero " + "" +
					"and shoule match the size of input_y");
			return null;
		}
		
		// get an initial guess of the function
		double mean_y = 0.0;
		Iterator<Double> iter = input_y.iterator();
		while (iter.hasNext()) {
			mean_y += iter.next();
		}
		mean_y = mean_y / feature_num;
		res_fun.m_init_value = mean_y;
		
		
		// prepare the iteration
		double[] h_value = new double[feature_num];
		// initialize h_value
		int index = 0;
		while (index < feature_num) {
			h_value[index] = mean_y;
			index += 1;
		}
		
		// begin the boosting process
		int iter_index = 0;
		while (iter_index < m_tree_number) {
			
			// calculate the gradient
			Vector<Double> gradient = new Vector<Double>();
			iter = input_y.iterator();
			index = 0;
			while (iter.hasNext()) {
				gradient.add(iter.next() - h_value[index]);
				
				// next
				index ++;
			}
			
			// begin to sample
			if (m_sampling_size_ratio < 0.99) {
				// sample without replacement
				
				// we need to sample
				RandomSample sampler = new RandomSample(feature_num, 
						(int)(m_sampling_size_ratio*feature_num));
				
				// get random index
				Vector<Integer> sampled_index = sampler.get_sample_index();
				
				// data for growing trees
				Vector<Vector<Double>> train_x = new Vector<Vector<Double>> (); 
				Vector<Double> train_y = new Vector<Double> ();
				Iterator<Integer> sample_iter = sampled_index.iterator();
				
				while (sample_iter.hasNext()) {
					int sel_index = sample_iter.next();
					
					// assign value
					train_y.add(gradient.get(sel_index));
					train_x.add(input_x.get(sel_index));
				}
				
				// fit a regression tree
				RegressionTree tree = new RegressionTree();
				
				if (m_tree_depth > 0) {
					tree.set_depth(m_tree_depth);
				}
				
				if (m_tree_min_nodes > 0) {
					tree.set_min_nodes(m_tree_min_nodes);
				}
				
				tree.build_regression_tree(train_x, train_y);
				
				// store tree information
				if (tree.m_root == null) {
					// clear buffer
					train_x.clear();
					train_y.clear();
					continue;
				}
				
				res_fun.m_trees.add(tree);
				
				// update h_value information, prepare for the next iteration
				int sel_index = 0;
				while (sel_index < feature_num) {
					
					h_value[sel_index] += m_learning_rate * 
							tree.get_single_value(input_x.get(sel_index));
					
					sel_index += 1;
				}
				
			} else {
				// use all data
				// fit a regression tree
				RegressionTree tree = new RegressionTree();
				
				// set parameters if needed
				if (m_tree_depth > 0) {
					tree.set_depth(m_tree_depth);
				}
				
				if (m_tree_min_nodes > 0) {
					tree.set_min_nodes(m_tree_min_nodes);
				}
				
				tree.build_regression_tree(input_x, gradient);
				
				if (tree.m_root == null) {
					// cannot update any more
					break;
				}
				// store tree information
				res_fun.m_trees.add(tree);
				
				// update h_value information, prepare for the next iteration
				for (int loop_index = 0; loop_index < feature_num; loop_index ++ ) {
					h_value[loop_index] += m_learning_rate * 
							tree.get_single_value(input_x.get(loop_index));
				}
			}
			
			// next iteration
			iter_index += 1;
		}
		
		// set the learning rate and return
		// res_fun.m_combine_weight = m_learning_rate;
		
		return res_fun;
	}
	
	/*
	 *  The function is used to learn a rank function from data
	 *  Input: x is preferred over y.
	 */
	public ResultFunction gradient_boosting_ranker(Vector<Vector<Double>> input_x, 
			Vector<Vector<Double>> input_y, double tau) {
		ResultFunction res_fun = new ResultFunction(m_learning_rate);
		
		int feature_num = input_x.size();
		
		if (feature_num != input_y.size() || feature_num == 0) {
			System.out.println("The size of input_x should be the same as the size " +
		"of input_y");
			
			// exit
			System.exit(0);
		}
		
		double [] h_value_x = new double[feature_num];
		double [] h_value_y = new double[feature_num];
		
		int iter_index = 0;
		while (iter_index < m_tree_number) {
			
			// in the boosting ranker, randomly select half samples without replacement in each
			// iteration
			RandomSample sampler = new RandomSample(feature_num, 
					(int)(0.5*feature_num));
			
			// get random index
			Vector<Integer> sampled_index = sampler.get_sample_index();
			
			Vector<Vector<Double>> gradient_x = new Vector<Vector<Double>>();
			Vector<Double> gradient_y = new Vector<Double>();
			
			Iterator<Integer> sample_iter = sampled_index.iterator();
			while(sample_iter.hasNext()) {
				int sel_index = sample_iter.next();
				
				gradient_x.add(input_x.get(sel_index));
				gradient_x.add(input_y.get(sel_index));
				
				// get sample data
				if (h_value_x[sel_index] < h_value_y[sel_index] + tau) {
					double neg_gradient = h_value_y[sel_index] + tau - h_value_x[sel_index];
					gradient_y.add(neg_gradient);
					gradient_y.add(-1 * neg_gradient);
				} else {
					gradient_y.add(0.0);
					gradient_y.add(0.0);
				}
				
			}
			
			// fit a regression tree
			RegressionTree tree = new RegressionTree();
			//tree.set_type(TerminalType.MAXIMAL);
			
			
			tree.build_regression_tree(gradient_x, gradient_y);
			
			// store tree information
			if (tree.m_root == null) {
				continue;
			}
			
			// update information
			res_fun.m_trees.add(tree);
			
			
			double err = 0.0;
			
			for (int loop_index = 0; loop_index < feature_num; loop_index ++) {
				h_value_x[loop_index] +=
						m_learning_rate * tree.get_single_value(input_x.get(loop_index));
				
				h_value_y[loop_index] +=
						m_learning_rate * tree.get_single_value(input_y.get(loop_index));
				
			    if (h_value_x[loop_index] < h_value_y[loop_index] + tau) {
			    	err += (h_value_x[loop_index] - h_value_y[loop_index] - tau) * 
			    			(h_value_x[loop_index] - h_value_y[loop_index] - tau);
			    }
			}
			
			System.out.println(String.format("%d-th iteration with error %f", iter_index + 1, err));
			//System.out.println(err);
			// next iteration
			iter_index += 1;
		}
		
		return res_fun;
	}
}