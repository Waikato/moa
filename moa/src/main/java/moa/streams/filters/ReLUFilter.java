package moa.streams.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moa.core.InstanceExample;
import moa.streams.MultiTargetInstanceStream;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Range;

public class ReLUFilter extends AbstractStreamFilter {

	/**
	 * ReLUFilter - A random projection of the filter space. 
	 * NOTE: Data should be standardized.
	 */
	private static final long serialVersionUID = 1L;
	protected InstancesHeader dataset;
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r', "Seed for random noise.", 1);
	public IntOption numLatentOption = new IntOption("numLatent", 'h', "Percent of basis functions (wrt number of input attributes).", 10);
	private int H = 200;
	protected Random random;
	protected double W[][];

	@Override
	public String getPurposeString() {
		return "Creates a random projection of the feature space with ReLU functions.";
	}

	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	/**
	 * Filter an instance.
	 * Assume that the instance has a single class label, as the final attribute. Note that this may not always be the case!
	 *
	 * @param	x	input instance
	 * @return	output instance
	 */
	public Instance filterInstance(Instance x) {


		if(dataset==null){
			initialize(x);
		}		

		double z_[] = new double[H+1];

		int d = x.numAttributes() - 1; // suppose one class attribute (at the end)

		for(int k = 0; k < H; k++) {
			// for each hidden unit ...
			double a_k = 0.; 								// k-th activation (dot product)
			for(int j = 0; j < d; j++) {
				a_k += (x.value(j) * W[k][j]);
			}
			z_[k] = (a_k > 0. ? a_k : 0.);				  // <------- can change threshold here
		}
		z_[H] = x.classValue();

		Instance z = new InstanceImpl(x.weight(),z_);
		z.setDataset(dataset);

		return z;
	}

	@Override
	protected void restartImpl() {
		this.random = new Random(this.randomSeedOption.getValue());
	}

	private void initialize(Instance instance) {
		this.random = new Random(this.randomSeedOption.getValue());

		int d = instance.numAttributes() - 1; // suppose one class attribute

		if (numLatentOption.getValue() < 0)
			// set the number
			H = numLatentOption.getValue();
		else
			// use as a percentage!
			H = d * numLatentOption.getValue() / 100;

		// initialize ReLU features
		W = new double[H][d];
		for(int j = 0; j < H; j++) {
			for(int k = 0; k < d; k++) {
				W[j][k] = this.random.nextGaussian();
			}
		}

		// initialize instance space
		Instances ds = new Instances();
		List<Attribute> v = new ArrayList<Attribute>(H);
		List<Integer> indexValues = new ArrayList<Integer>(H);

		for(int j = 0; j < H; j++) {
			v.add(new Attribute("z"+String.valueOf(j)));
			indexValues.add(j);
		}
		v.add(instance.dataset().classAttribute()); 
		indexValues.add(H);


		ds.setAttributes(v,indexValues);
		Range r= new Range("start-end");
		ds.setRangeOutputIndices(r);
		dataset=(new InstancesHeader(ds));
		dataset.setClassIndex(H);

	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
