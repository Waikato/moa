package moa.learners.featureanalysis;

import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.CapabilitiesHandler;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.Measurement;
import moa.core.Utils;
import moa.options.ClassOption;

/**
 * HoeffdingTree Ensemble Feature Importance.
 *
 * <p>This produce feature importances from ensembles of HoeffdingTree models and its subclasses.
 * This class does not interfere with the training algorithm of the underlying ensemble model.
 * The base learner of the ensemble model must be either a HoeffdingTree or one of its subclasses. </p>
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Rodrigo Fernandes de Mello, Bernhard Pfahringer, Albert Bifet.
 * Feature Scoring using Tree-Based Ensembles for Evolving Data Streams.
 * IEEE International Conference on Big Data (pp. 761-769), 2019</p>
 * </p>
 *
 * <p>Parameters:</p> <ul>
 * <li>-l : The ensemble classifier used to train and to be analyzed. </li>
 * <li>-t : HoeffdingTree FeatureImportance object. Important: the learner option of the HoeffdingTreeFeatureImportance
 * object is overridden by the ensemble base tree model. </li>
 * </ul>
 *
 * @author Heitor Murilo Gomes (heitor dot gomes at waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class FeatureImportanceHoeffdingTreeEnsemble extends AbstractClassifier implements MultiClassClassifier,
        CapabilitiesHandler, FeatureImportanceClassifier {

    public ClassOption ensembleLearnerOption = new ClassOption("ensembleLearner", 'l',
            "Ensemble learner to train and analyze.", Classifier.class,
            "moa.classifiers.meta.AdaptiveRandomForest");

    public ClassOption hoeffdingTreeFeatureImportanceOption = new ClassOption("hoeffdingTreeFeatureImportance", 't',
            "Hoeffding Tree object to use. Its learner option is overridden by the ensemble base tree model.",
            FeatureImportanceHoeffdingTree.class,
            "FeatureImportanceHoeffdingTree");

    protected Classifier ensemble;
    protected FeatureImportanceHoeffdingTree htFeatureImportanceBase;
    protected double[] featureImportances;

    @Override
    public double[] getFeatureImportances(boolean normalize) {

        /* We assume the subClassifiers are instances of HoeffdingTree */
        Classifier[] subClassifiers = (Classifier[]) this.ensemble.getSublearners();

        if(subClassifiers != null) {
            FeatureImportanceHoeffdingTree[] subFeatureImportanceWrapper = new FeatureImportanceHoeffdingTree[subClassifiers.length];

            for(int i = 0 ; i < subClassifiers.length ; ++i) {
                subFeatureImportanceWrapper[i] = (FeatureImportanceHoeffdingTree) this.htFeatureImportanceBase.copy();
                subFeatureImportanceWrapper[i].treeLearner = (HoeffdingTree) subClassifiers[i];
                if(subFeatureImportanceWrapper[i].featureImportances == null) {
                    if (this.featureImportances != null)
                        subFeatureImportanceWrapper[i].featureImportances = new double[this.featureImportances.length];
                    else {
                        System.err.println("Unable to infer the number of features. " +
                                "trainOnInstance() must be invoked prior to getFeatureImportances()");
                    }
                }
            }

            if (this.featureImportances != null)
                this.featureImportances = new double[this.featureImportances.length];

            for(int i = 0 ; i < subClassifiers.length ; ++i) {
                double[] treeFeatureImportances = subFeatureImportanceWrapper[i].getFeatureImportances(normalize);

                for(int j = 0 ; j < this.featureImportances.length ; ++j)
                    this.featureImportances[j] += Double.isNaN(treeFeatureImportances[j]) ? 0 : treeFeatureImportances[j];
            }

            // normalize the featureImportances
            // TODO: Double check if we should not sum after storing the new featureImportances[i]
            if(normalize) {
                double sumFeatureImportances = Utils.sum(this.featureImportances);
                for (int i = 0; i < this.featureImportances.length; ++i) {
                    this.featureImportances[i] /= sumFeatureImportances;
                }
            }
        }


        return this.featureImportances;
    }

    @Override
    public int[] getTopKFeatures(int k, boolean normalize) {
        if(this.getFeatureImportances(normalize) == null)
            return null;
        if(k > this.getFeatureImportances(normalize).length)
            k = this.getFeatureImportances(normalize).length;

        int[] topK = new int[k];
        double[] currentFeatureImportances = new double[this.getFeatureImportances(normalize).length];
        for(int i = 0 ; i < currentFeatureImportances.length ; ++i)
            currentFeatureImportances[i] = this.getFeatureImportances(normalize)[i];

        for(int i = 0 ; i < k ; ++i) {
            int currentTop = Utils.maxIndex(currentFeatureImportances);
            topK[i] = currentTop;
            currentFeatureImportances[currentTop] = -1;
        }
        return topK;
    }

    @Override
    public void resetLearningImpl() {
        this.ensemble = (Classifier) getPreparedClassOption(this.ensembleLearnerOption);
        this.ensemble.resetLearning();

        this.htFeatureImportanceBase = (FeatureImportanceHoeffdingTree) getPreparedClassOption(this.hoeffdingTreeFeatureImportanceOption);

        if(this.ensemble.getSubClassifiers() == null) {
            System.err.println("The classifier is not an ensemble or does not implement the getSubClassifiers() method. ");
        }

    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        // Initialize the featureImportances array.
        if (this.featureImportances == null)
            this.featureImportances = new double[instance.numAttributes() - 1];

        this.ensemble.trainOnInstance(instance);
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {
        return this.ensemble.getVotesForInstance(instance);
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return this.ensemble.getModelMeasurements();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }
}
