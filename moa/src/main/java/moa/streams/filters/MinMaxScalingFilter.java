package moa.streams.filters;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.SparseInstance;
import moa.core.InstanceExample;

import java.util.ArrayList;
import java.util.List;

public class MinMaxScalingFilter extends AbstractStreamFilter {

    private static final long serialVersionUID = 1L;

    private double[] LS;

    private double[] min;

    private double[] max;

    private double N;

    private int d;

    protected InstancesHeader streamHeader;

    @Override
    public String getPurposeString() {
        return "Apply online normalization on the data";
    }

    @Override
    protected void restartImpl() {
        this.d = 0;
        this.N = 0;
        this.LS = new double[0];
        this.min = new double[0];
        this.max = new double[0];
        this.streamHeader = null;
    }

    @Override
    public InstanceExample nextInstance() {
        Instance inst = (Instance) this.inputStream.nextInstance().getData();
        if (streamHeader == null) streamHeader = inst.getHeader();

        /* Initialization */
        if (d == 0) {
            d = inst.numAttributes();
            LS = new double[d];
            N = 0;
            initMin();
            initMax();
        }

        /* Accumulates the values */
        accumulate(inst);

        /* Do normalization */
        double[] values = new double[d];
        List<Integer> masked = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            double value = inst.getMaskedValue(i);
            if (inst.isMasked(i)) masked.add(i);
            if (inst.classIndex() == i) values[i] = value;
            else values[i] = (value - getMean(i)) / (max[i] - min[i]);
        }

        /* Create a new instance */
        Instance instance = (inst instanceof SparseInstance ?
                new SparseInstance(inst.weight(), values) :
                new DenseInstance(inst.weight(), values, masked, streamHeader));

        return new InstanceExample(instance);
    }

    private void accumulate(Instance X) {
        N++;
        for (int i = 0; i < d; i++) {
            LS[i] += X.value(i);
            if (X.value(i) > max[i]) max[i] = X.value(i);
            if (X.value(i) < min[i]) min[i] = X.value(i);
        }
    }

    private void initMin() {
        min = new double[d];
        for (int i = 0; i < d; i++) min[i] = Double.MAX_VALUE;
    }

    private void initMax() {
        max = new double[d];
        for (int i = 0; i < d; i++) max[i] = -Double.MAX_VALUE;
    }

    private double getMean(int index) {
        return (N > 0 ? LS[index] / N : 0);
    }

    @Override
    public InstancesHeader getHeader() {
        return streamHeader;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
