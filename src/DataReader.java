import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class DataReader {
	public static ArrayList<ArrayList<Double>> readX(String basePath, String fileName) {
		ArrayList<ArrayList<Double>> retval = new ArrayList<ArrayList<Double>>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(basePath + fileName)));
			String line;
			int i = 0;
			int numOfAttributes = -1;

			while ((line = br.readLine()) != null) {
				line = line.trim().replaceAll(" +|\t+|,", " ");
				String[] attributes = line.split(" ");
				if (numOfAttributes == -1) {
					numOfAttributes = attributes.length;
				}
				if (numOfAttributes != attributes.length) {
					br.close();
					throw new IOException("Line " + i + " has " + attributes.length + " attributes, but an earlier line had " + numOfAttributes + " attributes");
				}
				ArrayList<Double> record = new ArrayList<Double>();
				for (String attr : attributes) {
					record.add(Double.valueOf(attr));
				}
				retval.add(record);
				i++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return retval;
	}
	
	public static ArrayList<Double> readY(String basePath, String fileName) {
		ArrayList<Double> retval = new ArrayList<Double>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(basePath + fileName)));
			String line;
			int i = 0;
			int numOfAttributes = 1;

			while ((line = br.readLine()) != null) {
				line = line.trim().replaceAll(" +|\t+|,", " ");
				String[] attributes = line.split(" ");
				if (numOfAttributes != 1) {
					br.close();
					throw new IOException("Line " + i + " has " + attributes.length + " attributes, but the y file should only have one value per line");
				}
				retval.add(Double.valueOf(attributes[0]));
				i++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return retval;
	}
}
