package regressionTree;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class TerminalNode {
	
	public double terminalValue = 0.0;
	public int instanceCount = 0;
	public double squaredError = Double.MAX_VALUE;
	
	public TerminalNode(double terminalValue, int instanceCount, double squaredError) {
		this.terminalValue = terminalValue;
		this.instanceCount = instanceCount;
		this.squaredError = squaredError;
	}
	
    public void printNodeValue(OutputStreamWriter out) throws IOException {
    	String s= String.format("{"
    				+ "Weight: %d "
    				+ "Prediction: %f "
    				+ "SquaredError: %f "
    				+ "}", 
    				instanceCount,
    				terminalValue,
    				squaredError
    				);
    	out.write(s);
    	out.write("\n");
    	out.flush();
    }
}
