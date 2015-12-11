import parameterTuning.LatexResultsGenerator;
import parameterTuning.ParameterTuningParameters;
import parameterTuning.ParameterTuningTest;


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
