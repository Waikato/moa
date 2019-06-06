package moa.streams.filters;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.SparseInstance;
import moa.core.InstanceExample;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter to do online normalization on the data
 */
public class StandardizationFilter extends AbstractStreamFilter {

    private static final long serialVersionUID = 1L;

    private double[] LS;

    private double[] SS;

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
        this.SS = new double[0];
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
            SS = new double[d];
            N = 0;
        }

        /* Accumulates the values */
        accumulate(inst);

        /* Do normalization */
        List<Integer> masked = new ArrayList<>();
        double[] values = new double[d];
        for (int i = 0; i < d; i++) {
            double value = inst.getMaskedValue(i);
            if (inst.isMasked(i)) masked.add(i);
            if (inst.classIndex() == i) values[i] = value;
            else values[i] = (value - getMean(i)) / getDeviation(i);
        }

        /* Create a new instance */
        Instance instance = (inst instanceof  SparseInstance ?
                new SparseInstance(inst.weight(), values) :
                new DenseInstance(inst.weight(), values, masked, streamHeader));

        return new InstanceExample(instance);
    }

    private void accumulate(Instance X) {
        N++;
        if (N == 1) {
            for (int i = 0; i < d; i++) {
                LS[i] = X.value(i);
                SS[i] = 0;
            }
        } else {
            double LS_prev;
            for (int i = 0; i < d; i++) {
                LS_prev = LS[i];
                LS[i] = LS[i] + (X.value(i) - LS_prev) / N;
                SS[i] = SS[i] + (X.value(i) - LS_prev) * (X.value(i) - LS[i]);
            }
        }
    }

    private double getMean(int index) {
        return (N > 0 ? LS[index] : 0);
    }

    private double getVariance(int index) {
        return (N > 1 ? SS[index] / (N - 1) : 0);
    }

    private double getDeviation(int index) {
        return Math.sqrt(getVariance(index));
    }

    @Override
    public InstancesHeader getHeader() {
        return streamHeader;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
