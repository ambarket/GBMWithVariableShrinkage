package gbm.cv;
import gbm.GbmDataset;
import gbm.GbmParameters;
import gbm.ResultFunction;

import java.util.concurrent.Callable;

import regressionTree.RegressionTree;
import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class CrossValidationStepper implements Callable<Void> {
	public GbmParameters parameters;
	public ResultFunction function;
	public GbmDataset dataset;
	public boolean[] inSample;
	public int stepSize;
	public int numOfExamplesInSample;

	public CrossValidationStepper(GbmParameters parameters, ResultFunction function, GbmDataset dataset, boolean[] inSample, int numOfExamplesInSample, int stepSize) {
		this.parameters = parameters;
		this.function = function;
		this.dataset = dataset;
		this.inSample = inSample;
		this.numOfExamplesInSample = numOfExamplesInSample;
		this.stepSize = stepSize;
	}
	
	@Override
	public Void call() throws Exception {
		StopWatch timer = (new StopWatch()).start();
		for (int iteration = 0; iteration < stepSize; iteration++) {
			// Update the current pseudo responses (gradients) of all the training instances.
			dataset.updatePseudoResponses();
			
			// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
			int[] shuffledIndices = (new RandomSample()).fisherYatesShuffle(dataset.getNumberOfTrainingExamples());
			int sampleSize = (int)(parameters.bagFraction * numOfExamplesInSample);
			boolean[] inCurrentSample = new boolean[dataset.getNumberOfTrainingExamples()];
			int selected = 0, i = 0;
			while(selected < sampleSize) {
				if (this.inSample[shuffledIndices[i]]) {
					inCurrentSample[shuffledIndices[i]] = true;
					selected++;
				}
				i++;
			}
			
			// Fit a regression tree to predict the current pseudo responses on the training data.
			RegressionTree tree = (new RegressionTree(parameters, sampleSize)).build(dataset, inCurrentSample);
			
			// Update our predictions for each training and validation instance using the new tree.
			dataset.updatePredictionsWithLearnedValueFromNewTree(tree);

			// Calculate the training and validation error for this iteration.
			double trainingRMSE = dataset.calcTrainingRMSE(inSample);
			double validationRMSE = dataset.calcTrainingRMSE(inSample, true); // SecondFlag indicates to negate the inSample array
			double testRMSE = dataset.calcTestRMSE();
			// Add the tree to the function approximation, keep track of the training and validation errors.
			// Note validationRMSE will be NaN for the one that uses all training data.
			function.addTree(tree, trainingRMSE, validationRMSE, testRMSE);
		}
		Logger.println(Logger.LEVELS.DEBUG, "\tFinished " + stepSize + " in : " + timer.getElapsedSeconds());
		return null;
	}
}