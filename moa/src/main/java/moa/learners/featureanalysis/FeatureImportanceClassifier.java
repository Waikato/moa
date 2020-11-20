/*
 *    FeatureScore.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Heitor Murilo Gomes (hgomes at waikato dot ac dot nz)
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
package moa.learners.featureanalysis;

import moa.classifiers.Classifier;

/**
 * Feature Importance Classifier
 *
 * <p>This interface defines the methods to be implemented on a Classifier to allow it to produce feature importances.
 * </p>
 *
 * <p>See details in:<br> Heitor Murilo Gomes, Rodrigo Fernandes de Mello, Bernhard Pfahringer, Albert Bifet.
 * Feature Scoring using Tree-Based Ensembles for Evolving Data Streams.
 * IEEE International Conference on Big Data (pp. 761-769), 2019</p>
 * </p>
 *
 * @author Heitor Murilo Gomes
 */
public interface FeatureImportanceClassifier extends Classifier {

    /**
     * Obtain the current importance for each feature.
     *
     * @return array containing the importance/score estimated for each feature
     */
    double[] getFeatureImportances(boolean normalize);

    /**
     * The output is a double array where values indicates the
     * original feature index and the order of the array its
     * ranking. The size of this array is expected to be less than
     * the complete set of features.
     * @param k
     * @param normalize
     * @return the k features with the highest scores.
     */
    int[] getTopKFeatures(int k, boolean normalize);
}
