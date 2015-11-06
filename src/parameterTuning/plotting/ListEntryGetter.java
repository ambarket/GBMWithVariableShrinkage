package parameterTuning.plotting;
import java.util.ArrayList;

import parameterTuning.OptimalParameterRecord;
import utilities.DoubleCompare;


	public abstract class ListEntryGetter {
		public static String CVEMin = "0", CVEMax = "All";
		public static String ADTEMin = "0", ADTEMax = "All";
		public static String TISMin = "0", TISMax = "All";
		public static String NOTMin = "0", NOTMax = "500000";
		public static String BFMin = "0.4", BFMax = "1.1";
		public static String NOSMin = "0", NOSMax = "20";
		public static String LRMin = "0", LRMax = "2.5";
		public static String MEINMin = "0", MEINMax = "1000";
		
		public abstract String getXLabel();
		public abstract String getYLabel();
		public abstract String getXMin();
		public abstract String getXMax();
		public abstract String getYMin();
		public abstract String getYMax();
		public abstract boolean is3D();
		
		public String getMathematicaVeriableName(String datasetName, String prefix) {
			return datasetName + prefix + getXLabel() + "vs" + getYLabel();
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
		
		public abstract String getEntry(OptimalParameterRecord record);
		
		public static void setMinsAndMaxes(ArrayList<OptimalParameterRecord> records) {
			double maxCVE  = 0.0, minCVE  = Double.MAX_VALUE, 
					   maxADTE = 0.0, minADTE = Double.MAX_VALUE,
					   maxTIME = 0.0, minTIME = Double.MAX_VALUE,
					   maxBF = 0.0, minBF = Double.MAX_VALUE,
					   maxLR = 0.0, minLR = Double.MAX_VALUE;
			int maxNOT  = 0, minNOT  = Integer.MAX_VALUE,
			    maxNOS = 0, minNOS  = Integer.MAX_VALUE,
			    maxMEIN = 0, minMEIN = Integer.MAX_VALUE;
				for (int i = 0; i < records.size(); i++) {
					OptimalParameterRecord record = records.get(i);
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
					if (DoubleCompare.lessThan(record.parameters.maxLearningRate, minLR)) {
						minLR = record.parameters.maxLearningRate;
					}
					if (DoubleCompare.greaterThan(record.parameters.maxLearningRate, maxLR)) {
						maxLR = record.parameters.maxLearningRate;
					}
					if (record.optimalNumberOfTrees < minNOT) {
						minNOT = record.optimalNumberOfTrees;
					}
					if (record.optimalNumberOfTrees > maxNOT) {
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
					LRMax = "" + (maxLR + ((maxLR-minLR)/5)); 
					LRMin = "" + (minLR - ((maxLR-minLR)/5)); 
					BFMax = "" + (maxBF + ((maxBF-minBF)/5)); 
					BFMin = "" + (minBF - ((maxBF-minBF)/5)); 
					MEINMax = "" + (maxMEIN + ((maxMEIN-minMEIN)/5));
					MEINMin = "" + (minMEIN - ((maxMEIN-minMEIN)/5));
				}
		}
		
		
		public static class LR_NOS_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class NOT_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		//--------------------------BAG FRACTION---------------------------------------------------------------------
		
		public static class BF_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class BF_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class BF_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class BF_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "BagFraction";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		//--------------------------SPLITS---------------------------------------------------------------------
		public static class SPLITS_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class SPLITS_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}

		public static class SPLITS_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class SPLITS_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "NumberofSplits";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		//--------------------------LEARNING_RATE--------------------------------------
		public static class LR_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(OptimalParameterRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.optimalNumberOfTrees);
			}
			@Override
			public String getXMin() {
				return LRMin;
			}
			@Override
			public String getXMax() {
				return LRMax;
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
		}
		
		public static class LR_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(OptimalParameterRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.allDataTestError);
			}
			@Override
			public String getXMin() {
				return LRMin;
			}
			@Override
			public String getXMax() {
				return LRMax;
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
		}
		
		public static class LR_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.cvValidationError);
			}
			@Override
			public String getXMin() {
				return LRMin;
			}
			@Override
			public String getXMax() {
				return LRMax;
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
		}
		
		public static class LR_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "LearningRate";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(OptimalParameterRecord record) {
				return MathematicaListCreator.convertNObjectsIntoNDimensionalListEntry(record.parameters.maxLearningRate, record.timeInSeconds);
			}
			@Override
			public String getXMin() {
				return LRMin;
			}
			@Override
			public String getXMax() {
				return LRMax;
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
		}
		
		//--------------------------MIN EXAMPLES IN NODE--------------------------------------
		public static class MEIN_NOT_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "OptimalNumberOfTrees";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
	
	
		public static class MEIN_ADTE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "AllDataTestError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}

		public static class MEIN_CVE_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "CVError";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
		
		public static class MEIN_TIME_ListEntryGetter extends ListEntryGetter {
			public String getXLabel() {
				return "MinExamplesInNode";
			}
			public String getYLabel() {
				return "TimeinSeconds";
			}
			public String getEntry(OptimalParameterRecord record) {
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
		}
	}
