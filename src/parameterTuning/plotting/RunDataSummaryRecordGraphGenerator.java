package parameterTuning.plotting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import parameterTuning.ParameterTuningParameters;
import parameterTuning.RunDataSummaryRecord;
import parameterTuning.RunDataSummaryRecordFilter;
import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.DoubleCompare;
import utilities.SimpleHostLock;
import utilities.SumCountAverage;
import dataset.DatasetParameters;

public class RunDataSummaryRecordGraphGenerator {
	public enum GraphableProperty {TimeInSeconds, AllDataTestError, CvEnsembleTestError, CvValidationError, 
		OptimalNumberOfTrees, AvgNumberOfSplits, StdDevNumberOfSplits, AvgLearningRate, StdDevLearningRate,  
		MinLearningRate, MaxLearningRate, ConstantLearningRate, MaxNumberOfSplits, BagFraction, MinExamplesInNode};
	
	public enum GraphType {UniquePoints, AllPoints};
	
	public enum AxesType {ExactMinAndMax, ExtraSpaceBeyondMinAndMax};
	
	public static void generateAndSaveAllGraphs(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, String runDataSubDirectory) {
		String locksDir = tuningParameters.locksDirectory + datasetParameters.minimalName + "/RunDataSummaryGraphs/";
		new File(locksDir).mkdirs();
		if (SimpleHostLock.checkDoneLock(locksDir + "runDataSummaryGraphLock.txt")) {
			System.out.println(String.format("[%s] Already Created RunDataSummaryGraphs", datasetParameters.minimalName));
			return;
		}
		
		// Read in all RunDataSummaryRecords
		String runDataDirectory = tuningParameters.runDataProcessingDirectory + datasetParameters.minimalName + runDataSubDirectory;
		ArrayList<RunDataSummaryRecord> allRecords = RunDataSummaryRecord.readRunDataSummaryRecords(datasetParameters.minimalName, runDataDirectory);
	
		String outputDirectory = runDataDirectory + "ParameterComparisonGraphs/";
		

		
		HashSet<RunDataSummaryRecordFilter> filters = RunDataSummaryRecordFilter.getAllPossibleFilters(tuningParameters);
		
		for (RunDataSummaryRecordFilter filter : filters) {
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MaxLearningRate, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MinLearningRate, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.ConstantLearningRate, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MaxNumberOfSplits, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.AvgNumberOfSplits, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MinExamplesInNode, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.BagFraction, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MaxNumberOfSplits, GraphableProperty.MinExamplesInNode, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MinLearningRate, GraphableProperty.MaxLearningRate, GraphableProperty.AllDataTestError);
			
			generateAndSaveGraph(datasetParameters, tuningParameters, allRecords, filter, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, 
					GraphableProperty.MaxNumberOfSplits, GraphableProperty.ConstantLearningRate, GraphableProperty.AllDataTestError);
		}
		SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryGraphLock.txt");
	}
	
	private static void generateAndSaveGraph(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, 
			List<RunDataSummaryRecord> allRecords, 
			RunDataSummaryRecordFilter filter,
			AxesType axesType,
			String outputDirectory,
			GraphableProperty... axes) {
		
		if (axes.length < 2 || axes.length > 3) {
			System.out.println("Only defined for 2D or 3D graphs");
		}
		
		List<RunDataSummaryRecord> filteredRecords = null;
		if (filter == null) {
			filteredRecords = allRecords;
		} else {
			filteredRecords = filter.filterRecordsOnParameterValue(allRecords);
		}
		
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(filteredRecords);
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(filteredRecords);
		
		String constantUniquePointsDataListCode = "", constantUniquePointsPlotCode = "", constantUniquePointsLatexCode = "",
				variableUniquePointsDataListCode = "", variableUniquePointsPlotCode = "", variableUniquePointsLatexCode = "",
				constantAllPointsDataListCode = "", constantAllPointsPlotCode = "", constantAllPointsLatexCode = "",
				variableAllPointsDataListCode = "", variableAllPointsPlotCode = "", variableAllPointsLatexCode = "";
		boolean constantRecordsExist = !constantRecords.isEmpty() && Arrays.binarySearch(axes, GraphableProperty.MinLearningRate) < 0 && Arrays.binarySearch(axes, GraphableProperty.MaxLearningRate) < 0;
		boolean variableRecordsExist = !variableRecords.isEmpty() && Arrays.binarySearch(axes, GraphableProperty.ConstantLearningRate) < 0;
		if (constantRecordsExist) {
			constantUniquePointsDataListCode = getMathematicaDataListCode(datasetParameters, LearningRatePolicy.CONSTANT, GraphType.UniquePoints, constantRecords, axes);
			constantUniquePointsPlotCode = getMathematicaPlotCode(datasetParameters, tuningParameters, LearningRatePolicy.CONSTANT, axesType, GraphType.UniquePoints, constantRecords, filter, outputDirectory, axes);
			constantUniquePointsLatexCode = getLatexCode(datasetParameters, LearningRatePolicy.CONSTANT, GraphType.UniquePoints, filter, outputDirectory, axes);
			constantAllPointsDataListCode = getMathematicaDataListCode(datasetParameters, LearningRatePolicy.CONSTANT, GraphType.AllPoints, constantRecords, axes);
			constantAllPointsPlotCode = getMathematicaPlotCode(datasetParameters, tuningParameters, LearningRatePolicy.CONSTANT, axesType, GraphType.AllPoints, constantRecords, filter, outputDirectory, axes);
			constantAllPointsLatexCode = getLatexCode(datasetParameters, LearningRatePolicy.CONSTANT, GraphType.AllPoints, filter, outputDirectory, axes);
		}
		if (variableRecordsExist) {
			variableUniquePointsDataListCode = getMathematicaDataListCode(datasetParameters, LearningRatePolicy.REVISED_VARIABLE, GraphType.UniquePoints, variableRecords, axes);
			variableUniquePointsPlotCode = getMathematicaPlotCode(datasetParameters, tuningParameters, LearningRatePolicy.REVISED_VARIABLE, axesType, GraphType.UniquePoints, variableRecords, filter, outputDirectory, axes);
			variableUniquePointsLatexCode = getLatexCode(datasetParameters, LearningRatePolicy.REVISED_VARIABLE, GraphType.UniquePoints, filter, outputDirectory, axes);
			variableAllPointsDataListCode = getMathematicaDataListCode(datasetParameters, LearningRatePolicy.REVISED_VARIABLE, GraphType.AllPoints, variableRecords, axes);
			variableAllPointsPlotCode = getMathematicaPlotCode(datasetParameters, tuningParameters, LearningRatePolicy.REVISED_VARIABLE, axesType, GraphType.AllPoints, variableRecords, filter, outputDirectory, axes);
			variableAllPointsLatexCode = getLatexCode(datasetParameters, LearningRatePolicy.REVISED_VARIABLE, GraphType.AllPoints, filter, outputDirectory, axes);
		}
		
		if (!variableRecordsExist && !constantRecordsExist) {
			System.out.println("No records exist mathing the filter " + filter.getLongFilterDescription());
			return;
		} 
		
		if (constantUniquePointsDataListCode == null || constantAllPointsDataListCode == null) {
			System.out.println("Skipping the constant graphs of " + filter.getSubDirectory() + convertGraphablePropertyAxesArrayToMinimalString(axes) + 
			" because only 1 unique X or Y value exists.");
			constantRecordsExist = false;
		}
		if (variableUniquePointsDataListCode == null || variableAllPointsDataListCode == null) {
			System.out.println("Skipping the variable graphs of " + filter.getSubDirectory() + convertGraphablePropertyAxesArrayToMinimalString(axes) + 
			" because only 1 unique X or Y value exists.");
			variableRecordsExist = false;
		}
		if (!variableRecordsExist && !constantRecordsExist) {
			System.out.println("Both constant and variable graphs would have been pointless so skipping " + filter.getSubDirectory() + convertGraphablePropertyAxesArrayToMinimalString(axes) + " entirely");
			return;
		} 
		
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getNotebookDataFileName(datasetParameters, filter, axes);
		try {
			new File(fileDirectory).mkdirs();
			BufferedWriter bw = new BufferedWriter(new PrintWriter(new File(filePath)));
			if (constantRecordsExist) {
				bw.write(constantUniquePointsDataListCode + "\n" + constantAllPointsDataListCode + "\n\n" + 
						constantUniquePointsPlotCode + "\n" + constantAllPointsPlotCode + "\n\n");
			}
			if (variableRecordsExist) {
				bw.write(variableUniquePointsDataListCode + "\n" + variableAllPointsDataListCode + "\n\n" + 
					variableUniquePointsPlotCode + "\n" + variableAllPointsPlotCode + "\n\n");
			}
			if (constantRecordsExist) {
				bw.write(constantUniquePointsLatexCode + "\n" + constantAllPointsLatexCode + "\n\n");
			}
			if (variableRecordsExist) {
				bw.write(variableUniquePointsLatexCode + "\n" + variableAllPointsLatexCode);
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static String getMathematicaDataListCode(DatasetParameters datasetParameters, 
			LearningRatePolicy learningRatePolicy, 
			GraphType graphType,
			List<RunDataSummaryRecord> filteredRecords, 
			GraphableProperty[] axes) {
		
		String dataListVariableName = getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes);
		TreeSet<Point> points = getPoints(graphType, filteredRecords, axes);
		int numberOfUniquePoints = countNumberOfUniqueXAndYValues(points);
		
		// Theres no value in graphing this.
		if (numberOfUniquePoints <= 1) {
			return null;
		}
		
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append(dataListVariableName + " := {");
		for (Point point : points) {
			if (!first) {
				buffer.append(", ");
			}
			buffer.append(point.getMathematicaListEntry());
			first = false;
		}
		buffer.append("}\n");
		return buffer.toString();
	}

	/** 
	 * Return the minimum of the number of unique x value and unique y values (only if 3D).
	 * There's no point in graphing a 2D graph with only one X value, or a 3D graph with only 1 X or only one Y value
	 * @param points
	 * @return
	 */
	
	private static int countNumberOfUniqueXAndYValues(TreeSet<Point> points) {
		HashSet<Double> xValues = new HashSet<Double>();
		HashSet<Double> yValues = new HashSet<Double>();
		
		boolean threeDimensional = points.first().values.length == 3;
		for (Point point : points) {
			xValues.add(point.values[0]);
		
			if (threeDimensional) {
				yValues.add(point.values[1]);
			}
		}
		int retval = xValues.size();
		if (threeDimensional) {
			retval = Math.min(retval, yValues.size());
		}
		return retval;
	}
	
	private static String getMathematicaPlotCode(DatasetParameters datasetParameters, 
			ParameterTuningParameters tuningParameters, 
			LearningRatePolicy learningRatePolicy, 
			AxesType axesType,
			GraphType graphType, 
			List<RunDataSummaryRecord> filteredRecords, 
			RunDataSummaryRecordFilter filter,
			String outputDirectory,
			GraphableProperty[] axes) {
		
		String plotRange = getPlotRange(axesType, filteredRecords, axes);
		String ticks = getTicks(tuningParameters, axes);
		String axesLabel = getAxesLabel(axes);
		
		String plotVariableName = getPlotVariableName(datasetParameters, learningRatePolicy, graphType, axes);
		String dataListVariableName = getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes);

		StringBuffer buffer = new StringBuffer();

		if (axes.length == 2) {
			if (graphType == GraphType.UniquePoints) {
				buffer.append(String.format("%s := ListLinePlot[%s, %s, %s, %s]\n", plotVariableName, dataListVariableName, plotRange, ticks, axesLabel));
				buffer.append(plotVariableName + "\n\n");
			} else {
				buffer.append(String.format("%s := ListPlot[%s, %s, %s, %s]\n", plotVariableName, dataListVariableName, plotRange, ticks, axesLabel));
				buffer.append(plotVariableName + "\n\n");
			}
			
		} else if (axes.length == 3){
			if (graphType == GraphType.UniquePoints) {
				buffer.append(String.format("%s := ListPlot3D[%s, %s, %s, %s, %s] \n", plotVariableName, dataListVariableName, plotRange, ticks, axesLabel, "ColorFunction -> \"Rainbow\""));
				buffer.append(plotVariableName + "\n\n");
			} else {
				buffer.append(String.format("%s := ListPointPlot3D[%s, %s, %s, %s, %s] \n", plotVariableName, dataListVariableName, plotRange, ticks, axesLabel, getCodeToDropPrettyLineDownFromListPointPlot3D()));
				buffer.append(plotVariableName + "\n\n");
			}
		} else {
			throw new IllegalArgumentException();
		}
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getPlotGraphFileName(datasetParameters, learningRatePolicy, graphType, filter, axes);
	
		buffer.append(String.format("%sFilePath := \"%s\"\n", plotVariableName, filePath));
		buffer.append(String.format("Export[%sFilePath, %s , ImageResolution -> 300]\n\n", plotVariableName, plotVariableName));
		

		return buffer.toString();
	}
	
	private static String getCodeToDropPrettyLineDownFromListPointPlot3D() {
		return "Filling -> Bottom,  ColorFunction -> \"Rainbow\", BoxRatios -> 1, " +
				"FillingStyle -> Directive[LightGreen, Thick, Opacity[.5]],  ImageSize -> 400";
	}
	
	private static String getLatexCode(DatasetParameters datasetParameters, 
			LearningRatePolicy learningRatePolicy, 
			GraphType graphType,
			RunDataSummaryRecordFilter filter,
			String outputDirectory,
			GraphableProperty[] axes) {
		StringBuffer buffer = new StringBuffer();
		String latexCaption = getLatexCaption(datasetParameters, learningRatePolicy, filter, axes);
		String latexFigureId = getLatexFigureId(datasetParameters, learningRatePolicy, graphType, filter, axes);
		
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getPlotGraphFileName(datasetParameters, learningRatePolicy, graphType, filter, axes);
		
		buffer.append("\\begin{figure}[!htb]\\centering\n");
		buffer.append("\\includegraphics[width=1\\textwidth]{" + filePath + "}\n");
		buffer.append("\\caption{" + latexCaption + "}\n");
		buffer.append("\\label{fig:" + latexFigureId + "}\n");
		buffer.append("\\end{figure}\n\n");
		return buffer.toString();
	}
	
	private static String getDataListVariableName(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy, GraphType graphType, GraphableProperty[] axes) {
		String learningRatePolicyNiceMinimalName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "ConstantLR" : "VariableLR";
		return datasetParameters.minimalName + learningRatePolicyNiceMinimalName + graphType.name() + convertGraphablePropertyAxesArrayToMinimalString(axes);
	}
	
	private static String getPlotVariableName(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy, GraphType graphType, GraphableProperty[] axes) {
		return getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes) + "Plot";
	}
	
	private static String getNotebookDataFileName(DatasetParameters datasetParameters, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		//String filerDescription = (filter==null) ? "noFilter" : filter.getMinimalFilterDescription();
		return datasetParameters.minimalName + convertGraphablePropertyAxesArrayToMinimalString(axes) + "--NotebookData.txt";
	}
	
	private static String getPlotGraphFileName(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy, GraphType graphType, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String learningRateReallyMinimalName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "Const" : "Var";
		return learningRateReallyMinimalName + graphType.name() + ".png"; //getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes) + "--graph.png";
	}
	
	private static String getLatexFigureId(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy, GraphType graphType, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String filerDescription = (filter==null) ? "noFilter" : filter.getMinimalFilterDescription();
		return  getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes) + "--" + filerDescription;
	}
	
	private static String getPlotTitle(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String learningRatePolicyNiceName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "Constant LR" : "Variable LR";
		String axesInfo = convertGraphablePropertyAxesArrayToNiceString(axes);
		String filterInfo = (filter == null) ? "" : filter.getLongFilterDescription();
		return datasetParameters.minimalName + " " + learningRatePolicyNiceName + " " + axesInfo +  " " + filterInfo;
	}
	
	private static String getLatexCaption(DatasetParameters datasetParameters, LearningRatePolicy learningRatePolicy,  RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		return getPlotTitle(datasetParameters, learningRatePolicy, filter, axes);
	}
	
	private static TreeSet<Point> getPoints(GraphType graphType, List<RunDataSummaryRecord> allRecords, GraphableProperty[] axes) {
		TreeSet<Point> points = new TreeSet<Point>();
		
		if (graphType == GraphType.UniquePoints) {
			HashMap<UniqueXYPointKey, SumCountAverage> uniqueXYPointKeyToAvgZValueMap = new HashMap<>();
			
			for (RunDataSummaryRecord record : allRecords) {
				UniqueXYPointKey key = new UniqueXYPointKey(record, axes);
				SumCountAverage averageZValue = uniqueXYPointKeyToAvgZValueMap.get(key);
				if (averageZValue == null) {
					averageZValue = new SumCountAverage();
					uniqueXYPointKeyToAvgZValueMap.put(key, averageZValue);
				}
				averageZValue.addData(getPropertyValue(record, axes[axes.length-1]));
			}
			for (Map.Entry<UniqueXYPointKey, SumCountAverage> entry : uniqueXYPointKeyToAvgZValueMap.entrySet()) {
				points.add(new Point(entry.getKey(), entry.getValue()));
			}
		} else if (graphType == GraphType.AllPoints) {
			for (RunDataSummaryRecord record : allRecords) {
				points.add(new Point(record, axes));
			}
		} else {
			throw new IllegalArgumentException();
		}

		return points;
	}
	
	private static String getAxesLabel(GraphableProperty[] axes) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("AxesLabel->{");
		boolean first = true;
		
		for (GraphableProperty property : axes) {
			if (!first) {
				buffer.append(", ");
			}
			first = false;
			buffer.append(String.format("\"%s\"", property.name()));
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static String getPlotRange(AxesType axesType, List<RunDataSummaryRecord> records, GraphableProperty[] axes) {
		HashMap<GraphableProperty, MaxAndMin> maxAndMinValueMap = new HashMap<>();
		for (GraphableProperty property : axes) {
			maxAndMinValueMap.put(property, findMaxAndMinValues(records, property));
		}
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("PlotRange->{");
		boolean first = true;

		for (GraphableProperty property : axes) {
			if (!first) {
				buffer.append(", ");
			}
			first = false;
			if (axesType == AxesType.ExactMinAndMax) {
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min, maxAndMinValueMap.get(property).max));
			} else {
				double extraSpace = (maxAndMinValueMap.get(property).max - maxAndMinValueMap.get(property).min) / 5;
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min - extraSpace, maxAndMinValueMap.get(property).max + extraSpace));
			}
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static String getTicks(ParameterTuningParameters parameters, GraphableProperty[] axes) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Ticks->{");
		boolean first = true;

		for (GraphableProperty property : axes) {
			if (!first) {
				buffer.append(", ");
			}
			first = false;
			switch(property) {
				case AllDataTestError:
				case CvEnsembleTestError:
				case CvValidationError:
				case AvgLearningRate:
				case AvgNumberOfSplits:
				case StdDevLearningRate:
				case StdDevNumberOfSplits:
				case TimeInSeconds:
				case OptimalNumberOfTrees:
					buffer.append("Automatic");
					break;
				case BagFraction:
					buffer.append(convertDoubleArrayToCommaSeparatedList(parameters.bagFractions));
					break;
				case MaxLearningRate:
					buffer.append(convertDoubleArrayToCommaSeparatedList(parameters.maxLearningRates));
					break;
				case MinLearningRate:
					buffer.append(convertDoubleArrayToCommaSeparatedList(parameters.minLearningRates));
					break;
				case ConstantLearningRate:
					buffer.append(convertDoubleArrayToCommaSeparatedList(parameters.constantLearningRates));
					break;
				case MaxNumberOfSplits:
					buffer.append(convertIntArrayToCommaSeparatedList(parameters.maxNumberOfSplts));
					break;
				case MinExamplesInNode:
					buffer.append(convertIntArrayToCommaSeparatedList(parameters.minExamplesInNode));
					break;
				default:
					throw new IllegalArgumentException();
			}
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static String convertGraphablePropertyAxesArrayToMinimalString(GraphableProperty[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for (GraphableProperty val : array) {
			if (!first) {
				buffer.append("vs");
			}
			buffer.append(val.name());
			first = false;
		}
		return buffer.toString();
	}
	
	private static String convertGraphablePropertyAxesArrayToNiceString(GraphableProperty[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for (GraphableProperty val : array) {
			if (!first) {
				buffer.append(" vs. ");
			}
			buffer.append(val.name());
			first = false;
		}
		return buffer.toString();
	}

	private static String convertDoubleArrayToCommaSeparatedList(double[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append("{");
		for (double val : array) {
			if (!first) {
				buffer.append(", ");
			}
			buffer.append(String.format("%f", val));
			first = false;
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static String convertIntArrayToCommaSeparatedList(int[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append("{");
		for (int val : array) {
			if (!first) {
				buffer.append(", ");
			}
			buffer.append(String.format("%d", val));
			first = false;
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static MaxAndMin findMaxAndMinValues(List<RunDataSummaryRecord> records, GraphableProperty property) {
		MaxAndMin maxAndMin = new MaxAndMin();
		
		for (RunDataSummaryRecord record : records) {
			double value = getPropertyValue(record, property);
			if (DoubleCompare.lessThan(value, maxAndMin.min)) {
				maxAndMin.min = value;
			}
			if (DoubleCompare.greaterThan(value, maxAndMin.max)) {
				maxAndMin.max = value;
			}
		}
		return maxAndMin;
	}
	
	private static double getPropertyValue(RunDataSummaryRecord record, GraphableProperty property) {
		switch(property) {
			case AllDataTestError:
				return record.allDataTestError;
			case AvgLearningRate:
				return record.avgLearningRate;
			case AvgNumberOfSplits:
				return record.avgNumberOfSplits;
			case BagFraction:
				return record.parameters.bagFraction;
			case ConstantLearningRate:
				return record.parameters.maxLearningRate;
			case CvEnsembleTestError:
				return record.cvEnsembleTestError;
			case CvValidationError:
				return record.cvValidationError;
			case MaxLearningRate:
				return record.parameters.maxLearningRate;
			case MaxNumberOfSplits:
				return record.parameters.maxNumberOfSplits;
			case MinExamplesInNode:
				return record.parameters.minExamplesInNode;
			case MinLearningRate:
				return record.parameters.minLearningRate;
			case OptimalNumberOfTrees:
				return record.optimalNumberOfTrees;
			case StdDevLearningRate:
				return record.stdDevLearningRate;
			case StdDevNumberOfSplits:
				return record.stdDevNumberOfSplits;
			case TimeInSeconds:
				return record.timeInSeconds;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	
	private static class MaxAndMin {
		public double max = Double.MIN_VALUE;
		public double min = Double.MAX_VALUE;
	}
	
	private static class UniqueXYPointKey {
		public double[] XYvalues;
		public UniqueXYPointKey(RunDataSummaryRecord record, GraphableProperty[] axes) {
			XYvalues = new double[axes.length-1];
			
			for (int i = 0; i < XYvalues.length; i++) {
				XYvalues[i] = getPropertyValue(record, axes[i]);
			}
		}
		
		public UniqueXYPointKey(Point point) {
			XYvalues = new double[point.values.length-1];
			
			for (int i = 0; i < XYvalues.length; i++) {
				XYvalues[i] = point.values[i];
			}
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(XYvalues);
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
			UniqueXYPointKey other = (UniqueXYPointKey) obj;
			if (!Arrays.equals(XYvalues, other.XYvalues))
				return false;
			return true;
		}
	}
	
	private static class Point implements Comparable<Point> {
		private double[] values;
		
		public Point(RunDataSummaryRecord record, GraphableProperty[] axes) {
			values = new double[axes.length];
			for (int i = 0; i < axes.length; i++) {
				values[i] = getPropertyValue(record, axes[i]);
			}
		}
		
		public Point(UniqueXYPointKey key, SumCountAverage averageZValue) {
			values = new double[key.XYvalues.length + 1];
			for (int i = 0; i < key.XYvalues.length; i++) {
				values[i] = key.XYvalues[i];
			}
			values[key.XYvalues.length] = averageZValue.getMean();
		}
		
		public String getMathematicaListEntry() {
			return convertDoubleArrayToCommaSeparatedList(values);
		}

		@Override
		public int compareTo(Point that) {
			for (int i = 0; i < this.values.length; i++) {
				if (DoubleCompare.lessThan(this.values[i], that.values[i])) {
					return -1;
				}
				if (DoubleCompare.greaterThan(this.values[i], that.values[i])) {
					return 1;
				}
			}
			return 0;
		}
	}
}