package regressionTree;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;

import dataset.Attribute;
import dataset.Attribute.Type;
import utilities.DoubleCompare;

public class TreeNode {
	public Attribute.Type splitPredictorType = null;
	public int splitPredictorIndex = -1;
	
	// Numerical splits use splitValue, categorical splits use left/right categories
	public double numericSplitValue = 0;
	public HashSet<String> leftCategories = null;
	public HashSet<String> rightCategories = null;
	
	public double leftTerminalValue = 0.0;
	public double rightTerminalValue = 0.0;
	public double missingTerminalValue = 0.0;
	
	public int leftInstanceCount = 0;
	public int rightInstanceCount = 0;
	public int missingInstanceCount = 0;
	
	public double leftSquaredError = Double.MAX_VALUE;
	public double rightSquaredError = Double.MAX_VALUE;
	public double missingSquaredError = Double.MAX_VALUE;
	public double squaredErrorBeforeSplit = Double.MAX_VALUE;
	
	public TreeNode leftChild = null;
	public TreeNode rightChild = null;
	public TreeNode missingChild = null;
	
	public double maxLearningRate = 0.0;
	
	/*
	 * Use when unable to split the root node.
	 */
	public TreeNode(double missingTerminalValue, double maxLearningRate, double squaredErrorBeforeSplit, int numOfInstancesBeforeSplit) {
		this.splitPredictorIndex = -1;
		this.missingTerminalValue = missingTerminalValue;
		this.maxLearningRate = maxLearningRate;
		this.missingSquaredError = squaredErrorBeforeSplit;
		this.missingInstanceCount = numOfInstancesBeforeSplit;
	}
	
	public TreeNode(BestSplit bestSplit, double meanResponseInParent, double maxLearningRate) {
		this.splitPredictorType = bestSplit.splitPredictorType;
		this.splitPredictorIndex = bestSplit.splitPredictorIndex;
		this.numericSplitValue = bestSplit.numericSplitValue;
		this.leftCategories = bestSplit.leftCategories;
		this.rightCategories = bestSplit.rightCategories;
		
		this.leftSquaredError = bestSplit.leftSquaredError;
		this.rightSquaredError = bestSplit.rightSquaredError;
		
		this.leftInstanceCount = bestSplit.leftInstanceCount;
		this.rightInstanceCount = bestSplit.rightInstanceCount;
		
		this.leftTerminalValue = bestSplit.leftMeanResponse;
		this.rightTerminalValue = bestSplit.rightMeanResponse;
		
		// TODO: Refactor to actually support missing training data calculations
		this.missingSquaredError = bestSplit.squaredErrorBeforeSplit; 
		this.missingInstanceCount = 0;
		this.missingTerminalValue = meanResponseInParent;
		
		this.squaredErrorBeforeSplit = bestSplit.squaredErrorBeforeSplit;
		
		this.maxLearningRate = maxLearningRate;
	}
	
	// TODO: Doesn't account for missing values;
	public double getSquaredErrorImprovement() {
		return squaredErrorBeforeSplit - (leftSquaredError + rightSquaredError);
	}

	/**
	 * 
	 * @param node
	 * @param instance
	 * @return 0 = notYetFound, 1 = left, 2 = right, 3 = missing
	 */
	public int whichChild(Attribute[] instance) {
		return whichChild(this, instance);
	}
	
	/**
	 * 
	 * @param node
	 * @param instance
	 * @return 0 = notYetFound, 1 = left, 2 = right, 3 = missing
	 */
	public int whichChild(TreeNode node, Attribute[] instance) {
		if (instance[node.splitPredictorIndex].isMissingValue()) {
			return 3;
		}
		int whichChild = 0;
		switch (node.splitPredictorType) {
			case Numeric:
				if (DoubleCompare.lessThan(node.numericSplitValue, instance[node.splitPredictorIndex].getNumericValue())) {
					whichChild = 2;
				} else {
					whichChild = 1;
				}
				break;
			case Categorical:
				if (node.leftCategories.contains(instance[node.splitPredictorIndex].getCategoricalValue())) {
					whichChild = 1;
				} else if (node.rightCategories.contains(instance[node.splitPredictorIndex].getCategoricalValue())){
					whichChild = 2;
				} else { 
					// Category wasn't present in training data
					whichChild = 3;
				}
				break;
			default:
				throw new IllegalStateException("Unrecognized prodictor type in TreeNode.getLearnedValue");
		}
		return whichChild;
	}
					
