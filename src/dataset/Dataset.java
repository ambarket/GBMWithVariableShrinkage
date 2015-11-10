package dataset;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import utilities.RandomSample;
import utilities.RawFile;
import utilities.StopWatch;

public class Dataset {
	public static HashSet<String> formsOfMissingValue = new HashSet<String>(Arrays.asList("NA", "N/A", "?", "NULL"));
	public double trainingSampleFraction;
	private int numberOfTrainingExamples, numberOfTestExamples, numberOfPredictors;
	
	private Attribute.Type[] predictorTypes;
	private String[] predictorNames;
	private Attribute[][] trainingInstances;
	private Attribute[][] testInstances;
	
	private Attribute.Type responseType;
	private String responseName;
	private Attribute[] trainingResponses;
	private Attribute[] testResponses;
	
	// Store this so that we can later print out the responses along with their original position in the dataset file.
	private int[] shuffledIndices;
	
	// The minimum amount of improvement necessary to get training.
	private double minErrorDelta;
	/*  
	 *  sortedAttributeIndices is [numberOfPredictors][numberOfExamples], each element stores an index into instances/labels_y. 
	 *  They are sorted in ascending order of instances.get(instanceNum).get(attributeNum)
	 */
	private int[][] numericalPredictorSortedIndexMap;
	
	// Map attribute number -> Map< Category, Set<Examples in that category>>>
	private HashMap<Integer, HashMap<String, HashSet<Integer>>> categoricalPredictorIndexMap = new HashMap<Integer, HashMap<String, HashSet<Integer>>> ();

	public DatasetParameters parameters;
	//--------------------------------------Object Construction-----------------------------------------------------------
	public Dataset(DatasetParameters parameters, double trainingSampleFraction) {
		StopWatch timer = (new StopWatch()).start();
		if (!parameters.attributeTypeHeader) {
			throw new UnsupportedOperationException("Support for dataset files without an explicit attribute type header is not yet implemented");
		}
		if (parameters.responseVariableColumn < 0) {
			throw new IllegalArgumentException("responseVariableColumn must be specificed");
		}
		
		this.parameters = parameters;
		
		// Read dataset file as strings
		RawFile file = new RawFile(parameters.fileDirectory + parameters.fileName, parameters.attributeTypeHeader, parameters.attributeNameHeader);
		this.trainingSampleFraction = trainingSampleFraction;
		this.numberOfTrainingExamples = (int) (file.numberOfRecords * trainingSampleFraction);
		this.numberOfTestExamples = file.numberOfRecords - numberOfTrainingExamples;
		this.numberOfPredictors = file.numberOfAttributes - 1;
		
		// Extract, validate, and store the dataset information in Attribute.Type, Attribute, and Response objects
		extractAndStoreAttributeTypes(file, parameters.responseVariableColumn);
		extractAndStoreAttributeNames(file, parameters.responseVariableColumn);
		extractAndStoreExamples(file, parameters.responseVariableColumn, trainingSampleFraction);
		
		// Pre-process trainingData to improve speed of split calculation
		buildCategoricalPredictorIndexMap();
		buildnumericalPredictorSortedIndexMap();
		
		// Used to stop cross validation. Basically will stop when after n steps of m trees (Set to 3 steps of 5000 trees each)
		//	we didn't improve on the error by more than the minErrorDelta. Using this value is more fair across datasets, as the
		//	magnitude of the response variables in different datasets can differ significantly.
		this.minErrorDelta = calcMeanTrainingResponse() * 0.000001;
		
		System.out.println("Done processing dataset: " + timer.getElapsedSeconds());
	}
	
