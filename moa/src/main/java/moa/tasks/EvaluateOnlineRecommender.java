/*
 *    EvaluateOnlineRecommender.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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
package moa.tasks;

import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import com.github.javacliparser.IntOption;
import moa.recommender.dataset.Dataset;
import moa.recommender.predictor.RatingPredictor;
import moa.recommender.rc.data.RecommenderData;

/**
 * Test for evaluating a recommender by training and periodically testing 
 * on samples from a rating dataset. When finished, it will show the learning
 * curve of the recommender rating predictor.
 *
 * <p>Parameters:</p>
 * <ul>  
 * <li> d: dataset - the dataset to be used to train/test the rating predictor.</li>
 * <li> f: sample frequency - the frequency in which a rating from the dataset will be used to test the model </li>
 * </ul>
 *
 * @author Alex Catarineu (a.catarineu@gmail.com)
 * @version $Revision: 7 $
 */
public class EvaluateOnlineRecommender extends AuxiliarMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a online reccommender system.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption datasetOption = new ClassOption("dataset", 'd',
            "Dataset to evaluate.", Dataset.class, "moa.recommender.dataset.impl.MovielensDataset");

    public ClassOption ratingPredictorOption = new ClassOption("ratingPredictor", 's',
            "Rating Predictor to evaluate on.", RatingPredictor.class,
            "moa.recommender.predictor.BRISMFPredictor");
    
    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
            'f',
            "How many instances between samples of the learning performance.",
            100, 0, Integer.MAX_VALUE);

    public EvaluateOnlineRecommender() {
    }

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }
    
    @Override
    public Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        
        Dataset d = (Dataset) getPreparedClassOption(this.datasetOption);
        RatingPredictor rp = (RatingPredictor)getPreparedClassOption(this.ratingPredictorOption);
        LearningCurve learningCurve = new LearningCurve("n");
        RecommenderData data = rp.getData();
        data.clear();
        data.disableUpdates(false);
        long start = System.currentTimeMillis();
        long evalTime = 0;
        double sum = 0;
        int n = 0;
        //ArrayList<TestMetric> metrics = new ArrayList<TestMetric>();
        int sampleFrequency = this.sampleFrequencyOption.getValue();
        int count = 0;
        while (d.next())
          ++count;
        d.reset();
        while (d.next()) {
            Integer user = d.curUserID();
            Integer item = d.curItemID();
            Double rating = d.curRating();
            long startPredTime = System.currentTimeMillis();
            double pred = rp.predictRating(user, item);
            sum += Math.pow(pred - rating, 2);
            evalTime += System.currentTimeMillis() - startPredTime;
            data.setRating(user, item, rating);
            //System.out.println(data.countRatingsItem(item) + " " + data.countRatingsUser(user));
            //if (n++%100 == 99) metrics.add(new TestMetric("RMSE (" + n +")", Math.sqrt(sum/(double)n)));
            n++;
            if (n%sampleFrequency == sampleFrequency-1) {
               if (monitor.taskShouldAbort()) {
                    return null;
                }
                monitor.setCurrentActivityFractionComplete((double)n/(double)count);
                learningCurve.insertEntry(new LearningEvaluation(
                        new Measurement[]{
                            new Measurement(
                            "n",
                            n),
                            new Measurement(
                            "RMSE",
                            Math.sqrt(sum/(double)n)),
                            new Measurement(
                            "trainingTime",
                            (int)((System.currentTimeMillis() - start - evalTime)/1000)),
                            new Measurement(
                            "evalTime",
                            (int)(evalTime/1000))
                        }
                         ));
                if (monitor.resultPreviewRequested()) {
                    monitor.setLatestResultPreview(learningCurve.headerToString() + "\n" +
                      learningCurve.entryToString(learningCurve.numEntries() - 1));
                }
            }
        }
        //System.out.println(n + " " + Math.sqrt(sum/(double)n));
        //metrics.add(new TestMetric("RMSE (" + n +")", Math.sqrt(sum/(double)n)));
       // long trainingTime = System.currentTimeMillis() - start - evalTime;
        //return new TestStatistics((int)(trainingTime/1000),
        //        (int)(evalTime/1000),
        //        metrics.toArray(new TestMetric[metrics.size()]));
        
       
        return learningCurve;
    }
}
