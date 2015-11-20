package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/*
 * Reads the file at the specified path. No type conversion is done here, the raw strings are simple stored.
 * This method does guarantee that the dataset is rectangular. e.g. all rows contain the same exact number of elements.
 */
public class RawFile {
	public String[] attributeTypes = null;
	public String[] attributeNames = null;
	public ArrayList<String[]> data = new ArrayList<String[]>();
	public int numberOfAttributes = -1;
	public int numberOfRecords = -1;

	public RawFile (String filePath, boolean attributeTypeHeader, boolean attributeNameHeader) {	
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			
			
			if (attributeTypeHeader) {
				line = br.readLine().trim().replaceAll(" +|\t+|,", " ");
				attributeTypes = line.split(" ");
				numberOfAttributes = attributeTypes.length;
			}
			
			if (attributeNameHeader) {
				line = br.readLine().trim().replaceAll(" +|\t+|,", " ");
				attributeNames = line.split(" ");
				numberOfAttributes = attributeNames.length;
			}
			
			int i = 0;
			while ((line = br.readLine()) != null) {
				line = line.trim().replaceAll(" +|\t+|,", " ");
				String[] record = line.split(" ");
				if (numberOfAttributes == -1) {
					numberOfAttributes = record.length;
				}
				if (numberOfAttributes != record.length) {
					br.close();
					throw new IOException("Line " + i + " has " + record.length + " attributes, but an earlier line had " + numberOfAttributes + " attributes");
				}
				data.add(record);
				i++;
			}
			if (i == 0) {
				br.close();
				throw new IOException("The file has 0 records in it. Cannot continue");
			}
			numberOfRecords = i;
			br.close();
		} catch (IOException e) {
			System.err.println(StopWatch.getDateTimeStamp());
			e.printStackTrace();
			System.exit(1);
		} 
	}
	
}
