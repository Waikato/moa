/*
 *    PreviewCollection.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
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
package moa.evaluation.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that holds separate {@link PreviewCollection}s for mean and standard 
 * deviation values. The values can be calculated from a PreviewCollection that
 * contains the results of multiple runs for the same parameter configurations.
 *
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class MeanPreviewCollection {
    
    PreviewCollection<PreviewCollection<Preview>> origMultiRunPreviews;
    PreviewCollection<Preview>                    meanPreviews;
    PreviewCollection<Preview>                    stdPreviews;
    
    /**
     * On creation of a MeanPreviewCollection, the mean Previews and 
     * standard deviation Previews are calculated from the given 
     * PreviewCollection by averaging the measurements for all entries over 
     * the different runs that have been performed. 
     * This results in a collection of mean Previews and a separate collection 
     * of standard deviation Previews, both containing Previews for each 
     * parameter configuration.
     */
    public MeanPreviewCollection(
            PreviewCollection<PreviewCollection<Preview>> multiRunPreviews) 
    {
        this.origMultiRunPreviews = multiRunPreviews;
        
        // create new preview collections for mean and standard deviation
        this.meanPreviews = new PreviewCollection<Preview>(
                "mean preview entry id",
                "parameter value id",
                multiRunPreviews.taskClass,
                multiRunPreviews.variedParamName,
                multiRunPreviews.variedParamValues);
        this.stdPreviews = new PreviewCollection<Preview>(
                "mean preview entry id",
                "parameter value id",
                multiRunPreviews.taskClass,
                multiRunPreviews.variedParamName,
                multiRunPreviews.variedParamValues);
        
        // calculate maximum number of entries that each Preview can provide
        int numFolds             = multiRunPreviews.subPreviews.size();
        int numParamValues       = multiRunPreviews.variedParamValues.length;
        int numEntriesPerPreview = 
                multiRunPreviews.numEntries() / numFolds / numParamValues;
                
        for (int paramValue = 0; paramValue < numParamValues; paramValue++)
        {
            // construct mean and standard deviation Previews for this 
            // parameter value
            this.constructMeanStdPreviewsForParam(
                    numEntriesPerPreview, numParamValues, paramValue);
        }
    }
    
    /**
     * @return the PreviewCollection of mean values
     */
    public PreviewCollection<Preview> getMeanPreviews() {
        return this.meanPreviews;
    }
    
    /**
     * @return the PreviewCollection of standard deviation values
     */
    public PreviewCollection<Preview> getStdPreviews() {
        return this.stdPreviews;
    }
    
    /**
     * Construct the mean and standard deviation Previews for one specific 
     * parameter value.
     * 
     * @param numEntriesPerPreview
     * @param numParamValues
     * @param paramValue
     */
    private void constructMeanStdPreviewsForParam(
            int numEntriesPerPreview, 
            int numParamValues, 
            int paramValue) 
    {
        // calculate mean
        List<double[]> meanParamMeasurements = calculateMeanMeasurementsForParam(
                numEntriesPerPreview, numParamValues, paramValue);
        
        // calculate standard deviation
        List<double[]> stdParamMeasurements = calculateStdMeasurementsForParam(
                numEntriesPerPreview, 
                numParamValues, 
                paramValue,
                meanParamMeasurements);
        
        // get actual measurement names (first four are only additional IDs)
        String[] meanMeasurementNames = 
                this.origMultiRunPreviews.getMeasurementNames();
        meanMeasurementNames = Arrays.copyOfRange(
                meanMeasurementNames, 4, meanMeasurementNames.length);
        
        // Create names for standard deviations.
        // First name is used for indexing and remains unchanged. For the 
        // remaining names, [std] is prepended to the original name.
        String[] stdMeasurementNames = new String[meanMeasurementNames.length];
        stdMeasurementNames[0] = meanMeasurementNames[0];
        for (int m = 1; m < meanMeasurementNames.length; m++) {
            stdMeasurementNames[m] = "[std] " + meanMeasurementNames[m];
        }
        
        // wrap into LearningCurves
        LearningCurve meanLearningCurve = 
                new LearningCurve(meanMeasurementNames[0]);
        meanLearningCurve.setData(
                Arrays.asList(meanMeasurementNames), meanParamMeasurements);
        LearningCurve stdLearningCurve = 
                new LearningCurve(stdMeasurementNames[0]);
        stdLearningCurve.setData(
                Arrays.asList(stdMeasurementNames), stdParamMeasurements);
        
        // wrap into PreviewCollectionLearningCurveWrapper
        Preview meanParamValuePreview = 
                new PreviewCollectionLearningCurveWrapper(
                        meanLearningCurve, 
                        this.origMultiRunPreviews.taskClass);
        Preview stdParamValuePreview = 
                new PreviewCollectionLearningCurveWrapper(
                        stdLearningCurve, 
                        this.origMultiRunPreviews.taskClass);
        
        // store Previews in corresponding PreviewCollections
        this.meanPreviews.setPreview(paramValue, meanParamValuePreview);
        this.stdPreviews.setPreview(paramValue, stdParamValuePreview);
    }
    
    /**
     * Calculate the mean measurements for the given parameter value.
     * 
     * @param numEntriesPerPreview
     * @param numParamValues
     * @param paramValue
     * @return List of mean measurement arrays
     */
    private List<double[]> calculateMeanMeasurementsForParam(
            int numEntriesPerPreview,
            int numParamValues,
            int paramValue) 
    {
        List<double[]> paramMeasurementsSum  = 
                new ArrayList<double[]>(numEntriesPerPreview);
        List<double[]> meanParamMeasurements = 
                new ArrayList<double[]>(numEntriesPerPreview);
        
        int numCompleteFolds = 0;
        
        // sum up measurement values
        for (PreviewCollection<Preview> foldPreview : 
             this.origMultiRunPreviews.subPreviews) 
        {
            // check if there is a preview for each parameter value
            if (foldPreview.getPreviews().size() == numParamValues) {
                numCompleteFolds++;
                
                Preview foldParamPreview = 
                        foldPreview.getPreviews().get(paramValue);
                
                // add this Preview's measurements to the overall sum
                this.addPreviewMeasurementsToSum(
                        paramMeasurementsSum, 
                        foldParamPreview, 
                        numEntriesPerPreview);
            }
        }
        
        // divide sum by number of folds
        for (int entryIdx = 0; entryIdx < numEntriesPerPreview; entryIdx++) {
            double[] sumEntry  = paramMeasurementsSum.get(entryIdx);
            double[] meanEntry = new double[sumEntry.length];
            
            // first measurement is used for indexing -> simply copy
            meanEntry[0] = sumEntry[0];
            
            // calculate mean for remaining measurements
            for (int m = 1; m < sumEntry.length; m++) {
                meanEntry[m] = sumEntry[m] / numCompleteFolds;
            }
            
            meanParamMeasurements.add(meanEntry);
        }
        
        return meanParamMeasurements;
    }
    
    /**
     * Calculate the standard deviation measurements for the given parameter 
     * value.
     * 
     * @param numEntriesPerPreview
     * @param numParamValues
     * @param paramValue
     * @return List of standard deviation measurement arrays
     */
    private List<double[]> calculateStdMeasurementsForParam(
            int            numEntriesPerPreview,
            int            numParamValues,
            int            paramValue,
            List<double[]> meanParamMeasurements)
    {
        List<double[]> paramMeasurementsSquaredDiffSum = 
                new ArrayList<double[]>(numEntriesPerPreview);
        List<double[]> paramMeasurementsStd            = 
                new ArrayList<double[]>(numEntriesPerPreview);
        
        int numCompleteFolds = 0;
        
        // sum up squared differences between measurements and mean values
        for (PreviewCollection<Preview> foldPreview : 
             this.origMultiRunPreviews.subPreviews) 
        {
            // check if there is a preview for each parameter value
            if (foldPreview.getPreviews().size() == numParamValues) {
                numCompleteFolds++;
                
                Preview foldParamPreview = 
                        foldPreview.getPreviews().get(paramValue);
                
                // add this Preview's standardDeviations to the overall sum
                this.addPreviewMeasurementSquaredDiffsToSum(
                        meanParamMeasurements, 
                        paramMeasurementsSquaredDiffSum,
                        foldParamPreview, 
                        numEntriesPerPreview);
            }
        }
        
        // divide sum by number of folds and take square root 
        for (int entryIdx = 0; entryIdx < numEntriesPerPreview; entryIdx++) {
            double[] sumEntry = paramMeasurementsSquaredDiffSum.get(entryIdx);
            double[] stdEntry = new double[sumEntry.length];
            
            // first measurement is used for indexing -> simply copy
            stdEntry[0] = sumEntry[0];
            
            // calculate standard deviation for remaining measurements
            for (int m = 1; m < sumEntry.length; m++) {
                if (numCompleteFolds > 1) {
                    stdEntry[m] = Math.sqrt(sumEntry[m]/(numCompleteFolds-1));
                }
                else {
                    stdEntry[m] = Math.sqrt(sumEntry[m]);
                }
            }
            
            paramMeasurementsStd.add(stdEntry);
        }
        
        return paramMeasurementsStd;
    }
    
    /**
     * Add measurements from the given Preview to the overall sum.
     * 
     * @param measurementsSum
     * @param preview
     * @param numEntriesPerPreview
     */
    private void addPreviewMeasurementsToSum(
            List<double[]> measurementsSum, 
            Preview        preview, 
            int            numEntriesPerPreview) 
    {
        List<double[]> previewMeasurements = preview.getData();
        
        // add values for each measurement in each entry
        for (int entryIdx = 0; entryIdx < numEntriesPerPreview; entryIdx++) {
            double[] previewEntry = previewMeasurements.get(entryIdx);
            double[] sumEntry;
            
            if (measurementsSum.size() > entryIdx) {
                sumEntry = measurementsSum.get(entryIdx);
            }
            else {
                // initialize sum entry
                sumEntry = new double[previewEntry.length];
                measurementsSum.add(sumEntry);
            }
            
            // first measurement is used for indexing
            // -> simply copy from first preview
            if (sumEntry[0] == 0.0) {
                sumEntry[0] = previewEntry[0];
            }
            
            // add measurements of current entry
            for (int measure = 1; measure < sumEntry.length; measure++) {
                sumEntry[measure] += previewEntry[measure];
            }
        }
    }
    
    /**
     * Add squared deviations from the mean value from the given Preview to the
     * overall sum.
     * 
     * @param meanMeasurements
     * @param measurementsSquaredDiffSum
     * @param preview
     * @param numEntriesPerPreview
     */
    private void addPreviewMeasurementSquaredDiffsToSum(
            List<double[]> meanMeasurements, 
            List<double[]> measurementsSquaredDiffSum, 
            Preview        preview, 
            int            numEntriesPerPreview)
    {
        List<double[]> previewMeasurements = preview.getData();
        
        // add standard deviation for each measurement in each entry
        for (int entryIdx = 0; entryIdx < numEntriesPerPreview; entryIdx++) {
            double[] meanEntry    = meanMeasurements.get(entryIdx);
            double[] previewEntry = previewMeasurements.get(entryIdx);
            double[] squaredDiffSumEntry;
            
            if (measurementsSquaredDiffSum.size() > entryIdx) {
                squaredDiffSumEntry = measurementsSquaredDiffSum.get(entryIdx);
            }
            else {
                // initialize sum entry
                squaredDiffSumEntry = new double[previewEntry.length];
                measurementsSquaredDiffSum.add(squaredDiffSumEntry);
            }
            
            // first measurement is used for indexing
            // -> simply copy from first preview
            if (squaredDiffSumEntry[0] == 0.0) {
                squaredDiffSumEntry[0] = previewEntry[0];
            }
            
            // add squared differences for current entry
            for (int m = 1; m < previewEntry.length; m++) {
                double diff        = (meanEntry[m] - previewEntry[m]);
                double squaredDiff = diff * diff;
                squaredDiffSumEntry[m] += squaredDiff;
            }
        }
    }
}
