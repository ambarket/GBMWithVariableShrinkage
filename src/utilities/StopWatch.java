package utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StopWatch {

    double startTime, endTime;
	
	public StopWatch start() {
		startTime = System.nanoTime();
		return this;
	}
	
	public double getElapsedHours() {
		endTime = System.nanoTime();
		return (endTime - startTime) / 60000000000.0 / 24;
	}
	
	public double getElapsedMinutes() {
		endTime = System.nanoTime();
		return (endTime - startTime) / 60000000000.0;
	}
	
	public double getElapsedSeconds() {
		endTime = System.nanoTime();
		return (endTime - startTime) / 1000000000;
	}
	
	public double getElapsedMilliSeconds() {
		endTime = System.nanoTime();
		return (endTime - startTime) / 1000000;
	}
	
	public double getElapsedMicroSeconds() {
		endTime = System.nanoTime();
		return (endTime - startTime) / 1000;
	}
	
	public double getElapsedNanoSeconds() {
		endTime = System.nanoTime();
		return (endTime - startTime);
	}
	
	public String generateMessageWithTime(String message) {
		Unit bestUnit = determineMostAppropriateUnit();
		return String.format(message + ": %.4f %s", getTimeInUnit(bestUnit), bestUnit.name());
	}
	
	public void printMessageWithTime(String message) {
		System.out.println(StopWatch.getDateTimeStamp() + generateMessageWithTime(message));
	}
	
	public String getTimeInMostAppropriateUnit() {
		Unit u = determineMostAppropriateUnit();
		return getTimeInUnit(u) + " " + u.name();
	}
	
	public Unit determineMostAppropriateUnit() {
		endTime = System.nanoTime();
		int tmp = (int)Math.ceil(Math.log10(endTime - startTime));
		if (tmp <= 3) {
			return Unit.nanoseconds;
		}
		if (tmp <= 6) {
			return Unit.milliseconds;
		}
		if (tmp <= 11) {
			return Unit.seconds;
		}
		if (tmp <= 14) {
			return Unit.minutes;
		}
		return Unit.hours;
	}
	
	public double getTimeInUnit(Unit unit) {
		switch (unit) {
			case nanoseconds:
				return getElapsedNanoSeconds();
			case microseconds:
				return getElapsedMicroSeconds();
			case milliseconds:
				return getElapsedMilliSeconds();
			case seconds:
				return getElapsedSeconds();
			case minutes:
				return getElapsedMinutes();
			case hours:
				return getElapsedHours();
		}
		throw new IllegalArgumentException();
	}
	
	private static DateFormat df = new SimpleDateFormat("MM/dd HH:mm:ss");
	private static Calendar start = Calendar.getInstance();
	public  static String getDateTimeStamp() {
	    Calendar now = Calendar.getInstance();
		return "[" + df.format(now.getTime()) + ", " + df.format(start.getTime()) + "]\n\t";
	}
	
	private enum Unit {nanoseconds, microseconds, milliseconds, seconds, minutes, hours}
}
