/*
 *    RandomRBFGeneratorDrift.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.streams.generators;

import java.util.Random;
import moa.core.InstanceExample;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Stream generator for a random radial basis function stream with drift.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class RandomRBFGeneratorDrift extends RandomRBFGenerator {

    @Override
    public String getPurposeString() {
        return "Generates a random radial basis function stream with drift.";
    }

    private static final long serialVersionUID = 1L;

    public FloatOption speedChangeOption = new FloatOption("speedChange", 's',
            "Speed of change of centroids in the model.", 0, 0, Float.MAX_VALUE);

    public IntOption numDriftCentroidsOption = new IntOption("numDriftCentroids", 'k',
            "The number of centroids with drift.", 50, 0, Integer.MAX_VALUE);

    protected double[][] speedCentroids;

    @Override
    public InstanceExample nextInstance() {
        //Update Centroids with drift
        int len = this.numDriftCentroidsOption.getValue();
        if (len > this.centroids.length) {
            len = this.centroids.length;
        }
        for (int j = 0; j < len; j++) {
            for (int i = 0; i < this.numAttsOption.getValue(); i++) {
                this.centroids[j].centre[i] += this.speedCentroids[j][i] * this.speedChangeOption.getValue();
                if (this.centroids[j].centre[i] > 1) {
                    this.centroids[j].centre[i] = 1;
                    this.speedCentroids[j][i] = -this.speedCentroids[j][i];
                }
                if (this.centroids[j].centre[i] < 0) {
                    this.centroids[j].centre[i] = 0;
                    this.speedCentroids[j][i] = -this.speedCentroids[j][i];
                }
            }
        }
        return super.nextInstance();
    }

    @Override
    protected void generateCentroids() {
        super.generateCentroids();
        Random modelRand = new Random(this.modelRandomSeedOption.getValue());
        int len = this.numDriftCentroidsOption.getValue();
        if (len > this.centroids.length) {
            len = this.centroids.length;
        }
        this.speedCentroids = new double[len][this.numAttsOption.getValue()];
        for (int i = 0; i < len; i++) {
            double[] randSpeed = new double[this.numAttsOption.getValue()];
            double normSpeed = 0.0;
            for (int j = 0; j < randSpeed.length; j++) {
                randSpeed[j] = modelRand.nextDouble();
                normSpeed += randSpeed[j] * randSpeed[j];
            }
            normSpeed = Math.sqrt(normSpeed);
            for (int j = 0; j < randSpeed.length; j++) {
                randSpeed[j] /= normSpeed;
            }
            this.speedCentroids[i] = randSpeed;
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
