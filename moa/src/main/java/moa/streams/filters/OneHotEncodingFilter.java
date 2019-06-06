package moa.streams.filters;

import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.*;
import moa.core.DoubleVector;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.streams.InstanceStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter to one-hot encode categorical attributes
 */
public class OneHotEncodingFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "One-hot encode categorical attributes";
    }

    public FlagOption removeOtherAttributesOption = new FlagOption("removeOtherAttributes", 'r',
            "Check to remove all other numerical attributes and only keep one-hot encoded ones.");

    private static final long serialVersionUID = 1L;

    protected InstancesHeader streamHeader;

    private boolean removeOtherAttr;

    @Override
    protected void restartImpl() {
        this.streamHeader = null;
        this.removeOtherAttr = removeOtherAttributesOption.isSet();
    }

    @Override
    public InstanceExample nextInstance() {
        Instance inst = (Instance) this.inputStream.nextInstance().getData();

        // create a new header
        List<String> onehot = new ArrayList<>();
        onehot.add("0");
        onehot.add("1");

        FastVector attributes = new FastVector();
        DoubleVector values = new DoubleVector();
        List<Integer> masked = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < inst.numAttributes(); i++) {
            // TODO includes the class label or not??
            double value = inst.getMaskedValue(i);
            if (inst.isMasked(i)) masked.add(j);

            if (inst.attribute(i).isNominal() && i != inst.classIndex() && inst.attribute(i).numValues() > 2) {
                int V = inst.attribute(i).numValues();
                for (int k = 0; k < V; k++) {
                    attributes.addElement(new Attribute(inst.attribute(i).name() + "_" + k, onehot));
                    if (k == value) values.setValue(j, 1);
                    else values.setValue(j, 0);
                    j++;
                }
            } else {
                if (inst.attribute(i).isNumeric() && removeOtherAttr) continue;
                attributes.addElement(inst.attribute(i));
                values.setValue(j, value);
                j++;
            }
        }

        // create the new instance
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        Instance instance;
        if (inst instanceof SparseInstance) {
            instance = new SparseInstance(inst.weight(), values.getArrayCopy());
        } else {
            instance = new DenseInstance(inst.weight(), values.getArrayRef(), masked, streamHeader);
        }

        return new InstanceExample(instance);
    }

    @Override
    public InstancesHeader getHeader() {
        return streamHeader;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
