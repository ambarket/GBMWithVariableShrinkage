package gbm;

public class Response extends Attribute{

	private double psuedoResponse;
	

	public Response(Double numericValue) {
		super(numericValue);
		if (numericValue == null) {
			throw new UnsupportedOperationException("numericValue passed to Response constructor is null (missing value for response variable) should I support this or not?");
		}
		psuedoResponse = numericValue;
	}
	
	public Response(String categoricalValue) {
		super(categoricalValue);
		throw new UnsupportedOperationException("Categorical responses are not yet implemented");
	}
	
	public double getPsuedoResponse() {
		return psuedoResponse;
	}
	
	public void setPsuedoResponse(double psuedoResponse) {
		this.psuedoResponse = psuedoResponse;
	}

	
}
