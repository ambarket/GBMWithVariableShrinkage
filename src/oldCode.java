import gbm.Attribute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class oldCode {
	/* Maybe abandoning this :(
	public String getAttributeType(int attributeNum) {
		throw new NotImplementedException(); //TODO 
	}
	
	class BestSplitComputer {
		int minnumberOfExamplesInNode;
		
		// TODO: Build this for each new tree (because responses change
		SortedAttributeEntry[][] allSortedAttributeEntries = new SortedAttributeEntry[numOfAttributes][numberOfExamples]; 


		public BestSplit findBestSplit(Dataset dataset, int attributeNum, HashSet<Integer> realIndicesInParent) {
			if (dataset.getAttributeType(attributeNum) == "Categorical") {
				findBestCategoricalSplit(dataset, attributeNum, realIndicesInParent);
			} else if (dataset.getAttributeType(attributeNum) == "Numeric") {
				findBestNumericalSplit(dataset, attributeNum, realIndicesInParent);
			}
			
			return null;
		}
		
		private BestSplit findBestCategoricalSplit(Dataset dataset, int attributeNum, HashSet<Integer> realIndicesInParent) {
			throw new NotImplementedException(); //TODO 
		}
		
		private BestSplit findBestNumericalSplit(Dataset dataset, int attributeNum, HashSet<Integer> realIndicesInParent) {
			BestSplit bestSplit = new BestSplit();
			bestSplit.splitAttribute = attributeNum;
			
			// Pull out the sorted entries that map to examples in the parent.
			ArrayList<SortedAttributeEntry> sortedAttributeEntriesInParent = new ArrayList<SortedAttributeEntry>();
			for (int i = 0; i < allSortedAttributeEntries[attributeNum].length - 1; i++) {
				if (realIndicesInParent.contains(allSortedAttributeEntries[attributeNum][i].realIndex)) {
					sortedAttributeEntriesInParent.add(allSortedAttributeEntries[attributeNum][i]);
				}
			}

			int firstSplitIndex = minnumberOfExamplesInNode - 1;
			double sumLeft = 0.0, sumOfSquaresLeft = 0.0, meanLeft = 0.0, sumRight = 0.0, sumOfSquaresRight = 0.0, meanRight = 0.0;
			int countLeft = firstSplitIndex, countRight = sortedAttributeEntriesInParent.size() - firstSplitIndex;
			boolean sumOrDifference = true; // true == sum, false == difference
			
			// Initialize sums. TODO: Check this logic before actually implementing the sortedAttributeEntries.
			for (int i = firstSplitIndex; i >= 0; i--) {
				SortedAttributeEntry current = sortedAttributeEntriesInParent.get(i);
				if (sumOrDifference) {
					sumLeft += current.sumLeft;
					sumOfSquaresLeft += current.sumOfSquaresLeft;
				} else {
					sumLeft -= current.sumLeft;
					sumOfSquaresLeft -= current.sumOfSquaresLeft;
				}
				sumOrDifference = !sumOrDifference;
			}
			meanLeft = sumLeft / countLeft;
			sumOrDifference = true;
			for (int i = sortedAttributeEntriesInParent.size() - 1; i > firstSplitIndex ; i--) {
				SortedAttributeEntry current = sortedAttributeEntriesInParent.get(i);
				if (sumOrDifference) {
					sumRight += current.sumLeft;
					sumOfSquaresRight += current.sumOfSquaresLeft;
				} else {
					sumRight -= current.sumLeft;
					sumOfSquaresRight -= current.sumOfSquaresLeft;
				}
				sumOrDifference = !sumOrDifference;
			}
			// If the last thing we did was add a term, we need to subtract out the sums assigned to the left child.
			if (!sumOrDifference) {
				sumRight -= sumLeft;
				sumOfSquaresRight -= sumOfSquaresLeft;
			}
			meanRight = sumRight / countRight;
			
			// SplitIndex = lastIndex in left node
			int splitIndex = firstSplitIndex, lastSplitIndex = sortedAttributeEntriesInParent.size() - firstSplitIndex - 1;
			double leftError = Double.MAX_VALUE, rightError = Double.MAX_VALUE, totalError = Double.MAX_VALUE, minumumTotalError = Double.MAX_VALUE;
			
			SortedAttributeEntry lastLeft, firstRight;
			double lastLeftData, firstRightData;
			while (splitIndex <= lastSplitIndex) {
				/*
				 *  Find the next valid split point and update lastLeft and firstRight variables accordingly. 
				 *  The problem is we can't split between two examples that have the same attribute value.
				 */
/*
				lastLeft = sortedAttributeEntriesInParent.get(splitIndex);
				firstRight = sortedAttributeEntriesInParent.get(splitIndex+1);
				lastLeftData = dataset.instances.get(lastLeft.realIndex).get(attributeNum);
				firstRightData = dataset.instances.get(firstRight.realIndex).get(attributeNum);
				while(DoubleCompare.equals(lastLeftData, firstRightData) && splitIndex <= lastSplitIndex ) {
					splitIndex++;
					firstRightData = dataset.instances.get(sortedAttributeEntriesInParent.get(splitIndex + 1).realIndex).get(attributeNum);
				}
				if (DoubleCompare.equals(lastLeftData, firstRightData) && splitIndex == lastSplitIndex) {
					continue; // We can't perform any more valid splits. 
				}
				lastLeft = sortedAttributeEntriesInParent.get(splitIndex);
				firstRight = sortedAttributeEntriesInParent.get(splitIndex+1);

				// Calculate the total squared error resulting from this split. Formula is just sum[ (y_InChild - meanInChild)^2 ] factored out.
				countLeft = splitIndex;
				countRight = sortedAttributeEntriesInParent.size() - splitIndex;
				leftError = sumOfSquaresLeft - (2 * meanLeft * sumLeft) + (countLeft * meanLeft * meanLeft);
				rightError = sumOfSquaresRight - (2 * meanRight * sumRight) + (countRight * meanRight * meanRight);
				totalError = leftError + rightError;
				
				if (DoubleCompare.lessThan(totalError, minumumTotalError)) {
					bestSplit.splitValue = (lastLeftData + firstRightData) / 2;
					bestSplit.leftSquaredError = leftError;
					bestSplit.rightSquaredError = rightError;
					bestSplit.leftInstanceCount = countLeft;
					bestSplit.rightInstanceCount = countRight;
					bestSplit.leftMeanResponse = meanLeft;
					bestSplit.rightMeanResponse = meanRight;
					bestSplit.success = true;
					minumumTotalError = totalError;
				}
				
				// Shift the firstRight into the left child, update sums to reflect this.
				double sumShift 		 = firstRight.sumLeft - lastLeft.sumLeft,
					   sumOfSquaresShift = firstRight.sumOfSquaresLeft - lastLeft.sumOfSquaresLeft;
				sumLeft += sumShift;
				sumOfSquaresLeft += sumOfSquaresShift;
				sumRight -= sumShift;
				sumOfSquaresRight -= sumOfSquaresShift;
				
				splitIndex++;
			}
			
			return bestSplit;
		}
	}

	abstract class SortedAttributeEntry {
		int realIndex, sortedIndex;
		double sumLeft, sumOfSquaresLeft;
		double previousSumLeft, previousSumOfSquaresLeft;
		
		/*
		public virtual 
		class SortedNumericalAttributeEntry {
			double value;
			
			public boolean equals(Object that) {
				SortedNumericalAttributeEntry other = (SortedNumericalAttributeEntry)that;
				return DoubleCompare.equals(this.value, other.value);
			}
		}
		class SortedCategoricalAttributeEntry {
			String value;
			
			public boolean equals(Object that) {
				SortedCategoricalAttributeEntry other = (SortedCategoricalAttributeEntry)that;
				return this.value.equals(other.value);
			}
		}
		
	}
	*/

//-----------------------------------------------------------
	/*
	public static Attribute[] readY(String basePath, String fileName) {
		Attribute[] retval = new Attribute[]();
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
	
	public static ArrayList<Attribute[]> readX(String basePath, String fileName, boolean header) {
		ArrayList<Attribute[]> retval = new ArrayList<Attribute[]>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(basePath + fileName)));
			String line;
			int i = 0;
			int numOfAttributes = -1;
			
			if (header) {
				br.readLine();
			}
			
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
				Attribute[] record = new Attribute[]();
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
	
	public static ArrayList<Attribute> extractResponseColumn(int columnNum, ArrayList<ArrayList<Attribute>> dataset) {
		ArrayList<Attribute> retval = new ArrayList<>();
		
		for (ArrayList<Attribute> record : dataset) {
			retval.add(record.remove(columnNum));
		}
		
		return retval;
	}
	
	
	/*
	public Dataset(ArrayList<ArrayList<Attribute>> instances, Attribute[] responses) {
		if (instances.size() != responses.size() || instances.size() == 0) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of instances does not match" + 
		                  " with the nunber of observations or " + 
					"the number of instances is 0");
			System.exit(0);
		}
		
		
		this.numberOfExamples = instances.size();
		this.numOfAttributes = instances.get(0).size();
		this.instances = instances;
		// Will be set to the gradients by GradientBoostingTree.build each iteration, need to keep the original copy separate.
		this.responses = new Attribute[](responses);
		this.originalresponses = responses;
		
		numericalPredictorSortedIndexMap = new int[numOfAttributes][numberOfExamples];
		
		// Map attribute number -> Map< Category, Set<Examples in that category>>>
		HashMap<Integer, HashMap<String, HashSet<Integer>>> categorialAttributeIndexMap = new HashMap<Integer, HashMap<String, HashSet<Integer>>> ();
		
		
		StopWatch timer = (new StopWatch()).start();
		for (int attributeNum = 0; attributeNum < numOfAttributes; attributeNum++) {
			InstanceAttributeComparator comparator = new InstanceAttributeComparator(attributeNum);
			ArrayList<Map.Entry<Integer, ArrayList<Attribute>>> sortedInstances = new ArrayList<Map.Entry<Integer, ArrayList<Attribute>>>();
			for (int i = 0; i < numberOfExamples; i++) {
				sortedInstances.add(new AbstractMap.SimpleEntry<Integer, ArrayList<Attribute>>(i, instances.get(i)));
			}
			Collections.sort(sortedInstances, comparator);
			
			for (int i = 0; i < numberOfExamples; i++) {
				Map.Entry<Integer, Attribute[]> entry = sortedInstances.get(i);
				numericalPredictorSortedIndexMap[attributeNum][i] = entry.getKey();
			}
		}
		System.out.println("Done sorting: " + timer.getElapsedSeconds());
	}
	*/
}