	public double getMinErrorDelta() {
		return minErrorDelta;
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
	
	private void extractAndStoreExamples(RawFile file, int responseVariableColumn, double trainingSampleFraction) {
		this.shuffledIndices = (new RandomSample()).fisherYatesShuffle(file.numberOfRecords);
		
		trainingInstances = new Attribute[numberOfTrainingExamples][file.numberOfAttributes - 1];
		trainingResponses = new Attribute[numberOfTrainingExamples];
		testInstances = new Attribute[numberOfTestExamples][file.numberOfAttributes - 1];
		testResponses = new Attribute[numberOfTestExamples];
		
		for (int recordIndex = 0; recordIndex < file.numberOfRecords; recordIndex++) {
			Attribute[] instance = new Attribute[file.numberOfAttributes - 1];
			Attribute response = null;
			for (int attributeIndex = 0; attributeIndex < file.numberOfAttributes; attributeIndex++) {
				String stringElement = file.data.get(shuffledIndices[recordIndex])[attributeIndex];
				boolean missingValue = false;
				if (formsOfMissingValue.contains(stringElement)) {
					missingValue = true;
				}
				if (attributeIndex != responseVariableColumn) {
					if (predictorTypes[attributeIndex] == Attribute.Type.Numeric) {
						instance[(attributeIndex < responseVariableColumn) ? attributeIndex : attributeIndex - 1] = (missingValue) ? new Attribute(Attribute.Type.Numeric) : new Attribute(Double.parseDouble(stringElement));
					} else if (predictorTypes[attributeIndex] == Attribute.Type.Categorical) {
						instance[(attributeIndex < responseVariableColumn) ? attributeIndex : attributeIndex - 1] = (missingValue) ? new Attribute(Attribute.Type.Categorical) : new Attribute(stringElement);
					}
				} else {
					if (missingValue) {
						throw new IllegalStateException("Missing values not allowed in response field");
					} else {
						if (responseType == Attribute.Type.Numeric) {
							response = new Attribute(Double.parseDouble(stringElement));
						} else if (responseType  == Attribute.Type.Categorical) {
								response= new Attribute(stringElement);
						}
					}
				} 
			}
		
			if (recordIndex < numberOfTrainingExamples) {
				trainingInstances[recordIndex] = instance;
				trainingResponses[recordIndex] = response;
			} else {
				testInstances[recordIndex-numberOfTrainingExamples] = instance;
				testResponses[recordIndex-numberOfTrainingExamples] = response;
			}
		}
	}

	
	
	//---------------------------Pre-processing to speed up regression tree splits------------------------------------------------
	private void buildCategoricalPredictorIndexMap() {
		for (int predictorIndex = 0; predictorIndex < numberOfPredictors; predictorIndex++) {
			if (predictorTypes[predictorIndex] == Attribute.Type.Categorical) {
				HashMap<String, HashSet<Integer>> predictorIndexMap = new HashMap<String, HashSet<Integer>>();
				categoricalPredictorIndexMap.put(predictorIndex, predictorIndexMap);
				
				// No matter what we need to have this special missing category for the findOptimalSplit to work properly.
				predictorIndexMap.put(Attribute.MISSING_CATEGORY, new HashSet<Integer>());
				
				for (int instanceIndex = 0; instanceIndex < numberOfTrainingExamples; instanceIndex++) {
					// Note missing values have the special category "MISSING_CATEGORY" enforced by the Attribute constructor.
					String category = trainingInstances[instanceIndex][predictorIndex].getCategoricalValue(); 
					predictorIndexMap.putIfAbsent(category, new HashSet<Integer>());
					HashSet<Integer> examplesWithCategory = predictorIndexMap.get(category);
					examplesWithCategory.add(instanceIndex);
				}
			}
		}
	}
	
	private void buildnumericalPredictorSortedIndexMap() {
		numericalPredictorSortedIndexMap = new int[numberOfPredictors][numberOfTrainingExamples];
		for (int attributeNum = 0; attributeNum < numberOfPredictors; attributeNum++) {
			InstanceAttributeComparator comparator = new InstanceAttributeComparator(attributeNum);
			ArrayList<Map.Entry<Integer, Attribute[]>> sortedInstances = new ArrayList<Map.Entry<Integer, Attribute[]>>();
			for (int i = 0; i < numberOfTrainingExamples; i++) {
				sortedInstances.add(new AbstractMap.SimpleEntry<Integer, Attribute[]>(i, trainingInstances[i]));
			}
			Collections.sort(sortedInstances, comparator);
			
			for (int i = 0; i < numberOfTrainingExamples; i++) {
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
			
			if (arg0Value == null || arg1Value == null) {
				throw new IllegalStateException("Attribute objects are null in InstanceAttributeComparator, this shouldn't be possible");
			}
			// Note missing values are considered equal to eachother and greater than any real value, so they will always
			//	appear at the end of the numericalPredictorSortedIndexMap
			return arg0Value.compareTo(arg1Value);
		}
	}
	
	//-----------------------------GETTERS--------------------------------------------------------
	public int getNumberOfTrainingExamples() {
		return numberOfTrainingExamples;
	}
	
	public int getNumberOfTestExamples() {
		return numberOfTestExamples;
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

	public Attribute[][] getTrainingInstances() {
		return trainingInstances;
	}
	
	public Attribute[] getTrainingResponses() {
		return trainingResponses;
	}
	
	public Attribute[][] getTestInstances() {
		return testInstances;
	}
	
	public Attribute[] getTestResponses() {
		return testResponses;
	}

	public Attribute.Type getResponseType() {
		return responseType;
	}

	public String getResponseName() {
		return responseName;
	}
	
	public int[][] getNumericalPredictorSortedIndexMap() {
		return numericalPredictorSortedIndexMap;
	}

	public Map<Integer, HashMap<String, HashSet<Integer>>> getCategoricalPredictorIndexMap() {
		return categoricalPredictorIndexMap;
	}
	
	public int[] getShuffledIndicies() {
		return shuffledIndices;
	}
	
	public double calcMeanTrainingResponse() {
		Attribute[] responses = getTrainingResponses();
		double meanY = 0.0;
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			meanY += responses[i].getNumericValue();
		}
		meanY = meanY / numberOfTrainingExamples;
		return meanY;
	}
	
	public double calcMeanTrainingResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			if (inSample[i]) {
				meanY += trainingResponses[i].getNumericValue();
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcMeanTrainingResponse(boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcMeanTrainingResponse(inSample);
		}
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTrainingExamples; i++) {
			if (!inSample[i]) {
				meanY += trainingResponses[i].getNumericValue();
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcMeanTestResponse() {
		double meanY = 0.0;
		for (int i = 0; i < numberOfTestExamples; i++) {
			meanY += testResponses[i].getNumericValue();
		}
		meanY = meanY / numberOfTestExamples;
		return meanY;
	}
	
	public double calcMeanTestResponse(boolean[] inSample) {
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTestExamples; i++) {
			if (inSample[i]) {
				meanY += testResponses[i].getNumericValue();
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
	
	public double calcMeanTestResponse(boolean[] inSample, boolean negate) {
		if (!negate) {
			return calcMeanTrainingResponse(inSample);
		}
		double meanY = 0.0;
		int count = 0;
		for (int i = 0; i < numberOfTestExamples; i++) {
			if (!inSample[i]) {
				meanY += testResponses[i].getNumericValue();
				count++;
			}
		}
		meanY = meanY / count;
		return meanY;
	}
}
