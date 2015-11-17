package parameterTuning.plotting;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import utilities.DoubleCompare;

public class MathematicaListCreator {
	
	public static void addToMathematicaList(int treeNum, String error, StringBuilder b) {
		if (treeNum == 1) {
			b.append("{\n");
			b.append(" { ");
			b.append(treeNum);
			b.append(", ");
			b.append(error);
			b.append("} ");
		} else {
			b.append(", { ");
			b.append(treeNum);
			b.append(", ");
			b.append(error);
			b.append("} ");
		}
	}
	public static String closeOutMathematicaList(StringBuilder b) {
		b.append("\n");
		b.append("}");
		return b.toString();
	}
	
	public static String convertToMathematicaList(ArrayList<Double> error) {
		StringBuffer retval = new StringBuffer();
		retval.append("{\n");
		retval.append(String.format("\t{ %d, %.5f }", 1, error.get(0)));
		String val = null;
		for (int i = 1; i < error.size(); i++) {
			retval.append(",\t{ ");
			retval.append(i+1);
			retval.append(", ");
			val = "" + error.get(i);
			if (val.contains("E")) {
				val = String.format("%f", error.get(i));
			}
			retval.append(val);
			retval.append("} ");
		}
		retval.append("\n");
		retval.append("}");
		return retval.toString();
	}
	
	public static String convertToMathematicaList(double[] error) {
		StringBuffer retval = new StringBuffer();
		retval.append("{\n");
		retval.append(String.format("\t{ %d, %.5f }", 1, error[0]));
		String val = null;
		for (int i = 1; i < error.length; i++) {
			retval.append(",\t{ ");
			retval.append(i+1);
			retval.append(", ");
			val = "" + error[i];
			if (val.contains("E")) {
				val = String.format("%f", error[i]);
			}
			retval.append(val);
			retval.append("} ");
		}
		retval.append("\n");
		retval.append("}");
		return retval.toString();
	}
	
	public static String convertNObjectsIntoNDimensionalListEntry(Object... args) {

		StringBuffer retval = new StringBuffer();
		retval.append("{");
		for (int i = 0; i < args.length-1; i++) {
			double doub = Double.MIN_NORMAL;
			try {
				doub = (double)args[i];
			} catch (ClassCastException e) {
				
			}
			if (DoubleCompare.equals(doub, Double.MIN_VALUE)) {
				retval.append(args[i].toString() + ", ");
			} else {
				retval.append(String.format("%f", doub) + ", ");
			}
		}
		double doub = Double.MIN_NORMAL;
		try {
			doub = (double)args[args.length-1];
		} catch (ClassCastException e) {
			
		}
		if (DoubleCompare.equals(doub, Double.MIN_VALUE)) {
			retval.append(args[args.length-1].toString() + "}");
		} else {
			retval.append(String.format("%f", doub) + "}");
		}
		
		return retval.toString();
	}
}
