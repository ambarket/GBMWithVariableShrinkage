package utilities;

import java.util.ArrayList;

public class MathematicaListCreator {
	public static String convertToMathematicaList(ArrayList<Double> error) {
		StringBuffer retval = new StringBuffer();
		retval.append("{\n");
		for (int i = 0; i < error.size(); i++) {
			retval.append(String.format("\t{ %d, %.5f }", i+1, error.get(i)));
			if (i != error.size() - 1) {
				retval.append(",");
			} 
			retval.append("\n");
		}
		retval.append("}");
		return retval.toString();
	}
	
	public static String convertToMathematicaList(double[] error) {
		StringBuffer retval = new StringBuffer();
		retval.append("{\n");
		for (int i = 0; i < error.length; i++) {
			retval.append(String.format("\t{ %d, %.5f }", i+1, error[i]));
			if (i != error.length - 1) {
				retval.append(",");
			} 
			retval.append("\n");
		}
		retval.append("}");
		return retval.toString();
	}
	
	public static String convertNDoublesIntoNDimensionalListEntry(double... args) {
		StringBuffer retval = new StringBuffer();
		retval.append("{");
		for (int i = 0; i < args.length-1; i++) {
			retval.append(args[i] + ", ");
		}
		retval.append(args[args.length-1] + "}");
		return retval.toString();
	}
	
	public static String convertNObjectsIntoNDimensionalListEntry(Object... args) {
		StringBuffer retval = new StringBuffer();
		retval.append("{");
		for (int i = 0; i < args.length-1; i++) {
			retval.append(args[i].toString() + ", ");
		}
		retval.append(args[args.length-1].toString() + "}");
		return retval.toString();
	}
}
