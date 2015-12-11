package parameterTuning.plotting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import parameterTuning.ParameterTuningParameters;
import parameterTuning.RunDataSummaryRecord;
import parameterTuning.RunDataSummaryRecordFilter;
import regressionTree.RegressionTree.LearningRatePolicy;
import utilities.CommandLineExecutor;
import utilities.MaxAndMin;
import utilities.SimpleHostLock;
import utilities.StopWatch;
import utilities.SumCountAverage;

public class AvgAcrossDatasetsRunDataSummaryRecordGraphGenerator {
	public enum GraphableProperty {
		TimeInSeconds("Running Time"), AllDataTestError ("All Training Data GBM RMSE"), CvEnsembleTestError ("Aggregated Boosted Tree RMSE"), CvValidationError ("Cross Validation RMSE"), 
		OptimalNumberOfTrees("Optimal Number of Trees"), AvgNumberOfSplits("Avg. Number of Splits"), StdDevNumberOfSplits("Number of Splits Std. Dev"), AvgLearningRate("Avg. Shrinkage"), StdDevLearningRate("Shrinkage Std. Dev."),  
		MinLearningRate("Min Shrinkage"), MaxLearningRate("Max Shrinkage"), ConstantLearningRate("Constant Shrinkage"), MaxNumberOfSplits("Max Number of Splits"), BagFraction("Bag Fraction"), MinExamplesInNode("Min Leaf Size");
		
		String niceName = null;
		GraphableProperty(String niceName) {
			this.niceName = niceName;
		}
		public String toString() {
			return niceName;
		}
	};
	
	public enum GraphType {UniquePoints, AllPoints};
	
	public enum AxesType {ExactMinAndMax, ExtraSpaceBeyondMinAndMax};
	
	public static ArrayList<GraphableProperty[]> getAllAxes() {
		GraphableProperty[] yAxes = getYAxes();
	
	
		ArrayList<GraphableProperty[]> graphAxes = new ArrayList<GraphableProperty[]> ();
		
			for (GraphableProperty y : yAxes) {
				graphAxes.addAll(getAxesWithSpecifiedYAxis(y));
		}
		
		graphAxes.addAll(getAdditionalAxes());
		return graphAxes;
	}
	
	public static GraphableProperty[] getXAxes() {
		return new GraphableProperty[] {

				GraphableProperty.MaxNumberOfSplits, 
				GraphableProperty.MinExamplesInNode,
				GraphableProperty.BagFraction, 
				GraphableProperty.ConstantLearningRate, 
				GraphableProperty.MinLearningRate, 
				GraphableProperty.MaxLearningRate};
	}
	
	public static GraphableProperty[] getYAxes() {
		return new GraphableProperty[] {GraphableProperty.TimeInSeconds, 
				//GraphableProperty.AllDataTestError, 
				//GraphableProperty.CvEnsembleTestError, 
				GraphableProperty.OptimalNumberOfTrees,
				GraphableProperty.CvValidationError
				};
	}
	
	public static ArrayList<GraphableProperty[]> getAdditionalAxes() {
		ArrayList<GraphableProperty[]> retval = new ArrayList<>();
		retval.add(new GraphableProperty[] {GraphableProperty.CvValidationError, GraphableProperty.AllDataTestError});
		retval.add(new GraphableProperty[] {GraphableProperty.CvValidationError, GraphableProperty.CvEnsembleTestError});
		//retval.add(new GraphableProperty[] {GraphableProperty.AllDataTestError, GraphableProperty.CvEnsembleTestError});
		//retval.add(new GraphableProperty[] {GraphableProperty.CvValidationError, GraphableProperty.CvValidationError});
		return retval;
	}
	
	public static ArrayList<GraphableProperty[]> getAxesWithSpecifiedYAxis(GraphableProperty y) {
		GraphableProperty[] xAxes = getXAxes();
	
		ArrayList<GraphableProperty[]> graphAxes = new ArrayList<GraphableProperty[]> ();
		for (GraphableProperty x : xAxes) {
			graphAxes.add(new GraphableProperty[] {x, y});
		}
		return graphAxes;
	}
	
