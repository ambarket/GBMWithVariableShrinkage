package gbm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import utilities.RawFile;
import utilities.StopWatch;

public class Dataset {
	private int numberOfExamples, numberOfPredictors;
	
	private Attribute.Type[] predictorTypes;
	private String[] predictorNames;
	private Attribute[][] instances;
	
	private Attribute.Type responseType;
	private String responseName;
	private Attribute[] responses;
	
	/*  
	 *  sortedAttributeIndices is [numberOfPredictors][numberOfExamples], each element stores an index into instances/labels_y. 
	 *  They are sorted in ascending order of instances.get(instanceNum).get(attributeNum)
	 */
	private int[][] numericalPredictorSortedIndexMap;
	
	// Map attribute number -> Map< Category, Set<Examples in that category>>>
	private HashMap<Integer, HashMap<String, HashSet<Integer>>> categoricalPredictorIndexMap = new HashMap<Integer, HashMap<String, HashSet<Integer>>> ();
	
	//--------------------------------------Object Construction-----------------------------------------------------------
	public Dataset(String filePath, boolean attributeTypeHeader, boolean attributeNameHeader, int responseVariableColumn) {
		StopWatch timer = (new StopWatch()).start();
		if (!attributeTypeHeader) {
			throw new UnsupportedOperationException("Support for dataset files without an explicit attribute type header is not yet implemented");
		}
		if (responseVariableColumn < 0) {
			throw new IllegalArgumentException("responseVariableColumn must be specificed");
		}
		
		// Read dataset file as strings
		RawFile file = new RawFile(filePath, attributeTypeHeader, attributeNameHeader);
		numberOfExamples = file.numberOfRecords;
		numberOfPredictors = file.numberOfAttributes - 1;
		
		// Extract, validate, and store the dataset information in Attribute.Type, Attribute, and Response objects
		extractAndStoreAttributeTypes(file, responseVariableColumn);
		extractAndStoreAttributeNames(file, responseVariableColumn);
		extractAndStoreExamples(file, responseVariableColumn);
		
		// Pre-process trainingData to improve speed of split calculation
		buildCategoricalPredictorIndexMap();
		buildnumericalPredictorSortedIndexMap();
		
		System.out.println("Done processing dataset: " + timer.getElapsedSeconds());
	}
	
	private void extractAndStoreAttributeTypes(RawFile file, int responseVariableColumn) {
		predictorTypes = new Attribute.Type[file.attributeTypes.length - 1];
		for (int i = 0; i < file.numberOfAttributes; i++) {
			switch(file.attributeTypes[i].toUpperCase()) {
				case "NUMERIC":
				case "N":
					if (i == responseVariableColumn) {
						responseType = Attribute.Type.Numeric;
					} else {
						int index = (responseType == null) ? i : i - 1;
						predictorTypes[index] = Attribute.Type.Numeric;
					}
					break;
				case "CATEGORICAL":
				case "C":
					if (i == responseVariableColumn) {
						responseType = Attribute.Type.Categorical;
					} else {
						int index = (responseType == null) ? i : i - 1;
						predictorTypes[index] = Attribute.Type.Categorical;
					}
					break;
				default:
					throw new IllegalArgumentException("The value for attribute type in column " + i + 
							"( " + file.attributeTypes[i] + " ) was not recognized. Value must be one of " +
							"{NUMERIC, N, CATEGORICAL, C}");
			}
		}
	}
	
	private void extractAndStoreAttributeNames(RawFile file, int responseVariableColumn) {
		responseName = file.attributeNames[responseVariableColumn];
		predictorNames = new String[file.numberOfAttributes - 1];
		for (int i = 0; i < file.numberOfAttributes - 1; i++) {
			int index = (i < responseVariableColumn) ? i : i - 1;
			predictorNames[index] = file.attributeNames[i];
		}
	}
	
