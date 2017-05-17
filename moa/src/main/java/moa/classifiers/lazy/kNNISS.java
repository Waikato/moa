/*
 *    kNNBFE.java
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

package moa.classifiers.lazy;
import java.io.*;
import java.sql.Array;
import java.util.Arrays;
import java.util.List;

import com.github.javacliparser.*;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.lazy.neighboursearch.*;
import moa.classifiers.lazy.rankingfunctions.*;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Utils;

/**
 * k Nearest Neighbor with Iterative Subset Selection.<p>
 *
 * Valid options are:<p>
 *
 * -k number of neighbours <br> -m max instances <br> -f number of best features to select subsets from <br> -t interval between when features are re-ranked
 *
 * @author Lanqin Yuan (fyempathy@gmail.com)
 * Orignial MOA kNN Jesse Read (jesse@tsc.uc3m.es)
 * @version 1
 */
public class kNNISS extends AbstractClassifier
{

    private static final long serialVersionUID = 2L; // some random number i entered

    // options
	public IntOption kOption = new IntOption( "k", 'k', "The number of neighbours", 10, 1, Integer.MAX_VALUE);

	public IntOption limitOption = new IntOption( "limit", 'w', "The maximum number of instances to store", 1000, 1, Integer.MAX_VALUE);

    public IntOption featureLimitOption = new IntOption( "featureLimit", 'f', "The number of best features to select subsets from from", 10, 1, Integer.MAX_VALUE);
    public IntOption reselectionInterval = new IntOption( "reselectionInterval", 't', "The interval between when features are re-ranked", 1000, 1, Integer.MAX_VALUE);


    public FloatOption decayFactorOption = new FloatOption("decayFactor", 'd', "The threshold value.", 0.1, 0.0, 1.0);
    public FloatOption accuracyGainWeightOption = new FloatOption("accuracyGainFactor", 'g', "How much weight to put into accuracy gain for ranking features.", 0.0, 0.0, 1.0);

    public MultiChoiceOption rankingOption = new MultiChoiceOption(
        "ranking", 'm', "ranking method to use", new String[]{
            "SU","InfoGain","MeanDistance"},

        new String[]{
                "Symmetric Uncertainty",
                "Information gain",
                "Average Euclidean distance"
        }, 0);



    public FlagOption hillClimbOption = new FlagOption("hillCilmbing", 'h', "Whether or not to enable adaptive F via hill climbing");
    public IntOption hillClimbWindowOption = new IntOption( "hillClimbWindow", 'a', "The number of neighbours", 2, 0, 10);

    // accuracy difference
    public StringOption outputNameOption = new StringOption("outputName",'n',"filename for output of accuracy difference as features are added to the subsets","");
    public BufferedWriter bw;

    protected int[] bestFeatures;

    // correct and incorrect count for each class, used to rank subsets
    protected  int[] correctCount;
    protected  int[] wrongCount;
    protected double[] correctPercent;

    // 0 = subset of n features, n-1 = subset of only the top feature
    protected int bestSubset = 0;
    protected int featuresCount = 0;

    protected int reselectionCounter = 0;
    protected int C = 0;
    protected boolean initialised = false;

    protected RankingFunction rankingFunction = null;

    @Override
    public String getPurposeString() {
        return "kNNISS: kNN with ISS feature selection.";
    }

    protected Instances window;

