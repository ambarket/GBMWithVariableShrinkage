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
		}
	}
	
	public static void runParamTuning3() {
		ParameterTuningTest.runOnAllDatasets(ParameterTuningParameters.getRangesForTest3());
	}
	
	public static void processParamTuning3() {
		ParameterTuningTest.processAllDatasets(ParameterTuningParameters.getRangesForTest3());
	}
	
	
}
