/*
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
 */

package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;

/**
 * Per-group pooled-covariance Mahalanobis distance strategy (when a single
 * global covariance matrix is not used). Self-contained using Apache Commons
 * Math (bundled with MOA) with zero dependency on custom {@code weka.core.*}
 * classes.
 */
public class PerGroupMahalanobisDistance implements ConceptDistance {

    private static final long serialVersionUID = 1L;

    @Override
    public double distance(Instances query, Instances target) {
        double[][] a = extractMatrix(query);
        double[][] b = extractMatrix(target);
        if (a.length == 0 || b.length == 0 || a[0].length != b[0].length) {
            return -1.0;
        }
        int d = a[0].length;
        double[] meanDiff = new double[d];
        for (int c = 0; c < d; c++) {
            double meanA = 0.0;
            for (int r = 0; r < a.length; r++) {
                meanA += a[r][c];
            }
            meanA /= a.length;
            double meanB = 0.0;
            for (int r = 0; r < b.length; r++) {
                meanB += b[r][c];
            }
            meanB /= b.length;
            meanDiff[c] = meanA - meanB;
        }

        double[][] covA = MahalanobisDistanceSingleMatrix.computePopulationCovariance(a);
        double[][] covB = MahalanobisDistanceSingleMatrix.computePopulationCovariance(b);
        double total = (double) (a.length + b.length);
        double wA = a.length / total;
        double wB = b.length / total;
        double[][] covTotal = new double[d][d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                covTotal[i][j] = covA[i][j] * wA + covB[i][j] * wB;
            }
        }

        double[][] inv = new LUDecomposition(new Array2DRowRealMatrix(covTotal))
                .getSolver().getInverse().getData();
        double sum = 0.0;
        for (int c = 0; c < d; c++) {
            double acc = 0.0;
            for (int r = 0; r < d; r++) {
                acc += meanDiff[r] * inv[r][c];
            }
            sum += acc * meanDiff[c];
        }
        return Math.sqrt(sum);
    }

    private static double[][] extractMatrix(Instances insts) {
        int m = insts.numInstances();
        int d = insts.numAttributes();
        double[][] mat = new double[m][d];
        for (int i = 0; i < m; i++) {
            Instance inst = insts.instance(i);
            for (int k = 0; k < d; k++) {
                mat[i][k] = inst.value(k);
            }
        }
        return mat;
    }
}
