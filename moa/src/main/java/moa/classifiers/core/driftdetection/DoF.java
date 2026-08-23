package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Arrays;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.core.Measurement;
import moa.options.ClassOption;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * <p>Parinaz Sobhani and Hamid Beigy: New Drift Detection Method for Data
 * Streams. Adaptive and Intelligent Systems 2011: Volume 6943, pp 88-97.</p>
 *
 * Adapted to change classifier using threshold.
 *
 * @author Silas Garrido (sgtcs@cin.ufpe.br)
 *
 */
public class DoF extends AbstractClassifier {

    public ClassOption learnerOption = new ClassOption("Learner", 'l',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");
    public IntOption windowSizeOption = new IntOption(
            "windowSize", 'w', "Window size.", 15, 0, Integer.MAX_VALUE);
    public FloatOption thresholdOption = new FloatOption("stddev",
            's', "Standard deviations.", 2.0);
    protected Classifier classifier;
    protected LinkedList<Instance> currentBatch;
    protected LinkedList<Instance> referenceBatch;
    protected double[] minAttributes;
    protected double[] maxAttributes;
    protected double[] knnWeight;
    protected int[] knnIndex;
    protected int countBatch;
    protected boolean newReferenceBatch;
    protected boolean initializeRange;
    protected int changeDetected = 0;
    private RunningStat rs;

    private void initialize() {
        this.currentBatch = new LinkedList();
        this.referenceBatch = new LinkedList();
        this.knnWeight = new double[this.windowSizeOption.getValue()];
        this.knnIndex = new int[this.windowSizeOption.getValue()];
        this.countBatch = 0;
        this.newReferenceBatch = true;
        this.initializeRange = true;
        this.rs = new RunningStat();
    }

    @Override
    public void resetLearningImpl() {
        this.classifier = ((Classifier) getPreparedClassOption(this.learnerOption)).copy();
        this.classifier.resetLearning();
        initialize();
    }

    private void initializeRangeAttributes(Instance inst) {
        this.minAttributes = new double[inst.numAttributes() - 1];
        this.maxAttributes = new double[inst.numAttributes() - 1];
        Arrays.fill(this.minAttributes, Double.MAX_VALUE);
        Arrays.fill(this.maxAttributes, Double.MIN_VALUE);
    }

    private void calcRangeAttributes(Instance inst) {
        double[] temp = inst.toDoubleArray();

        for (int i = 0; i < this.minAttributes.length; i++) {
            if ((inst.attribute(i).isNumeric()) && (temp[i] < this.minAttributes[i])) {
                this.minAttributes[i] = temp[i];
            }

            if ((inst.attribute(i).isNumeric()) && (temp[i] > this.maxAttributes[i])) {
                this.maxAttributes[i] = temp[i];
            }
        }
    }

    private double calcDist(Instance inst1, Instance inst2) {
        double[] temp1 = inst1.toDoubleArray();
        double[] temp2 = inst2.toDoubleArray();
        double dist = 0.0, tempDist = 0.0;
        int numAttributes = inst1.numAttributes() - 1;

        for (int i = 0; i < numAttributes; i++) {
            if (inst1.attribute(i).isNumeric()) {
                tempDist = ((temp1[i] - temp2[i]) / (this.maxAttributes[i] - this.minAttributes[i]));
            } else {
                if (temp1[i] != temp2[i]) {
                    tempDist = 1.0;
                }
            }

            tempDist = Math.pow(tempDist, 2.0);
            dist += tempDist;
        }

        return Math.sqrt(dist);
    }

    private void nearestNeighbor() {
        Arrays.fill(this.knnWeight, Double.MAX_VALUE);
        double tempDist;

        for (int i = 0; i < this.currentBatch.size(); i++) {
            for (int j = 0; j < this.referenceBatch.size(); j++) {
                tempDist = calcDist(this.currentBatch.get(i), this.referenceBatch.get(j));

                if (tempDist < this.knnWeight[i]) {
                    this.knnWeight[i] = tempDist;
                    this.knnIndex[i] = j;
                }
            }
        }
    }

    private double degreeOfDrift() {
        double numerator = 0.0, denominator = 0.0;
        double[] tempCurrent, tempReference;
        int position;

        for (int i = 0; i < this.knnWeight.length; i++) {
            tempCurrent = this.currentBatch.get(i).toDoubleArray();
            tempReference = this.referenceBatch.get(this.knnIndex[i]).toDoubleArray();
            position = this.currentBatch.get(i).numAttributes() - 1;

            if (tempCurrent[position] != tempReference[position]) {
                numerator += ((this.knnWeight[i] == 0.0) ? 0.0 : (1.0 / this.knnWeight[i]));
            }

            denominator += ((this.knnWeight[i] == 0.0) ? 0.0 : (1.0 / this.knnWeight[i]));
        }

        return numerator / denominator;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.initializeRange) {
            initializeRangeAttributes(inst);
            this.initializeRange = false;
        }

        calcRangeAttributes(inst);

        if (this.newReferenceBatch) {
            this.referenceBatch.addLast(inst);

            if (this.countBatch + 1 == this.windowSizeOption.getValue()) {
                this.countBatch = -1;
                this.newReferenceBatch = false;
            }
        } else {
            this.currentBatch.addLast(inst);

            if (this.countBatch + 1 == this.windowSizeOption.getValue()) {
                nearestNeighbor();

                double tempDof = degreeOfDrift();
                this.rs.put(tempDof);

                LinkedList<Instance> temp = this.referenceBatch;
                if (tempDof > this.rs.getAverage() + this.rs.getStandardDeviation() * this.thresholdOption.getValue() ||
                        tempDof < this.rs.getAverage() - this.rs.getStandardDeviation() * this.thresholdOption.getValue()) {
                    this.changeDetected++;
                    this.classifier.resetLearning();
                    initialize();
                    this.referenceBatch = temp;
                    this.newReferenceBatch = false;
                }

                this.countBatch = -1;
                this.currentBatch.clear();
            }
        }

        this.classifier.trainOnInstance(inst);
        this.countBatch++;
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        return this.classifier.getVotesForInstance(inst);
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        ((AbstractClassifier) this.classifier).getModelDescription(out, indent);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> measurementList = new LinkedList();
        measurementList.add(new Measurement("Change detected", this.changeDetected));
        Measurement[] modelMeasurements = ((AbstractClassifier) this.classifier).getModelMeasurements();
        if (modelMeasurements != null) {
            measurementList.addAll(Arrays.asList(modelMeasurements));
        }
        this.changeDetected = 0;
        return measurementList.toArray(new Measurement[measurementList.size()]);
    }
}

class RunningStat {

    private int count = 0;
    private double average = 0.0;
    private double pwrSumAvg = 0.0;
    private double stdDev = 0.0;

    /**
     * Incoming new values used to calculate the running statistics
     *
     * @param value
     */
    public void put(double value) {

        count++;
        average += (value - average) / count;
        pwrSumAvg += (value * value - pwrSumAvg) / count;
        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));

    }

    public double getAverage() {

        return average;
    }

    public double getStandardDeviation() {

        return Double.isNaN(stdDev) ? 0.0 : stdDev;
    }
}
