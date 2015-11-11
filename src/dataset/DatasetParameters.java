package dataset;

public class DatasetParameters {
	public String minimalName;
	public String fullName;
	
	public String fileDirectory;
	public String fileName;
	public boolean attributeTypeHeader;
	public boolean attributeNameHeader;
	public int responseVariableColumn;
	
	public DatasetParameters(String minimalName, String fullName, String fileDirectory, String fileName, int responseVariableColumn, boolean attributeTypeHeader, boolean attributeNameHeader) {
		this.minimalName = minimalName;
		this.fullName = fullName;
		this.fileDirectory = (!fileDirectory.contains(System.getProperty("user.dir"))) ? (System.getProperty("user.dir") + "/"+ fileDirectory) : fileDirectory;
		this.fileName = fileName;
		this.responseVariableColumn = responseVariableColumn;
		this.attributeNameHeader = attributeNameHeader;
		this.attributeTypeHeader = attributeTypeHeader;
	}
	public DatasetParameters(String minimalName, String fullName, String fileDirectory, String fileName, int responseVariableColumn) {
		this( minimalName,  fullName,  fileDirectory,  fileName,  responseVariableColumn, true, true);
	}
}
