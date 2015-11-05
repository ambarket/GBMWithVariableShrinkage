package parameterTuning.plotting;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import parameterTuning.OptimalParameterRecord;
import regressionTree.RegressionTree.LearningRatePolicy;


public class PairwiseOptimalParameterRecordPlots {
	public static void generatePairwiseOptimalParameterRecordPlots(String datasetName, String paramTuneDir, ArrayList<OptimalParameterRecord> records) {
		ListEntryGetter.setMinsAndMaxes(records);
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.NOT_CVE_ListEntryGetter());
		
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_TIME_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_TIME_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_TIME_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_TIME_ListEntryGetter());
		
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_NOT_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_NOT_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_NOT_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_NOT_ListEntryGetter());
		
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_CVE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_CVE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_CVE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_CVE_ListEntryGetter());
		
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.LR_ADTE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.BF_ADTE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.SPLITS_ADTE_ListEntryGetter());
		plotStuffFromOptimalParameterRecords(datasetName, paramTuneDir, records, new ListEntryGetter.MEIN_ADTE_ListEntryGetter());
	}
	
	public static void plotStuffFromOptimalParameterRecords(String datasetName, String paramTuningDir, ArrayList<OptimalParameterRecord> records, ListEntryGetter entryGetter) {
		StringBuffer constantBuffer = new StringBuffer();
		StringBuffer variableBuffer = new StringBuffer();
		constantBuffer.append(entryGetter.getMathematicaVeriableName(datasetName, "ConstantLR") + " := {");
		variableBuffer.append(entryGetter.getMathematicaVeriableName(datasetName, "VariableLR") + " := {");
		boolean firstConstantWritten = false, firstVariableWritten = false;
		for (int i = 0; i < records.size(); i++) {
			OptimalParameterRecord record = records.get(i);
			String entry = entryGetter.getEntry(record);
			if (record.parameters.learningRatePolicy == LearningRatePolicy.CONSTANT) {
				if (firstConstantWritten) {
					constantBuffer.append(", ");
				} 
				constantBuffer.append("\n\t" + entry );
				firstConstantWritten = true;
			} else {
				if (firstVariableWritten) {
					variableBuffer.append(", ");
				} 
				variableBuffer.append("\n\t" + entry );
				firstVariableWritten = true;
			}
		}
		constantBuffer.append("\n}\n");
		variableBuffer.append("\n}\n");

		/*
		constant.write("ListPlot3D[Transpose@Partition[Last /@ constant, 15]"
				+ ", DataRange->{{0, 5}, {0, 500000}, {0, 40}}"
				+ ", PlotRange -> {{0, 5}, {0, 500000}, {0, 40}}"
				+ ", ColorFunction -> \"Rainbow\""
				+ ", AxesLabel->{\"Learning-Rate\", \"Optimal Number of Trees\", \"All Data Test RMSE\"}"
				+ ", PlotLabel->{\"Constant Learning-Rate and Optimal Number of Trees vs All Data Test RMSE\"}"
				+ "] \n");
		
		variable.write("ListPlot3D[Transpose@Partition[Last /@ variable, 15]"
				+ ", DataRange->{{0, 5}, {0, 500000}, {0, 40}}"
				+ ", PlotRange -> {{0, 5}, {0, 500000}, {0, 40}}"
				+ ", CoorFunction -> \"Rainbow\""
				+ ", AxesLabel->{\"Learning-Rate\", \"Optimal Number of Trees\", \"All Data Test RMSE\"}"
				+ ", PlotLabel->{\"Constant Learning-Rate and Optimal Number of Trees vs All Data Test RMSE\"}"
				+ "] \n");
		*/
		
		constantBuffer.append("constantPlot := ListPlot[" + entryGetter.getMathematicaVeriableName(datasetName, "ConstantLR")
				//+ ", DataRange -> \"All\""
				//+ ", PlotRange -> \"All\""
				+ ", PlotRange -> {{" + entryGetter.getXMin() + ", " + entryGetter.getXMax() + "}, {" + entryGetter.getYMin() + ", " + entryGetter.getYMax() + "}}"
				//+ ", ColorFunction -> \"Rainbow\""
				+ ", AxesLabel->{\"" + entryGetter.getXLabel() + "\", \"" + entryGetter.getYLabel() + "\"}"
				//+ ", PlotLabel->\"" + entryGetter.getPlotTitle(datasetName, "ConstantLR") + "\""
				+ "] \n\n");
		
		variableBuffer.append("variablePlot := ListPlot[" + entryGetter.getMathematicaVeriableName(datasetName, "VariableLR")
				//+ ", DataRange -> {All}"
				//+ ", PlotRange -> {All}"
				+ ", PlotRange -> {{" + entryGetter.getXMin() + ", " + entryGetter.getXMax() + "}, {" + entryGetter.getYMin() + ", " + entryGetter.getYMax() + "}}"
				//+ ", ColorFunction -> \"Rainbow\""
				+ ", AxesLabel->{\"" + entryGetter.getXLabel() + "\", \"" + entryGetter.getYLabel() + "\"}"
				//+ ", PlotLabel->\"" + entryGetter.getPlotTitle(datasetName, "Variable_LR") + "\""
				+ "] \n\n");
		
		String pairWiseGraphsDirectory = (paramTuningDir + "pairWiseGraphs/").replace("\\", "/");
		
		StringBuffer saveToFiles = new StringBuffer();
		StringBuffer latexCode = new StringBuffer();
		String constantFileName = pairWiseGraphsDirectory + entryGetter.getPlotFileNameNoExtension(datasetName, "Constant_LR");
		String variableFileName = pairWiseGraphsDirectory + entryGetter.getPlotFileNameNoExtension(datasetName, "Variable_LR");
		
		saveToFiles.append("constantPlotFileName := \"" + constantFileName + "\"\n");
		saveToFiles.append("Export[constantPlotFileName <> \".png\", constantPlot, ImageResolution -> 300]\n\n");
		
		saveToFiles.append("variablePlotFileName := \"" + variableFileName + "\"\n");
		saveToFiles.append("Export[variablePlotFileName <> \".png\", variablePlot, ImageResolution -> 300]\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{" + constantFileName + "}\n");
		latexCode.append("\\caption{" + entryGetter.getLatexCaption(datasetName, "Constant LR") + "}\n");
		latexCode.append("\\label{fig:" + entryGetter.getPlotFileNameNoExtension(datasetName, "Constant_LR")  + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		latexCode.append("\\begin{figure}[!htb]\\centering\n");
		latexCode.append("\\includegraphics[width=1\\textwidth]{" + variableFileName + "}\n");
		latexCode.append("\\caption{" + entryGetter.getLatexCaption(datasetName, "Variable LR") + "}\n");
		latexCode.append("\\label{fig:" +  entryGetter.getPlotFileNameNoExtension(datasetName, "Variable_LR") + "}\n");
		latexCode.append("\\end{figure}\n\n");
		
		try {
			
			new File(pairWiseGraphsDirectory).mkdirs();
			BufferedWriter file = new BufferedWriter(new PrintWriter(new File(pairWiseGraphsDirectory + entryGetter.getTxtFileName(datasetName, ""))));
			file.write( constantBuffer.toString() + variableBuffer.toString() + saveToFiles.toString() + latexCode.toString());
			file.flush();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
