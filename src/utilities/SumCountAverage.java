package utilities;

import java.util.ArrayList;


public class SumCountAverage {
	private double 	sum = 0.0, sumOfSquares = 0.0, mean = 0.0; 
	private int count = 0;
	private boolean meanOutOfDate = false;
	
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
			mean = sum / count;
		}
		return mean;
	}
	
	public SumCountAverage reset() {
		sum = 0.0;
		sumOfSquares = 0.0;
		mean = 0.0; 
		count = 0;
		meanOutOfDate = false;
		return this;
	}
	
	public double getSquaredError() {
		if (meanOutOfDate) {
			mean = sum / count;
		}
		return sumOfSquares - (mean * sum);
	}
	
	public double getMeanSquaredError() {
		return getSquaredError() / count;
	}
	
	public double getRootMeanSquaredError() {
		//double retval = Math.sqrt(getMeanSquaredError());
		//System.out.println(retval);
		//if (Double.isNaN(retval)) {
		//	System.out.println();
		//}
		return Math.sqrt(getMeanSquaredError());
	}
	
	public void addData(double data) {
		sum += data;
		sumOfSquares += data * data;
		count++;
		meanOutOfDate = true;
	}
	
	public void subtractData(double data) {
		sum -= data;
		sumOfSquares -= data * data;
		count--;
		meanOutOfDate = true;
	}
	
	public void addSumCountAverage(SumCountAverage data) {
		sum += data.sum;
		sumOfSquares += data.sumOfSquares;
		count += data.count;
		meanOutOfDate = true;
	}
	
	public void subtractSumCountAverage(SumCountAverage data) {
		sum -= data.sum;
		sumOfSquares -= data.sumOfSquares;
		count -= data.count;
		meanOutOfDate = true;
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
	
	public SumCountAverage(double initialSum, double initialSumOfSquares, int initialCount) {
		this.sum = initialSum;
		this.sumOfSquares = initialSumOfSquares;
		this.count = initialCount;
	}
}
