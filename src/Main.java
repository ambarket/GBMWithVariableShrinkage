import java.util.ArrayList;


public class Main {
	public static final String powerPlantFiles = System.getProperty("user.dir") + "/data/PowerPlant/";
	
	public static final String powerPlantX5 = "sheet5OnlyPredictive.txt";
	public static final String powerPlantY5 = "sheet5Labels.txt";
	
	public static void main(String[] args) {
		StopWatch timer = (new StopWatch()).start();
		ArrayList<ArrayList<Double>> powerPlantX = DataReader.readX(powerPlantFiles, powerPlantX5);
		ArrayList<Double> powerPlantY = DataReader.readY(powerPlantFiles, powerPlantY5);
		System.out.println("Finished reading Dataset. Time in Seconds: " + timer.getElapsedSeconds());
		
		timer.start();
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
		
		GradientBoostingTree.ResultFunction function = boostedTree.gradient_boosting_tree(powerPlantXTraining, powerPlantYTraining);
		System.out.println("Finished boosting trees. Time in Seconds: " + timer.getElapsedSeconds());
		
		timer.start();
		double validationRmse = 0.0;
		for (int i = 0; i < powerPlantXValidation.size(); i++) {
			double tmp = (function.get_value(powerPlantXValidation.get(i)) - powerPlantYValidation.get(i));
			validationRmse += tmp * tmp;
		}
		validationRmse /= powerPlantXValidation.size();
		validationRmse = Math.sqrt(validationRmse);
		
		double trainingRmse = 0.0;
		for (int i = 0; i < powerPlantXTraining.size(); i++) {
			double tmp = (function.get_value(powerPlantXTraining.get(i)) - powerPlantYTraining.get(i));
			trainingRmse += tmp * tmp;
		}
		trainingRmse /= powerPlantXTraining.size();
		trainingRmse = Math.sqrt(trainingRmse);
		
		System.out.println("Training RMSE: " + trainingRmse + "\nValidation RMSE: " + validationRmse + "\nTime in Seconds: " + timer.getElapsedSeconds());
	}
}
