/*
 *    Cluster.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.cluster;

import java.util.*;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.AbstractMOAObject;
import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.GaussianNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.Utils;

public abstract class Cluster extends AbstractMOAObject {

	private static final long serialVersionUID = 1L;

	private double id = -1;
    private double gtLabel = -1;

    private HashMap<String, String> measure_values;

    public Cluster() {
        this.measure_values = new HashMap<>();
        this.labelFeature = new LabelFeature(0.0);
    }

    public Cluster(double decayFactor) {
        this.measure_values = new HashMap<>();
        this.labelFeature = new LabelFeature(decayFactor);
    }

    /**
     * @return the center of the cluster
     */
    public abstract double[] getCenter();

    /**
     * Returns the weight of this cluster, not necessarily normalized.
     * It could, for instance, simply return the number of points contained
     * in this cluster.
     * @return the weight
     */
    public abstract double getWeight();

    /**
     * Returns the probability of the given point belonging to
     * this cluster.
     *
     * @param instance
     * @return a value between 0 and 1
     */
    public abstract double getInclusionProbability(Instance instance);


    //TODO: for non sphere cluster sample points, find out MIN MAX neighbours within cluster
    //and return the relative distance
    //public abstract double getRelativeHullDistance(Instance instance);
    
    @Override
    public void getDescription(StringBuilder sb, int i) {
        sb.append("Cluster Object");
    }

    public void setId(double id) {
        this.id = id;
    }

    public double getId() {
        return id;
    }

    public boolean isGroundTruth(){
        return gtLabel != -1;
    }

    public void setGroundTruth(double truth){
        gtLabel = truth;
    }

    public double getGroundTruth(){
        return gtLabel;
    }


    /**
     * Samples this cluster by returning a point from inside it.
     * @param random a random number source
     * @return an Instance that lies inside this cluster
     */
    public abstract Instance sample(Random random);


    public void setMeasureValue(String measureKey, String value){
        measure_values.put(measureKey, value);
    }

    public void setMeasureValue(String measureKey, double value){
        measure_values.put(measureKey, Double.toString(value));
    }


    public String getMeasureValue(String measureKey){
        if(measure_values.containsKey(measureKey))
            return measure_values.get(measureKey);
        else
            return "";
    }


    protected void getClusterSpecificInfo(ArrayList<String> infoTitle,ArrayList<String> infoValue){
        infoTitle.add("ClusterID");
        infoValue.add(Integer.toString((int)getId()));

        infoTitle.add("Type");
        infoValue.add(getClass().getSimpleName());

        double c[] = getCenter();
        if(c!=null)
        for (int i = 0; i < c.length; i++) {
            infoTitle.add("Dim"+i);
            infoValue.add(Double.toString(c[i]));
        }

        infoTitle.add("Weight");
        infoValue.add(Double.toString(getWeight()));
        
    }

    public String getInfo() {
        ArrayList<String> infoTitle = new ArrayList<String>();
        ArrayList<String> infoValue = new ArrayList<String>();
        getClusterSpecificInfo(infoTitle, infoValue);

        StringBuffer sb = new StringBuffer();

        //Cluster properties
        sb.append("<html>");
        sb.append("<table>");
        int i = 0;
        while(i < infoTitle.size() && i < infoValue.size()){
            sb.append("<tr><td>"+infoTitle.get(i)+"</td><td>"+infoValue.get(i)+"</td></tr>");
            i++;
        }
        sb.append("</table>");

        //Evaluation info
        sb.append("<br>");
        sb.append("<b>Evaluation</b><br>");
        sb.append("<table>");
        Iterator miterator = measure_values.entrySet().iterator();
        while(miterator.hasNext()) {
             Map.Entry e = (Map.Entry)miterator.next();
             sb.append("<tr><td>"+e.getKey()+"</td><td>"+e.getValue()+"</td></tr>");
        }
        sb.append("</table>");
        sb.append("</html>");
        return sb.toString();
    }


    ////////////////////////////////////
    // From this part it concerns SSL //
    ////////////////////////////////////

    /** Keeps track of the count of the class labels */
    protected LabelFeature labelFeature;

    /** Observe the distribution of attribute values per class (one observer per attribute) */
    private AutoExpandVector<AttributeClassObserver> attrObservers = new AutoExpandVector<>();

    /** The base learner to train on labeled points in this cluster*/
    protected Classifier learner;

    /** Number of labeled data points */
    protected int numLabeledPoints;

    /** Number of unlabeled data points */
    protected int numUnlabeledPoints;

    // some getter and setter methods
    public Classifier getLearner() { return this.learner; }
    public void setLearner(Classifier learner) { this.learner = learner; }
    public int getNumLabeledPoints() { return this.numLabeledPoints; }
    public int getNumUnlabeledPoints() { return this.numUnlabeledPoints; }

    /**
     * Increments the count of a label in this cluster.
     * If the label is not yet seen in the cluster, its count will be initialized
     *
     * @param inst the instance, may have label or not
     * @param amount the amount to increment
     */
    public void updateLabelWeight(Instance inst, double amount, long timestamp) {
        if (!inst.classIsMissing() && !inst.classIsMasked()) {
            // update the label's weight
            this.labelFeature.increment(inst.classValue(), amount, timestamp);
            // update the observer
            for (int i = 0; i < inst.numAttributes(); i++) {
                AttributeClassObserver obs = attrObservers.get(i);
                // initialize the observer if needed
                if (obs == null) {
                    obs = inst.attribute(i).isNominal() ?
                            new NominalAttributeClassObserver() : new GaussianNumericAttributeClassObserver();
                    this.attrObservers.set(i, obs);
                }
                // update the observer
                obs.observeAttributeClass(inst.value(i), (int)inst.classValue(), inst.weight());
            }
            numLabeledPoints++;
        } else {
            numUnlabeledPoints++;
        }
    }

    /**
     * Returns the attribute per class observer
     * @return the observer
     */
    public AutoExpandVector<AttributeClassObserver> getAttributeObservers() {
        return this.attrObservers;
    }

    /**
     * Gets the majority label in this cluster
     * @return the majority label
     */
    public double getMajorityLabel() {
        return this.labelFeature.getMajorityLabel();
    }

    /**
     * Gets the frequency of each label in this cluster
     * @return the frequency (probability) of each label in this cluster
     */
    public double[] getLabelVotes() {
        return this.labelFeature.getVotes();
    }

    /**
     * Returns the label count
     * @return the label count
     */
    public LabelFeature getLabelFeature() { return this.labelFeature; }

    /**
     * Returns a clone of the label count
     * @return a clone of the label count
     */
    public LabelFeature getLabelFeatureCopy() { return this.labelFeature.copy(); }

    /**
     * Accumulates the label weight from another cluster, and dynamically decays the weights
     * @param c a cluster
     * @param timestamp the timestamp when it is updated
     */
    public void accumulateWeight(Cluster c, long timestamp) {
        this.labelFeature.accumulate(c.labelFeature, timestamp);
    }

    /**
     * Accumulates the label weight from another cluster
     * @param c a cluster
     */
    public void accumulateWeight(Cluster c) {
        this.labelFeature.accumulate(c.labelFeature);
    }

    /**
     * Sets the decay factor for the label feature
     * @param lambda the decay factor
     */
    public void setDecayFactor(double lambda) {
        this.labelFeature.setDecayFactor(lambda);
    }

    /**
     * Gets the center point of the cluster
     * @return the center point, as a pseudo instance
     */
    public abstract Instance getCenterPoint(InstancesHeader header);

    /**
     * An simple ensemble that keeps the base learners in one cluster.
     * This is to handle the case of merging clusters of CluStream
     */
    class ClusterEnsemble extends AbstractClassifier {

        /** The base learner in the ensemble*/
        private Classifier baseLearner;

        /** Size of the ensemble */
        private int size;

        /** A fixed-size ensemble of models */
        private Classifier[] ensemble;

        /** To keep track of learners that have not been created in the ensemble */
        private int filled;

        /**
         * Initializes the ensemble
         * @param baseLearner the base learner
         * @param size the size of the ensemble
         */
        ClusterEnsemble(Classifier baseLearner, int size) {
            this.baseLearner = baseLearner.copy();
            this.size = size;
            this.ensemble = new Classifier[this.size];
            this.ensemble[0] = this.baseLearner.copy(); // init the first learner in the ensemble
            this.filled = 0;
        }

        public Classifier[] models() {
            return ensemble;
        }

        @Override
        public double[] getVotesForInstance(Instance inst) {
            // majority voting from all the learners
            // TODO weighted?
            DoubleVector combinedVotes = new DoubleVector();
            for (Classifier classifier : ensemble) {
                if (classifier == null) continue;
                double[] votes = classifier.getVotesForInstance(inst);
                for (int i = 0; i < votes.length; i++) {
                    combinedVotes.addToValue(i, votes[i]);
                }
            }
            combinedVotes.normalize();
            return combinedVotes.getArrayRef();
        }

        @Override
        public void resetLearningImpl() {
            this.baseLearner.resetLearning(); // reset the base learner
            this.ensemble = new Classifier[this.size]; // empty the ensemble
        }

        @Override
        public void trainOnInstanceImpl(Instance inst) {
            // for a new instance, train it on all the learners in the ensemble?
            for (Classifier classifier : ensemble) {
                if (classifier == null) continue;
                classifier.trainOnInstance(inst);
            }
        }

        /**
         * Adds a new classifier in the ensemble (when the clusters are merged)
         * @param classifier the classifier
         */
        public void addModel(Classifier classifier, Instance inst) {
            if (classifier == null) return;
            // if ensemble is not full, add the model in
            // else, replace the one with the lowest confidence score on the given instance
            if (filled < size) {
                ensemble[filled++] = classifier;
            } else {
                // create a copy of the ensemble to operate on
                Classifier[] _ensemble = new Classifier[ensemble.length + 1];
                int i;
                for (i = 0; i < ensemble.length; i++) { _ensemble[i] = ensemble[i]; }
                _ensemble[i] = classifier;

                // compute the confidence scores of each learner
                double[] confidences = computeConfidences(inst, _ensemble);

                // sort the confidence by indices
                Integer[] indices = new Integer[confidences.length];
                for (i = 0; i < confidences.length; i++) { indices[i] = i; }
                Comparator<Integer> comp = new ClassifierComparator(confidences);
                Arrays.sort(indices, comp);

                // put the top k (size) result to the ensemble
                for (i = 0; i < size; i++) {
                    ensemble[i] = _ensemble[indices[size - i - 1]];
                }
            }
        }

        private class ClassifierComparator implements Comparator<Integer> {

            private double[] content;

            ClassifierComparator(double[] content) { this.content = content; }

            @Override
            public int compare(Integer i1, Integer i2) {
                return Double.compare(content[i1], content[i2]);
            }
        }

        /**
         * Computes the confidence score computed from the prediction of each learner in the ensemble and
         * from the new learner to be added in
         * @param inst the instance
         * @param models the ensemble of models
         * @return the array of confidence scores, with the first k scores from the ensemble, and
         * the last one from the new model
         */
        private double[] computeConfidences(Instance inst, Classifier[] models) {
            // get confidences from the ensemble & from the new learner
            double[] confidences = new double[models.length];
            for (int i = 0; i < models.length; i++) {
                int label = Utils.maxIndex(models[i].getVotesForInstance(inst));
                confidences[i] = models[i].getConfidenceForPrediction(inst, label);
            }
            return confidences;
        }

        @Override
        protected Measurement[] getModelMeasurementsImpl() {
            return new Measurement[0];
        }

        @Override
        public void getModelDescription(StringBuilder out, int indent) { }

        @Override
        public boolean isRandomizable() {
            return false;
        }
    }
}