	@Override
	public void setModelContext(InstancesHeader context)
    {
		try 
		{
			this.window = new Instances(context,0); //new StringReader(context.toString())
			this.window.setClassIndex(context.classIndex());
		}
		catch(Exception e)
        {
			System.err.println("Error: no model Context available.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 *  a method for initializing a classifier learner
	 */
    @Override
    public void resetLearningImpl() {
		this.window = null;
    }

	/**
	 * a method to train a new instance
	 * @param inst
     */
    @Override
    public void trainOnInstanceImpl(Instance inst) 
    {

        // sets C as the number of classes that a instance can be classified as and expands C if a new class value is seen
		if (inst.classValue() > C)
		{
            C = (int) inst.classValue();
        }

		// if window is empty, initialise the window
		if (this.window == null)
		{
			this.window = new Instances(inst.dataset());
		}
		
		// if window is full, delete last element in window
		if (this.limitOption.getValue() <= this.window.numInstances())
		{
		    rankingFunction.removeInstance(this.window.get(0));
			this.window.delete(0);
		}
		// add element to window
		this.window.add(inst);
        rankingFunction.addInstance(inst);
    }

	/**
	 * a method to obtain the prediction result
	 * @param inst instance to predict the class value of
	 * @return array containing votes for the class values
     */
	@Override
    public double[] getVotesForInstance(Instance inst)
    {
        // vote array for class
		double v[] = new double[C+1];

        // check if enough time as passed
        if(reselectionCounter <=0)
        {
            // get a new ranked list of features
            selectFeatureSubset(featureLimitOption.getValue());
            reselectionCounter = reselectionInterval.getValue();
        }
        else
        {
            reselectionCounter--;
        }

		try
        {
            int lowerBound = 0;
            int upperBound = -1;
            if(hillClimbOption.isSet())
            {
                lowerBound = bestSubset - hillClimbWindowOption.getValue();
                if(lowerBound < 0)
                    lowerBound = 0;
                upperBound = bestSubset + hillClimbWindowOption.getValue() + 1;
                if(upperBound >= featuresCount)
                    upperBound = featuresCount;
            }
            else
            {
                lowerBound = 0;
                upperBound =  featuresCount;
            }
            // initialise search
            CumulativeLinearNNSearch cumulativeLinearNNSearch = new CumulativeLinearNNSearch();
            cumulativeLinearNNSearch.initialiseCumulativeSearch(inst, this.window, bestFeatures,upperBound);

            // get a vote if there is enough instances in the window
			if (this.window.numInstances() > 0)
			{
			    // backward elimination
                for(int z = upperBound - 1; z >= lowerBound;z--)
                {
                    // set number of features to consider in the search
                    cumulativeLinearNNSearch.setNumberOfActiveFeatures(z+1);

                    // get knn search result
                    Instances neighbours = cumulativeLinearNNSearch.kNNSearch(inst,Math.min(kOption.getValue(),this.window.numInstances()));


                    // temp votes for current subset
                    double t[] = new double[C+1];
                    for(int i = 0; i < neighbours.numInstances(); i++)
                    {
                        t[(int)neighbours.instance(i).classValue()]++;
                    }

                    // set best subset as return for prediction before re-selecting best subset
                    if(bestSubset == z)
                    {
                        // save votes
                        v = t.clone();
                    }

                    // assign accuracy values for guess to subset
                    if(Utils.maxIndex(t) == (int)inst.classValue()) // if predicted value by this subset is equal to the true class value
                    {
                        // increment correct count for that subset
                        correctCount[z]++;
                    }
                    else
                    {
                        wrongCount[z]++;
                    }
                }

                // calculate accuracy for selection of best subset
                for(int i = 0; i < correctPercent.length;i++)
                {
                    correctPercent[i] = (double)correctCount[i]/(double)(wrongCount[i] + correctCount[i]);
                }

                // update best subset based on new accuracy values
                bestSubset = Utils.maxIndex(correctPercent);
            }
		}
		catch(Exception e)
        {
			//System.err.println("Error: kNN search failed.");
			e.printStackTrace();
			//System.exit(1);
			return new double[inst.numClasses()];
		}
		return v;
    }

    /**
     *  Select the best subset of features from active features.
     * @param f Number of features specified
     */
    private void selectFeatureSubset(int f)
    {
        // initialisation
        if (!initialised)
        {
            // might be bug if first instance is missing attributes TODO check
            // check if there is actually enough features
            // if there is less features overall than F (limit specified)
            if(f >= window.numAttributes())
            {
                featuresCount = window.numAttributes() - 1; // -1 as the class attribute should not be included
                if(featuresCount < 0)
                    featuresCount = 0; // really should never happen as there should always be at least 1 feature
            }
            else
            {
                featuresCount = f;
            }


            bestSubset = 0;
            // reset subset counts as subsets will be different
            correctCount = new int[window.numAttributes()]; //int[featuresCount];
            wrongCount = new int[window.numAttributes()]; //int[featuresCount];
            correctPercent = new double[window.numAttributes()]; //double[featuresCount];
            initialiseRankingFunction();
            // add first instance to ranking function

            initialised = true;

            // Only dump if filename is specified
            String fileName = outputNameOption.getValue();
            if (!fileName.equals(""))
            {
                try
                {
                    File file = new File(fileName);

                    // if file doesnt exists, then create it
                    if (!file.exists())
                    {
                        file.createNewFile();
                    }


                    // write headers
                    bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
                    for (int i = 0; i < correctPercent.length; i++)
                    {
                        bw.write("Prediction Accuracy for subset of size" + (i + 1) + ",");
                    }
                    for (int i = 0; i < correctPercent.length; i++)
                    {
                        bw.write("Accuracy gain for subset of size " + (i + 1) + ",");
                    }
                    bw.write("Predicted number of relevant features out of total features,");

                    bw.write(featureLimitOption.getValue() + " Number of best ranked features considered");
                    bw.write(System.lineSeparator());

                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        else // if already initilised
        {
            // decay counts based on option
            double decayFactor = decayFactorOption.getValue();
            for (int i = 0;i < featuresCount; i++)
            {
                correctCount[i] *= (1-decayFactor);
                wrongCount[i] *= (1-decayFactor);
            }

            // calculate accuracy gain from adding feature to subset
            // utilise accuracy difference by adding features to subset
            double[] accuracyArray = computeAccuracyDiff(correctPercent); // not currently used other than to dump

            // Write ag to file if specified
            if (bw != null)
            {
                try
                {
                    for (int i = 0; i < correctPercent.length; i++)
                    {
                        bw.write(Double.toString(correctPercent[i]) + ",");

                    }
                    for (int i = 0; i < accuracyArray.length; i++)
                    {
                        bw.write(Double.toString(accuracyArray[i]) + ",");
                    }
                    bw.write(bestSubset + " out of " + window.numAttributes());
                    bw.write(System.lineSeparator());
                    bw.flush();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            // set best features
            bestFeatures = rankingFunction.rankFeatures(window, accuracyArray,bestFeatures);
			//System.out.println(Arrays.toString(bestFeatures));

        }
    }

    /**
     * Computes the prediction accuracy gained by adding the next best ranked feature F.
     * Done by comparing the difference in prediction accuracy of the subset containing F and the subset not containing F.
     * @param correctPercentage array of correct percentages with the best ranked feature being in index 0
     * @return array containing the prediction accuracy percentage gained or lost by adding the feature onto the subset
     */
    protected double[] computeAccuracyDiff(double[] correctPercentage)
    {
        double[] accuracyDiff = new double[correctPercentage.length];
        for (int i = 0; i < correctPercentage.length; i++)
        {
            if (i == 0)
                accuracyDiff[i] = 0;
            else {
                accuracyDiff[i] = correctPercentage[i] - correctPercentage[i - 1];
            }
        }
        return accuracyDiff;
    }

    /**
     * Initialises ranking function based on option set
     */
    public void initialiseRankingFunction()
    {
        // initialise best features as just the first k features
        bestFeatures = new int[featuresCount];
        for(int i = 0; i < bestFeatures.length;i++)
        {
            bestFeatures[i] = i;
        }
        // initialise ranking function
        // select ranking function based on option
        switch (this.rankingOption.getChosenIndex())
        {
            case 0:
                rankingFunction = new SymmetricUncertaintyRanking();
                break;
            case 1:
                rankingFunction = new InfoGainRanking();
                break;
            case 2:
                rankingFunction = new MeanEuclideanDistanceRanking();
                break;
            default:
                break;
        }
        rankingFunction.initialise(featuresCount,accuracyGainWeightOption.getValue(),window.classIndex());
    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent)
    {

    }

    public boolean isRandomizable() {
        return false;
    }

}