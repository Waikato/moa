/*
 *    StreamObj.java
 *    Copyright (C) 2013 Aristotle University of Thessaloniki, Greece
 *    @author D. Georgiadis, A. Gounaris, A. Papadopoulos, K. Tsichlas, Y. Manolopoulos
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

package moa.clusterers.outliers.Angiulli;

import moa.clusterers.outliers.utils.mtree.DistanceFunctions.EuclideanCoordinate;

public class StreamObj implements EuclideanCoordinate, Comparable<StreamObj> {
    private final double[] values;
    private final int hashCode;

    public StreamObj(double... values) {
        this.values = values;

        int h = 1;
        for (double value : values) {
            h = 31 * (int) h + (int) value;
        }
        this.hashCode = h;
    }

    @Override
    public int dimensions() {
        return values.length;
    }

    @Override
    public double get(int index) {
        return values[index];
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StreamObj) {
            StreamObj that = (StreamObj) obj;
            if (this.dimensions() != that.dimensions()) {
                return false;
            }
            for (int i = 0; i < this.dimensions(); i++) {
                if (this.values[i] != that.values[i]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(StreamObj that) {
        int dimensions = Math.min(this.dimensions(), that.dimensions());
        for (int i = 0; i < dimensions; i++) {
            double v1 = this.values[i];
            double v2 = that.values[i];
            if (v1 > v2) {
                return +1;
            }
            if (v1 < v2) {
                return -1;
            }
        }

        if (this.dimensions() > dimensions) {
            return +1;
        }

        if (that.dimensions() > dimensions) {
            return -1;
        }

        return 0;
    }
}