	private void extractAndStoreExamples(RawFile file, int responseVariableColumn) {
		instances = new Attribute[file.numberOfRecords][file.numberOfAttributes - 1];
		responses = new Attribute[file.numberOfRecords];
		for (int recordIndex = 0; recordIndex < file.numberOfRecords; recordIndex++) {
			Attribute[] instance = new Attribute[file.numberOfAttributes - 1];
			Attribute response = null;
			for (int attributeIndex = 0; attributeIndex < file.numberOfAttributes; attributeIndex++) {
				String stringElement = file.data.get(recordIndex)[attributeIndex];
				boolean missingValue = false;
				// TODO: Make more generic?
				if (stringElement.equalsIgnoreCase("NA")) {
					missingValue = true;
				}
				if (attributeIndex != responseVariableColumn) {
					if (predictorTypes[attributeIndex] == Attribute.Type.Numeric) {
						instance[(attributeIndex < responseVariableColumn) ? attributeIndex : attributeIndex - 1] = new Attribute((missingValue) ? null : Double.parseDouble(stringElement));
					} else if (predictorTypes[attributeIndex] == Attribute.Type.Categorical) {
						instance[(attributeIndex < responseVariableColumn) ? attributeIndex : attributeIndex - 1] = new Attribute((missingValue) ? null : stringElement);
					}
				} else {
					if (responseType == Attribute.Type.Numeric) {
						response = new Attribute((missingValue) ? null : Double.parseDouble(stringElement));
					} else if (responseType  == Attribute.Type.Categorical) {
						response= new Attribute((missingValue) ? null : stringElement);
					}
				} 
			}
			instances[recordIndex] = instance;
			responses[recordIndex] = response;
		}
	}

	
	
	//---------------------------Pre-processing to speed up regression tree splits------------------------------------------------
	private void buildCategoricalPredictorIndexMap() {
		for (int predictorIndex = 0; predictorIndex < numberOfPredictors; predictorIndex++) {
			if (predictorTypes[predictorIndex] == Attribute.Type.Categorical) {
				HashMap<String, HashSet<Integer>> predictorIndexMap = new HashMap<String, HashSet<Integer>>();
				categoricalPredictorIndexMap.put(predictorIndex, predictorIndexMap);
				
				for (int instanceIndex = 0; instanceIndex < numberOfExamples; instanceIndex++) {
					String category = instances[instanceIndex][predictorIndex].getCategoricalValue();
					predictorIndexMap.putIfAbsent(category, new HashSet<Integer>());
					HashSet<Integer> examplesWithCategory = predictorIndexMap.get(category);
					examplesWithCategory.add(instanceIndex);
				}
			}
		}
	}
	
	private void buildnumericalPredictorSortedIndexMap() {
		numericalPredictorSortedIndexMap = new int[numberOfPredictors][numberOfExamples];
		for (int attributeNum = 0; attributeNum < numberOfPredictors; attributeNum++) {
			InstanceAttributeComparator comparator = new InstanceAttributeComparator(attributeNum);
			ArrayList<Map.Entry<Integer, Attribute[]>> sortedInstances = new ArrayList<Map.Entry<Integer, Attribute[]>>();
			for (int i = 0; i < numberOfExamples; i++) {
				sortedInstances.add(new AbstractMap.SimpleEntry<Integer, Attribute[]>(i, instances[i]));
			}
			Collections.sort(sortedInstances, comparator);
			
			for (int i = 0; i < numberOfExamples; i++) {
				Map.Entry<Integer, Attribute[]> entry = sortedInstances.get(i);
				numericalPredictorSortedIndexMap[attributeNum][i] = entry.getKey();
			}
		}
	}
	
	
	private class InstanceAttributeComparator implements Comparator<Map.Entry<Integer, Attribute[]>> {
		int attributeNum;
		InstanceAttributeComparator(int attributeNum) {
			this.attributeNum = attributeNum;
		}
		@Override
		public int compare(Map.Entry<Integer, Attribute[]> arg0, Map.Entry<Integer, Attribute[]> arg1) {
			Attribute arg0Value = arg0.getValue()[attributeNum];
			Attribute arg1Value = arg1.getValue()[attributeNum];
			
			if (arg0Value == null) {
				if (arg1Value == null) {
					return 0;
				} else {
					return 1; // null > anyValue so push it to the back of the sortedIndices.
				}
			}
			if (arg1Value == null) {
				return -1; 
			}
			return arg0Value.compareTo(arg1Value);
		}
	}
	
	//-----------------------------GETTERS--------------------------------------------------------
	public int getNumberOfExamples() {
		return numberOfExamples;
	}

	public int getNumberOfPredictors() {
		return numberOfPredictors;
	}

	public Attribute.Type[] getPredictorTypes() {
		return predictorTypes;
	}

	public String[] getPredictorNames() {
		return predictorNames;
	}

	public Attribute[][] getInstances() {
		return instances;
	}

	public Attribute.Type getResponseType() {
		return responseType;
	}

	public String getResponseName() {
		return responseName;
	}

	public Attribute[] getResponses() {
		return responses;
	}

	public int[][] getNumericalPredictorSortedIndexMap() {
		return numericalPredictorSortedIndexMap;
	}

	public HashMap<Integer, HashMap<String, HashSet<Integer>>> getCategoricalPredictorIndexMap() {
		return categoricalPredictorIndexMap;
	}
}
