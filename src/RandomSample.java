/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.Vector;
import java.util.Random;

public class RandomSample {
	// class members
	// generate "m_number" of data with the value within the range [0, m_max].
	private int m_max;
	private int m_number;
	
	RandomSample(int max, int number) {
		m_max = max;
		m_number = number;
	}
	
	public Vector<Integer> get_sample_index() {
		Vector<Integer> re_res = new Vector<Integer>();
		
		// random number generator
		Random rgen = new Random();
		
		// number of data in the vector
		boolean[] bool_tag = new boolean[m_max];
		
		
		int num = 0;
		for (int loop_index = 0; loop_index < m_number; loop_index ++) {
			do {
				 num = rgen.nextInt(m_max);
			} while(bool_tag[num]);
			
			bool_tag[num] = true;
			re_res.add(num);
		}
		
		return re_res;
	}
}
