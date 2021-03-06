package utilities;
/*
* The stochastic gradient boosting method.
* yorkey: yangchadam AT gmail.com
* 
*/



public class RandomSample {
	
	public MersenneTwisterFast rand = new MersenneTwisterFast();
	
	/**
	 * Source: https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
	 * @return A random permutation of the numbers 0 through currnumberOfExamples-1. 
	 * So reading the first Main.currNumOfValidationExamples; of these numbers should be the same as random
	 * uniform sampling and is super efficient.
	 */
	public int[] fisherYatesShuffle(int numberOfExamples) {
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
	
	public int[] fisherYatesShuffleInPlace(int[] existingSpace) {
		int length = existingSpace.length;
		
		for (int i = 0; i < length; i++) {
			existingSpace[i] = i;
		}

		for (int i = length-1, j = 0, tmp = 0; i > 0; i--) {
			j = rand.nextInt(i);
			tmp = existingSpace[i];
			existingSpace[i] = existingSpace[j];
			existingSpace[j] = tmp;
		}
		return existingSpace;
	}
}
