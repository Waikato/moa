package moa.streams.filters;

import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.InstanceExample;
import moa.core.utils.AttributeDefinitionUtil;

public class SelectAttributesFilter extends AbstractMultiLabelStreamFilter implements MultiLabelStreamFilter {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected InstancesHeader dataset;
	public StringOption inputStringOption = new StringOption("inputStringOption", 'i',
			"Selection of attributes to be used as input.", "1");
	public StringOption outputStringOption = new StringOption("outputStringOption", 'o',
			"Selection of attributes to be used as output.", "-1");

	protected List<Integer> outputIndexes;
	protected List<Integer> inputIndexes;

	@Override
	public String getPurposeString() {
		return "Selects input and output attributes.";
	}

	@Override
	public InstancesHeader getHeader() {
		return dataset;
	}

	@Override
	public InstanceExample nextInstance() {
		Instance instance = (this.inputStream.nextInstance().getData());
		if (dataset == null) {
			initialize(instance);
		}
		return new InstanceExample(processInstance(instance));
	}

	private void initialize(Instance instance) {
		outputIndexes = AttributeDefinitionUtil.parseAttributeDefinition(outputStringOption.getValue(),
				instance.numAttributes(), null);
		inputIndexes = AttributeDefinitionUtil.parseAttributeDefinition(inputStringOption.getValue(),
				instance.numAttributes(), null);
		int totAttributes = inputIndexes.size() + outputIndexes.size();
		dataset = new InstancesHeader();
		List<Attribute> v = new ArrayList<>(totAttributes);
		List<Integer> newInstanceInputIndexes = new ArrayList<>();
		List<Integer> newInstanceOutputIndexes = new ArrayList<>();
		int count = 0;
		for (Integer i : inputIndexes) {
			v.add(instance.attribute(i));
			newInstanceInputIndexes.add(count);
			count++;
		}
		for (Integer i : outputIndexes) {
			v.add(instance.attribute(i));
			newInstanceOutputIndexes.add(count);
			count++;
		}
		dataset.setAttributes(v);
		dataset.setInputIndexes(newInstanceInputIndexes);
		dataset.setOutputIndexes(newInstanceOutputIndexes);
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	@Override
	protected void restartImpl() {

	}

	private Instance processInstance(Instance instance) {
		double[] attValues = new double[dataset.numAttributes()];
		Instance newInstance = new InstanceImpl(instance.weight(), attValues);
		newInstance.setDataset(dataset);

		for (Integer j : inputIndexes)
			newInstance.setValue(instance.attribute(j), instance.value(j));

		for (Integer j : outputIndexes)
			newInstance.setValue(instance.attribute(j), instance.value(j));
		return newInstance;
	}
}
