package moa.streams;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import java.util.Objects;
import java.util.Random;

/**
 * This stream generator is a wrapper that takes any stream generator and
 * removes the label of randomly selected instances
 */
public class SemiSupervisedStream extends AbstractOptionHandler implements InstanceStream {

    /** The stream generator to be wrapped up */
    private InstanceStream stream;

    /** Type of stream generator to be used */
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream generator to simulate semi-supervised setting", InstanceStream.class,
            "generators.SEAGenerator");

    /** Float option to choose the threshold above which the label of an instance will be removed */
    public FloatOption thresholdOption = new FloatOption("threshold", 't',
            "The probability threshold above which an instance will have its labels removed",
            0.5);

    /** Random generator to obtain the probability of removing label from an instance.
     * It is not seeded otherwise the probability will always be the same number */
    private Random random = new Random();

    /** Threshold above which the label of an instance will be removed */
    private double threshold;

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "A wrapper that takes any stream generator and " +
                "removes the label of randomly selected instances to simulate semi-supervised setting";
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // sets up the threshold
        this.threshold = this.thresholdOption.getValue();

        // sets up the stream generator
        this.stream = (InstanceStream) getPreparedClassOption(this.streamOption);
        if (this.stream instanceof AbstractOptionHandler) {
            ((AbstractOptionHandler) this.stream).prepareForUse(monitor, repository);
        }
    }

    @Override
    public InstancesHeader getHeader() {
        Objects.requireNonNull(this.stream, "The stream must not be null");
        return stream.getHeader();
    }

    @Override
    public long estimatedRemainingInstances() {
        Objects.requireNonNull(this.stream, "The stream must not be null");
        return stream.estimatedRemainingInstances();
    }

    @Override
    public boolean hasMoreInstances() {
        Objects.requireNonNull(this.stream, "The stream must not be null");
        return stream.hasMoreInstances();
    }

    @Override
    public Example<Instance> nextInstance() {
        Objects.requireNonNull(this.stream, "The stream must not be null");

        Example<Instance> inst = this.stream.nextInstance();
        // if the probability is above the threshold, remove the label
        if (this.random.nextDouble() >= this.threshold) {
            InstanceExample instEx = new InstanceExample(inst.getData().copy());
            instEx.instance.setMissing(instEx.instance.classIndex());
            return instEx;
        }
        // else just return the labeled instance
        return inst;
    }

    @Override
    public boolean isRestartable() {
        Objects.requireNonNull(this.stream, "The stream must not be null");
        return this.stream.isRestartable();
    }

    @Override
    public void restart() {
        Objects.requireNonNull(this.stream, "The stream must not be null");
        this.stream.restart();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO
    }
}
