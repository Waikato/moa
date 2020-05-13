package moa.clusterers.meta;

import com.yahoo.labs.samoa.instances.Attribute;

// the representation of a numerical / real parameter
public class NumericalParameter implements IParameter {
	private String parameter;
	private double value;
	private double[] range;
	private double std;
	private Attribute attribute;
	private boolean optimise;

	public NumericalParameter(NumericalParameter x) {
		this.parameter = x.parameter;
		this.value = x.value;
		this.attribute = new Attribute(x.parameter);
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = x.range.clone();
			this.std = x.std;
		}

	}

	public NumericalParameter(ParameterConfiguration x) {
		this.parameter = x.parameter;
		this.value = (double) x.value;
		this.attribute = new Attribute(x.parameter);
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = new double[x.range.length];
			for (int i = 0; i < x.range.length; i++) {
				range[i] = (double) x.range[i];
			}
			this.std = (this.range[1] - this.range[0]) / 2;
		}
	}

	public NumericalParameter copy() {
		return new NumericalParameter(this);
	}

	public String getCLIString() {
		return ("-" + this.parameter + " " + this.value);
	}

	public String getCLIValueString() {
		return ("" + this.value);
	}

	public double getValue() {
		return this.value;
	}

	public String getParameter() {
		return this.parameter;
	}

	public void sampleNewConfig(double lambda, double reset,  int verbose) {

		if(!this.optimise){
			return;
		}

		// trying to balanced exploitation vs exploration by resetting the std
		if (Math.random() < reset) {
			this.std = (this.range[1] - this.range[0]) / 2;
		}

		// update configuration
		// for numeric features use truncated normal distribution
		TruncatedNormal trncnormal = new TruncatedNormal(this.value, this.std, this.range[0], this.range[1]);
		double newValue = trncnormal.sample();

		if (verbose >= 3) {
			System.out.println("Sample new configuration for numerical parameter -" + this.parameter + " with mean: "
					+ this.value + ", std: " + this.std + ", lb: " + this.range[0] + ", ub: " + this.range[1]
					+ "\t=>\t -" + this.parameter + " " + newValue);
		}

		this.value = newValue;

		// adapt distribution
		// this.std = this.std * (Math.pow((1.0 / nbNewConfigurations), (1.0 /
		// nbVariable)));

		this.std = this.std * Math.pow(2, -1 * lambda);

	}
}