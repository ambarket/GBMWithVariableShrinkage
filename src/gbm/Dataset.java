package gbm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import utilities.Logger;
import utilities.StopWatch;

public class Dataset {
	public final ArrayList<ArrayList<Double>> instances_x;
	public final ArrayList<Double> responses_y; // Updated to be the gradient after each iteration
	public final ArrayList<Double> originalResponses_y;
	
	public final double meanY;
	public  final int numOfExamples, numOfAttributes;
	
	/*  
	 *  sortedAttributeIndices is [numOfAttributes][numOfExamples], each element stores an index into instances_x/labels_y. 
	 *  They are sorted in ascending order of instances_x.get(instanceNum).get(attributeNum)
	 */
	public final int[][] sortedAttributeIndices;
	
	public Dataset(ArrayList<ArrayList<Double>> instances_x, ArrayList<Double> responses_y) {
		if (instances_x.size() != responses_y.size() || instances_x.size() == 0) {
			Logger.println(Logger.LEVELS.DEBUG, "The number of instances does not match" + 
		                  " with the nunber of observations or " + 
					"the number of instances is 0");
			System.exit(0);
		}
		
		
		this.numOfExamples = instances_x.size();
		this.numOfAttributes = instances_x.get(0).size();
		this.instances_x = instances_x;
		// Will be set to the gradients by GradientBoostingTree.build each iteration, need to keep the original copy separate.
		this.responses_y = new ArrayList<Double>(responses_y);
		this.originalResponses_y = responses_y;
		
		this.meanY = calcMeanY();
		
		// TODO Calculate sortedAttributeIndices
		sortedAttributeIndices = new int[numOfAttributes][numOfExamples];
		
		StopWatch timer = (new StopWatch()).start();
		for (int attributeNum = 0; attributeNum < numOfAttributes; attributeNum++) {
			InstanceAttributeComparator comparator = new InstanceAttributeComparator(attributeNum);
			ArrayList<Map.Entry<Integer, ArrayList<Double>>> sortedInstances = new ArrayList<Map.Entry<Integer, ArrayList<Double>>>();
			for (int i = 0; i < numOfExamples; i++) {
				sortedInstances.add(new AbstractMap.SimpleEntry<Integer, ArrayList<Double>>(i, instances_x.get(i)));
			}
			Collections.sort(sortedInstances, comparator);
			
			for (int i = 0; i < numOfExamples; i++) {
				Map.Entry<Integer, ArrayList<Double>> entry = sortedInstances.get(i);
				sortedAttributeIndices[attributeNum][i] = entry.getKey();
			}
		}
		System.out.println("Done sorting: " + timer.getElapsedSeconds());
		
	}
	
	private double calcMeanY() {
		double meanY = 0.0;
		Iterator<Double> iter = responses_y.iterator();
		while (iter.hasNext()) {
			meanY += iter.next();
		}
		meanY = meanY / numOfExamples;
		return meanY;
	}
	
	class InstanceAttributeComparator implements Comparator<Map.Entry<Integer, ArrayList<Double>>> {
		int attributeNum;
		public InstanceAttributeComparator(int attributeNum) {
			this.attributeNum = attributeNum;
		}
		public int compare(Map.Entry<Integer, ArrayList<Double>> arg0, Map.Entry<Integer, ArrayList<Double>> arg1) {
			return arg0.getValue().get(attributeNum).compareTo(arg1.getValue().get(attributeNum));
		}
	}
}
