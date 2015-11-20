package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandLineExecutor {
    public static String runProgramAndWaitForItToComplete(String directory, String... command) throws InterruptedException, IOException {
    	 ProcessBuilder pb =new ProcessBuilder(command);
    	 pb = pb.directory(new File(directory));
    	 System.out.println(StopWatch.getDateTimeStamp() + "\tStarting: \n\t\tDirectory:  " + pb.directory() + " \n\t\tCommand: " + pb.command());
    	 Process p = pb.start();
    	 p.toString();
        
         Thread closeChildThread = new Thread() {
             public void run() {
				p.destroyForcibly();
             }
         };

         Runtime.getRuntime().addShutdownHook(closeChildThread); 
         
         // Read command standard output
         BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
         BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

         StringBuilder stdOutAndError = new StringBuilder();
         String s;
         while ((s = stdInput.readLine()) != null) {
        	 stdOutAndError.append("\t\t" + StopWatch.getDateTimeStamp() + "StdOut: " + s);
         }

         // Read command errors
         while ((s = stdError.readLine()) != null) {
        	 stdOutAndError.append("\t\t" + StopWatch.getDateTimeStamp() + "StdError: " + s);
         }
         
         p.waitFor();
         System.out.println(StopWatch.getDateTimeStamp() + "\tFinished: \n\t\tDirectory:  " + pb.directory() + " \n\t\tCommand: " + pb.command());
         return stdOutAndError.toString();
    }
    
    
    /*
    public static void runProgramAndWaitForItToComplete(String command) {

        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            // Get input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Read command standard output
            String s;
            System.out.println(StopWatch.getDateTimeStamp() + "Standard output: ");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(StopWatch.getDateTimeStamp() + s);
            }

            // Read command errors
            System.out.println(StopWatch.getDateTimeStamp() + "Standard error: ");
            while ((s = stdError.readLine()) != null) {
                System.out.println(StopWatch.getDateTimeStamp() + s);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }*/
}
