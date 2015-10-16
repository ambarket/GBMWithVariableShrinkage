/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/


import java.util.ArrayList;
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
	
	public ArrayList<Integer> get_sample_index() {
		ArrayList<Integer> re_res = new ArrayList<Integer>();
		
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
	
	public static MersenneTwisterFast rand = new MersenneTwisterFast();
	
	/**
	 * Source: https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
	 * @return A random permutation of the numbers 0 through currNumOfExamples-1. 
	 * So reading the first Main.currNumOfValidationExamples; of these numbers should be the same as random
	 * uniform sampling and is super efficient.
	 */
	public static int[] fisherYatesShuffle(int numberOfExamples) {
		int[] retval = new int[numberOfExamples];
		for (int i = 0; i < numberOfExamples; i++) {
			retval[i] = i;
		}

		for (int i = numberOfExamples-1, j = 0, tmp = 0; i > 0; i--) {
			j = rand.nextInt(i);
			tmp = retval[i];
			retval[i] = retval[j];
			retval[j] = tmp;
		}
		return retval;
	}
}
