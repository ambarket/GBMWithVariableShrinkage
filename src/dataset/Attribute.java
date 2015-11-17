package dataset;

import utilities.DoubleCompare;

public class Attribute implements Comparable<Attribute> {

	public enum Type {Numeric, Categorical};
	public static String MISSING_CATEGORY = "MISSING_CATEGORY";
	
	private Type type;
	private boolean missing;
	
	private Double numericValue;
	private String categoricalValue;
	
	public boolean isMissingValue() {
		return missing;
	}
	
	public Type getType() {
		return type;
	}
	
	public Attribute(Type attributeType) {
		missing = true;
		this.type = attributeType;
		if (this.type == Type.Categorical) {
			categoricalValue = MISSING_CATEGORY;
		}
	}
	
	public Attribute(Double numericValue) {
		this.numericValue = numericValue;
		if (numericValue == null) {
			missing = true;
		}
		this.type = Type.Numeric;
	}
	
	public Attribute(String stringValue) {
		this.categoricalValue = stringValue;
		if (stringValue == null) {
			missing = true;
		}
		this.type = Type.Categorical;
	}
	
	public String getCategoricalValue() {
		if (this.type != Type.Categorical) {
			throw new IllegalStateException("Attempt to get the categorical value of a numeric attribute");
		}
		return categoricalValue;
	}
	
	public Double getNumericValue() {
		if (this.type != Type.Numeric) {
			throw new IllegalStateException("Attempt to get the numeric value of a categorical attribute");
		}
		return numericValue;
	}
	
	public int compareTo(Attribute that) {
		if (this.type != that.type) {
			throw new IllegalStateException("Attempt to compare a numeric attribute to a categorical attribute in compareTo method");
		}
		// MISSING > Having a value
		if (this.missing && that.missing) {
			return 0;
		}
		if (this.missing && !that.missing) {
			return 1;
		}
		if (!this.missing && that.missing) {
			return -1;
		}
		if (this.type == Type.Numeric) {
			return DoubleCompare.compare(this.numericValue, that.numericValue);
		}
		if (this.type == Type.Categorical) {
			return this.categoricalValue.compareTo(that.categoricalValue);
		}
		throw new IllegalStateException("Unsupported attribute type in Attribute.compareTo " + this.type.name());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((categoricalValue == null) ? 0 : categoricalValue.hashCode());
		result = prime * result + (missing ? 1231 : 1237);
		result = prime * result
				+ ((numericValue == null) ? 0 : numericValue.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Attribute other = (Attribute) obj;
		if (categoricalValue == null) {
			if (other.categoricalValue != null)
				return false;
		} else if (!categoricalValue.equals(other.categoricalValue))
			return false;
		if (missing != other.missing)
			return false;
		if (numericValue == null) {
			if (other.numericValue != null)
				return false;
		} else if (!numericValue.equals(other.numericValue))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