	public double getLearnedValue(Attribute[] instance) {
		TreeNode current = this;
		
		while (true) {
			// SplitAttribute will be -1 only if we failed to split the root node and reduce the error.
			//	In that case we will just return the mean response over all the training data passed to
			//	the build function which will be stored in current.missingTerminalValue. 
			
			// TODO: THis needs to be refactored. Need to just make terminal node objects 
			//	instead of keeping terminal info all in one object
			if (current.splitPredictorIndex == -1) {
				return current.missingTerminalValue;
				
			}
		
			int whichChild = whichChild(current, instance);
			switch(whichChild) {
				case 1:
					if (current.leftChild == null) {
						// TODO: Finidh implementing, will likely require adding actual terminal nodes.
						return maxLearningRate * current.leftTerminalValue;
					}
					current = current.leftChild;
					break;
				case 2:
					if (current.rightChild == null) {
						return maxLearningRate * current.rightTerminalValue;
					}
					current = current.rightChild;
					break;
				case 3:
					if (current.missingChild == null) {
						return maxLearningRate * current.missingTerminalValue;
					}
					current = current.missingChild;
					break;
			}
		}
	}
	
	//-----------------------------------Printing Functions-------------------------------------------
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
    	String s = null;
    	if (splitPredictorType == Type.Numeric) {
    		//s= String.format("{Attr: %d Val: %.2f ErrorReduction: %.5f Weight: %d}", splitPredictorIndex, numericSplitValue, getSquaredErrorImprovement(), leftInstanceCount + rightInstanceCount);
    		s= String.format("{"
    				+ "Attr: %d "
    				+ "Val: %.5f "
    				+ "ErrorReduction: %.5f "
    				+ "Weight: %d "
    				+ "LeftPred: %f "
    				+ "RightPred: %f "
    				+ "}", 
    				
    				splitPredictorIndex, 
    				numericSplitValue, 
    				getSquaredErrorImprovement(), 
    				leftInstanceCount + rightInstanceCount,
    				maxLearningRate * leftTerminalValue,
    				maxLearningRate * rightTerminalValue
    				);
    	} else if (splitPredictorType == Type.Categorical) {
    		s= String.format("{"
    				+ "Attr: %d "
    				+ "Val: %.2s "
    				+ "ErrorReduction: %.5f "
    				+ "Weight: %d "
    				+ "LeftPred: %f "
    				+ "RightPred: %f "
    				+ "}", 
    				
    				splitPredictorIndex, 
    				"Categories coming soon", 
    				getSquaredErrorImprovement(), 
    				leftInstanceCount + rightInstanceCount,
    				maxLearningRate * leftTerminalValue,
    				maxLearningRate * rightTerminalValue
    				);
    	}
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
		return "SplitAttribute: " + splitPredictorIndex + "\n" +
				"splitValue: " + numericSplitValue + "\n" +
				"leftInstanceCount: " + leftInstanceCount + "\n" +
				"rightInstanceCount: " + rightInstanceCount + "\n" +
				"leftTerminalValue: " + leftTerminalValue + "\n" +
				"rightTerminalValue: " + rightTerminalValue + "\n" +
				"leftSquaredError: " + leftSquaredError + "\n" +
				"rightSquaredError: " + rightSquaredError + "\n" +
				"squaredErrorBeforeSplit: " + squaredErrorBeforeSplit + "\n" +
				"ErrorImprovement: " + (squaredErrorBeforeSplit - (leftSquaredError + rightSquaredError)) + "\n";
	}

}