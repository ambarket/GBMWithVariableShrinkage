package dataset;

public class DatasetParameters {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (attributeNameHeader ? 1231 : 1237);
		result = prime * result + (attributeTypeHeader ? 1231 : 1237);
		result = prime * result + ((fileDirectory == null) ? 0 : fileDirectory.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
		result = prime * result + ((minimalName == null) ? 0 : minimalName.hashCode());
		result = prime * result + responseVariableColumn;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatasetParameters other = (DatasetParameters) obj;
		if (attributeNameHeader != other.attributeNameHeader)
			return false;
		if (attributeTypeHeader != other.attributeTypeHeader)
			return false;
		if (fileDirectory == null) {
			if (other.fileDirectory != null)
				return false;
		} else if (!fileDirectory.equals(other.fileDirectory))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		if (minimalName == null) {
			if (other.minimalName != null)
				return false;
		} else if (!minimalName.equals(other.minimalName))
			return false;
		if (responseVariableColumn != other.responseVariableColumn)
			return false;
		return true;
	}
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
