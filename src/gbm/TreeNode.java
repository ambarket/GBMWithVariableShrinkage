package gbm;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class TreeNode {
	public double splitValue;
	public int    splitAttribute;
	public double leftTerminalValue;
	public double rightTerminalValue;
	
	public int leftInstanceCount;
	public int rightInstanceCount;
	
	public double leftSquaredError;
	public double rightSquaredError;
	public double squaredErrorBeforeSplit;
	
	// Each non-leaf node has a left child and a right child.
	public TreeNode leftChild;
	public TreeNode rightChild;
	
	public TreeNode() {
		splitAttribute = 0;
		splitValue = 0.0;
		
		leftTerminalValue = 0.0;
		rightTerminalValue = 0.0;
		leftSquaredError = 0.0;
	    rightSquaredError = 0.0;
		squaredErrorBeforeSplit = 0.0;
		leftInstanceCount = 0;
		rightInstanceCount = 0;
		leftChild = null;
		rightChild = null;
		
	}
	// Construction function
	public TreeNode(double splitValue, int splitAttribute, 
			int leftInstanceCount, int rightInstanceCount, 
			double leftTerminalValue, double rightTerminalValue,
			double leftSquaredError, double rightSquaredError, double squaredErrorBeforeSplit) {
		this.splitValue = splitValue;
		this.splitAttribute = splitAttribute;
		this.leftChild = null;
		this.rightChild = null;
		this.leftTerminalValue = leftTerminalValue;
		this.rightTerminalValue = rightTerminalValue;
		this.leftInstanceCount = leftInstanceCount;
		this.rightInstanceCount = rightInstanceCount;
		this.leftSquaredError = leftSquaredError;
		this.rightSquaredError = rightSquaredError;
		this.squaredErrorBeforeSplit = squaredErrorBeforeSplit;
	}
	
	
    public void printTree(OutputStreamWriter out) throws IOException {
        if (rightChild != null) {
            rightChild.printTree(out, true, "");
        }
        printNodeValue(out);
        if (leftChild != null) {
        	leftChild.printTree(out, false, "");
        }
    }
    private void printNodeValue(OutputStreamWriter out) throws IOException {
    	String s= String.format("{Attr: %d Val: %.2f ErrorReduction: %.5f Weight: %d}", splitAttribute, splitValue, squaredErrorBeforeSplit - (leftSquaredError + rightSquaredError), leftInstanceCount + rightInstanceCount);
    	out.write(s);
    	//out.write("{Attr: " + splitAttribute + 
    		//	" Value: " + splitValue + 
    			//" leftTerm: " + leftTerminalValue + 
    			//" rightTerm: " + rightTerminalValue + 
    			//" leftMSE: " + leftMeanSquaredError + 
    			//" rightMSE: " + rightMeanSquaredError + 
    		//	"}");
    	out.write("\n");
    	out.flush();
    }
    // use string and not stringbuffer on purpose as we need to change the indent at each recursion
    private void printTree(OutputStreamWriter out, boolean isRight, String indent) throws IOException {
        if (rightChild != null) {
            rightChild.printTree(out, true, indent + (isRight ? "        " : " |      "));
        }
        out.write(indent);
        if (isRight) {
            out.write(" /");
            out.flush();
        } else {
            out.write(" \\");
            out.flush();
        }
        out.write("----- ");
        printNodeValue(out);
        if (leftChild != null) {
        	leftChild.printTree(out, false, indent + (isRight ? " |      " : "        "));
        }
    }
    
	public String toString() {
		return "SplitAttribute: " + splitAttribute + "\n" +
				"splitValue: " + splitValue + "\n" +
				"leftInstanceCount: " + leftInstanceCount + "\n" +
				"rightInstanceCount: " + rightInstanceCount + "\n" +
				"leftTerminalValue: " + leftTerminalValue + "\n" +
				"rightTerminalValue: " + rightTerminalValue + "\n" +
				"leftSquaredError: " + leftSquaredError + "\n" +
				"rightSquaredError: " + rightSquaredError + "\n" +
				"squaredErrorBeforeSplit: " + squaredErrorBeforeSplit + "\n";
	}

}