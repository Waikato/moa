/*
 *    StochasticGradientTree.java
 *    Copyright (C) 2025 University of Waikato, Hamilton, New Zealand
 *    @author Henry Gouk (henry.gouk@ed.ac.uk)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.classifiers.trees;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.MultiLabelClassifier;
import moa.classifiers.MultiTargetRegressor;
import moa.classifiers.Regressor;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Statistics;
import moa.core.StringUtils;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

public class StochasticGradientTree extends AbstractClassifier implements MultiClassClassifier, Regressor, MultiLabelClassifier, MultiTargetRegressor {
    private static final long serialVersionUID = 1L;

    protected Node root;
    protected Discretizer[] discretizers;
    protected int numObservations;

    public ClassOption discretizerOption = new ClassOption("discretizer", 'D',
            "Discretizer to use for numeric attributes.", Discretizer.class, EqualWidthDiscretizer.class.getName() + " -bins 64");

    public IntOption gracePeriodOption = new IntOption("gracePeriod", 'G',
            "The number of instances a leaf should observe between split attempts.", 200, 1, Integer.MAX_VALUE);

    public FloatOption lambdaOption = new FloatOption("lambda", 'L',
            "Regularization parameter lambda.", 0.1, 0.0, Float.MAX_VALUE);

    public IntOption warmStartOption = new IntOption("warmStart", 'W',
            "Number of instances to use for fitting the discritizers", 1000, 0, Integer.MAX_VALUE);

    public FloatOption confidenceOption = new FloatOption("confidence", 'C',
            "The level of confidence required that a split candidate is an improvement before before the split is actually performed.", 1E-6, 0.0, 1.0);

    public MultiChoiceOption splitTestOption = new MultiChoiceOption("splitTest", 'T',
            "Which type of hypothesis test to use for determining when to split.",
            new String[] { "TTest" },
            new String[] { "Use a t-Test for checking statistical significance." },
            0);

    public FlagOption disableResplitsOption = new FlagOption("disableResplits", 'R',
            "Disable node resplitting.");

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        List<Measurement> ret = new LinkedList<Measurement>();

        ret.add(new Measurement("leaf nodes", root == null ? 0 : root.getNumLeafNodes()));
        ret.add(new Measurement("nodes", root == null ? 0 : root.getNumNodes()));

        return ret.toArray(new Measurement[ret.size()]);
    }

    @Override
    public void resetLearningImpl() {
        root = null;
        discretizers = null;
        numObservations = 0;
    }

    @Override
    public void trainOnInstanceImpl(MultiLabelInstance inst) {
        trainOnInstanceImpl((Instance) inst);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (root == null) {
            if (inst instanceof MultiLabelInstance) {
                int numLabels = ((MultiLabelInstance) inst).numOutputAttributes();
                int numValues = 0;

                for (int i = 0; i < numLabels; i++) {
                    if (inst.outputAttribute(i).isNominal()) {
                        numValues += inst.outputAttribute(i).numValues();
                    } else {
                        numValues += 1;
                    }
                }

                root = new Node(new double[numValues]);
            } else {
                if (inst.classAttribute().isNominal()) {
                    root = new Node(new double[inst.classAttribute().numValues()]);
                } else {
                    root = new Node(new double[1]);
                }
            }
        }

        numObservations++;

        if (numObservations <= warmStartOption.getValue()) {
            observeDiscretizers(inst);
            return;
        }

        if (!disableResplitsOption.isSet()) {
            Node current = root;

            while (current != null) {
                trainNode(inst, current);
                current = current.getChild(inst);
            }
        } else {
            Node leaf = root.findLeaf(inst);
            trainNode(inst, leaf);
        }
    }

    protected void trainNode(Instance inst, Node node) {
        node.updateStats(inst, computeGradients(inst, node.getPredictions(inst)));

        if (node.getCount() % gracePeriodOption.getValue() == 0) {
            Split split = node.findBestSplit();

            if (splitTestOption.getChosenIndex() == 0) {
                double c = Statistics.normalInverse(1.0 - confidenceOption.getValue());
                double bestUpper = split.deltaLoss + c * Math.sqrt(split.deltaLossVar / node.getCount());
                double baselineLower = split.baselineLoss - c * Math.sqrt(split.baselineVar / node.getCount());

                if (bestUpper < baselineLower) {
                    node.applySplit(split);
                }
            } else {
                throw new IllegalStateException("Unknown split test option: " + splitTestOption.getChosenIndex());
            }
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (root == null) {
            return new double[7];
        }

        double[] votes = root.findLeaf(inst).getPredictions(inst);

        if (inst instanceof MultiLabelInstance) {
            int ctr = 0;
            MultiLabelInstance mlInst = (MultiLabelInstance) inst;

            for (int i = 0; i < mlInst.numOutputAttributes(); i++) {
                if (mlInst.outputAttribute(i).isNominal()) {
                    int numValues = mlInst.outputAttribute(i).numValues();
                    double[] dist = new double[numValues];
                    System.arraycopy(votes, ctr, dist, 0, numValues);
                    softmax(dist);
                    System.arraycopy(dist, 0, votes, ctr, numValues);
                    ctr += numValues;
                } else {
                    ctr += 1;
                }
            }
        } else {
            if (inst.classAttribute().isNominal()) {
                softmax(votes);
            }
        }

        return votes;

        // if (inst.classAttribute().isNominal()) {
        //     if (root == null) {
        //         return new double[inst.classAttribute().numValues()];
        //     } else {
        //         Node leaf = root.findLeaf(inst);
        //         double[] values = leaf.getPredictions(inst);
        //         softmax(values);
        //         return values;
        //     }
        // } else {
        //     if (root == null) {
        //         return new double[] { 0.0 };
        //     } else {
        //         Node leaf = root.findLeaf(inst);
        //         return leaf.getPredictions(inst);
        //     }
        // }
    }

    @Override
    public Prediction getPredictionForInstance(MultiLabelInstance inst) {
        return getPredictionForInstance(inst);
    }

    @Override
    public Prediction getPredictionForInstance(Instance inst) {
        double[] votes = getVotesForInstance(inst);

        if (!(inst instanceof MultiLabelInstance)) {
            Prediction pred = new MultiLabelPrediction(1);
            pred.setVotes(votes);
            return pred;
        }

        Prediction prediction = new MultiLabelPrediction(inst.numOutputAttributes());

        int ctr = 0;

        for (int i = 0; i < inst.numOutputAttributes(); i++) {
            if (inst.outputAttribute(i).isNominal()) {
                double[] dist = new double[inst.outputAttribute(i).numValues()];
                System.arraycopy(votes, ctr, dist, 0, dist.length);
                prediction.setVotes(i, dist);
                ctr += dist.length;
            } else {
                prediction.setVotes(i, new double[] { votes[ctr] });
                ctr += 1;
            }
        }

        return prediction;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        if (root != null) {
            root.describeSubtree(out, indent);
        }
    }

    protected void observeDiscretizers(Instance inst) {
        if (discretizers == null) {
            discretizers = new Discretizer[inst.numAttributes()];
            for (int i = 0; i < inst.numAttributes(); i++) {
                if (i != inst.classIndex() && inst.attribute(i).isNumeric()) {
                    discretizers[i] = (Discretizer) getPreparedClassOption(discretizerOption);
                }
            }
        }

        for (int i = 0; i < inst.numAttributes(); i++) {
            if (i != inst.classIndex() && inst.attribute(i).isNumeric()) {
                discretizers[i].observe(inst.value(i));
            }
        }
    }

    protected void softmax(double[] vec) {
        double max = Double.NEGATIVE_INFINITY;

        for (double v : vec) {
            if (v > max) {
                max = v;
            }
        }

        double sum = 0.0;

        for (int i = 0; i < vec.length; i++) {
            vec[i] = Math.exp(vec[i] - max);
            sum += vec[i];
        }

        for (int i = 0; i < vec.length; i++) {
            vec[i] /= sum;
        }
    }

    protected Gradients computeGradients(Instance inst, double[] predictions) {
        if (inst instanceof MultiLabelInstance) {
            MultiLabelInstance mlInst = (MultiLabelInstance) inst;
            int numLabels = mlInst.numberOutputTargets();
            double[][] gradients = new double[numLabels][];
            double[][] hessians = new double[numLabels][];
            int ctr = 0;

            for (int i = 0; i < numLabels; i++) {
                if (mlInst.outputAttribute(i).isNominal()) {
                    int label = (int) mlInst.classValue(i);
                    gradients[i] = new double[mlInst.outputAttribute(i).numValues()];
                    hessians[i] = new double[mlInst.outputAttribute(i).numValues()];
                    double[] preds = new double[mlInst.outputAttribute(i).numValues()];
                    System.arraycopy(predictions, ctr, preds, 0, preds.length);
                    computeSoftmaxGradients(preds, label, gradients[i], hessians[i]);
                    ctr += preds.length;
                } else {
                    double label = mlInst.classValue(i);
                    gradients[i] = new double[1];
                    hessians[i] = new double[1];
                    computeSquaredErrorGradients(predictions[i], label, gradients[i], hessians[i]);
                    ctr += 1;
                }
            }

            Gradients grads = new Gradients(predictions.length);
            ctr = 0;

            // Flatten gradients and hessians
            for (int i = 0; i < numLabels; i++) {
                for (int j = 0; j < gradients[i].length; j++) {
                    grads.gradients[ctr] = gradients[i][j];
                    grads.hessians[ctr] = hessians[i][j];
                    ctr++;
                }
            }

            return grads;
        } else {
            if (inst.classAttribute().isNominal()) {
                int label = (int) inst.classValue();
                Gradients grads = new Gradients(predictions.length);
                computeSoftmaxGradients(predictions, label, grads.gradients, grads.hessians);
                return grads;
            } else {
                double label = inst.classValue();
                Gradients grads = new Gradients(1);
                computeSquaredErrorGradients(predictions[0], label, grads.gradients, grads.hessians);
                return grads;
            }
        }
    }

    protected void computeSoftmaxGradients(double[] predictions, int label, double[] grads, double[] hess) {
        predictions = predictions.clone();
        softmax(predictions);

        for (int i = 0; i < predictions.length; i++) {
            double prob = predictions[i];
            double indicator = (i == label) ? 1.0 : 0.0;
            grads[i] = prob - indicator;
            hess[i] = prob * (1.0 - prob);
        }
    }

    protected void computeSquaredErrorGradients(double prediction, double label, double[] grads, double[] hess) {
        grads[0] = prediction - label;
        hess[0] = 1.0;
    }

    public abstract static class Discretizer extends AbstractOptionHandler {
        public IntOption binsOption = new IntOption("bins", 'b',
            "Number of bins for discretization.", 64, 1, Integer.MAX_VALUE);

        abstract void observe(double value);
        abstract int getBin(double value);

        @Override
        public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        }
    }

    public static class EqualWidthDiscretizer extends Discretizer {
        private static final long serialVersionUID = 1L;

        private double min;
        private double max;
        private boolean initialized = false;
        
        @Override
        public void observe(double value) {
            if (!initialized) {
                min = value;
                max = value;
                initialized = true;
            } else {
                if (value < min) {
                    min = value;
                }

                if (value > max) {
                    max = value;
                }
            }
        }

        @Override
        public int getBin(double value) {
            int numBins = binsOption.getValue();

            if (!initialized || numBins <= 0 || min == max) {
                return 0;
            }

            double binWidth = (max - min) / numBins;
            int bin = (int) ((value - min) / binWidth);

            if (bin < 0) {
                return 0;
            }

            if (bin >= numBins) {
                return numBins - 1;
            }

            return bin;
        }

        @Override
        public String getPurposeString() {
            return "Discretizer that uses equal-width bins.";
        }

        @Override
        public Discretizer copy() {
            EqualWidthDiscretizer copy = new EqualWidthDiscretizer();
            copy.binsOption = (IntOption) this.binsOption.copy();
            copy.min = this.min;
            copy.max = this.max;
            copy.initialized = this.initialized;
            return copy;
        }

        @Override
        public void getDescription(StringBuilder sb, int indent) {
            String pad = new String(new char[indent]).replace('\0', ' ');
            sb.append(pad).append(this.getClass().getName()).append("\n");
            sb.append(pad).append("Bins: ").append(binsOption.getValue()).append("\n");
            sb.append(pad).append("Min: ").append(min).append("\n");
            sb.append(pad).append("Max: ").append(max).append("\n");
        }
    }

    protected static class Gradients implements Serializable {
        private static final long serialVersionUID = 1L;

        public double[] gradients;
        public double[] hessians;

        public Gradients(int size) {
            this.gradients = new double[size];
            this.hessians = new double[size];
        }

        public void add(Gradients other) {
            if (this.gradients.length != other.gradients.length) {
                throw new IllegalArgumentException("Gradient sizes do not match.");
            }

            for (int i = 0; i < this.gradients.length; i++) {
                this.gradients[i] += other.gradients[i];
                this.hessians[i] += other.hessians[i];
            }
        }

        public void subtract(Gradients other) {
            if (this.gradients.length != other.gradients.length) {
                throw new IllegalArgumentException("Gradient sizes do not match.");
            }

            for (int i = 0; i < this.gradients.length; i++) {
                this.gradients[i] -= other.gradients[i];
                this.hessians[i] -= other.hessians[i];
            }
        }

        public Gradients clone() {
            Gradients copy = new Gradients(this.gradients.length);
            System.arraycopy(this.gradients, 0, copy.gradients, 0, this.gradients.length);
            System.arraycopy(this.hessians, 0, copy.hessians, 0, this.hessians.length);
            return copy;
        }
    }

    protected static class GradientStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private int count;
        private Gradients sum;
        private Gradients scaledVariance;
        private double[] scaledCovariance;

        public GradientStats(int size) {
            this.count = 0;
            this.sum = new Gradients(size);
            this.scaledVariance = new Gradients(size);
            this.scaledCovariance = new double[size];
        }
        
        public void add(Gradients grads) {
            if (grads.gradients.length != sum.gradients.length) {
                throw new IllegalArgumentException("Gradient sizes do not match.");
            }

            Gradients oldMean = getMean();
            sum.add(grads);
            count++;
            Gradients newMean = getMean();

            for (int i = 0; i < grads.gradients.length; i++) {
                scaledVariance.gradients[i] += (grads.gradients[i] - oldMean.gradients[i]) * (grads.gradients[i] - newMean.gradients[i]);
                scaledVariance.hessians[i] += (grads.hessians[i] - oldMean.hessians[i]) * (grads.hessians[i] - newMean.hessians[i]);
                scaledCovariance[i] += (grads.gradients[i] - oldMean.gradients[i]) * (grads.hessians[i] - newMean.hessians[i]);
            }
        }

        public void add(GradientStats other) {
            if (other.sum.gradients.length != this.sum.gradients.length) {
                throw new IllegalArgumentException("Gradient sizes do not match.");
            }

            if (other.count == 0) {
                return;
            }

            if (this.count == 0) {
                this.count = other.count;
                this.sum = other.sum.clone();
                this.scaledVariance = other.scaledVariance.clone();
                this.scaledCovariance = other.scaledCovariance.clone();
                return;
            }

            Gradients meanDiff = other.getMean();
            meanDiff.subtract(this.getMean());

            for (int i = 0; i < sum.gradients.length; i++) {
                this.scaledVariance.gradients[i] += other.scaledVariance.gradients[i] + (this.count * other.count * meanDiff.gradients[i] * meanDiff.gradients[i]) / (this.count + other.count);
                this.scaledVariance.hessians[i] += other.scaledVariance.hessians[i] + (this.count * other.count * meanDiff.hessians[i] * meanDiff.hessians[i]) / (this.count + other.count);
                this.scaledCovariance[i] += other.scaledCovariance[i] + (this.count * other.count * meanDiff.gradients[i] * meanDiff.hessians[i]) / (this.count + other.count);
            }

            sum.add(other.sum);
            this.count += other.count;
        }

        public Gradients getMean() {
            if (count == 0) {
                return new Gradients(sum.gradients.length);
            }

            Gradients mean = new Gradients(sum.gradients.length);

            for (int i = 0; i < sum.gradients.length; i++) {
                mean.gradients[i] = sum.gradients[i] / count;
                mean.hessians[i] = sum.hessians[i] / count;
            }

            return mean;
        }

        public Gradients getVariance() {
            if (count <= 1) {
                Gradients ret = new Gradients(sum.gradients.length);

                for (int i = 0; i < sum.gradients.length; i++) {
                    ret.gradients[i] = Double.POSITIVE_INFINITY;
                    ret.hessians[i] = Double.POSITIVE_INFINITY;
                }
            }

            Gradients variance = new Gradients(sum.gradients.length);

            for (int i = 0; i < sum.gradients.length; i++) {
                variance.gradients[i] = scaledVariance.gradients[i] / (count - 1);
                variance.hessians[i] = scaledVariance.hessians[i] / (count - 1);
            }

            return variance;
        }

        public double[] getCovariance() {
            double[] covariance = new double[scaledCovariance.length];

            if (count <= 1) {
                for (int i = 0; i < scaledCovariance.length; i++) {
                    covariance[i] = Double.POSITIVE_INFINITY;
                }
            } else {
                for (int i = 0; i < scaledCovariance.length; i++) {
                    covariance[i] = scaledCovariance[i] / (count - 1);
                }
            }
            
            return covariance;
        }

        public int getCount() {
            return count;
        }
    }

    protected static class Split implements Serializable {
        private static final long serialVersionUID = 1L;

        public int attributeIndex;
        public int threshold;
        public boolean isNominal;
        public double deltaLoss;
        public double deltaLossVar;
        public double[][] deltaValues;
        public double baselineLoss;
        public double baselineVar;
    }

    protected class Node implements Serializable {
        private static final long serialVersionUID = 1L;

        private double[] values;
        private Node[] children;
        private int splitAttributeIndex;
        private int splitThreshold;
        private GradientStats totalStats;
        private GradientStats[][] attributeStats;

        public Node(double[] values) {
            this.values = values.clone();
            this.children = null;
            this.totalStats = null;
            this.attributeStats = null;
            this.splitAttributeIndex = -1;
            this.splitThreshold = -1;
        }

        public int getNumLeafNodes() {
            if (this.children == null) {
                return 1;
            } else {
                int l = 0;

                for (Node n : children) {
                    l += n.getNumLeafNodes();
                }

                return l;
            }
        }

        public int getNumNodes() {
            int l = 1;

            if (children != null) {
                for (Node n : children) {
                    l += n.getNumNodes();
                }
            }

            return l;
        }

        public void describeSubtree(StringBuilder out, int indent) {
            if (this.children == null) {
                StringUtils.appendIndented(out, indent, values.toString());
                StringUtils.appendNewline(out);
            } else {
                if (splitThreshold != -1) {
                    for (int i = 0; i < children.length; i++) {
                        StringUtils.appendIndented(out, indent, "if att_" + splitAttributeIndex + " == " + i + ":\n");
                        children[i].describeSubtree(out, indent + 2);
                    }
                } else {
                    StringUtils.appendIndented(out, indent, "if att_" + splitAttributeIndex + " <= " + splitThreshold + ":\n");
                    children[0].describeSubtree(out, indent + 2);
                    StringUtils.appendIndented(out, indent, "else:\n");
                    children[1].describeSubtree(out, indent + 2);
                }
            }
        }

        public Node getChild(Instance inst) {
            if (children == null) {
                return null;
            } else {
                if (inst.attribute(splitAttributeIndex).isNominal()) {
                    int attrValue = (int) inst.value(splitAttributeIndex);
                    return children[attrValue];
                } else {
                    int attrValue = discretizers[splitAttributeIndex].getBin(inst.value(splitAttributeIndex));

                    if (attrValue <= splitThreshold) {
                        return children[0];
                    } else {
                        return children[1];
                    }
                }
            }
        }

        public Node findLeaf(Instance inst) {
            if (children == null) {
                return this;
            } else {
                return getChild(inst).findLeaf(inst);
            }
        }

        public void updateStats(Instance inst, Gradients grads) {
            if (totalStats == null) {
                totalStats = new GradientStats(values.length);
            }

            if (attributeStats == null) {
                attributeStats = new GradientStats[inst.numAttributes()][];

                for (int i = 0; i < inst.numAttributes(); i++) {
                    if (i != inst.classIndex()) {
                        if (inst.attribute(i).isNominal()) {
                            attributeStats[i] = new GradientStats[inst.attribute(i).numValues()];

                            for (int j = 0; j < inst.attribute(i).numValues(); j++) {
                                attributeStats[i][j] = new GradientStats(values.length);
                            }
                        } else {
                            int numBins = discretizers[i].binsOption.getValue();
                            attributeStats[i] = new GradientStats[numBins];

                            for (int j = 0; j < numBins; j++) {
                                attributeStats[i][j] = new GradientStats(values.length);
                            }
                        }
                    }
                }
            }

            totalStats.add(grads);

            for (int i = 0; i < inst.numAttributes(); i++) {
                if (i != inst.classIndex()) {
                    int bin;

                    if (inst.attribute(i).isNominal()) {
                        bin = (int) inst.value(i);
                    } else {
                        bin = discretizers[i].getBin(inst.value(i));
                    }

                    attributeStats[i][bin].add(grads);
                }
            }
        }

        protected double combineMean(double mean1, int count1, double mean2, int count2) {
            if (count1 + count2 == 0) {
                return 0.0;
            }

            return (mean1 * count1 + mean2 * count2) / (count1 + count2);
        }

        protected double combineVariance(double mean1, double var1, int count1, double mean2, double var2, int count2) {
            if (count1 == 0) {
                return var2;
            }

            if (count2 == 0) {
                return var1;
            }
            
            double m = combineMean(mean1, count1, mean2, count2);

            double s1 = var1 * (count1 - 1);
            double s2 = var2 * (count2 - 1);

            double t1 = s1 + count1 * Math.pow(mean1, 2);
            double t2 = s2 + count2 * Math.pow(mean2, 2);
            double t = t1 + t2;

            double s = t / (count1 + count2) - m;
            return ((double)(count1 + count2) / (count1 + count2 - 1)) * s;
        }

        protected double[] computeDeltaPreds(GradientStats stats) {
            double[] deltaPreds = new double[stats.sum.gradients.length];
            Gradients mean = stats.getMean();

            for (int i = 0; i < deltaPreds.length; i++) {
                deltaPreds[i] = -mean.gradients[i] / (mean.hessians[i] + lambdaOption.getValue());
            }

            return deltaPreds;
        }

        protected double computeLossMean(GradientStats stats, double[] deltaPreds) {
            double loss = 0.0;
            Gradients mean = stats.getMean();

            for (int i = 0; i < mean.gradients.length; i++) {
                loss += mean.gradients[i] * deltaPreds[i]
                    + 0.5 * mean.hessians[i] * Math.pow(deltaPreds[i], 2);
            }

            return loss;
        }

        protected double computeLossVar(GradientStats stats, double[] deltaPreds) {
            double lossVar = 0.0;
            Gradients vars = stats.getVariance();
            double[] covs = stats.getCovariance();

            for (int i = 0; i < deltaPreds.length; i++) {
                lossVar += Math.pow(deltaPreds[i], 2) * vars.gradients[i]
                    + 0.25 * Math.pow(deltaPreds[i], 4) * vars.hessians[i]
                    + Math.pow(deltaPreds[i], 3) * covs[i];
            }

            return lossVar;
        }

        public Split computeNominalSplit(int attributeIndex) {
            Split split = new Split();
            split.attributeIndex = attributeIndex;
            split.isNominal = true;
            split.deltaValues = new double[attributeStats[attributeIndex].length][values.length];
            split.deltaLoss = 0.0;
            int obs = 0;

            for (int j = 0; j < attributeStats[attributeIndex].length; j++) {
                split.deltaValues[j] = computeDeltaPreds(attributeStats[attributeIndex][j]);
                double leafMean = computeLossMean(attributeStats[attributeIndex][j], split.deltaValues[j]);
                double leafVar = computeLossVar(attributeStats[attributeIndex][j], split.deltaValues[j]);
                int leafObs = attributeStats[attributeIndex][j].getCount();
                split.deltaLoss = combineMean(split.deltaLoss, obs, leafMean, leafObs);
                split.deltaLossVar = combineVariance(split.deltaLoss, split.deltaLossVar, obs, leafMean, leafVar, leafObs);
                obs += leafObs;
            }

            return split;
        }

        public Split computeNumericSplit(int attributeIndex, int threshold, GradientStats leftStats, GradientStats rightStats) {
            Split candidate = new Split();
            candidate.attributeIndex = attributeIndex;
            candidate.isNominal = false;
            candidate.deltaValues = new double[2][];

            double[] leftDeltaValues = computeDeltaPreds(leftStats);
            double leftMean = computeLossMean(leftStats, leftDeltaValues);
            double leftVar = computeLossVar(leftStats, leftDeltaValues);
            int leftObs = leftStats.getCount();
            
            double[] rightDeltaValues = computeDeltaPreds(rightStats);
            double rightMean = computeLossMean(rightStats, rightDeltaValues);
            double rightVar = computeLossVar(rightStats, rightDeltaValues);
            int rightObs = rightStats.getCount();
            
            double splitMean = combineMean(leftMean, leftObs, rightMean, rightObs);
            double splitVar = combineVariance(leftMean, leftVar, leftObs, rightMean, rightVar, rightObs);

            candidate.deltaLoss = splitMean;
            candidate.deltaLossVar = splitVar;
            candidate.threshold = threshold;
            candidate.deltaValues[0] = leftDeltaValues;
            candidate.deltaValues[1] = rightDeltaValues;

            return candidate;
        }

        public Split findBestSplit() {
            Split best = new Split();
            best.deltaLoss = Double.POSITIVE_INFINITY;
            double baselineLoss = 0.0;
            double baselineVar = 0.0;
            
            for (int i = 0; i < attributeStats.length; i++) {
                if (attributeStats[i] == null) {
                    // This attribute is the class attribute
                    continue;
                }

                if (discretizers[i] == null) {
                    Split candidate = computeNominalSplit(i);

                    if (candidate.deltaLoss < best.deltaLoss) {
                        best = candidate;
                    }

                    if (this.splitAttributeIndex == i) {
                        baselineLoss = candidate.deltaLoss;
                        baselineVar = candidate.deltaLossVar;
                    }
                } else {
                    GradientStats[] forwardCumulative = new GradientStats[attributeStats[i].length - 1];
                    GradientStats[] backwardCumulative = new GradientStats[attributeStats[i].length - 1];

                    for (int j = 0; j < forwardCumulative.length; j++) {
                        forwardCumulative[j] = new GradientStats(values.length);
                        forwardCumulative[j].add(attributeStats[i][j]);

                        if (j > 0) {
                            forwardCumulative[j].add(forwardCumulative[j - 1]);
                        }
                    }

                    for (int j = backwardCumulative.length - 1; j >= 0; j--) {
                        backwardCumulative[j] = new GradientStats(values.length);
                        backwardCumulative[j].add(attributeStats[i][j + 1]);

                        if (j + 1 < backwardCumulative.length) {
                            backwardCumulative[j].add(backwardCumulative[j + 1]);
                        }
                    }

                    for (int j = 0; j < forwardCumulative.length; j++) {
                        Split res = computeNumericSplit(i, j, forwardCumulative[j], backwardCumulative[j]);

                        if (res.deltaLoss < best.deltaLoss) {
                            best = res;
                        }

                        if (this.splitAttributeIndex == i && this.splitThreshold == j) {
                            baselineLoss = res.deltaLoss;
                            baselineVar = res.deltaLossVar;
                        }
                    }
                }
            }

            best.baselineLoss = baselineLoss;
            best.baselineVar = baselineVar;

            return best;
        }

        public void applySplit(Split split) {
            if (disableResplitsOption.isSet()) {
                totalStats = null;
                attributeStats = null;
            }

            this.splitAttributeIndex = split.attributeIndex;
            this.splitThreshold = split.threshold;

            if (split.isNominal) {
                this.children = new Node[split.deltaValues.length];

                for (int i = 0; i < split.deltaValues.length; i++) {
                    double[] newValues = new double[values.length];

                    for (int j = 0; j < values.length; j++) {
                        newValues[j] = values[j] + split.deltaValues[i][j];
                    }

                    this.children[i] = new Node(newValues);
                }
            } else {
                this.children = new Node[2];

                double[] leftValues = new double[values.length];
                double[] rightValues = new double[values.length];

                for (int j = 0; j < values.length; j++) {
                    leftValues[j] = values[j] + split.deltaValues[0][j];
                    rightValues[j] = values[j] + split.deltaValues[1][j];
                }

                this.children[0] = new Node(leftValues);
                this.children[1] = new Node(rightValues);
            }
        }

        public int getCount() {
            return totalStats == null ? 0 : totalStats.getCount();
        }

        public double[] getPredictions(Instance inst) {
            return values.clone();
        }
    }
}
