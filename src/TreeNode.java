
public class TreeNode {
	public double splitValue;
	public int    splitAttribute;
	public double leftTerminalValue;
	public double rightTerminalValue;
	
	public int leftObsInNode;
	public int rightObsInNode;
	
	public double leftSquaredError;
	public double rightSquaredError;
	
	// Each non-leaf node has a left child and a right child.
	public TreeNode leftChild;
	public TreeNode rightChild;
	
	// Construction function
	public TreeNode(double splitValue, int splitAttribute, 
			int leftObsInNode, int rightObsInNode, 
			double leftTerminalValue, double rightTerminalValue,
			double leftSquaredError, double rightSquaredError) {
		this.splitValue = splitValue;
		this.splitAttribute = splitAttribute;
		this.leftChild = null;
		this.rightChild = null;
		this.leftTerminalValue = leftTerminalValue;
		this.rightTerminalValue = rightTerminalValue;
		this.leftObsInNode = leftObsInNode;
		this.rightObsInNode = rightObsInNode;
		this.leftSquaredError = leftSquaredError;
		this.rightSquaredError = rightSquaredError;
	}
}