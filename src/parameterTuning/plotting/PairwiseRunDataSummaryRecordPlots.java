package parameterTuning.plotting;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import parameterTuning.RunDataSummaryRecord;
import parameterTuning.RunDataSummaryRecordFilter;
import parameterTuning.plotting.ListEntryGetter.MaxLR_MinLR_ADTE_ListEntryGetter;
import regressionTree.RegressionTree.LearningRatePolicy;


public class PairwiseRunDataSummaryRecordPlots {

	public static void main(String[] args) {
		generatePairwiseRunDataSummaryRecordPlots("sfsdfs", "Z:/GBMWithVariableShrinkage/parameterTuning/4/nasa/Averages/Old/");
	}

	public static void generatePairwiseRunDataSummaryRecordPlots(String datasetName, String paramTuneDir) {
		ArrayList<RunDataSummaryRecord> records = RunDataSummaryRecord.readRunDataSummaryRecords(datasetName, paramTuneDir);
		/*
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.NOT_CVE_ListEntryGetter(), null);
		
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_TIME_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_TIME_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_TIME_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_TIME_ListEntryGetter(), null);
		
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_NOT_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_NOT_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_NOT_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_NOT_ListEntryGetter(), null);
		
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_CVE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_CVE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_CVE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_CVE_ListEntryGetter(), null);
		
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_ADTE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_ADTE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_ADTE_ListEntryGetter(), null);
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_ADTE_ListEntryGetter(), null);
		*/
		
		RunDataSummaryRecordFilter filter = new RunDataSummaryRecordFilter(
				RunDataSummaryRecordFilter.bagFractionEqualsThreeQuartersFilter, 
				RunDataSummaryRecordFilter.examplesInNodeEquals1Filter,
				RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable
				/*RunDataSummaryRecordFilter.maxSplitsEquals128Filter*/);
		
		plotStuffFromRunDataSummaryRecords(datasetName, paramTuneDir, records, MaxLR_MinLR_ADTE_ListEntryGetter.class, filter);
	}
	
