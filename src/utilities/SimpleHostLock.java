package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

import parameterTuning.ParameterTuningParameters;

public class SimpleHostLock {
	public static boolean checkAndClaimHostLock(String hostLockFilePath) {
		File hostLock;
		if ((hostLock = new File(hostLockFilePath)).exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(hostLock));
				String hostName = br.readLine();
				br.close();
				
				return (hostName != null) && (hostName).equals(InetAddress.getLocalHost().getHostName());
			} catch (FileNotFoundException e) {
				System.out.println(StopWatch.getDateTimeStamp() + "Host lock file exists but file not found. Makes no sense.");
			} catch (IOException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new PrintWriter(hostLock));
				bw.write(InetAddress.getLocalHost().getHostName() + "\n");
				bw.flush();
				bw.close();
				return true;
			} catch (IOException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
				System.exit(1);
			} 
		}
		System.err.println(StopWatch.getDateTimeStamp() + "ERROR Shouldnt reach here in checkAndClaimHostLock");
		return false;
	}
	
	public static boolean checkDoneLock(String doneLockFilePath) {
		return new File(doneLockFilePath).exists();
	}
	
	public static boolean writeDoneLock(String doneLockFilePath) {
		File doneLock;
		doneLock = new File(doneLockFilePath);
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(doneLock));
			bw.write(InetAddress.getLocalHost().getHostName() + "\n");
			bw.flush();
			bw.close();
			return true;
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		} 
		System.err.println(StopWatch.getDateTimeStamp() + "ERROR Shouldnt reach here in writeDoneLock");
		return false;
	}
	
	public static boolean isTimeToShutdown(ParameterTuningParameters parameters) {
		if (!new File(parameters.hostsThatShouldShutdownFile).exists()) {
			System.err.println("WARNING: parameters.hostsThatShouldShutdownFile doesn't exist");
			return false;
		}
		// Check if we are being told to shutdown
		try {
			BufferedReader br = new BufferedReader(new FileReader(parameters.hostsThatShouldShutdownFile));
			String hostName = InetAddress.getLocalHost().getHostName(), line = null;;
			while ((line = br.readLine()) != null) {
				if (line.contains(hostName)) {
					br.close();
					return true;
				}
			}
			br.close();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
}
