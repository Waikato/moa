package moa.classifiers.lazy;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;

/**
 * Weighted k-Nearest Neighbor.<p>
 *
 * Valid options are:<p>
 *
 * -k number of neighbours <br> -m max instances <br> 
 *
 * @author Maroua Bahri (maroua.bahri@inria.fr)
 * Paper:
 * "Effective Weighted k-Nearest Neighbors for Dynamic Data Streams"
 *  Maroua Bahri
 * https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=10020652
 * BibTex:
 * "@inproceedings{bahri2022effective,
 * title={Effective Weighted k-Nearest Neighbors for Dynamic Data Streams},
 * author={Bahri, Maroua},
 * booktitle={IEEE International Conference on Big Data (Big Data)},
 * pages={3341--3347},
 * year={2022},
 * organization={IEEE}
 * }"
 */
public class WeightedkNN extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

	public IntOption kOption = new IntOption( "k", 'k', "The number of neighbors", 10, 1, Integer.MAX_VALUE);

	public IntOption limitOption = new IntOption( "limit", 'w', "The maximum number of instances to store", 1000, 1, Integer.MAX_VALUE);

	int C = 0;
    @Override
    public String getPurposeString() {
        return "kNN: special.";
    }

    protected Instances window;


	@Override
	public void setModelContext(InstancesHeader context) {
		try {
			this.window = new Instances(context,0);  
			this.window.setClassIndex(context.classIndex());
		} catch(Exception e) {
			System.err.println("Error: no Model Context available.");
			e.printStackTrace();
			System.exit(1);
		}
	}

    @Override
    public void resetLearningImpl() {
		this.window = null;
	}

    @Override
    public void trainOnInstanceImpl(Instance inst) {
		// Weight the most recent instance
		inst.setWeight(2);
		if (inst.classValue() > C)
			C = (int)inst.classValue();
		if (this.window == null) {
			this.window = new Instances(inst.dataset());
		}
		if (this.limitOption.getValue() <= this.window.numInstances()) {
			this.window.delete(0);
		}
		this.window.add(inst);
		// Update the weight of instances inside the window
		if (this.window.size()>1 ){
			updateWeights();
		}

    }


	/** Update the weights of the instances using with 2 - (i-1)/(w-1), where i is the index of the current instance
	 *	Examples:
	 *  current:  2 - (1-1)/w = 2
	 *	old    :  2 - (w-1)/(w-1) = 1
     */
	 private void updateWeights(){
		for (int i=0 ; i < this.window.size() ; i++){
			double weight = 2-(double)(this.window.size()-i-1)/(this.window.size()-1);
			this.window.instance(i).setWeight(weight);
		}
	}


	@Override
	public double[] getVotesForInstance(Instance inst) {
		double v[] = new double[C+1];
		try {
			if(this.window.numInstances()>0) {
				int [] knnW = kNN(inst, this.window, Math.min(kOption.getValue(), this.window.numInstances()));

				for (int nnIdx : knnW) {
					v[(int) this.window.instance(nnIdx).classValue()]+=this.window.instance(nnIdx).weight();
				}
			}

		} catch(Exception e) {
			//System.err.println("Error: kNN search failed.");
			//e.printStackTrace();
			//System.exit(1);
			return new double[inst.numClasses()];
		}
		return v;
	}

	private int [] kNN(Instance sample, Instances samples, int k){
		double distances[] = get1ToNDistances(sample, samples);
		int nnIndices[] = nArgMin(k, distances);

		return nnIndices;
	}


	private double getDistance(Instance sample, Instance sample2)
	{
		double sum = 0;
		double diff;
		for (int i=0; i<sample.numInputAttributes(); i++)
		{
			if(sample.attribute(i).isNominal() == true) {
				if (isMissingValue(sample.valueInputAttribute(i)) ||
						isMissingValue(sample2.valueInputAttribute(i)) ||
						((int) sample.valueInputAttribute(i) != (int) sample2.valueInputAttribute(i))) {
					diff = 1.0;
				}
				else {
					diff = 0.0;
				}
			}
			else{// attribute(i).isNumeric
				if (isMissingValue(sample.valueInputAttribute(i)) ||
						isMissingValue(sample2.valueInputAttribute(i))) {
					if (isMissingValue(sample.valueInputAttribute(i)) &&
							isMissingValue(sample2.valueInputAttribute(i))) {
						diff = 1.0;
					}
					else{
						if (isMissingValue(sample2.valueInputAttribute(i)))
							diff = sample.valueInputAttribute(i);
						else diff = sample2.valueInputAttribute(i);
					}
				}
				else
					diff = sample.valueInputAttribute(i)-sample2.valueInputAttribute(i);
			}

			sum += diff*diff;
		}
		return Math.sqrt(sum);
	}

	public static boolean isMissingValue(double val) {

		return Double.isNaN(val);
	}

	/**
	 * Returns the Euclidean distance between one instance and a collection of instances in an 1D-array.
	 */
	private double[] get1ToNDistances(Instance sample, Instances samples){
		double distances[] = new double[samples.numInstances()];
		for (int i=0; i<samples.numInstances(); i++){
			distances[i] = this.getDistance(sample, samples.get(i));
		}
		return distances;
	}

	/**
	 * Returns the n smallest indices of the smallest values (sorted).
	 */
	private int[] nArgMin(int n, double[] values, int startIdx, int endIdx){
		int indices[] = new int[n];
		for (int i=0; i<n; i++){
			double minValue = Double.MAX_VALUE;
			for (int j=startIdx; j<endIdx+1; j++){
				if (values[j] < minValue){
					boolean alreadyUsed = false;
					for (int k=0; k<i; k++){
						if (indices[k]==j){
							alreadyUsed = true;
						}
					}
					if (!alreadyUsed){
						indices[i] = j;
						minValue = values[j];
					}
				}
			}
		}
		return indices;
	}

	private int[] nArgMin(int n, double[] values){
		return nArgMin(n, values, 0, values.length-1);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

	public boolean isRandomizable() {
		return false;
	}

}