package utilities;

public class MaxAndMin {
	public double max = Double.MIN_VALUE;
	public double min = Double.MAX_VALUE;
	
	public void updateMaxAndMinIfNecessary(double newValue) {
		if (newValue < min) {
			min = newValue;
		}
		if (newValue > max) {
			max = newValue;
		}
	}
}
