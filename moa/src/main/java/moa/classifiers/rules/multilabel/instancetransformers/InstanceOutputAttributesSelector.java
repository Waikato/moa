package moa.classifiers.rules.multilabel.instancetransformers;

import java.util.ArrayList;
import java.util.List;

import moa.AbstractMOAObject;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import com.yahoo.labs.samoa.instances.Range;

/**
 * Transforms instances considering only a subset of output attributes
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */
public class InstanceOutputAttributesSelector extends AbstractMOAObject implements InstanceTransformer {

	private static final long serialVersionUID = 1L;

	public InstancesHeader targetInstances;
	public int [] targetOutputIndices;
	public int numSourceInstancesOutputs;

	public InstanceOutputAttributesSelector(){
		
	}
	public InstanceOutputAttributesSelector(InstancesHeader sourceInstances, int [] targetOutputIndices){
		this.targetOutputIndices=targetOutputIndices;
		this.numSourceInstancesOutputs=sourceInstances.numOutputAttributes();

		int totAttributes=sourceInstances.numInputAttributes()+this.targetOutputIndices.length;
		targetInstances= new InstancesHeader();

		List<Attribute> v = new ArrayList<Attribute>(totAttributes);
		List<Integer> indexValues = new ArrayList<Integer>(totAttributes);
		int numInputs=sourceInstances.numInputAttributes();
		for (int i=0; i<numInputs;i++)
		{
			v.add(sourceInstances.inputAttribute(i));
			indexValues.add(i);
		}

		for (int i=0; i<this.targetOutputIndices.length;i++)
		{
			v.add(sourceInstances.outputAttribute(this.targetOutputIndices[i]));
			indexValues.add(numInputs+i);
		}

		targetInstances.setAttributes(v,indexValues);
		Range r= new Range("-" + targetOutputIndices.length);
		r.setUpper(totAttributes);
		targetInstances.setRangeOutputIndices(r);
	}


	@Override
	public Instance sourceInstanceToTarget(Instance sourceInstance) {
		double [] attValues = new double[targetInstances.numAttributes()];
		Instance newInstance=new InstanceImpl(sourceInstance.weight(),attValues);
		int numInputs=this.targetInstances.numInputAttributes();
		for (int i=0; i<numInputs; i++){
			newInstance.setValue(i, sourceInstance.valueInputAttribute(i));
		}
		for (int i=0; i<this.targetOutputIndices.length; i++){
			newInstance.setValue(numInputs+i, sourceInstance.valueOutputAttribute(targetOutputIndices[i]));
		}
		newInstance.setDataset(targetInstances);
		return newInstance;
	}

	@Override
	public Prediction targetPredictionToSource(Prediction targetPrediction) {
		Prediction sourcePrediction=new MultiLabelPrediction(this.numSourceInstancesOutputs);
		for (int i=0; i<targetPrediction.numOutputAttributes();i++){
			sourcePrediction.setVotes(this.targetOutputIndices[i], targetPrediction.getVotes(i));
		}
		return sourcePrediction;
	}


	@Override
	public void getDescription(StringBuilder sb, int indent) {	
	}



}