	public static void plotStuffFromRunDataSummaryRecords(String datasetName, String paramTuningDir, List<RunDataSummaryRecord> allRecords, 
			Class<?> ListEntryGetterClass, RunDataSummaryRecordFilter filter) {
		List<RunDataSummaryRecord> filteredRecords = filter.filterRecordsOnParameterValue(allRecords);
		Constructor<?> ctor;
		ListEntryGetter constantEntryGetter = null, variableEntryGetter = null;
	
		try {
			ctor = ListEntryGetterClass.getConstructor();
			constantEntryGetter = (ListEntryGetter) ctor.newInstance();
			variableEntryGetter = (ListEntryGetter) ctor.newInstance();
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		
		List<RunDataSummaryRecord> constantRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsConstant.filterRecordsOnParameterValue(filteredRecords);
		List<RunDataSummaryRecord> variableRecords = RunDataSummaryRecordFilter.learningRatePolicyEqualsRevisedVariable.filterRecordsOnParameterValue(filteredRecords);
		
		String pairWiseGraphsDirectory = (paramTuningDir + "pairWiseGraphs/").replace("\\", "/");
		
		StringBuffer constantBuffer = new StringBuffer();
		StringBuffer variableBuffer = new StringBuffer();
		StringBuffer latexCodeBuffer = new StringBuffer();
		
		if (!constantRecords.isEmpty()) {
			constantEntryGetter.setMinsAndMaxes(constantRecords);
			buildDataListAndPlotString(datasetName, "ConstantLR", pairWiseGraphsDirectory, constantBuffer, constantRecords, constantEntryGetter, latexCodeBuffer, filter);
		}
		if (!variableRecords.isEmpty()) {
			variableEntryGetter.setMinsAndMaxes(variableRecords);
			buildDataListAndPlotString(datasetName, "VariableLR", pairWiseGraphsDirectory, variableBuffer, variableRecords, variableEntryGetter, latexCodeBuffer, filter);
			RunDataSummaryRecord.saveRunDataSummaryRecords(paramTuningDir, "sdfasdfsdfs", variableRecords);
		}
	
		try {
			new File(pairWiseGraphsDirectory).mkdirs();
			BufferedWriter file = new BufferedWriter(new PrintWriter(new File(pairWiseGraphsDirectory + constantEntryGetter.getPlotFileNameNoExtension(datasetName, "") +  filter.getMinimalFilterDescription() + ".txt")));
			file.write( constantBuffer.toString() + variableBuffer.toString() + latexCodeBuffer.toString());
			file.flush();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void buildDataListAndPlotString(String datasetName, String plotNamePrefix, String pairWiseGraphsDirectory, 
			StringBuffer buffer, List<RunDataSummaryRecord> records, ListEntryGetter entryGetter, StringBuffer latexCodeBuffer,
			RunDataSummaryRecordFilter filter ) {
		boolean firstEntryWritten = false;

		buffer.append(entryGetter.getMathematicaVeriableName(datasetName, plotNamePrefix) + " := {");
		
		for (RunDataSummaryRecord record : records) {
			String entry = entryGetter.getEntry(record);
			if (firstEntryWritten) {
				buffer.append(", ");
			} 
			buffer.append("\n\t" + entry );
			firstEntryWritten = true;
		}
		buffer.append("\n}\n");
		
		if (!entryGetter.is3D()) {
			buffer.append(plotNamePrefix + "Plot := ListPlot[" + entryGetter.getMathematicaVeriableName(datasetName, plotNamePrefix)
					//+ ", DataRange -> \"All\""
					//+ ", PlotRange -> \"All\""
					+ ", PlotRange -> {{" + entryGetter.getXMin() + ", " + entryGetter.getXMax() + "}, {" + entryGetter.getYMin() + ", " + entryGetter.getYMax() + "}}"
					//+ ", ColorFunction -> \"Rainbow\""
					+ ", AxesLabel->{\"" + entryGetter.getXLabel() + "\", \"" + entryGetter.getYLabel() + "\"}"
					//+ ", PlotLabel->\"" + entryGetter.getPlotTitle(datasetName, "ConstantLR") + "\""
					+ "] \nconstantPlot\n\n");
		} else {
			buffer.append(plotNamePrefix + "Plot := ListPlot3D[" + entryGetter.getMathematicaVeriableName(datasetName, plotNamePrefix)
					+ ", PlotRange -> {{" + entryGetter.getXMin() + ", " + entryGetter.getXMax() + "}, "
									+ "{" + entryGetter.getYMin() + ", " + entryGetter.getYMax() + "}, "
									+ "{" + entryGetter.getZMin() + ", " + entryGetter.getZMax() + "}}"
					+ ", AxesLabel->{\"" + entryGetter.getXLabel() + "\", \"" + entryGetter.getYLabel() + "\", \"" + entryGetter.getZLabel() + "\"}"
					+ "] \n" + plotNamePrefix + "Plot\n\n");
		}
		
		String fileName = pairWiseGraphsDirectory + entryGetter.getPlotFileNameNoExtension(datasetName, plotNamePrefix) + filter.getMinimalFilterDescription() ;
		buffer.append(plotNamePrefix + "PlotFileName := \"" + fileName + "\"\n");
		buffer.append("Export[" + plotNamePrefix + "PlotFileName <> \".png\", " + plotNamePrefix + "Plot , ImageResolution -> 300]\n\n");
		
		latexCodeBuffer.append("\\begin{figure}[!htb]\\centering\n");
		latexCodeBuffer.append("\\includegraphics[width=1\\textwidth]{" + fileName + "}\n");
		latexCodeBuffer.append("\\caption{" + entryGetter.getLatexCaption(datasetName, plotNamePrefix) + filter.getLongFilterDescription() + "}\n");
		latexCodeBuffer.append("\\label{fig:" +  entryGetter.getPlotFileNameNoExtension(datasetName, plotNamePrefix) + filter.getMinimalFilterDescription()+ "}\n");
		latexCodeBuffer.append("\\end{figure}\n\n");
		return;
	}
}
