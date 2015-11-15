package utilities;

import java.io.File;
import java.io.IOException;

public class CommandLineExecutor {
    public static void runProgramAndWaitForItToComplete(String directory, String... command) throws InterruptedException, IOException {
    	 ProcessBuilder pb =new ProcessBuilder(command);
    	 pb = pb.directory(new File(directory));
    	 Process p = pb.start();
         p.waitFor();
         


         Thread closeChildThread = new Thread() {
             public void run() {
            	 p .destroy();
             }
         };

         Runtime.getRuntime().addShutdownHook(closeChildThread); 
         
         /*
         // Read command standard output
         BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
         BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

         String s;
         System.out.println("Standard output: ");
         while ((s = stdInput.readLine()) != null) {
             System.out.println(s);
         }

         // Read command errors
         System.out.println("Standard error: ");
         while ((s = stdError.readLine()) != null) {
             System.out.println(s);
         }
         */
         
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
            System.out.println("Standard output: ");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // Read command errors
            System.out.println("Standard error: ");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }*/
}
