package regressionTree;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import dataset.Attribute;
import dataset.Attribute.Type;
import utilities.SumCountAverage;

public class TreeNode {
	public Attribute.Type splitPredictorType = null;
	public int splitPredictorIndex = -1;
	
	// Numerical splits use splitValue, categorical splits use left/right categories
	public double numericSplitValue = 0;
	public ArrayList<String> leftCategories = null;
	public ArrayList<String> rightCategories = null;
	
	public double squaredErrorBeforeSplit = Double.MAX_VALUE;
	
	public TreeNode leftChild = null;
	public TreeNode rightChild = null;
	public TreeNode missingChild = null;
	public TerminalNode leftTerminalNode = null;
	public TerminalNode rightTerminalNode = null;
	public TerminalNode missingTerminalNode = null;
	
	/**
	 * Use when unable to split the root node. No split is more optimal than just keeping everything in the root.
	 */
	public TreeNode(double meanResponseInParent, double squaredErrorBeforeSplit, int numOfInstancesBeforeSplit) {
		missingTerminalNode = new TerminalNode(meanResponseInParent, numOfInstancesBeforeSplit, squaredErrorBeforeSplit);
		this.leftTerminalNode = new TerminalNode(0, 0, 0);
		this.rightTerminalNode = new TerminalNode(0, 0, 0);
		this.splitPredictorIndex = -1;
	}
	
	/** MeanResponseInParent and minExamplesInNode are required in case there were no missing values,
	 *	in that case the missing terminal node will be set to have the MeanResponseInParent as the prediction
	 *	and its instance count will be set to the minimum so that when using the variable learning rate scheme
	 *	it will receive the lowest possible learning rate.
	 */
	public TreeNode(BestSplit bestSplit, double meanResponseInParent, int minExamplesInNode) {
		this.splitPredictorType = bestSplit.splitPredictorType;
		this.splitPredictorIndex = bestSplit.splitPredictorIndex;
		this.numericSplitValue = bestSplit.numericSplitValue;
		this.leftCategories = bestSplit.leftCategories;
		this.rightCategories = bestSplit.rightCategories;
		
		this.squaredErrorBeforeSplit = bestSplit.squaredErrorBeforeSplit;
		
		this.leftTerminalNode = new TerminalNode(bestSplit.leftMeanResponse, bestSplit.leftInstanceCount, bestSplit.leftSquaredError);
		this.rightTerminalNode = new TerminalNode(bestSplit.rightMeanResponse, bestSplit.rightInstanceCount, bestSplit.rightSquaredError);
		
		if (bestSplit.missingInstanceCount > 0) {
			this.missingTerminalNode = new TerminalNode(bestSplit.missingMeanResponse, bestSplit.missingInstanceCount, bestSplit.missingSquaredError);
		} else {
			/* 
			 * We still need a missing node in case test data is missing at this point. 
			 *	In that case need to return prediction as though the parent was never split.
			 * Squared error in this node is 0 because there weren't any actual missing values, this makes sure 
			 *	getSquaredErrorImprovement() will work as expected.
			 * I debated about what this instance count should be. Note it only matters when using variable learning rates.
			 * 	On one hand it should never be allowed to be below minExamplesInNode because that allows lower than the 
			 * 	minimum learning rate to be computed. However, in the case where there actually were missing values (directly above), 
			 *  we allow the count to be below minExamplesInNode anyway because there's really no choice. Thus I think 0 makes the most sense 
			 * 	here. And it also makes sense that test instances with missing values where the training data has no missing values
			 * 	should receive the ultimate punishment from the variable learning rate scheme, as we truly have learned nothing about 
			 *  that data.
			 */
			this.missingTerminalNode = new TerminalNode(meanResponseInParent, 0, 0);
		}
	}
	
