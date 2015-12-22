package utilities;

import java.util.ArrayList;

/**
 * Source: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance (Online Algorithm / Parallel Algorithm)
 * @author amb6470
 *
 */
public class SumCountAveragef {
	private double mean = 0.0; 
	private int count = 0;
	private double sqauredError = 0.0;
	
	public int getCount() {
		return count;
	}
	
	public double getMean() {
		return mean;
	}
	
	public SumCountAverage reset() {
		mean = 0.0; 
		count = 0;
		sqauredError = 0.0;
		return this;
	}
	
	public String toString() {
		return "\tMean: " + mean + "\n\t"
				+ "Count: " + count + "\n\t" 
				+ "sqauredError: " + sqauredError + "\n\t";
	}
	
	public double getSquaredError() {
		if (sqauredError < 0) {
			if (DoubleCompare.equals(sqauredError, 0.0, 0.01 * Math.abs(this.mean))) {
				sqauredError = 0;
			} else {
				throw new IllegalStateException("SquaredError is negative \n" + toString()); 
			}
		}
		return sqauredError;
	}
	
	/**
	 * Same as population variance
	 * @return
	 */
	public double getMeanSquaredError() {
		return sqauredError / count;
	}
	
	/**
	 * Same as population standard deviation
	 * @return
	 */
	public double getRootMeanSquaredError() {
		return Math.sqrt(sqauredError / count);
	}
	
	public void addData(double data) {
		count++;
		double delta = data - mean;
		mean += delta / count;
		sqauredError += delta * (data - mean);
	}
	
	public void subtractData(double data) {
		count--;
		double delta = data - mean;
		mean -= delta / count;
		sqauredError -= delta * (data - mean);
		
		if (count == 0) {
			if (!DoubleCompare.equals(mean, 0.0) || !DoubleCompare.equals(sqauredError, 0.0)) {
				(new IllegalStateException("SumCountAverage reached zero elements but sums are not 0, must have subtracted different data than was added.")).printStackTrace();
				System.exit(1);
			}
			
		}
	}
	
	public void addSumCountAverage(SumCountAverage data) {
		if (this.count == 0) {
			this.mean = data.mean;
			this.count = data.count;
			this.sqauredError = data.sqauredError;
		} else {
			double delta = data.mean - this.mean;
			int allCount = this.count + data.count;
			
			this.mean = this.mean + delta * (data.count / this.count);		
			this.sqauredError = this.sqauredError + data.sqauredError + (delta * delta * (this.count * data.count / allCount));
			this.count = allCount;
		}
	}
	
	public void subtractSumCountAverage(SumCountAverage data) {
		int countAll = this.count;
		
		if (this.count == 0 && data.count == 0) {
			reset();
			return;
		} else if (this.count == data.count) {
			if (DoubleCompare.equals(this.mean, data.mean) && DoubleCompare.equals(this.sqauredError, data.sqauredError)) {
				reset();
				return;
			} else {
				System.err.println("wtf");
			}

		}
		double meanAll = this.mean;
		double tmp = data.count / countAll;
		this.count -= data.count;
		this.mean = (meanAll - (data.mean * tmp)) / (1 - tmp);
		double delta = data.mean - this.mean;
		this.sqauredError = this.sqauredError - data.sqauredError - (delta * delta * this.count * tmp);
	}
	
	public SumCountAverage() {
		
	}
	
	public SumCountAverage(ArrayList<Double> data) {
		for (double d : data) {
			addData(d);
		}
	}
	
	public SumCountAverage(ArrayList<Double> data, double divisor) {
		for (double d : data) {
			addData(d / divisor);
		}
	}
}
