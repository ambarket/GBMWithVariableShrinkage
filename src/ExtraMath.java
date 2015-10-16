import java.util.ArrayList;


public class ExtraMath {
	/**
	 * Calculates squared error of entries [minIndex, maxIndex) in sortedExamples
	 * @param sortedExamples
	 * @param minIndex
	 * @param maxIndex
	 * @param mean
	 * @return
	 */
	private double calcSquaredError(ArrayList<RegressionTree.ListData> sortedExamples, int minIndex, int maxIndex, double mean) {
		double error = 0.0;
		double tmp = 0.0;
		for (int i = minIndex; i < maxIndex; i++) {
			tmp = sortedExamples.get(i).getY() - mean;
			error += tmp*tmp;
		}
		return error;
	}
}
