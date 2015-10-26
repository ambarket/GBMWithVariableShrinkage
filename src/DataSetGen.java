import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import utilities.MersenneTwisterFast;


public class DataSetGen {
	public static void gen() throws IOException {
		
		String training = System.getProperty("user.dir") + "/data/gen2/" + "TRAINING.txt";
		String test= System.getProperty("user.dir") + "/data/gen2/" + "TEST.txt";
		
		int numOfAttributes = 10;
		int numOfTrainExamples = 9000;
		int numOfTestExamples = 2000;
		int numOfCategories = 30;
		int numOfCharInCategory = 6;
		
		genAndWriteFile(training,numOfAttributes, numOfTrainExamples,  numOfCategories, numOfCharInCategory);
		genAndWriteFile(test,numOfAttributes, numOfTestExamples,  numOfCategories, numOfCharInCategory);
	}
	private static char rndChar () {
	    int rnd = (int) (Math.random() * 52); // or use Random or whatever
	    char base = (rnd < 26) ? 'A' : 'a';
	    return (char) (base + rnd % 26);

	}
	public static void genAndWriteFile(String fileName, int numOfAttributes, int numOfTrainExamples, int numOfCategories, int numOfCharInCategory ) throws IOException {
		BufferedWriter trainingFile = new BufferedWriter(new PrintWriter(new File(fileName)));
		MersenneTwisterFast rand = new MersenneTwisterFast();
		String[] types = {"C", "N"};
		String[] attrTypes = new String[numOfAttributes];
		String[] categories = new String[numOfCategories];
	
		for (int i = 0; i < numOfCategories; i++) {
			String s = "";
			for (int j = 0; j < numOfCharInCategory; j++) {
				s = s + rndChar();
			}
			categories[i] = s;
		}
		
		for (int i = 0; i < numOfAttributes; i++) {
			int type = rand.nextInt(2);
			if (i == numOfAttributes-1) {
				type = 1; // Force response to be numeric
			} else {
				type = 0; // For all categorical preicotrs
			}
			
			attrTypes[i] = types[type];
			trainingFile.write(attrTypes[i]);
			if (i != numOfAttributes-1) {
				trainingFile.write("\t");
			} else {
				trainingFile.write("\n");
			}
		}
		
		trainingFile.write("A\tB\tC\tD\tE\tF\tG\tH\tI\tRESP\n");
		
		for (int i = 0; i < numOfTrainExamples; i++) {
			for (int j = 0; j < numOfAttributes; j++) {
				switch(attrTypes[j]) {
					case "C":
						trainingFile.write(categories[rand.nextInt(numOfCategories)]);
						break;
					case "N":
						double val = (Math.round(rand.nextInt(1000000) * rand.nextDouble(true, true)) / 100);
						trainingFile.write("" + val);
						break;
				}
				if (j != numOfAttributes-1) {
					trainingFile.write("\t");
				} else if (i != numOfTrainExamples-1){
					trainingFile.write("\n");
				}
			}
		}
		trainingFile.flush();
		trainingFile.close();
	}
}
