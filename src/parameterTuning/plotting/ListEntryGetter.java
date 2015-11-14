package parameterTuning.plotting;
import java.util.List;

import parameterTuning.RunDataSummaryRecord;
import utilities.DoubleCompare;


	public abstract class ListEntryGetter {
		public String CVEMin = "0", CVEMax = "All";
		public String ADTEMin = "0", ADTEMax = "All";
		public String TISMin = "0", TISMax = "All";
		public String NOTMin = "0", NOTMax = "500000";
		public String BFMin = "0.4", BFMax = "1.1";
		public String NOSMin = "0", NOSMax = "20";
		public String MAXLRMin = "0", MAXLRMax = "2.5";
		public String MINLRMin = "0", MINLRMax = "2.5";
		public String MEINMin = "0", MEINMax = "1000";
		
		public abstract String getXLabel();
		public abstract String getYLabel();
		public abstract String getZLabel();
		public abstract String getXMin();
		public abstract String getXMax();
		public abstract String getYMin();
		public abstract String getYMax();
		public abstract String getZMin();
		public abstract String getZMax();
		public abstract boolean is3D();
		
		public String getMathematicaVeriableName(String datasetName, String prefix) {
			if (is3D()) {
				return datasetName + prefix + getXLabel() + "vs" + getYLabel() + "vs" + getZLabel();
			} else {
				return datasetName + prefix + getXLabel() + "vs" + getYLabel();
			}
		}
		
		public String getPlotTitle(String datasetName, String prefix) {
			return datasetName + " " + prefix + " " + getXLabel() + " vs " + getYLabel();
		}
		public String getTxtFileName(String datasetName, String prefix) {
			return getMathematicaVeriableName(datasetName, prefix) + ".txt";
		}
		
		public String getPlotFileNameNoExtension(String datasetName, String prefix) {
			return getMathematicaVeriableName(datasetName, prefix);
		}
		
		public String getLatexCaption(String datasetName, String prefix) {
			return getPlotTitle(datasetName, prefix);
		}
		
		public abstract String getEntry(RunDataSummaryRecord record);
		
		public void setMinsAndMaxes(List<RunDataSummaryRecord> records) {
			double maxCVE  = 0.0, minCVE  = Double.MAX_VALUE, 
					   maxADTE = 0.0, minADTE = Double.MAX_VALUE,
					   maxTIME = 0.0, minTIME = Double.MAX_VALUE,
					   maxBF = 0.0, minBF = Double.MAX_VALUE,
					   maxMAXLR = 0.0, minMAXLR = Double.MAX_VALUE,
					   maxMINLR = 0.0, minMINLR = Double.MAX_VALUE;
			double maxNOT  = 0, minNOT  = Double.MAX_VALUE;
			int maxNOS = 0, minNOS  = Integer.MAX_VALUE,
			    maxMEIN = 0, minMEIN = Integer.MAX_VALUE;
		
				for (RunDataSummaryRecord record : records) {
					if (DoubleCompare.lessThan(record.allDataTestError, minADTE)) {
						minADTE = record.allDataTestError;
					}
					if (DoubleCompare.greaterThan(record.allDataTestError, maxADTE)) {
						maxADTE = record.allDataTestError;
					}
					if (DoubleCompare.lessThan(record.cvValidationError, minCVE)) {
						minCVE = record.cvValidationError;
					}
					if (DoubleCompare.greaterThan(record.cvValidationError, maxCVE)) {
						maxCVE = record.cvValidationError;
					}
					if (DoubleCompare.lessThan(record.timeInSeconds, minTIME)) {
						minTIME = record.timeInSeconds;
					}
					if (DoubleCompare.greaterThan(record.timeInSeconds, maxTIME)) {
						maxTIME = record.timeInSeconds;
					}
					if (DoubleCompare.lessThan(record.parameters.bagFraction, minBF)) {
						minBF = record.parameters.bagFraction;
					}
					if (DoubleCompare.greaterThan(record.parameters.bagFraction, maxBF)) {
						maxBF = record.parameters.bagFraction;
					}
					if (DoubleCompare.lessThan(record.parameters.maxLearningRate, minMAXLR)) {
						minMAXLR = record.parameters.maxLearningRate;
					}
					if (DoubleCompare.greaterThan(record.parameters.maxLearningRate, maxMAXLR)) {
						maxMAXLR = record.parameters.maxLearningRate;
					}
					if (DoubleCompare.lessThan(record.parameters.minLearningRate, minMINLR)) {
						minMINLR = record.parameters.minLearningRate;
					}
					if (DoubleCompare.greaterThan(record.parameters.minLearningRate, maxMINLR)) {
						maxMINLR = record.parameters.minLearningRate;
					}
					if (DoubleCompare.lessThan(record.optimalNumberOfTrees, minNOT)) {
						minNOT = record.optimalNumberOfTrees;
					}
					if (DoubleCompare.greaterThan(record.optimalNumberOfTrees, maxNOT)) {
						maxNOT = record.optimalNumberOfTrees;
					}
					if (record.parameters.maxNumberOfSplits < minNOS) {
						minNOS = record.parameters.maxNumberOfSplits;
					}
					if (record.parameters.maxNumberOfSplits > maxNOS) {
						maxNOS = record.parameters.maxNumberOfSplits;
					}
					if (record.parameters.minExamplesInNode < minMEIN) {
						minMEIN = record.parameters.minExamplesInNode;
					}
					if (record.parameters.minExamplesInNode > maxMEIN) {
						maxMEIN = record.parameters.minExamplesInNode;
					}
					
					/*
					CVEMax = "" + (maxCVE + ((maxCVE-minCVE)/5)); 
					CVEMin = "" + (minCVE - ((maxCVE-minCVE)/5)); 
					ADTEMax = "" + (maxADTE + ((maxADTE-minADTE)/5)); 
					ADTEMin = "" + (minADTE - ((maxADTE-minADTE)/5)); 
					TISMax = "" + (maxTIME + ((maxTIME-minTIME)/5)); 
					TISMin = "" + (minTIME - ((maxTIME-minTIME)/5)); 
					NOTMax = "" + (maxNOT + ((maxNOT-minNOT)/5)); 
					NOTMin = "" + (minNOT - ((maxNOT-minNOT)/5)); 
					NOSMax = "" + (maxNOS + ((maxNOS-minNOS)/5)); 
					NOSMin = "" + (minNOS - ((maxNOS-minNOS)/5)); 
					MAXLRMax = "" + (maxMAXLR + ((maxMAXLR-minMAXLR)/5)); 
					MAXLRMin = "" + (minMAXLR - ((maxMAXLR-minMAXLR)/5)); 
					MINLRMax = "" + (maxMINLR + ((maxMINLR-minMINLR)/5)); 
					MINLRMin = "" + (minMINLR - ((maxMINLR-minMINLR)/5)); 
					BFMax = "" + (maxBF + ((maxBF-minBF)/5)); 
					BFMin = "" + (minBF - ((maxBF-minBF)/5)); 
					MEINMax = "" + (maxMEIN + ((maxMEIN-minMEIN)/5));
					MEINMin = "" + (minMEIN - ((maxMEIN-minMEIN)/5));
					*/
					CVEMax = String.format("%f", maxCVE);
					CVEMin = String.format("%f", minCVE);
					ADTEMax = String.format("%f", maxADTE);
					ADTEMin= String.format("%f", minADTE);
					TISMax = String.format("%f", maxTIME);
					TISMin = String.format("%f", minTIME);
					NOTMax = String.format("%f", maxNOT);
					NOTMin = String.format("%f", minNOT);
					NOSMax = String.format("%d", maxNOS);
					NOSMin = String.format("%d", minNOS);
					MAXLRMax = String.format("%f", maxMAXLR);
					MAXLRMin = String.format("%f", minMAXLR);
					MINLRMax = String.format("%f", maxMINLR);
					MINLRMin = String.format("%f", minMINLR);
					BFMax = String.format("%f", maxBF);
					BFMin = String.format("%f", minBF);
					MEINMax = String.format("%d", maxMEIN);
					MEINMin = String.format("%d", minMEIN);
				}
		}
		
		public static class MaxLR_MinLR_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MaxLearningRate";
			}
			public String getYLabel() {
				return "MinLearningRate";
			}
			@Override
			public String getZLabel() {
				return "AllDataTestError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(
						record.parameters.maxLearningRate, 
						record.parameters.minLearningRate, 
						record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return MAXLRMin;
			}
			@Override
			public String getXMax() {
				return MAXLRMax;
			}
			@Override
			public String getYMin() {
				return MINLRMin;
			}
			@Override
			public String getYMax() {
				return MINLRMax;
			}
			@Override
			public boolean is3D() {
				return true;
			}

			@Override
			public String getZMin() {
				return ADTEMin;
			}
			@Override
			public String getZMax() {
				return ADTEMax;
			}
		}
		
		
		public static class MaxLR_NOS_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(
						record.parameters.maxLearningRate, 
						record.parameters.numOfTrees, 
						record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return NOTMin;
			}
			@Override
			public String getXMax() {
				return NOTMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return true;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class NOT_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.optimalNumberOfTrees, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return NOTMin;
			}
			@Override
			public String getXMax() {
				return NOTMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		//--------------------------BAG FRACTION---------------------------------------------------------------------
		
		public static class BF_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.bagFraction, record.optimalNumberOfTrees);
			}
			@Override
			public String getXMin() {
				return BFMin;
			}
			@Override
			public String getXMax() {
				return BFMax;
			}
			@Override
			public String getYMin() {
				return NOTMin;
			}
			@Override
			public String getYMax() {
				return NOTMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class BF_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.bagFraction, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return BFMin;
			}
			@Override
			public String getXMax() {
				return BFMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class BF_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.bagFraction, record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return BFMin;
			}
			@Override
			public String getXMax() {
				return BFMax;
			}
			@Override
			public String getYMin() {
				return ADTEMin;
			}
			@Override
			public String getYMax() {
				return ADTEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class BF_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.bagFraction, record.timeInSeconds);
			}
			@Override
			public String getXMin() {
				return BFMin;
			}
			@Override
			public String getXMax() {
				return BFMax;
			}
			@Override
			public String getYMin() {
				return TISMin;
			}
			@Override
			public String getYMax() {
				return TISMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		//--------------------------SPLITS---------------------------------------------------------------------
		public static class SPLITS_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxNumberOfSplits, record.optimalNumberOfTrees);
			}
			@Override
			public String getXMin() {
				return NOSMin;
			}
			@Override
			public String getXMax() {
				return NOSMax;
			}
			@Override
			public String getYMin() {
				return NOTMin;
			}
			@Override
			public String getYMax() {
				return NOTMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class SPLITS_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxNumberOfSplits, record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return NOSMin;
			}
			@Override
			public String getXMax() {
				return NOSMax;
			}
			@Override
			public String getYMin() {
				return ADTEMin;
			}
			@Override
			public String getYMax() {
				return ADTEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}

		public static class SPLITS_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxNumberOfSplits, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return NOSMin;
			}
			@Override
			public String getXMax() {
				return NOSMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class SPLITS_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxNumberOfSplits, record.timeInSeconds);
			}
			@Override
			public String getXMin() {
				return NOSMin;
			}
			@Override
			public String getXMax() {
				return NOSMax;
			}
			@Override
			public String getYMin() {
				return TISMin;
			}
			@Override
			public String getYMax() {
				return TISMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		//--------------------------LEARNING_RATE--------------------------------------
		public static class LR_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.optimalNumberOfTrees);
			}
			@Override
			public String getXMin() {
				return MAXLRMin;
			}
			@Override
			public String getXMax() {
				return MAXLRMax;
			}
			@Override
			public String getYMin() {
				return NOTMin;
			}
			@Override
			public String getYMax() {
				return NOTMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class LR_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return MAXLRMin;
			}
			@Override
			public String getXMax() {
				return MAXLRMax;
			}
			@Override
			public String getYMin() {
				return ADTEMin;
			}
			@Override
			public String getYMax() {
				return ADTEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class LR_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return MAXLRMin;
			}
			@Override
			public String getXMax() {
				return MAXLRMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class LR_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.timeInSeconds);
			}
			@Override
			public String getXMin() {
				return MAXLRMin;
			}
			@Override
			public String getXMax() {
				return MAXLRMax;
			}
			@Override
			public String getYMin() {
				return TISMin;
			}
			@Override
			public String getYMax() {
				return TISMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		//--------------------------MIN EXAMPLES IN NODE--------------------------------------
		public static class MEIN_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.minExamplesInNode, record.optimalNumberOfTrees);
			}
			@Override
			public String getXMin() {
				return MEINMin;
			}
			@Override
			public String getXMax() {
				return MEINMax;
			}
			@Override
			public String getYMin() {
				return NOTMin;
			}
			@Override
			public String getYMax() {
				return NOTMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
	
	
		public static class MEIN_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.minExamplesInNode, record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return MEINMin;
			}
			@Override
			public String getXMax() {
				return MEINMax;
			}
			@Override
			public String getYMin() {
				return ADTEMin;
			}
			@Override
			public String getYMax() {
				return ADTEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}

		public static class MEIN_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.minExamplesInNode, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return MEINMin;
			}
			@Override
			public String getXMax() {
				return MEINMax;
			}
			@Override
			public String getYMin() {
				return CVEMin;
			}
			@Override
			public String getYMax() {
				return CVEMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
		
		public static class MEIN_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(RunDataSummaryRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.minExamplesInNode, record.timeInSeconds);
			}
			@Override
			public String getXMin() {
				return MEINMin;
			}
			@Override
			public String getXMax() {
				return MEINMax;
			}
			@Override
			public String getYMin() {
				return TISMin;
			}
			@Override
			public String getYMax() {
				return TISMax;
			}
			@Override
			public boolean is3D() {
				return false;
			}
			@Override
			public String getZLabel() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMin() {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getZMax() {
				// TODO Auto-generated method stub
				return null;
			}
		}
	}
