package moa.classifiers.functions;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * Majority class learner using window time.
 * It always returns the majority class from the last captured window
 */
public class WindowMajorityClass extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Majority class classifier: always predicts the class that has been observed most frequently the in the training data.";
    }

    public IntOption windowSizeOption = new IntOption("windowSize", 'w',
            "The size of the window to observe the classes", 1000);

    /** Keeps track of the class count */
    private DoubleVector observedClasses;

    /** The window size */
    private int windowSize;

    /** The pointer that checks whether the window is full*/
    private int pointer;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);
        this.windowSize = this.windowSizeOption.getValue();
        this.pointer = 0;
        this.observedClasses = new DoubleVector();
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        this.observedClasses.normalize();
        return this.observedClasses.getArrayCopy();
    }

    @Override
    public void resetLearningImpl() {
        this.observedClasses = new DoubleVector();
        this.pointer = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.pointer < this.windowSize) {
            if (!inst.classIsMissing() && !inst.classIsMasked()) {
                this.observedClasses.addToValue((int) inst.classValue(), inst.weight());
            }
            this.pointer++;
        } else {
            this.resetLearningImpl();
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
