import gbm.Dataset;
import gbm.GradientBoostingTree;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import utilities.DataReader;
import utilities.Logger;
import utilities.StopWatch;


public class Main {
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	
	public static final String powerPlantX5 = "sheet5OnlyPredictive.txt";
	public static final String powerPlantY5 = "sheet5Labels.txt";
	
	public static void main(String[] args) {
		recreateRExperiment();
	}
	
	/*
	public static void powerPlant() {
		
		StopWatch timer = (new StopWatch()).start();
		ArrayList<ArrayList<Double>> powerPlantX = DataReader.readX(powerPlantFiles, powerPlantX5, false);
		ArrayList<Double> powerPlantY = DataReader.readY(powerPlantFiles, powerPlantY5);
		
		GradientBoostingTree boostedTree = new GradientBoostingTree(0.75, 0.001, 5000, 10, 7);
		
		int[] shuffledIndices = RandomSample.fisherYatesShuffle(powerPlantX.size());
		
		ArrayList<ArrayList<Double>> powerPlantXTraining = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> powerPlantXValidation = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> powerPlantYTraining = new ArrayList<Double>();
		ArrayList<Double> powerPlantYValidation = new ArrayList<Double>();
		
		for ( int i = 0; i < shuffledIndices.length; i++) {
			if ( i < .3 * shuffledIndices.length) {
				powerPlantXValidation.add(powerPlantX.get(shuffledIndices[i]));
				powerPlantYValidation.add(powerPlantY.get(shuffledIndices[i]));
			} else {
				powerPlantXTraining.add(powerPlantX.get(shuffledIndices[i]));
				powerPlantYTraining.add(powerPlantY.get(shuffledIndices[i]));
			}
				
		}
		Logger.println(Logger.LEVELS.DEBUG, "Finished reading Dataset. Time in Seconds: " + timer.getElapsedSeconds());
		
		timer.start();
		GradientBoostingTree.ResultFunction function = boostedTree.buildGradientBoostingMachine(powerPlantXTraining, powerPlantYTraining);
		Logger.println(Logger.LEVELS.DEBUG, "Finished boosting trees. Time in Seconds: " + timer.getElapsedSeconds());
		
		timer.start();
		double validationRmse = 0.0;
		for (int i = 0; i < powerPlantXValidation.size(); i++) {
			double tmp = (function.predictLabel(powerPlantXValidation.get(i)) - powerPlantYValidation.get(i));
			validationRmse += tmp * tmp;
		}
		validationRmse /= powerPlantXValidation.size();
		validationRmse = Math.sqrt(validationRmse);
		
		double trainingRmse = 0.0;
		for (int i = 0; i < powerPlantXTraining.size(); i++) {
			double tmp = (function.predictLabel(powerPlantXTraining.get(i)) - powerPlantYTraining.get(i));
			trainingRmse += tmp * tmp;
		}
		trainingRmse /= powerPlantXTraining.size();
		trainingRmse = Math.sqrt(trainingRmse);
		
		Logger.println(Logger.LEVELS.DEBUG, "Training RMSE: " + trainingRmse + "\nValidation RMSE: " + validationRmse + "\nTime in Seconds: " + timer.getElapsedSeconds());
	}
	*/
	
	public static void recreateRExperiment() {
		StopWatch timer = (new StopWatch()).start();
		
		String training = "TRAINING.txt";
		String test = "TEST.txt";
		
		ArrayList<ArrayList<Double>> trainingX = DataReader.readX(powerPlantFiles, training, true);
		ArrayList<ArrayList<Double>> testX= DataReader.readX(powerPlantFiles, test, true);
		
		ArrayList<Double> trainingY = extractResponseColumn(4, trainingX);
		ArrayList<Double> testY = extractResponseColumn(4, testX);
		
		Logger.println(Logger.LEVELS.INFO, "Read Data in " + timer.getElapsedSeconds() + " seconds");
		
		Dataset trainingDataset = new Dataset(trainingX, trainingY);
		timer.start();
		GradientBoostingTree boostedTree = new GradientBoostingTree(trainingDataset, 1, 0.001, 500, 10, 3);
		GradientBoostingTree.ResultFunction function = boostedTree.buildGradientBoostingMachine();
		Logger.println(Logger.LEVELS.INFO, "Trained GBM " + timer.getElapsedSeconds() + " seconds");
		
		timer.start();
		double validationRmse = 0.0;
		for (int i = 0; i < testX.size(); i++) {
			double tmp = (function.predictLabel(testX.get(i)) - testY.get(i));
			validationRmse += tmp * tmp;
		}
		validationRmse /= testX.size();
		validationRmse = Math.sqrt(validationRmse);
		
		double trainingRmse = 0.0;
		for (int i = 0; i < trainingX.size(); i++) {
			double tmp = (function.predictLabel(trainingX.get(i)) - trainingY.get(i));
			trainingRmse += tmp * tmp;
		}
		trainingRmse /= trainingX.size();
		trainingRmse = Math.sqrt(trainingRmse);
		
		
		Logger.println(Logger.LEVELS.INFO, "Training RMSE: " + trainingRmse + "\nValidation RMSE: " + validationRmse + "\nTime in Seconds: " + timer.getElapsedSeconds());
		
		timer.start();
		double[] relativeInf = function.calcRelativeInfluences();
		Logger.println(Logger.LEVELS.INFO, "Relative Influences\n--------------------");
		for (int i = 0; i < relativeInf.length; i++) {
			Logger.println(Logger.LEVELS.INFO, i + ": " + relativeInf[i] + "%");
		}
		Logger.println(Logger.LEVELS.INFO, "Calc Rel Inf in " + timer.getElapsedSeconds() + " seconds");
		
		try {
			function.trees.get(0).root.printTree(new OutputStreamWriter(System.out));
			Logger.println();
			function.trees.get(149).root.printTree(new OutputStreamWriter(System.out));
			Logger.println();
			
			//function.trees.get(29999).root.printTree(new OutputStreamWriter(System.out));
			////Logger.println();
			//function.trees.get(49999).root.printTree(new OutputStreamWriter(System.out));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Double> extractResponseColumn(int columnNum, ArrayList<ArrayList<Double>> dataset) {
		ArrayList<Double> retval = new ArrayList<>();
		
		for (ArrayList<Double> record : dataset) {
			retval.add(record.remove(columnNum));
		}
		
		return retval;
	}
}
