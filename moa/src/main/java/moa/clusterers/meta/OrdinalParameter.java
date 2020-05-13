package moa.clusterers.meta;

import com.yahoo.labs.samoa.instances.Attribute;

// the representation of an integer parameter
public class OrdinalParameter implements IParameter {
	private String parameter;
	private String value;
	private int numericValue;
	private String[] range;
	private double std;
	private Attribute attribute;
	private boolean optimise;

	// copy constructor
	public OrdinalParameter(OrdinalParameter x) {
		this.parameter = x.parameter;
		this.value = x.value;
		this.numericValue = x.numericValue;
		this.attribute = x.attribute;
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = x.range.clone();
			this.std = x.std;
		}
	}

	// init constructor
	public OrdinalParameter(ParameterConfiguration x) {
		this.parameter = x.parameter;
		this.value = String.valueOf(x.value);
		this.attribute = new Attribute(x.parameter);
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = new String[x.range.length];
			for (int i = 0; i < x.range.length; i++) {
				range[i] = String.valueOf(x.range[i]);
				if (this.range[i].equals(this.value)) {
					this.numericValue = i; // get index of init value
				}
			}
			this.std = (this.range.length - 0) / 2;
		}

	}

	public OrdinalParameter copy() {
		return new OrdinalParameter(this);
	}

	public String getCLIString() {
		return ("-" + this.parameter + " " + this.value);
	}

	public String getCLIValueString() {
		return ("" + this.value);
	}

	public double getValue() {
		return this.numericValue;
	}

	public String getParameter() {
		return this.parameter;
	}

	public void sampleNewConfig(double lambda, double reset, int verbose) {

		if(!this.optimise){
			return;
		}
		
		// update configuration
		if (Math.random() < reset) {
			double upper = (double) (this.range.length - 1);
			this.std = upper / 2;
		}

		// treat index of range as integer parameter
		TruncatedNormal trncnormal = new TruncatedNormal(this.numericValue, this.std, 0.0, (double) (this.range.length - 1)); // limits are the indexes of the range
		int newValue = (int) Math.round(trncnormal.sample());

		if (verbose >= 3) {
			System.out.println("Sample new configuration for ordinal parameter -" + this.parameter + " with mean: "
					+ this.numericValue + ", std: " + this.std + ", lb: " + 0 + ", ub: " + (this.range.length - 1)
					+ "\t=>\t -" + this.parameter + " " + this.range[newValue] + " (" + newValue + ")");
		}

		this.numericValue = newValue;
		this.value = this.range[this.numericValue];

		// adapt distribution
		this.std = this.std * Math.pow(2, -1 * lambda);
	}

}