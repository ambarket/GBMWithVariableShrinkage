import parameterTuning.ParameterTuningParameters;
import parameterTuning.ParameterTuningTest;


public class Main {
	public static void main(String[] args) {
		switch(args[0]) {
			case "runParamTuning3":
				runParamTuning3();
				break;
			case "processParamTuning3":
				processParamTuning3();
				break;
			case "runParamTuning4":
				runParamTuning4();
				break;
			case "processParamTuning4":
				processParamTuning4();
				break;
		}
	}
	
	public static void runParamTuning3() {
		ParameterTuningTest.runOnAllDatasets(ParameterTuningParameters.getRangesForTest3());
	}
	
	public static void processParamTuning3() {
		ParameterTuningTest.processAllDatasets(ParameterTuningParameters.getRangesForTest3());
	}
	
	public static void runParamTuning4() {
		ParameterTuningTest.runOnAllDatasets(ParameterTuningParameters.getRangesForTest4());
	}
	
	public static void processParamTuning4() {
		ParameterTuningTest.processAllDatasets(ParameterTuningParameters.getRangesForTest4());
	}
	
	
}
