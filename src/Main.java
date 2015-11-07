import parameterTuning.ParameterTuningTest3;


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
		ParameterTuningTest3.runCrimeCommunities();
		ParameterTuningTest3.runPowerPlant();
		ParameterTuningTest3.runNASA();
		ParameterTuningTest3.runBikeSharing();
	}
	
	public static void processParamTuning3() {
		ParameterTuningTest3.processNASA();
		ParameterTuningTest3.processBikeSharing();
	}
	
	
}
