/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


/*
 *  The following class implements a regression tree
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

public class RegressionTree {
	public enum TerminalType {
		AVERAGE, MAXIMAL
	}
	
	// class members
	private int m_min_nodes;
	private int m_max_depth;
	private int m_current_depth;
	private TerminalType m_type;
	
	public class Node {
		public double m_node_value;
		public int    m_feature_index;
		public double m_terminal_left;
		public double m_terminal_right;
		
		// Each non-leaf node has a left
		// child and a right child.
		public Node m_left_child;
		public Node m_right_child;
		
		// Construction function
		public Node(double value, int feature_index, double value_left, double value_right) {
			this.m_node_value = value;
			m_feature_index = feature_index;
			m_left_child = null;
			m_right_child = null;
			m_terminal_left = value_left;
			m_terminal_right = value_right;
		}
	}
	
	// root
	public Node m_root;
	
	// construction function
	public RegressionTree() {
		m_min_nodes = 10;
		m_max_depth = 3;
		m_root = null;
		m_current_depth = 0;
		m_type = TerminalType.AVERAGE;
	}
	
	public void set_type(TerminalType type) {
		m_type = type;
	}
	
	// set parameters
	public void set_min_nodes(int min_nodes) {
		if (min_nodes < 3) {
			System.out.println("The number of terminal nodes is too small");
			System.exit(0);
		}
		m_min_nodes = min_nodes;
	}
	
	public void set_depth(int depth) {
		if (depth < 1) {
			System.out.println("The number of depth is too small");
			System.exit(0);
		}
		m_max_depth = depth;
	}
	
	// get fit value
	public double get_single_value(Vector<Double> feature_x) {
		double re_res = 0.0;
		
		if (m_root == null) {
			// failed in building the tree
			return re_res;
		}
		
		Node current = m_root;
		
		while (true) {
			// current node information
			int c_feature_index = current.m_feature_index;
			double c_node_value = current.m_node_value;
			double c_node_left_value = current.m_terminal_left;
			double c_node_right_value = current.m_terminal_right;
			
			if (feature_x.get(c_feature_index) < c_node_value) {
				// we should consider left child
				current = current.m_left_child;
				
				if (current == null) {
					re_res = c_node_left_value;
					break;
				}
			} else {
				// we should consider right child
				current = current.m_right_child;
				
				if (current == null) {
					re_res = c_node_right_value;
					break;
				}
			}
		}
		
		return re_res;
	}
	
	/* 
	 *  Print nodes and corresponding information
	 */
	private void print_each(Node current, String info, String kind) {
		if (current != null) {
			// print current node information
			System.out.println("Current node information: " + 
					String.format("(Level, side)=(%s,%s) with split information (%f, %d)", 
							info, kind, current.m_node_value, current.m_feature_index));
			
			print_each(current.m_left_child, info + "-", "left");
			print_each(current.m_right_child, info + "-", "right");
		}
	}
	
	public void print_nodes() {
		print_each(m_root, "", "");
	}
	
	/*
	 *  The following class is used for split point in the regression tree
	 */
	public class BestSplit {
		public int m_feature_index;
		public double m_node_value;
		public boolean m_status;
		
		// construction function
		BestSplit() {
			m_feature_index = 0;
			m_node_value    = 0.0;
			m_status        = false; // by default, it fails
		}
	}
	
	/*
	 *  The following class is used to define a split in the regression
	 *  tree method
	 */
	public class SplitRes {
		public Vector<Vector<Double>> m_feature_left;
		public Vector<Vector<Double>> m_feature_right;
		public double m_left_value;
		public double m_right_value;
		public Vector<Double>         m_obs_left;
		public Vector<Double>         m_obs_right;
		
		// construction function
		SplitRes() {
			m_feature_left = new Vector<Vector<Double>>();
			m_feature_right = new Vector<Vector<Double>>();
		    m_obs_left     = new Vector<Double>();
		    m_obs_right    = new Vector<Double>();
			m_left_value = 0.0;
			m_right_value = 0.0;
		}
	}
	
	
	// Class used for sorting
	public class ListData {
		Double m_x;
		Double m_y;
		
		ListData(double x, double y) {
			m_x = x;
			m_y = y;
		}
		
		public Double getx() {
			return m_x;
		}
		
		public Double gety() {
			return m_y;
		}
	}
	
	public class ComparatorFeature implements Comparator<Object> {
		public int compare(Object arg0, Object arg1) {
			ListData data0 = (ListData) arg0;
			ListData data1 = (ListData) arg1;
			
			return data0.getx().compareTo(
					data1.getx());
		}
	}
	
	/*
	 *  The following function gets the best split given the data
	 */
	private BestSplit get_optimal_split(Vector<Vector<Double>> feature_x, 
			Vector<Double> obs_y) {
		BestSplit split_point = new BestSplit();
		
		if (m_current_depth > m_max_depth) {
			return split_point;
		}
		
		int feature_num = feature_x.size();
		
		if (m_min_nodes * 2 > feature_num) {
			// the number of observations in terminals
			// is too small
			return split_point;
		}
		int feature_dim = feature_x.get(0).size();
		
		
		double min_err = 0;
		int    split_index = -1;
		double node_value = 0.0;
		
		// begin to get the best split information
		for (int loop_i = 0; loop_i < feature_dim; loop_i ++) {
			// get the optimal split for the loop_index feature
			
			// get data sorted by the loop_i-th feature
			List<ListData> list_feature = new ArrayList<ListData>();
			for (int loop_j = 0; loop_j < feature_num; loop_j ++) {	
				list_feature.add(new ListData(feature_x.get(loop_j).get(loop_i), 
						obs_y.get(loop_j)));
			}
			
			// sort the list
			ComparatorFeature my_comparator = new ComparatorFeature();
			Collections.sort(list_feature, my_comparator);
			
			// begin to split
			double sum_left = 0.0;
			double mean_left = 0.0;
			int    count_left = 0;
			double sum_right = 0.0;
			double mean_right = 0.0;
			int count_right = 0;
			double current_node_value = 0;
			double current_err = 0.0;
			
			// initialize left
			for (int loop_j = 0; loop_j < m_min_nodes; loop_j ++) {
				ListData fetched_data = list_feature.get(loop_j);
				sum_left += fetched_data.gety();
				count_left ++;
			}
			mean_left = sum_left / count_left;
			// initialize right
			for (int loop_j = m_min_nodes; loop_j < feature_num; loop_j ++) {
				ListData fetched_data = list_feature.get(loop_j);
				sum_right += fetched_data.gety();
				count_right ++;
			}
			mean_right = sum_right / count_right;
			
			// calculate the current error
			// err = ||x_l - mean(x_l)||_2^2 + ||x_r - mean(x_r)||_2^2
			// = ||x||_2^2 - left_count * mean(x_l)^2 - right_count * mean(x_r)^2
			// = constant - left_count * mean(x_l)^2 - right_count * mean(x_r)^2
			// Thus, we only need to check "- left_count * mean(x_l)^2 - right_count * mean(x_r)^2"
			current_err = -1 * count_left * mean_left * mean_left - 
					count_right * mean_right * mean_right; 
			
			// current node value
			current_node_value = (list_feature.get(m_min_nodes).getx() + 
					list_feature.get(m_min_nodes - 1).getx()) /2;
			
			if (current_err < min_err && current_node_value != 
					list_feature.get(m_min_nodes - 1).getx()) {
				split_index = loop_i;
				node_value = current_node_value;
				min_err = current_err;
			}
			
			// begin to find the best split point for the feature
			for (int loop_j = m_min_nodes; loop_j <= feature_num - m_min_nodes - 1; loop_j ++) {
				ListData fetched_data = list_feature.get(loop_j);
				double y = fetched_data.gety();
				sum_left += y;
				count_left ++;
				mean_left = sum_left / count_left;
				
				
				sum_right -= y;
				count_right --;
				mean_right = sum_right / count_right;
				
				
				current_err = -1 * count_left * mean_left * mean_left - 
						count_right * mean_right * mean_right; 
				// current node value
				current_node_value = (list_feature.get(loop_j + 1).getx() + 
						fetched_data.getx())/2;
				
				if (current_err < min_err && 
						(current_node_value != fetched_data.getx())) {
					split_index = loop_i;
					node_value = current_node_value;
					min_err = current_err;
				}
				
			}
		}
		// set the optimal split point
		if (split_index == -1) {
			// failed to split data
			return split_point;
		}
		split_point.m_feature_index = split_index;
		split_point.m_node_value    = node_value;
		split_point.m_status = true;
		
		return split_point;
	}
	
	/*
	 *  Split data into the left node and the right node based on the best splitting
	 *  point.
	 */
	private SplitRes split_data(Vector<Vector<Double>> feature_x, 
			Vector<Double> obs_y, BestSplit best_split) {
		SplitRes split_res = new SplitRes();
		
		int feature_index = best_split.m_feature_index;
		double node_value    = best_split.m_node_value;
		
		for (int loop_i = 0; loop_i < obs_y.size(); loop_i ++) {
			Vector<Double> ith_feature = feature_x.get(loop_i);
			if (ith_feature.get(feature_index) < node_value) {
				// append to the left
				// feature
				split_res.m_feature_left.add(ith_feature);
				// observation
				split_res.m_obs_left.add(obs_y.get(loop_i));
			} else {
				// append to the right
				split_res.m_feature_right.add(ith_feature);
				split_res.m_obs_right.add(obs_y.get(loop_i));
			}
		}
		
		/*
		// used in debugging the program
		if (split_res.m_obs_left.size() < m_min_nodes || split_res.m_obs_right.size() < m_min_nodes) {
			get_optimal_split(feature_x, obs_y);
			System.exit(0);
		}
		*/
		
		// update terminal values
		if (m_type == TerminalType.AVERAGE) {
			double mean_value = 0.0;
			Iterator<Double> iter = split_res.m_obs_left.iterator();
			while (iter.hasNext()) {
				mean_value += iter.next();
			}
			mean_value = mean_value / split_res.m_obs_left.size();
			split_res.m_left_value = mean_value;
			
			mean_value = 0.0;
			iter = split_res.m_obs_right.iterator();
			while (iter.hasNext()) {
				mean_value += iter.next();
			}
			mean_value = mean_value / split_res.m_obs_right.size();
			split_res.m_right_value = mean_value;
		} else if(m_type == TerminalType.MAXIMAL) {
			double max_value = 0.0;
			Iterator<Double> iter = split_res.m_obs_left.iterator();
			if (iter.hasNext()) {
				max_value = iter.next();
			}
			
			while (iter.hasNext()) {
				double sel_value = iter.next();
				if (max_value < sel_value) {
					max_value = sel_value;
				}
			}
			
			split_res.m_left_value = max_value;
			
			
			// right value
			max_value = 0.0;
			iter = split_res.m_obs_right.iterator();
			if (iter.hasNext()) {
				max_value = iter.next();
			}
			
			while (iter.hasNext()) {
				double sel_value = iter.next();
				if (max_value < sel_value) {
					max_value = sel_value;
				}
			}
			
			split_res.m_right_value = max_value;
			
		} else {
			System.out.println("Unknown terminal type");
			System.exit(0);
		}
		
		// return the result
		return split_res;
	}
	
	/*
	 *  The following function builds a regression tree from data
	 */
	private Node tree_builder(Vector<Vector<Double>> feature_x, 
			Vector<Double> obs_y) {
		
		// obtain the optimal split point
		m_current_depth = m_current_depth + 1;
		
		BestSplit best_split = get_optimal_split(feature_x, obs_y);
		
		if (best_split.m_status == false) {
			if (m_current_depth > 0)
				m_current_depth = m_current_depth - 1;
			return null;
		}
		// split the data
		SplitRes split_data = split_data(feature_x, obs_y, best_split);
		
		// append current value to tree
		Node new_node = new Node(best_split.m_node_value, best_split.m_feature_index, 
				split_data.m_left_value, split_data.m_right_value);
		
		if (m_root == null) {
			m_root = new_node;
			m_current_depth = 0;
			// append left and right side
			m_root.m_left_child = tree_builder(split_data.m_feature_left, 
					split_data.m_obs_left); // left
			m_root.m_right_child = tree_builder(split_data.m_feature_right, 
					split_data.m_obs_right); // right
		} else {
			// append left and right side
			new_node.m_left_child = tree_builder(split_data.m_feature_left, 
					split_data.m_obs_left); // left
			new_node.m_right_child = tree_builder(split_data.m_feature_right, 
					split_data.m_obs_right); // right
		}
		if (m_current_depth > 0)
			m_current_depth --;
		return new_node;
	}
	
	// build the regression tree
	/*
	 *  The min_node is minimum number of observations in the trees terminal nodes. 
	 */
	public void build_regression_tree(Vector<Vector<Double>> feature_x, 
			Vector<Double> obs_y) {
		int feature_num = feature_x.size();
		
		if (feature_num != obs_y.size() || feature_num == 0) {
			System.out.println("The number of features does not match" + 
		                  " with the nunber of observations or " + 
					"the feature number is 0");
			System.exit(0);
		}
		
		if (m_min_nodes * 2 > feature_num) {
			System.out.println("The number of featrues is too small");
			System.exit(0);
		}
		
		// build the regression tree
		tree_builder(feature_x, obs_y);
	}
}
