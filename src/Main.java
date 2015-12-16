import dataset.Dataset;
import gbm.GbmParameters;
import gbm.GradientBoostingTree;
import gbm.ResultFunction;
import parameterTuning.LatexResultsGenerator;
import parameterTuning.ParameterTuningParameters;
import parameterTuning.ParameterTuningTest;
import regressionTree.RegressionTree.LearningRatePolicy;
import regressionTree.RegressionTree.SplitsPolicy;


public class Main {
	public static void main(String[] args) {
		switch(args[0]) {
			case "runParamTuning5":
				runParamTuning5();
				break;
			case "runParamTuning6":
				runParamTuning6();
				break;
			case "processParamTuning5":
				processParamTuning5();
				break;
			case "writeResultsSection":
				LatexResultsGenerator.writeEntireResultsSection(ParameterTuningParameters.getRangesForTest5());
				break;
			case "buildSingleModel":
				Dataset ds = new Dataset(ParameterTuningParameters.nasaParameters, 0.8);
				ResultFunction function1 = GradientBoostingTree.buildGradientBoostingMachine(
						new GbmParameters(0.01, 0.4, 2, 0.75, 1, 2, LearningRatePolicy.REVISED_VARIABLE, SplitsPolicy.CONSTANT), 
						ds);
				function1.printFirstTwoTrees("Variable Shrinkage ");
				LatexResultsGenerator.writeBeamerResults(ParameterTuningParameters.getRangesForTest5());
				//ResultFunction function2 = GradientBoostingTree.buildGradientBoostingMachine(
				//		new GbmParameters(0.01, 0.4, 2, 0.75, 1, 2, LearningRatePolicy.CONSTANT, SplitsPolicy.CONSTANT), 
				//		ds);
				//function2.printFirstTwoTrees("Constant Shrinkage ");
				//function.allowUserToPrintTrees();
				break;
		}
	}
	
	public static void runParamTuning5() {
		ParameterTuningTest.runOnAllDatasets(ParameterTuningParameters.getRangesForTest5());
	}
	
	public static void runParamTuning6() {
		ParameterTuningTest.runOnAllDatasets(ParameterTuningParameters.getRangesForTest6());
	}
	
	
	public static void processParamTuning5() {
		ParameterTuningTest.processAllDatasets(ParameterTuningParameters.getRangesForTest5());
	}
	
	
}
