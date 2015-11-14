package parameterTuning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import parameterTuning.RunDataSummaryRecord.FilterableProperty;
import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.DoubleCompare;

import com.google.common.collect.ImmutableMap;

public class RunDataSummaryRecordFilter {	
	public Map<FilterableProperty, Object> filter;
	
	public RunDataSummaryRecordFilter(Map<FilterableProperty, Object> filter) {
		this.filter = filter;
	}
	/**
	 * Use to concatenate predefined filters;
	 * @param filters
	 */
	public RunDataSummaryRecordFilter(Map<FilterableProperty, Object>... filters) {
		this.filter = new HashMap<FilterableProperty, Object>();
		for (Map<FilterableProperty, Object> map : filters) {
			this.filter.putAll(map);
		}
	}
	
	public RunDataSummaryRecordFilter(RunDataSummaryRecordFilter... filters) {
		this.filter = new HashMap<FilterableProperty, Object>();
		for (RunDataSummaryRecordFilter filter : filters) {
			this.filter.putAll(filter.filter);
		}
	}
	
	public String getLongFilterDescription() {
		StringBuffer description = new StringBuffer();
		for (Map.Entry<FilterableProperty, Object> filterEntry : filter.entrySet()) {
			if (description.length() > 0) {
				description.append(" and ");
			}
			description.append(filterEntry.getKey().name() + " = " + filterEntry.getValue().toString());
		}
		return description.toString();
	}
	
	public String getMinimalFilterDescription() {
		StringBuffer description = new StringBuffer();
		for (Map.Entry<FilterableProperty, Object> filterEntry : filter.entrySet()) {
			if (description.length() > 0) {
				description.append("and");
			}
			description.append(filterEntry.getKey().name() + "=" + filterEntry.getValue().toString());
		}
		return description.toString();
	}
	
	public List<RunDataSummaryRecord> filterRecordsOnParameterValue(List<RunDataSummaryRecord> allRecords) {
		if (filter == null) {
			return allRecords;
		}
		HashSet<RunDataSummaryRecord> filteredRecords = new HashSet<>(allRecords);
		
		for (Map.Entry<FilterableProperty, Object> filterEntry : filter.entrySet()) {
			HashSet<RunDataSummaryRecord> recordsToRemove = new HashSet<>();
			for (RunDataSummaryRecord record : filteredRecords) {
				if (!doesRecordMatchFilter(record, filterEntry)) {
					recordsToRemove.add(record);
				}
			}
			filteredRecords.removeAll(recordsToRemove);
		}
		
		return new ArrayList<RunDataSummaryRecord>(filteredRecords);
	}
	
	private static boolean doesRecordMatchFilter(RunDataSummaryRecord record, Map.Entry<FilterableProperty, Object> filterEntry) {
		switch (filterEntry.getKey()) {
			case BagFraction:
				return DoubleCompare.equals(record.parameters.bagFraction, (Double)filterEntry.getValue());
			case LearningRatePolicy:
				return record.parameters.learningRatePolicy == (LearningRatePolicy)filterEntry.getValue();
			case MaxLearningRate:
				return DoubleCompare.equals(record.parameters.maxLearningRate, (Double)filterEntry.getValue());
			case MinLearningRate:
				return DoubleCompare.equals(record.parameters.minLearningRate, (Double)filterEntry.getValue());
			case MaxNumberOfSplits:
				return record.parameters.maxNumberOfSplits == (Integer)filterEntry.getValue();
			case MinExamplesInNode:
				return record.parameters.minExamplesInNode == (Integer)filterEntry.getValue();
		}
		System.out.println("ERROR: Shouldn't reach here in RunDataSummaryRecord.doesRecordMatchFilter");
		return false;
	}
	
	// Predefined Bag Fraction Filters
	public static RunDataSummaryRecordFilter learningRatePolicyEqualsConstant = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.LearningRatePolicy, LearningRatePolicy.CONSTANT).build());
	public static RunDataSummaryRecordFilter learningRatePolicyEqualsRevisedVariable = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.LearningRatePolicy, LearningRatePolicy.REVISED_VARIABLE).build());

	// Predefined Bag Fraction Filters
	public static RunDataSummaryRecordFilter bagFractionEqualsOneQuarterFilter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.BagFraction, 0.25).build());
	public static RunDataSummaryRecordFilter bagFractionEqualsOneHalfFilter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.BagFraction, 0.5).build());
	public static RunDataSummaryRecordFilter bagFractionEqualsThreeQuartersFilter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.BagFraction, 0.75).build());
	public static RunDataSummaryRecordFilter bagFractionEqualsOneFilter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.BagFraction, 1).build());
	
	// Predefined MinExamplesInNode Filters
	public static RunDataSummaryRecordFilter examplesInNodeEquals1Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MinExamplesInNode, 1).build());
	public static RunDataSummaryRecordFilter examplesInNodeEquals50Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MinExamplesInNode, 50).build());
	public static RunDataSummaryRecordFilter examplesInNodeEquals100Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MinExamplesInNode, 100).build());
	public static RunDataSummaryRecordFilter examplesInNodeEquals250Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MinExamplesInNode, 250).build());
	
	// Predefined Number Of Splits Filters
	public static RunDataSummaryRecordFilter maxSplitsEquals128Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 128).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals64Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 64).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals32Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 32).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals16Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 16).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals8Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 8).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals4Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 4).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals2Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 2).build());
	public static RunDataSummaryRecordFilter maxSplitsEquals1Filter = 
			new RunDataSummaryRecordFilter(new ImmutableMap.Builder<FilterableProperty, Object>()
					.put(FilterableProperty.MaxNumberOfSplits, 1).build());
}
