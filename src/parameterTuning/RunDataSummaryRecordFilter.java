package parameterTuning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableMap;

import gbm.GbmParameters;
import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.DoubleCompare;
import utilities.StopWatch;


public class RunDataSummaryRecordFilter {	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RunDataSummaryRecordFilter other = (RunDataSummaryRecordFilter) obj;
		if (filter == null && other.filter == null) {
			return true;
		} else if ((filter == null && other.filter != null) || (filter != null && other.filter == null)) {
			return false;
		} else if (!filter.keySet().containsAll(other.filter.keySet()) || !other.filter.keySet().containsAll(filter.keySet())) {
				return false;
		} else {
			for (FilterableProperty thisKey : filter.keySet()) {
				if (!doPropertiesMatch(thisKey, filter.get(thisKey), other.filter.get(thisKey))) {
					return false;
				}
			}
		}
		return true;
	}
	public enum FilterableProperty {BagFraction, MinExamplesInNode, MinLearningRate, MaxLearningRate, MaxNumberOfSplits, LearningRatePolicy}
	
	public Map<FilterableProperty, Object> filter;
	
	public RunDataSummaryRecordFilter(Map<FilterableProperty, Object> filter) {
		this.filter = filter;
	}
	/**
	 * Use to concatenate predefined filters;
	 * @param filters
	 */
	@SafeVarargs
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
	
	public RunDataSummaryRecordFilter(FilterableProperty[] properties, Object[] values) {
		if (properties.length != values.length) {
			throw new IllegalArgumentException();
		}
		
		this.filter = new HashMap<FilterableProperty, Object>();
		for (int i = 0; i < properties.length; i++) {
			this.filter.put(properties[i], values[i]);
		}
	}
	
	public RunDataSummaryRecordFilter(LearningRatePolicy lrPolicy, Object minLR, Object maxLR, Object maxNumberOfSplits, Object bagFraction, Object minExamplesInNode) {
		 this(new FilterableProperty[] {FilterableProperty.LearningRatePolicy, FilterableProperty.MinLearningRate, FilterableProperty.MaxLearningRate, FilterableProperty.MaxNumberOfSplits, FilterableProperty.BagFraction, FilterableProperty.MinExamplesInNode},
					new Object[] {lrPolicy, minLR, maxLR, maxNumberOfSplits, bagFraction, minExamplesInNode});
	}
	
	public RunDataSummaryRecordFilter(Object minLR, Object maxLR, Object maxNumberOfSplits, Object bagFraction, Object minExamplesInNode) {
		 this(new FilterableProperty[] {FilterableProperty.MinLearningRate, FilterableProperty.MaxLearningRate, FilterableProperty.MaxNumberOfSplits, FilterableProperty.BagFraction, FilterableProperty.MinExamplesInNode},
					new Object[] {minLR, maxLR, maxNumberOfSplits, bagFraction, minExamplesInNode});
	}
	
	public String getLongFilterDescription() {
		StringBuffer description = new StringBuffer();
		for (Map.Entry<FilterableProperty, Object> filterEntry : filter.entrySet()) {
			if (description.length() > 0) {
				description.append(" and ");
			}
			description.append(filterEntry.getKey().name() + "=" + filterEntry.getValue().toString());
		}
		return description.toString();
	}
	
	public String getMinimalFilterDescription() {
		StringBuffer description = new StringBuffer();
		for (Map.Entry<FilterableProperty, Object> filterEntry : filter.entrySet()) {
			if (description.length() > 0) {
				description.append("and");
			}
			description.append(getMinimizedPropertyName(filterEntry.getKey()) + "=" + filterEntry.getValue().toString());
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
	
	public String getSubDirectory() {
		StringBuffer buffer = new StringBuffer();
		TreeSet<FilterableProperty> sortedKeys = new TreeSet<>(filter.keySet());
		for (FilterableProperty property : sortedKeys) {
			Object value = filter.get(property);
			double doubleValue = Double.NaN;
			try {
				doubleValue = (double)value;
			} catch (ClassCastException e) {}
			String stringValue = value.toString();
			if (!Double.isNaN(doubleValue)) {
				stringValue = String.format("%f", doubleValue);
			}
			buffer.append(getMinimizedPropertyName(property) + "-" + stringValue + "/");
		}
		return buffer.toString();
	}
	
	private static String getMinimizedPropertyName(FilterableProperty property) {
		switch(property) {
			case BagFraction:
				return "BF";
			case LearningRatePolicy:
				return "LRPolicy";
			case MaxLearningRate:
				return "MaxLR";
			case MaxNumberOfSplits:
				return "Splits";
			case MinExamplesInNode:
				return "MEIN";
			case MinLearningRate:
				return "MinLR";
			default:
				throw new IllegalArgumentException();
		
		}
	}
	
	private static boolean doesRecordMatchFilter(RunDataSummaryRecord record, Map.Entry<FilterableProperty, Object> filterEntry) {
		try {
			String cast = (String)filterEntry.getValue();
			if (cast.equals("ALL")) {
				return true;
			} else {
				throw new IllegalArgumentException("Only String filter value supported is \"All\"");
			}
		} catch (ClassCastException e) {
			// No Problem keep checking.
		}
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
				return record.parameters.maxNumberOfSplits == (int)filterEntry.getValue();
			case MinExamplesInNode:
				return record.parameters.minExamplesInNode == (int)filterEntry.getValue();
		}
		System.err.println(StopWatch.getDateTimeStamp() + "ERROR: Shouldn't reach here in RunDataSummaryRecord.doesRecordMatchFilter");
		return false;
	}
	
	private static boolean doPropertiesMatch(FilterableProperty property, Object value1, Object value2) {
		String cast1 = null, cast2 = null;
		try {
			cast1 = (String)value1;
		} catch (ClassCastException e) {
			cast1 = null;
		}
		try {
			cast2 = (String)value2;
		} catch (ClassCastException e) {
			cast2 = null;
		}
		if (cast1 != null || cast2 != null) {
			return cast1.equals(cast2);
		}
	
		switch (property) {
			case BagFraction:
				return DoubleCompare.equals((double)value1, (double)value2);
			case LearningRatePolicy:
				return (LearningRatePolicy)value1 == (LearningRatePolicy)value2;
			case MaxLearningRate:
				return DoubleCompare.equals((double)value1, (double)value2);
			case MinLearningRate:
				return DoubleCompare.equals((double)value1, (double)value2);
			case MaxNumberOfSplits:
				return (int)value1 == (int)value2;
			case MinExamplesInNode:
				return (int)value1 == (int)value2;
		}
		System.err.println(StopWatch.getDateTimeStamp() + "ERROR: Shouldn't reach here in RunDataSummaryRecord.doesRecordMatchFilter");
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
	
	public static HashSet<RunDataSummaryRecordFilter> getAllPossibleFiltersForSpecifiedParameters(ParameterTuningParameters tuningParameters, GbmParameters parameters) {
		HashSet<RunDataSummaryRecordFilter> filters = new HashSet<RunDataSummaryRecordFilter>();
		Set<Double> possibleMaxLearningRates = new TreeSet<Double>();
		// both constant and variable use this field, so want to account for all of them. 
		for (double lr : tuningParameters.constantLearningRates) {
			possibleMaxLearningRates.add(lr);
		}
		for (double lr : tuningParameters.maxLearningRates) {
			possibleMaxLearningRates.add(lr);
		}
		Object minLR, maxLR, numberOfSplits, bagFraction, minExamplesInNode;
		if (parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
			minLR = 0.0;
		} else {
			minLR = parameters.minLearningRate;
		}
		maxLR = parameters.maxLearningRate;
		numberOfSplits = parameters.maxNumberOfSplits;
		bagFraction = parameters.bagFraction;
		minExamplesInNode = parameters.minExamplesInNode;
		
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", "ALL", "ALL"));
		
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", "ALL", "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", "ALL", "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, "ALL", "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", "ALL", minExamplesInNode));
		
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, "ALL", "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, "ALL", "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", "ALL", "ALL"));
		
		filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, "ALL", "ALL"));
		
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, bagFraction, "ALL"));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, "ALL", minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, bagFraction, minExamplesInNode));
		filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, bagFraction, minExamplesInNode));
		
		return filters;
		
	}
	public static HashSet<RunDataSummaryRecordFilter> getAllPossibleFilters(ParameterTuningParameters tuningParameters) {
		HashSet<RunDataSummaryRecordFilter> filters = new HashSet<RunDataSummaryRecordFilter>();
		Set<Double> possibleMaxLearningRates = new TreeSet<Double>();
		// both constant and variable use this field, so want to account for all of them. 
		for (double lr : tuningParameters.constantLearningRates) {
			possibleMaxLearningRates.add(lr);
		}
		for (double lr : tuningParameters.maxLearningRates) {
			possibleMaxLearningRates.add(lr);
		}
		
		for (double minLR : tuningParameters.minLearningRates) {
			for (double maxLR : possibleMaxLearningRates) {
				for (int numberOfSplits : tuningParameters.maxNumberOfSplts) {
					for (double bagFraction : tuningParameters.bagFractions) {
						for (int minExamplesInNode : tuningParameters.minExamplesInNode) {
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", "ALL", "ALL"));
							
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", "ALL", "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", "ALL", "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, "ALL", "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", "ALL", minExamplesInNode));
							
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", "ALL", bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, "ALL", "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, "ALL", "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", "ALL", "ALL"));
							
							filters.add(new RunDataSummaryRecordFilter("ALL", "ALL", numberOfSplits, bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, "ALL", bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", "ALL", bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, "ALL", "ALL"));
							
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, bagFraction, "ALL"));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, numberOfSplits, "ALL", minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, maxLR, "ALL", bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter(minLR, "ALL", numberOfSplits, bagFraction, minExamplesInNode));
							filters.add(new RunDataSummaryRecordFilter("ALL", maxLR, numberOfSplits, bagFraction, minExamplesInNode));
						}
					}
				}
			}
		}
		return filters;
	}
}
