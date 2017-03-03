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

public class RBFFilter extends AbstractStreamFilter {

	/**
	 * NOTE: DATA SHOULD BE STANDARDIZED FIRST
	 */
	private static final long serialVersionUID = 1L;
	protected InstancesHeader dataset;
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r', "Seed for random noise.", 1);
	public IntOption numLatentOption = new IntOption("numLatent", 'h', "Number of latent variables in the projected space.", 1);
	protected Random random;
	protected double c[];
	protected double r[];

    @Override
    public String getPurposeString() {
        return "Creates a random projection of the feature space with RBF functions.";
    }

	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	@Override
	public InstanceExample nextInstance() {

		Instance x = (Instance) ((Instance) this.inputStream.nextInstance().getData());

		if(dataset==null){
			System.out.println("INIT. ");
			initialize(x);
		}		

		double z_[] = new double[dataset.numAttributes()];
		Instance z = new InstanceImpl(x.weight(),z_);

		int d = x.numAttributes();
		int h = numLatentOption.getValue();
		int j_c = x.classIndex();

		for(int k = 0; k < h; k++) {
			double sum_k = 0.;
			for(int j = 0; j < d; j++) {
				sum_k += (x.value(j) - c[k]);
			}
			double v = sum_k / Math.pow(r[k],2);
			z.setValue(k,Math.exp(-sum_k));
		}
		z.setValue(h,x.classValue());
		z.setDataset(dataset);

		return new InstanceExample(z);
	}

	@Override
    protected void restartImpl() {
		System.out.println("RESET ");
        this.random = new Random(this.randomSeedOption.getValue());
    }

	private void initialize(Instance instance) {

		int h = numLatentOption.getValue();

		// initialize RBF features
		c = new double[h];
		r = new double[h];
		for(int j = 0; j < h; j++) {
			c[j] = this.random.nextGaussian();
			r[j] = this.random.nextDouble();
		}

		// initialize instance space
		Instances ds = new Instances();
		List<Attribute> v = new ArrayList<Attribute>(h);
		List<Integer> indexValues = new ArrayList<Integer>(h);

		for(int j = 0; j < h; j++) {
			v.add(new Attribute("z"+String.valueOf(j)));
			indexValues.add(j);
		}
		v.add(instance.dataset().classAttribute()); 
		indexValues.add(h);


		ds.setAttributes(v,indexValues);
		Range r= new Range("start-end");
		//r.setUpper(h);
		ds.setRangeOutputIndices(r);
		dataset=(new InstancesHeader(ds));
		dataset.setClassIndex(h);
		System.out.println(""+dataset);
		System.out.println("classIndex :  "+dataset.classIndex());
		System.out.println("numClasses :  "+dataset.numClasses());
		System.out.println("numInstances: "+dataset.numInstances());
		System.out.println("numAttribute: "+dataset.numAttributes());
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

}
