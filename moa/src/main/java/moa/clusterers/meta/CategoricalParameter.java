package moa.clusterers.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.yahoo.labs.samoa.instances.Attribute;

// the representation of a categorical / nominal parameter
public class CategoricalParameter implements IParameter {
	private String parameter;
	private int numericValue;
	private String value;
	private String[] range;
	private Attribute attribute;
	private ArrayList<Double> probabilities;
	private boolean optimise;

	public CategoricalParameter(CategoricalParameter x) {
		this.parameter = x.parameter;
		this.numericValue = x.numericValue;
		this.value = x.value;
		this.attribute = x.attribute;
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = x.range.clone();
			this.probabilities = new ArrayList<Double>(x.probabilities);
		}
	}

	public CategoricalParameter(ParameterConfiguration x) {
		this.parameter = x.parameter;
		this.value = String.valueOf(x.value);
		this.attribute = new Attribute(x.parameter, Arrays.asList(range));
		this.optimise = x.optimise;

		if(this.optimise){
			this.range = new String[x.range.length];
			for (int i = 0; i < x.range.length; i++) {
				range[i] = String.valueOf(x.range[i]);
				if (this.range[i].equals(this.value)) {
					this.numericValue = i; // get index of init value
				}
			}
			this.probabilities = new ArrayList<Double>(x.range.length);
			for (int i = 0; i < x.range.length; i++) {
				this.probabilities.add(1.0 / x.range.length); // equal probabilities
			}
		}
	}

	public CategoricalParameter copy() {
		return new CategoricalParameter(this);
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

	public String[] getRange() {
		return this.range;
	}

	public void sampleNewConfig(double lambda, double reset, int verbose) {

		if(!this.optimise){
			return;
		}

		if (Math.random() < reset) {
			for (int i = 0; i < this.probabilities.size(); i++) {
				this.probabilities.set(i, 1.0/this.probabilities.size());
			}
		}

		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		for (int i = 0; i < this.probabilities.size(); i++) {
			map.put(i, this.probabilities.get(i));
		}
		// update configuration
		this.numericValue = EnsembleClustererAbstract.sampleProportionally(map, true);
		String newValue = this.range[this.numericValue];

		if (verbose >= 3) {
			System.out
					.print("Sample new configuration for nominal parameter -" + this.parameter + "with probabilities");
			for (int i = 0; i < this.probabilities.size(); i++) {
				System.out.print(" " + this.probabilities.get(i));
			}
			System.out.println("\t=>\t -" + this.parameter + " " + newValue);
		}
		this.value = newValue;

		// adapt distribution
		// TODO not directly transferable from irace: (1-((iter -1) / maxIter))
		// this.probabilities.set(this.numericValue,
		// this.probabilities.get(this.numericValue) + (1.0/iter));
		this.probabilities.set(this.numericValue,
				this.probabilities.get(this.numericValue) * (2 - Math.pow(2, -1 * lambda)));

		// divide by sum (TODO is this even necessary with our proportional sampling
		// strategy?)
		double sum = 0.0;
		for (int i = 0; i < this.probabilities.size(); i++) {
			sum += this.probabilities.get(i);
		}
		for (int i = 0; i < this.probabilities.size(); i++) {
			this.probabilities.set(i, this.probabilities.get(i) / sum);
		}
	}
}