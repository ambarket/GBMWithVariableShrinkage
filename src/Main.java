import parameterTuning.ParameterTuningTest3;


public class Main {
	public static void main(String[] args) {
		switch(args[0]) {
			case "powerPlant":
				ParameterTuningTest3.runPowerPlant();
			case "nasa":
				ParameterTuningTest3.runNASA();
			case "bikeSharing":
				ParameterTuningTest3.runBikeSharing();
		}
	}
}
