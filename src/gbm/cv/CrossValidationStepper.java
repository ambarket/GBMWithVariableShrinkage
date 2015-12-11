package gbm.cv;
import java.util.concurrent.Callable;

import gbm.GbmParameters;
import gbm.MinimalistGbmDataset;
import gbm.ResultFunction;
import regressionTree.RegressionTree;
import utilities.Logger;
import utilities.RandomSample;
import utilities.StopWatch;

public class CrossValidationStepper implements Callable<Void> {
	public GbmParameters parameters;
	public ResultFunction function;
	public MinimalistGbmDataset dataset;
	public boolean[] inSample;
	public int stepSize;
	public int numOfExamplesInSample;
	
	private boolean[] inCurrentSample;
	private int[] shuffledIndices;
	private int numberOfTrainingExamples;
	private RandomSample randomSample;
	private int sampleSize;
	private StopWatch timer;

	public CrossValidationStepper(GbmParameters parameters, ResultFunction function, MinimalistGbmDataset dataset, boolean[] inSample, int numOfExamplesInSample, int stepSize) {
		this.parameters = parameters;
		this.function = function;
		this.dataset = dataset;
		this.inSample = inSample;
		this.numOfExamplesInSample = numOfExamplesInSample;
		this.stepSize = stepSize;
		
		// Avoid allocation of memory and excess method calls.
		this.numberOfTrainingExamples = dataset.getNumberOfTrainingExamples();
		this.inCurrentSample =  new boolean[numberOfTrainingExamples];
		this.randomSample = new RandomSample();
		this.sampleSize = (int)(parameters.bagFraction * numOfExamplesInSample);
		this.shuffledIndices = new int[numberOfTrainingExamples];
		this.timer = new StopWatch();
	}
	
	@Override
	public Void call() throws Exception {
		timer.start();
		
		for (int iteration = 0; iteration < stepSize; iteration++) {
			for (int j = 0; j < numberOfTrainingExamples; j++) {
				inCurrentSample[j] = false;
			}
			
			// Update the current pseudo responses (gradients) of all the training instances.
			dataset.updatePseudoResponses(iteration);
			
			// Sample bagFraction * numberOfTrainingExamples to use to grow the next tree.
			randomSample.fisherYatesShuffleInPlace(this.shuffledIndices);
			
			
			int selected = 0, i = 0;
			
			while(selected < sampleSize) {
				if (this.inSample[shuffledIndices[i]]) {
					inCurrentSample[shuffledIndices[i]] = true;
					selected++;
				}
				i++;
			}
			// Fit a regression tree to predict the current pseudo responses on the training data.
			RegressionTree tree = (new RegressionTree(parameters, sampleSize)).build(dataset, inCurrentSample, function.trees.size()+1);
			
			// Update our predictions and all error calculations for each training and validation instance using the new tree.
			dataset.updateAllMetadataBasedOnNewTree(tree, inSample, iteration);
			
			// Add the tree to the result function in case we actually want to be able to see relative influences/printouts/actually have a usable model beyond just the experiment.
			// Don't add it to save a ton of memory.
			//function.addTree(tree);
		}
		Logger.println(Logger.LEVELS.DEBUG, "\tFinished " + stepSize + " in : " + timer.getElapsedSeconds());
		return null;
	}
}