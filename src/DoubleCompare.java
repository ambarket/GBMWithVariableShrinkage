
public class DoubleCompare {
	private final static double EPSILON = 0.00001;


	/**
	 * Returns true if two doubles are considered equal.  Tests if the absolute
	 * difference between two doubles has a difference less then .00001.   This
	 * should be fine when comparing prices, because prices have a precision of
	 * .001.
	 *
	 * @param a double to compare.
	 * @param b double to compare.
	 * @return true true if two doubles are considered equal.
	 */
	public static boolean equals(double a, double b){
	    return a == b ? true : Math.abs(a - b) < EPSILON;
	}


	/**
	 * Returns true if two doubles are considered equal. Tests if the absolute
	 * difference between the two doubles has a difference less then a given
	 * double (epsilon). Determining the given epsilon is highly dependant on the
	 * precision of the doubles that are being compared.
	 *
	 * @param a double to compare.
	 * @param b double to compare
	 * @param epsilon double which is compared to the absolute difference of two
	 * doubles to determine if they are equal.
	 * @return true if a is considered equal to b.
	 */
	public static boolean equals(double a, double b, double epsilon){
	    return a == b ? true : Math.abs(a - b) < epsilon;
	}


	/**
	 * Returns true if the first double is considered greater than the second
	 * double.  Test if the difference of first minus second is greater then
	 * .00001.  This should be fine when comparing prices, because prices have a
	 * precision of .001.
	 *
	 * @param a first double
	 * @param b second double
	 * @return true if the first double is considered greater than the second
	 *              double
	 */
	public static boolean greaterThan(double a, double b){
	    return greaterThan(a, b, EPSILON);
	}


	/**
	 * Returns true if the first double is considered greater than the second
	 * double.  Test if the difference of first minus second is greater then
	 * a given double (epsilon).  Determining the given epsilon is highly
	 * dependant on the precision of the doubles that are being compared.
	 *
	 * @param a first double
	 * @param b second double
	 * @return true if the first double is considered greater than the second
	 *              double
	 */
	public static boolean greaterThan(double a, double b, double epsilon){
	    return a - b > epsilon;
	}


	/**
	 * Returns true if the first double is considered less than the second
	 * double.  Test if the difference of second minus first is greater then
	 * .00001.  This should be fine when comparing prices, because prices have a
	 * precision of .001.
	 *
	 * @param a first double
	 * @param b second double
	 * @return true if the first double is considered less than the second
	 *              double
	 */
	public static boolean lessThan(double a, double b){
	    return lessThan(a, b, EPSILON);
	}


	/**
	 * Returns true if the first double is considered less than the second
	 * double.  Test if the difference of second minus first is greater then
	 * a given double (epsilon).  Determining the given epsilon is highly
	 * dependant on the precision of the doubles that are being compared.
	 *
	 * @param a first double
	 * @param b second double
	 * @return true if the first double is considered less than the second
	 *              double
	 */
	public static boolean lessThan(double a, double b, double epsilon){
	    return b - a > epsilon;
	}
	
}
