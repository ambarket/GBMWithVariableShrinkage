import gbm.Dataset;
import gbm.GradientBoostingTree;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import utilities.Logger;
import utilities.StopWatch;


public class Main {
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	
	public static void main(String[] args) {
		recreateRExperiment();
	}
	
	public static final int NUMBER_OF_TREES = 10000;
	public static final double LEARNING_RATE = 0.001;
	public static final double BAG_FRACTION = 1;
	public static final int MAX_NUMBER_OF_SPLITS = 3;
	public static final int MIN_EXAMPLES_IN_NODE = 10;
	public static void recreateRExperiment() {
		StopWatch timer = (new StopWatch());
		
		Dataset trainingDataset = new Dataset(powerPlantFiles + "TRAINING.txt", true, true, 4);
		Dataset testDataset = new Dataset(powerPlantFiles + "TEST.txt", true, true, 4);
		
		timer.start();
		GradientBoostingTree boostedTree = new GradientBoostingTree(trainingDataset, BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS);
		GradientBoostingTree.ResultFunction function = boostedTree.buildGradientBoostingMachine();
		Logger.println("Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();
		double[] relativeInf = function.calcRelativeInfluences();
		Logger.println("Relative Influences\n--------------------");
		for (int i = 0; i < relativeInf.length; i++) {
			Logger.println(i + ": " + relativeInf[i] + "%");
		}
		Logger.println("Calc Rel Inf in " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();
		
		double trainingRmse = trainingDataset.calculateRootMeanSquaredError(function);
		double validationRmse = testDataset.calculateRootMeanSquaredError(function);
		Logger.println("Training RMSE: " + trainingRmse + "\nValidation RMSE: " + validationRmse + "\nTime in Seconds: " + timer.getElapsedSeconds());
		

		String userInput = "";
		Scanner sc = new Scanner(System.in);
		while(!userInput.equalsIgnoreCase("n")) {
			System.out.println("Would you like to print an individual tree? Enter a number between 0 and " + (NUMBER_OF_TREES-1) + " or type 'N'");
			userInput = sc.nextLine();
			int value = 0;
			try {
				value = Integer.parseInt(userInput);
			} catch(Exception e) {
				System.out.println("Try again :)");
				continue;
			}
			
			if (value > (NUMBER_OF_TREES-1)) {
				continue;
			}
			
			try {
				function.trees.get(value).root.printTree(new OutputStreamWriter(System.out));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		sc.close();
	}
}