	public static void generateAndSaveGraphsOfConstantVsVariableLR(ParameterTuningParameters tuningParameters, String runDataSubDirectory, int n) {
		ArrayList<RunDataSummaryRecord> allRecords = RunDataSummaryRecord.getAverageRecordsAcrossDatasets(tuningParameters, runDataSubDirectory);
		
		ExecutorService executor = Executors.newCachedThreadPool();
		
		String outputDirectory = tuningParameters.runDataProcessingDirectory + "avgSummaryGraphs/";

		ArrayList<GraphableProperty[]> graphAxes = getAllAxes();
		int submissionNumber = 0;
		int totalNumberOfTests = graphAxes.size();
		StopWatch globalTimer = new StopWatch().start();
		Queue<Future<Void>> futureQueue = new LinkedList<Future<Void>>();

		for (GraphableProperty[] axes : graphAxes) {
			futureQueue.add(executor.submit(
					new RunDataSummaryGraphTask(tuningParameters, allRecords, null, AxesType.ExtraSpaceBeyondMinAndMax, outputDirectory, submissionNumber, totalNumberOfTests, globalTimer, axes)));
			
			if (futureQueue.size() >= 8) {
				System.out.println(StopWatch.getDateTimeStamp() + "Reached 8 run data summary graph threads, waiting for some to finish");
				while (futureQueue.size() > 4) {
					try {
						futureQueue.poll().get();

					} catch (InterruptedException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					} catch (ExecutionException e) {
						System.err.println(StopWatch.getDateTimeStamp());
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Submitted the last of the run data summary graph jobs, just waiting until they are all done.");
		while (!futureQueue.isEmpty()) {
			try {
				futureQueue.poll().get();
			} catch (InterruptedException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
			}
		}
		System.out.println(StopWatch.getDateTimeStamp() + "Finished generating run data summary graph for all filters and axes.");
		executor.shutdownNow();
	}
	
	private static class RunDataSummaryGraphTask implements Callable<Void>{
		ParameterTuningParameters tuningParameters;
		List<RunDataSummaryRecord> allRecords; 
		RunDataSummaryRecordFilter filter;
		AxesType axesType;
		String outputDirectory;
		GraphableProperty[] axes;
		int submissionNumber;
		int totalNumberOfTests;
		StopWatch globalTimer;
		
		public RunDataSummaryGraphTask(ParameterTuningParameters tuningParameters, 
				List<RunDataSummaryRecord> allRecords, 
				RunDataSummaryRecordFilter filter,
				AxesType axesType,
				String outputDirectory,
				int submissionNumber,
				int totalNumberOfTests,
				StopWatch globalTimer,
				GraphableProperty... axes) {
			this.tuningParameters = tuningParameters;
			this.allRecords = allRecords;
			this.filter = filter;
			this.axesType = axesType;
			this.outputDirectory = outputDirectory;
			this.axes = axes;
			this.submissionNumber = submissionNumber;
			this.totalNumberOfTests = totalNumberOfTests;
			this.globalTimer = globalTimer;
		}
		
		@Override
		public Void call() {
			StopWatch timer = new StopWatch().start();
			String testSubDirectory = ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
			String locksDir = tuningParameters.locksDirectory + "/RunDataSummaryGraphs/" + testSubDirectory;
			new File(locksDir).mkdirs();
			if (SimpleHostLock.checkDoneLock(locksDir + "runDataSummaryGraphLock.txt")) {
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[AVERAGED] Already generated run data summary graph for %s (%d out of %d) in %.4f minutes. Have been running for %.4f minutes total."
						, testSubDirectory, submissionNumber, totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
				return null;
			}
			
			
			if (axes.length < 2 || axes.length > 3) {
				System.out.println(StopWatch.getDateTimeStamp() + "Only defined for 2D or 3D graphs");
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
					combinedUniquePointsPlotCode = "", combinedUniquePointsLatexCode = "",
					constantAllPointsDataListCode = "", constantAllPointsPlotCode = "", constantAllPointsLatexCode = "",
					variableAllPointsDataListCode = "", variableAllPointsPlotCode = "", variableAllPointsLatexCode = "",
					combinedAllPointsPlotCode = "", combinedAllPointsLatexCode = "";
			boolean constantRecordsExist = !constantRecords.isEmpty() && Arrays.binarySearch(axes, GraphableProperty.MinLearningRate) < 0 && Arrays.binarySearch(axes, GraphableProperty.MaxLearningRate) < 0;
			boolean variableRecordsExist = !variableRecords.isEmpty() && Arrays.binarySearch(axes, GraphableProperty.ConstantLearningRate) < 0;
			if (constantRecordsExist) {
				constantUniquePointsDataListCode = getMathematicaDataListCode(LearningRatePolicy.CONSTANT, GraphType.UniquePoints, constantRecords, axes);
				constantUniquePointsPlotCode = getMathematicaPlotCode(tuningParameters, LearningRatePolicy.CONSTANT, axesType, GraphType.UniquePoints, constantRecords, filter, outputDirectory, axes);
				constantUniquePointsLatexCode = getLatexCode(LearningRatePolicy.CONSTANT, GraphType.UniquePoints, filter, outputDirectory, axes);
				constantAllPointsDataListCode = getMathematicaDataListCode(LearningRatePolicy.CONSTANT, GraphType.AllPoints, constantRecords, axes);
				constantAllPointsPlotCode = getMathematicaPlotCode(tuningParameters, LearningRatePolicy.CONSTANT, axesType, GraphType.AllPoints, constantRecords, filter, outputDirectory, axes);
				constantAllPointsLatexCode = getLatexCode(LearningRatePolicy.CONSTANT, GraphType.AllPoints, filter, outputDirectory, axes);
			}
			if (variableRecordsExist) {
				variableUniquePointsDataListCode = getMathematicaDataListCode(LearningRatePolicy.REVISED_VARIABLE, GraphType.UniquePoints, variableRecords, axes);
				variableUniquePointsPlotCode = getMathematicaPlotCode(tuningParameters, LearningRatePolicy.REVISED_VARIABLE, axesType, GraphType.UniquePoints, variableRecords, filter, outputDirectory, axes);
				variableUniquePointsLatexCode = getLatexCode(LearningRatePolicy.REVISED_VARIABLE, GraphType.UniquePoints, filter, outputDirectory, axes);
				variableAllPointsDataListCode = getMathematicaDataListCode(LearningRatePolicy.REVISED_VARIABLE, GraphType.AllPoints, variableRecords, axes);
				variableAllPointsPlotCode = getMathematicaPlotCode(tuningParameters, LearningRatePolicy.REVISED_VARIABLE, axesType, GraphType.AllPoints, variableRecords, filter, outputDirectory, axes);
				variableAllPointsLatexCode = getLatexCode(LearningRatePolicy.REVISED_VARIABLE, GraphType.AllPoints, filter, outputDirectory, axes);
			}
			if (constantRecordsExist && variableRecordsExist) {
				combinedAllPointsPlotCode = getMathematicaCombinedPlotCode(tuningParameters, axesType, GraphType.AllPoints, constantRecords, variableRecords, filter, outputDirectory, axes);
				//combinedAllPointsLatexCode = getLatexCode(datasetParameters, null, GraphType.AllPoints, filter, outputDirectory, axes);
				combinedUniquePointsPlotCode = getMathematicaCombinedPlotCode(tuningParameters, axesType, GraphType.UniquePoints, constantRecords, variableRecords, filter, outputDirectory, axes);
				//combinedUniquePointsLatexCode = getLatexCode(datasetParameters, null, GraphType.UniquePoints, filter, outputDirectory, axes);
			}
			
			if (!variableRecordsExist && !constantRecordsExist) {
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[AVERAGED] No records exist in the run data summary graph for %s (%d out of %d) in %.4f minutes. Have been running for %.4f minutes total.", 
						testSubDirectory, submissionNumber, totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
				SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryGraphLock.txt");
				return null;
			} 
			
			if (constantUniquePointsDataListCode == null || constantAllPointsDataListCode == null) {
				System.out.println(StopWatch.getDateTimeStamp() + "Skipping the constant graphs of " + filter.getSubDirectory() + convertGraphablePropertyAxesArrayToMinimalString(axes) + 
				" because only 1 unique X or Y value exists.");
				constantRecordsExist = false;
			}
			if (variableUniquePointsDataListCode == null || variableAllPointsDataListCode == null) {
				System.out.println(StopWatch.getDateTimeStamp() + "Skipping the variable graphs of " + filter.getSubDirectory() + convertGraphablePropertyAxesArrayToMinimalString(axes) + 
				" because only 1 unique X or Y value exists.");
				variableRecordsExist = false;
			}
			if (!variableRecordsExist && !constantRecordsExist) {
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[AVERAGED]Both constant and variable graphs would have been pointless so skipping the run data summary graph for %s (%d out of %d) in %.4f minutes. Have been running for %.4f minutes total.", 
						testSubDirectory, submissionNumber, totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
				SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryGraphLock.txt");
				return null;
			} 
			
			String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
			String mathematicaFilePath = fileDirectory + getNotebookDataFileName(filter, axes);
			String latexFilePath = fileDirectory + getLatexCodeFileName(filter, axes);
			try {
				new File(fileDirectory).mkdirs();
				BufferedWriter mathematica = new BufferedWriter(new PrintWriter(new File(mathematicaFilePath)));
				BufferedWriter latexCodeWriter = new BufferedWriter(new PrintWriter(new File(latexFilePath)));
				if (constantRecordsExist) {
					mathematica.write(constantUniquePointsDataListCode + "\n" + constantAllPointsDataListCode + "\n\n" + 
							constantUniquePointsPlotCode + "\n" + constantAllPointsPlotCode + "\n\n");
				}
				if (variableRecordsExist) {
					mathematica.write(variableUniquePointsDataListCode + "\n" + variableAllPointsDataListCode + "\n\n" + 
						variableUniquePointsPlotCode + "\n" + variableAllPointsPlotCode + "\n\n");
				}
				if (constantRecordsExist && variableRecordsExist) {
					mathematica.write(combinedUniquePointsPlotCode + "\n" + combinedAllPointsPlotCode + "\n\n");
				}
				
				if (constantRecordsExist) {
					latexCodeWriter.write(constantUniquePointsLatexCode + "\n" + constantAllPointsLatexCode + "\n\n");
				}
				if (variableRecordsExist) {
					latexCodeWriter.write(variableUniquePointsLatexCode + "\n" + variableAllPointsLatexCode);
				}
				
				
				latexCodeWriter.flush();
				latexCodeWriter.close();
				mathematica.flush();
				mathematica.close();
			} catch (Exception e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
				System.exit(1);
			}
			
			try {
				StopWatch mathematicaCurveTimer = new StopWatch().start();
				mathematicaCurveTimer.printMessageWithTime("Starting execution of " + mathematicaFilePath);
				CommandLineExecutor.runProgramAndWaitForItToComplete(fileDirectory, new String[] {"math", "-script", getNotebookDataFileName(filter, axes)});
				//RecursiveFileDeleter.deleteDirectory(new File(mathematicaFilePath));
				mathematicaCurveTimer.printMessageWithTime("Finished execution of " + mathematicaFilePath);
			} catch (Exception e) {
				System.err.println(StopWatch.getDateTimeStamp());
				e.printStackTrace();
				System.out.println(StopWatch.getDateTimeStamp() + String.format("[AVERAGED] Failed to execute the mathematica code for the run data summary graph for %s, not writing done lock. (%d out of %d) in %.4f minutes. Have been running for %.4f minutes total.", 
						testSubDirectory, submissionNumber, totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
				return null;
			}
			System.out.println(StopWatch.getDateTimeStamp() + String.format("[AVERAGED] Successfully generated the run data summary graph for %s (%d out of %d) in %.4f minutes. Have been running for %.4f minutes total.", 
					testSubDirectory, submissionNumber, totalNumberOfTests, timer.getElapsedMinutes(), globalTimer.getElapsedMinutes()));
			SimpleHostLock.writeDoneLock(locksDir + "runDataSummaryGraphLock.txt");
			return null;
		}
	}
	
	private static String getMathematicaDataListCode( 
			LearningRatePolicy learningRatePolicy, 
			GraphType graphType,
			List<RunDataSummaryRecord> filteredRecords, 
			GraphableProperty[] axes) {
		
		String dataListVariableName = getDataListVariableName(learningRatePolicy, graphType, axes);
		TreeSet<Point> points = getPoints(graphType, filteredRecords, axes);
		int numberOfUniquePoints = countNumberOfUniqueXAndYValues(points);
		
		// Theres no value in graphing this.
		if (numberOfUniquePoints <= 1) {
			return null;
		}
		
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append(dataListVariableName + " = {");
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
	
	private static String getMathematicaCombinedPlotCode(
			ParameterTuningParameters tuningParameters, 
			AxesType axesType,
			GraphType graphType, 
			List<RunDataSummaryRecord> constantRecords, 
			List<RunDataSummaryRecord> variableRecords, 
			RunDataSummaryRecordFilter filter,
			String outputDirectory,
			GraphableProperty[] axes) {
		
		String plotRange = getPlotRangeOfCombinedPlot(axesType, constantRecords, variableRecords, axes);
		String ticks = getTicks(tuningParameters, axes);
		String frame = getFrame(axes);
		
		String plotVariableName = getPlotVariableName(null, graphType, axes);
		String constantDataListVariableName = getDataListVariableName(LearningRatePolicy.CONSTANT, graphType, axes);
		String variableDataListVariableName =getDataListVariableName(LearningRatePolicy.REVISED_VARIABLE, graphType, axes);
		StringBuffer buffer = new StringBuffer();

		String plotCommand = null;
		String extraCommands = "";
		if (axes.length == 2) {
			if (graphType == GraphType.UniquePoints) {
				plotCommand = "ListLinePlot";
			} else {
				plotCommand = "ListPlot";
			}
		} else {
			if (graphType == GraphType.UniquePoints) {
				plotCommand = "ListPlot3D";
			} else {
				plotCommand = "ListPointPlot3D";
				extraCommands = ", " + getCodeToDropPrettyLineDownFromListPointPlot3D();
			}
		}
		String plotRangePadding = "PlotRangePadding->{{Scaled[0.03],Scaled[0.03]}, {Scaled[0.03], Scaled[0.03]}}";
		String imageMargins = "ImageMargins->{{0,0},{5,5}}";
		buffer.append(String.format("%s = %s[%s, %s, %s, %s, %s, %s, %s, %s %s]\n", 
				plotVariableName, 
				plotCommand,
				("{" + constantDataListVariableName + ", " + variableDataListVariableName + "}"), 
				plotRange, 
				ticks, 
				"PlotStyle -> {{Red, Opacity[0.35]}, {Blue, Opacity[0.35]}}",
				"PlotMarkers -> {Automatic, Medium}",
				frame,
				plotRangePadding,
				imageMargins,
				extraCommands)
			);
		
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getPlotGraphFileName(null, graphType, filter, axes);
	
		buffer.append(String.format("%sFilePath = \"%s\"\n", plotVariableName, filePath));
		buffer.append(String.format("Export[%sFilePath, %s , ImageResolution -> 300]\n\n", plotVariableName, plotVariableName));
		

		return buffer.toString();
	}
	
	public static void generateAndExecutePlotLegend(ParameterTuningParameters tuningParameters) {
		String file = tuningParameters.runDataProcessingDirectory + "/runDataSummaryGraphLegend";
		
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter(file + ".m"));
			bw.append("runDataSummaryGraphLegend = PointLegend[{Red, Blue}, {\"Constant Shrinkage\", \"Variable Shrinkage\"}]\n\n");
			bw.append("fileName = \"" + file  + "\"\n");
			bw.append("Export[fileName <> \".png\", runDataSummaryGraphLegend, ImageResolution -> 300]\n\n");
			bw.flush();
			bw.close();
			CommandLineExecutor.executeMathematicaScript(tuningParameters.runDataProcessingDirectory, "runDataSummaryGraphLegend.m");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	private static String getMathematicaPlotCode(
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
		String frame = getFrame(axes);
		
		String plotVariableName = getPlotVariableName(learningRatePolicy, graphType, axes);
		String dataListVariableName = getDataListVariableName(learningRatePolicy, graphType, axes);

		StringBuffer buffer = new StringBuffer();

		String plotCommand = null;
		String extraCommands = "";
		if (axes.length == 2) {
			if (graphType == GraphType.UniquePoints) {
				plotCommand = "ListLinePlot";
			} else {
				plotCommand = "ListPlot";
			}
		} else {
			if (graphType == GraphType.UniquePoints) {
				plotCommand = "ListPlot3D";
			} else {
				plotCommand = "ListPointPlot3D";
				extraCommands = ", " + getCodeToDropPrettyLineDownFromListPointPlot3D();
			}
		}

		String plotStyle = "PlotStyle -> " + ((learningRatePolicy == LearningRatePolicy.CONSTANT) ? "{Red, Opacity[0.35]}" : "{Blue, Opacity[0.35]}");
		String plotRangePadding = "PlotRangePadding->{{Scaled[0.03],Scaled[0.03]}, {Scaled[0.03], Scaled[0.03]}}";
		String imageMargins = "ImageMargins->{{0,0},{5,5}}";
		buffer.append(String.format("%s = %s[%s, %s, %s, %s, %s, %s, %s, %s %s]\n", 
				plotVariableName, 
				plotCommand,
				dataListVariableName, 
				plotRange, 
				ticks, 
				plotStyle,
				frame,
				plotRangePadding,
				imageMargins,
				"PlotMarkers -> {Automatic, Medium}",
				extraCommands)
			);
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getPlotGraphFileName(learningRatePolicy, graphType, filter, axes);
	
		buffer.append(String.format("%sFilePath = \"%s\"\n", plotVariableName, filePath));
		buffer.append(String.format("Export[%sFilePath, %s , ImageResolution -> 300]\n\n", plotVariableName, plotVariableName));
		

		return buffer.toString();
	}
	
	private static String getCodeToDropPrettyLineDownFromListPointPlot3D() {
		return "Filling -> Bottom,  ColorFunction -> \"Rainbow\", BoxRatios -> 1, " +
				"FillingStyle -> Directive[LightGreen, Thick, Opacity[.5]],  ImageSize -> 400";
	}
	
	private static String getLatexCode(
			LearningRatePolicy learningRatePolicy, 
			GraphType graphType,
			RunDataSummaryRecordFilter filter,
			String outputDirectory,
			GraphableProperty[] axes) {
		StringBuffer buffer = new StringBuffer();
		String latexCaption = getLatexCaption(learningRatePolicy, filter, axes);
		String latexFigureId = getLatexFigureId(learningRatePolicy, graphType, filter, axes);
		
		String fileDirectory = outputDirectory + ((filter == null) ? "NoFilter/" : filter.getSubDirectory()) + convertGraphablePropertyAxesArrayToMinimalString(axes) + "/";
		String filePath = fileDirectory + getPlotGraphFileName(learningRatePolicy, graphType, filter, axes);
		
		buffer.append("\\begin{figure}[!htb]\\centering\n");
		buffer.append("\\includegraphics[width=1\\textwidth]{" + filePath + "}\n");
		buffer.append("\\caption{" + latexCaption + "}\n");
		buffer.append("\\label{fig:" + latexFigureId + "}\n");
		buffer.append("\\end{figure}\n\n");
		return buffer.toString();
	}
	
	private static String getDataListVariableName(LearningRatePolicy learningRatePolicy, GraphType graphType, GraphableProperty[] axes) {
		String learningRatePolicyNiceMinimalName = null;
		if (learningRatePolicy == null) {
			learningRatePolicyNiceMinimalName = "AllLR";
		} else {
			learningRatePolicyNiceMinimalName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "ConstantLR" : "VariableLR";
		}
		return learningRatePolicyNiceMinimalName + graphType.name() + convertGraphablePropertyAxesArrayToMinimalString(axes);
	}
	
	private static String getPlotVariableName(LearningRatePolicy learningRatePolicy, GraphType graphType, GraphableProperty[] axes) {
		return getDataListVariableName(learningRatePolicy, graphType, axes) + "Plot";
	}
	
	private static String getNotebookDataFileName(RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		//String filerDescription = (filter==null) ? "noFilter" : filter.getMinimalFilterDescription();
		return convertGraphablePropertyAxesArrayToMinimalString(axes) + "--NotebookData.m";
	}
	
	private static String getLatexCodeFileName( RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		//String filerDescription = (filter==null) ? "noFilter" : filter.getMinimalFilterDescription();
		return convertGraphablePropertyAxesArrayToMinimalString(axes) + "--LatexCode.txt";
	}
	
	private static String getPlotGraphFileName(LearningRatePolicy learningRatePolicy, GraphType graphType, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String learningRateReallyMinimalName = null;
		if (learningRatePolicy == null) {
			learningRateReallyMinimalName = "All";
		} else {
			learningRateReallyMinimalName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "Const" : "Var";
		}
		return learningRateReallyMinimalName + graphType.name() + ".png"; //getDataListVariableName(datasetParameters, learningRatePolicy, graphType, axes) + "--graph.png";
	}
	
	private static String getLatexFigureId(LearningRatePolicy learningRatePolicy, GraphType graphType, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String filerDescription = (filter==null) ? "noFilter" : filter.getMinimalFilterDescription();
		return  getDataListVariableName(learningRatePolicy, graphType, axes) + "--" + filerDescription;
	}
	
	private static String getPlotTitle(LearningRatePolicy learningRatePolicy, RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		String learningRatePolicyNiceName = (learningRatePolicy == LearningRatePolicy.CONSTANT) ? "Constant LR" : "Variable LR";
		String axesInfo = convertGraphablePropertyAxesArrayToNiceString(axes);
		String filterInfo = (filter == null) ? "" : filter.getLongFilterDescription();
		return learningRatePolicyNiceName + " " + axesInfo +  " " + filterInfo;
	}
	
	private static String getLatexCaption(LearningRatePolicy learningRatePolicy,  RunDataSummaryRecordFilter filter, GraphableProperty[] axes) {
		return getPlotTitle(learningRatePolicy, filter, axes);
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
	
	private static String getFrame(GraphableProperty[] axes) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Frame->True");
		buffer.append(", FrameStyle->Black");
		buffer.append(", FrameTicksStyle->Black");
		buffer.append(", LabelStyle->{Black, 12}");
		buffer.append(", FrameLabel->{");
		boolean first = true;
		
		for (GraphableProperty property : axes) {
			if (!first) {
				buffer.append(", ");
				
			} else {
				first = false;
			}
			buffer.append(String.format("\"%s\"", property.toString()));
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
		buffer.append("PlotRange->{All, {0,1}}");
		boolean first = true;

		/*
		for (GraphableProperty property : axes) {
			
			if (!first) {
				buffer.append(", ");
			}
			first = false;
			/*
			if (axesType == AxesType.ExactMinAndMax) {
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min, maxAndMinValueMap.get(property).max));
			} else {
				// Use imagePadding instead
				double extraSpace = 0;//(maxAndMinValueMap.get(property).max - maxAndMinValueMap.get(property).min) / 5;
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min - extraSpace, maxAndMinValueMap.get(property).max + extraSpace));
			}
			
			buffer.append("All");
		}
		buffer.append("}");
		*/
		return buffer.toString();
	}
	
	private static String getPlotRangeOfCombinedPlot(AxesType axesType, List<RunDataSummaryRecord> constantRecords, List<RunDataSummaryRecord> variableRecords, GraphableProperty[] axes) {
		HashMap<GraphableProperty, MaxAndMin> maxAndMinValueMap = new HashMap<>();
		for (GraphableProperty property : axes) {
			maxAndMinValueMap.put(property, findMaxAndMinValues(constantRecords, variableRecords, property));
		}
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("PlotRange->{All, {0,1}}");
		/*
		boolean first = true;

		/*
		for (GraphableProperty property : axes) {
			if (!first) {
				buffer.append(", ");
			}
			first = false;
			if (axesType == AxesType.ExactMinAndMax) {
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min, maxAndMinValueMap.get(property).max));
			} else {
				double extraSpace = 0;//(maxAndMinValueMap.get(property).max - maxAndMinValueMap.get(property).min) / 5;
				buffer.append(String.format("{%f, %f}", maxAndMinValueMap.get(property).min - extraSpace, maxAndMinValueMap.get(property).max + extraSpace));
			}
		}
		
		buffer.append("}");
		*/
		return buffer.toString();
	}
	
	private static String getTicks(ParameterTuningParameters parameters, GraphableProperty[] axes) {
		StringBuilder[] axesTicks = new StringBuilder[axes.length];
		for (int i = 0; i < axes.length; i++) {
			GraphableProperty property = axes[i];
			axesTicks[i] = new StringBuilder();
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
					axesTicks[i].append("Automatic");
					break;
				case BagFraction:
					axesTicks[i].append(convertDoubleArrayToCommaSeparatedTicksList(parameters.bagFractions));
					break;
				case MaxLearningRate:
					axesTicks[i].append(convertDoubleArrayToCommaSeparatedTicksList(parameters.maxLearningRates));
					break;
				case MinLearningRate:
					axesTicks[i].append(convertDoubleArrayToCommaSeparatedTicksList(parameters.minLearningRates));
					break;
				case ConstantLearningRate:
					axesTicks[i].append(convertDoubleArrayToCommaSeparatedTicksList(parameters.constantLearningRates));
					break;
				case MaxNumberOfSplits:
					axesTicks[i].append(convertIntArrayToCommaSeparatedTicksList(parameters.maxNumberOfSplts));
					break;
				case MinExamplesInNode:
					axesTicks[i].append(convertIntArrayToCommaSeparatedTicksList(parameters.minExamplesInNode));
					break;
				default:
					throw new IllegalArgumentException();
			}
		}
		
		// {Left, Right}, {Bottom, Top}
		StringBuilder builder = new StringBuilder();
		if (axesTicks.length == 2) {
			builder.append("FrameTicks->{ {" + axesTicks[1].toString() + ", None}, {" + axesTicks[0].toString() + ", None}}");
		} else {
			builder.append("FrameTicks->{ {" + axesTicks[1].toString() + ", None}, {" + axesTicks[0].toString() + ", None}}, {" + axesTicks[2].toString() + ", None}}");
		}

		return builder.toString();
	}
	
	public static String convertGraphablePropertyAxesArrayToMinimalString(GraphableProperty[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for (GraphableProperty val : array) {
			if (!first) {
				buffer.append("vs");
			}
			buffer.append(getMinimalPropertyName(val));
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
			buffer.append(val.toString());
			first = false;
		}
		return buffer.toString();
	}

	private static String convertDoubleArrayToCommaSeparatedTicksList(double[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append("{");
		double avgDist = (array[0] + array[array.length-1]) / (array.length * 4);
		double lastPrinted = Double.MIN_VALUE;
		for (double val : array) {
			// Trying to avoid collision
			if (lastPrinted + avgDist < val) {
				if (!first) {
					buffer.append(", ");
				}
				first = false;
				buffer.append((String.format("%f", val)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1"));
			}
		}
		buffer.append("}");
		return buffer.toString();
	}
	
	private static String convertIntArrayToCommaSeparatedTicksList(int[] array) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		buffer.append("{");
		double avgDist = (array[0] + array[array.length-1]) / (array.length * 4);
		double lastPrinted = Double.MIN_VALUE;
		for (int val : array) {
			// Trying to avoid collision
			if (lastPrinted + avgDist < val) {
				if (!first) {
					buffer.append(", ");
				}
				first = false;
				buffer.append(String.format("%d", val));
			}
		}
		buffer.append("}");
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
	

	
	private static MaxAndMin findMaxAndMinValues(List<RunDataSummaryRecord> constantRecords, List<RunDataSummaryRecord> variableRecords, GraphableProperty property) {
		MaxAndMin maxAndMin = new MaxAndMin();
		
		for (RunDataSummaryRecord record : constantRecords) {
			double value = getPropertyValue(record, property);
			if (value < maxAndMin.min) {
				maxAndMin.min = value;
			}
			if (value > maxAndMin.max) {
				maxAndMin.max = value;
			}
		}
		for (RunDataSummaryRecord record : variableRecords) {
			double value = getPropertyValue(record, property);
			if (value < maxAndMin.min) {
				maxAndMin.min = value;
			}
			if (value > maxAndMin.max) {
				maxAndMin.max = value;
			}
		}
		return maxAndMin;
	}
	
	private static MaxAndMin findMaxAndMinValues(List<RunDataSummaryRecord> records, GraphableProperty property) {
		MaxAndMin maxAndMin = new MaxAndMin();
		
		for (RunDataSummaryRecord record : records) {
			double value = getPropertyValue(record, property);
			if (value < maxAndMin.min) {
				maxAndMin.min = value;
			}
			if (value > maxAndMin.max) {
				maxAndMin.max = value;
			}
		}
		return maxAndMin;
	}
	
	public static String getMinimalPropertyName(GraphableProperty property) {
		switch(property) {
			case AllDataTestError:
				return "ADTE";
			case AvgLearningRate:
				return "AvgLR";
			case AvgNumberOfSplits:
				return "AvgSplits";
			case BagFraction:
				return "BF";
			case ConstantLearningRate:
				return "ConstantLR";
			case CvEnsembleTestError:
				return "CvEnsTestError";
			case CvValidationError:
				return "CvValidError";
			case MaxLearningRate:
				return "MaxLR";
			case MaxNumberOfSplits:
				return "MaxSplits";
			case MinExamplesInNode:
				return "MEIN";
			case MinLearningRate:
				return "MinLR";
			case OptimalNumberOfTrees:
				return "OptNOT";
			case StdDevLearningRate:
				return "StdDevLR";
			case StdDevNumberOfSplits:
				return "StdDevSplits";
			case TimeInSeconds:
				return "TIME";
			default:
				throw new IllegalArgumentException();
		}
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
				if (this.values[i] < that.values[i]) {
					return -1;
				}
				if (this.values[i] > that.values[i]) {
					return 1;
				}
			}
			return 0;
		}
	}
}
