package utilities;

import java.util.ArrayList;


public class SumCountAverage {
	private double 	sum = 0.0, sumOfSquares = 0.0, mean = 0.0; 
	private int count = 0;
	private boolean meanOutOfDate = false;
	private double shift = 0;
	boolean explicitShiftProvided = false;
	
	public double getSum() {
		return sum;
	}
	
	public double getSumOfSquares() {
		return sumOfSquares;
	}
	
	public int getCount() {
		return count;
	}
	
	public double getMean() {
		if (meanOutOfDate) {
			mean = shift + sum / count;
		}
		return mean;
	}
	
	public SumCountAverage reset() {
		sum = 0.0;
		sumOfSquares = 0.0;
		mean = 0.0; 
		count = 0;
		shift = 0.0;
		meanOutOfDate = false;
		return this;
	}
	
	public String toString() {
		return "\tSum: " + sum + "\n\t"
				+ "SumOfSqaures: " + sumOfSquares + "\n\t" 
				+ "Count: " + count + "\n\t" 
				+ "Shift: " + shift + "\n\t" 
				;//+ "SqauredError: " + getSquaredError();
	}
	
	public double getSquaredError() {
		if (count == 0) { return 0; } // Will hit for empty missing nodes.
		double error = sumOfSquares - ((sum * sum) / count);
		if (error < 0) {
			//error = 0.0;
			
			if (error + (0.001 * Math.abs(shift)) >= 0.0) {
				error = 0.0;
			} else {
				(new IllegalStateException("SumCountAverageOld sqauredError is negative. \n\t"
						+ "Sum: " + sum + "\n\t"
						+ "SumOfSqaures: " + sumOfSquares + "\n\t" 
						+ "Count: " + count + "\n\t" 
						+ "Shift: " + shift + "\n\t" 
						+ "SqauredError: " + error)).printStackTrace();
				System.exit(1);
			}
			
		} 
		if (count == 1) {
			
		}
		return error;
	}
	
	/**
	 * Same as population variance
	 * @return
	 */
	public double getMeanSquaredError() {
		return getSquaredError() / count;
	}
	
	/**
	 * Same as population standard deviation
	 * @return
	 */
	public double getRootMeanSquaredError() {
		return Math.sqrt(getMeanSquaredError());
	}
	
	public void addData(double data) {
		if (count == 0 && !explicitShiftProvided) {
			shift = data;
		}
		sum += (data - shift);
		sumOfSquares += (data - shift) * (data - shift);
		count++;
		meanOutOfDate = true;
	}
	
	public void subtractData(double data) {
		sum -= (data - shift);
		sumOfSquares -= (data - shift) * (data - shift);
		count--;
		meanOutOfDate = true;
		if (count == 0) {
			if (!DoubleCompare.equals(sum, 0.0) || !DoubleCompare.equals(sumOfSquares, 0.0)) {
				(new IllegalStateException("SumCountAverageOld reached zero elements but sums are not 0, must have subtracted different data than was added.")).printStackTrace();
				System.exit(1);
			}
			
		}
	}
	
	public void addSumCountAverage(SumCountAverage data) {
		if (count == 0 && !explicitShiftProvided) {
			shift = data.shift;
		}
		if (!DoubleCompare.equals(this.shift, data.shift)) {
			(new IllegalStateException("Can only add SumCountAverageOlds with the same shift")).printStackTrace();
			System.exit(1);
		}

		sum += data.sum;
		sumOfSquares += data.sumOfSquares;
		count += data.count;
		meanOutOfDate = true;
	}
	
	public void subtractSumCountAverage(SumCountAverage data) {
		if (!DoubleCompare.equals(this.shift, data.shift)) {
			(new IllegalStateException("Can only subtract SumCountAverageOlds with the same shift")).printStackTrace();
			System.exit(1);
		}
		
		sum -= data.sum;
		sumOfSquares -= data.sumOfSquares;
		count -= data.count;
		meanOutOfDate = true;

		if (count < 0) {
			(new IllegalStateException("Cannot remove more elements than were added in SumCountAverageOld")).printStackTrace();
		}
	}
	
	public SumCountAverage() {
		
	}
	
	public SumCountAverage(double shift) {
		this.explicitShiftProvided = true;
		this.shift = shift;
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
	
	public SumCountAverage(double initialSum, double initialSumOfSquares, int initialCount) {
		this.sum = initialSum;
		this.sumOfSquares = initialSumOfSquares;
		this.count = initialCount;
		this.shift = this.getMean();
	}
}
