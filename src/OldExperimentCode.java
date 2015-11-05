
import gbm.GbmDataset;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.ResultFunction;
import utilities.Logger;
import utilities.StopWatch;
import dataset.Dataset;


public class OldExperimentCode {
	/*
	public static void experiment2() {
		StopWatch timer = (new StopWatch());
		
		//Dataset trainingDataset = new Dataset( System.getProperty("user.dir") + "/data/ServoMotor/" + "TRAINING.txt", true, true, 4);
		//Dataset testDataset = new Dataset(System.getProperty("user.dir") + "/data/ServoMotor/" + "TEST.txt", true, true, 4);
		
		Dataset trainingDataset = new Dataset( System.getProperty("user.dir") + "/data/gen2/" + "TRAINING.txt", true, true, 9, 1);
		Dataset validationDataset = new Dataset(System.getProperty("user.dir") + "/data/gen2/" + "TEST.txt", true, true, 9, 1);
		
		timer.start();
		GbmParameters parameters = new GbmParameters(BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS, LEARNING_RATE_POLICY);
		ResultFunction function = GradientBoostingTree.buildGradientBoostingMachine(parameters, trainingDataset, validationDataset);
		Logger.println("Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();
		double[] relativeInf = function.calcRelativeInfluences();
		Logger.println("Relative Influences\n--------------------");
		for (int i = 0; i < relativeInf.length; i++) {
			Logger.println(i + ": " + relativeInf[i] + "%");
		}
		Logger.println("Calc Rel Inf in " + timer.getElapsedSeconds() + " seconds");
		
		
		timer.start();

		Logger.println("Training RMSE: " + function.trainingError.get(NUMBER_OF_TREES-1) + "\nValidation RMSE: " + function.validationError.get(NUMBER_OF_TREES-1) + "\nTime in Seconds: " + timer.getElapsedSeconds());
		

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
	
	*/
	/*
	public static void recreateRExperiment() {
		StopWatch timer = (new StopWatch());
		
		Dataset trainingDataset = new Dataset(powerPlantFiles + "TRAINING.txt", true, true, 4, 1);
		Dataset validationDataset = new Dataset(powerPlantFiles + "TEST.txt", true, true, 4, 1);
		
		timer.start();
		GbmParameters parameters = new GbmParameters(BAG_FRACTION, LEARNING_RATE, NUMBER_OF_TREES, MIN_EXAMPLES_IN_NODE, MAX_NUMBER_OF_SPLITS, LEARNING_RATE_POLICY.CONSTANT, SPLITS_POLICY.CONSTANT);
		ResultFunction function = GradientBoostingTree.buildGradientBoostingMachine(parameters, trainingDataset);
		Logger.println("Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		System.out.println(function.getRelativeInfluencesString());
		
		
		timer.start();
		GbmDataset v = new GbmDataset(validationDataset);
		
		Logger.println("Training RMSE: " + function.trainingError.get(NUMBER_OF_TREES-1) + "\nValidation RMSE: " + v.calcTrainingRMSE(function) + "\nTime in Seconds: " + timer.getElapsedSeconds());
	
		allowUserToPrintTrees(function);
	}
	*/
}