	public double getSquaredErrorImprovement() {
		// If we couldn't split the root node, then there was no improvement.
		if (splitPredictorIndex == -1) {
			return 0; 
		} else {
			return squaredErrorBeforeSplit - (leftTerminalNode.squaredError + rightTerminalNode.squaredError + missingTerminalNode.squaredError);
		}
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
				if (node.numericSplitValue < instance[node.splitPredictorIndex].getNumericValue()) {
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
	
	public SumCountAverage sumNumberOfExamplesInTerminalNodes(SumCountAverage sca) {
		if (this.leftChild == null) {
			sca.addData(this.leftTerminalNode.instanceCount);
		} else {
			this.leftChild.sumNumberOfExamplesInTerminalNodes(sca);
		}
		if (this.rightChild == null) {
			sca.addData(this.rightTerminalNode.instanceCount);
		}  else {
			this.rightChild.sumNumberOfExamplesInTerminalNodes(sca);
		}
		if (this.missingChild == null) {
			if (this.missingTerminalNode.instanceCount != 0) {
				sca.addData(this.missingTerminalNode.instanceCount);
			}
		} else {
			this.missingChild.sumNumberOfExamplesInTerminalNodes(sca);
		}
		return sca;
	}
					
	public TerminalNode getLearnedTerminalNode(Attribute[] instance) {
		TreeNode current = this;
		
		while (true) {
			// SplitAttribute will be -1 only if we failed to split the root node and reduce the error.
			//	In that case we will just return the mean response over all the training data passed to
			//	the build function which will be stored in current.missingTerminalNode. 
			if (current.splitPredictorIndex == -1) {
				return current.missingTerminalNode;
				
			}
		
			int whichChild = whichChild(current, instance);
			switch(whichChild) {
				case 1:
					if (current.leftChild == null) {
						return current.leftTerminalNode;
					}
					current = current.leftChild;
					break;
				case 2:
					if (current.rightChild == null) {
						return current.rightTerminalNode;
					}
					current = current.rightChild;
					break;
				case 3:
					if (current.missingChild == null) {
						return current.missingTerminalNode;
					}
					current = current.missingChild;
					break;
			}
		}
	}
	
	public void calcRelativeInfluences(double[] relativeInfluences) {
		if (this.splitPredictorIndex != -1) {
			relativeInfluences[this.splitPredictorIndex] += Math.round(((this.getSquaredErrorImprovement())) * 10) / 10.0;
		}
		
		if (this.leftChild != null) {
			this.leftChild.calcRelativeInfluences(relativeInfluences);
		}
		
		if (this.rightChild != null) {
			this.rightChild.calcRelativeInfluences(relativeInfluences);
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
    		s= String.format("{"
    				+ "Attr: %d "
    				+ "Val: %.5f "
    				+ "ErrorReduction: %.5f "
    				+ "Weight: %d "
    				+ "}", 
    				
    				splitPredictorIndex, 
    				numericSplitValue, 
    				getSquaredErrorImprovement(), 
    				leftTerminalNode.instanceCount + rightTerminalNode.instanceCount
    				);
    	} else if (splitPredictorType == Type.Categorical) {
    		s= String.format("{"
    				+ "Attr: %d "
    				+ "Val: %.2s "
    				+ "ErrorReduction: %.5f "
    				+ "Weight: %d "
    				+ "}", 
    				
    				splitPredictorIndex, 
    				"Categories coming soon", 
    				getSquaredErrorImprovement(), 
    				leftTerminalNode.instanceCount + rightTerminalNode.instanceCount
    				);
    	}
    	out.write(s);
    	out.write("\n");
    	out.flush();
    }
    // use string and not stringbuffer on purpose as we need to change the indent at each recursion
    private void printTree(OutputStreamWriter out, boolean isRight, String indent) throws IOException {
        if (rightChild != null) {
            rightChild.printTree(out, true, indent + (isRight ? "        " : " |      "));
        } else {
            rightTerminalNode.printNodeValue(out);
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
        } else {
        	leftTerminalNode.printNodeValue(out);
        }
    }
    
    public void printLatexTree(StringBuilder output, String indent, double learningRateToExampleCountRatio, double minLearningRate, String[] predictorNames) {
    	
    	double leftLearningRate = ((this.leftTerminalNode.instanceCount * learningRateToExampleCountRatio) + minLearningRate);
    	double rightLearningRate = ((this.rightTerminalNode.instanceCount * learningRateToExampleCountRatio) + minLearningRate);
    	double missingLearningRate = ((this.missingTerminalNode.instanceCount * learningRateToExampleCountRatio) + minLearningRate);
    	output.append("\t\t\t child{  \n");
    	if (this.leftChild == null) {
	    	output.append(indent + String.format("\t\t\t\t node[draw, align=left] {\\tiny Cnt: %d \\\\[1mm] \\tiny Avg: %f \\\\[1mm] \\tiny Shr: %f}\n", this.leftTerminalNode.instanceCount, this.leftTerminalNode.terminalValue, leftLearningRate));
    	} else {
        	output.append(indent + String.format("\t\t\tnode[draw]{\\tiny $%s < %s$}  \n", predictorNames[this.leftChild.splitPredictorIndex], (String.format("%f",this.leftChild.numericSplitValue)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1")));
        	this.leftChild.printLatexTree(output, "\t", learningRateToExampleCountRatio, minLearningRate, predictorNames);
    	}
    	output.append(indent + "\t\t\t\t edge from parent  node[left,fill=white] {\\tiny $True$ \\hspace{3mm}}\n");
		output.append(indent + "\t\t }  \n");

    	output.append(indent + "\t\t\t child{  \n");
    	if (this.rightChild == null) {
        	output.append(indent + String.format("\t\t\t\t node[draw, align=left] {\\tiny Cnt: %d \\\\[1mm] \\tiny Avg: %f \\\\[1mm] \\tiny Shr: %f}\n", this.rightTerminalNode.instanceCount, this.rightTerminalNode.terminalValue, rightLearningRate));
    	} else {
        	output.append(indent + String.format("\t\t\tnode[draw]{\\tiny $%s < %s$}  \n", predictorNames[this.rightChild.splitPredictorIndex], (String.format("%f",this.rightChild.numericSplitValue)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1")));
        	this.rightChild.printLatexTree(output, "\t", learningRateToExampleCountRatio, minLearningRate, predictorNames);
    	}
    	output.append(indent + "\t\t\t\t edge from parent  node[midway,fill=white] {\\tiny $False$ \\hspace{3mm}}\n");
		output.append(indent + "\t\t\t }  \n");
		
    	output.append(indent + "\t\t\t child{  \n");
    	if (this.missingChild == null) {
        	output.append(indent + String.format("\t\t node[draw, align=left] {\\tiny Cnt: %d \\\\[1mm] \\tiny Avg: %f \\\\[1mm] \\tiny Shr: %f}\n", this.missingTerminalNode.instanceCount, this.missingTerminalNode.terminalValue, missingLearningRate));
    	} else {
        	output.append(indent + String.format("\t\t\tnode[draw]{\\tiny $%s < %s$}  \n", predictorNames[this.missingChild.splitPredictorIndex], (String.format("%f",this.missingChild.numericSplitValue)).replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1")));
        	this.missingChild.printLatexTree(output, "\t", learningRateToExampleCountRatio, minLearningRate, predictorNames);
    	}
    	output.append(indent + "\t\t\t\t edge from parent  node[right,fill=white] {\\tiny $Missing$ \\hspace{3mm}}\n");
		output.append(indent + "\t\t\t }  \n");
    }
    
	public String toString() {
		return "SplitAttribute: " + splitPredictorIndex + "\n" +
				"splitValue: " + numericSplitValue + "\n" +
				"leftInstanceCount: " + leftTerminalNode.instanceCount + "\n" +
				"rightInstanceCount: " + rightTerminalNode.instanceCount + "\n" +
				"leftTerminalValue: " + leftTerminalNode.terminalValue + "\n" +
				"rightTerminalValue: " + rightTerminalNode.terminalValue + "\n" +
				"leftSquaredError: " + leftTerminalNode.squaredError + "\n" +
				"rightSquaredError: " + rightTerminalNode.squaredError + "\n" +
				"squaredErrorBeforeSplit: " + squaredErrorBeforeSplit + "\n" +
				"ErrorImprovement: " + getSquaredErrorImprovement() + "\n";
	}

}
