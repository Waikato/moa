/*
 *    GreenwaldKhannaQuantileSummary.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.core;

import java.io.Serializable;
import java.util.ArrayList;

import moa.AbstractMOAObject;

/**
 * Class for representing summaries of Greenwald and Khanna quantiles.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class GreenwaldKhannaQuantileSummary extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected static class Tuple implements Serializable {

        private static final long serialVersionUID = 1L;

        public double v;

        public long g;

        public long delta;

        public Tuple(double v, long g, long delta) {
            this.v = v;
            this.g = g;
            this.delta = delta;
        }

        public Tuple(double v) {
            this(v, 1, 0);
        }
    }

    protected Tuple[] summary;

    protected int numTuples = 0;

    protected long numObservations = 0;

    public GreenwaldKhannaQuantileSummary(int maxTuples) {
        this.summary = new Tuple[maxTuples];
    }

    public void insert(double val) {
        int i = findIndexOfTupleGreaterThan(val);
        Tuple nextT = this.summary[i];
        if (nextT == null) {
            insertTuple(new Tuple(val, 1, 0), i);
        } else {
            insertTuple(new Tuple(val, 1, nextT.g + nextT.delta - 1), i);
        }
        if (this.numTuples == this.summary.length) {
            // use method 1
            deleteMergeableTupleMostFull();
            // if (mergeMethod == 1) {
            // deleteMergeableTupleMostFull();
            // } else if (mergeMethod == 2) {
            // deleteTupleMostFull();
            // } else {
            // long maxDelta = findMaxDelta();
            // compress(maxDelta);
            // while (numTuples == summary.length) {
            // maxDelta++;
            // compress(maxDelta);
            // }
            // }
        }
        this.numObservations++;
    }

    protected void insertTuple(Tuple t, int index) {
        System.arraycopy(this.summary, index, this.summary, index + 1,
                this.numTuples - index);
        this.summary[index] = t;
        this.numTuples++;
    }

    protected void deleteTuple(int index) {
        this.summary[index] = new Tuple(this.summary[index + 1].v,
                this.summary[index].g + this.summary[index + 1].g,
                this.summary[index + 1].delta);
        System.arraycopy(this.summary, index + 2, this.summary, index + 1,
                this.numTuples - index - 2);
        this.summary[this.numTuples - 1] = null;
        this.numTuples--;
    }

    protected void deleteTupleMostFull() {
        long leastFullness = Long.MAX_VALUE;
        int leastFullIndex = 0;
        for (int i = 1; i < this.numTuples - 1; i++) {
            long fullness = this.summary[i].g + this.summary[i + 1].g
                    + this.summary[i + 1].delta;
            if (fullness < leastFullness) {
                leastFullness = fullness;
                leastFullIndex = i;
            }
        }
        if (leastFullIndex > 0) {
            deleteTuple(leastFullIndex);
        }
    }

    protected void deleteMergeableTupleMostFull() {
        long leastFullness = Long.MAX_VALUE;
        int leastFullIndex = 0;
        for (int i = 1; i < this.numTuples - 1; i++) {
            long fullness = this.summary[i].g + this.summary[i + 1].g
                    + this.summary[i + 1].delta;
            if ((this.summary[i].delta >= this.summary[i + 1].delta)
                    && (fullness < leastFullness)) {
                leastFullness = fullness;
                leastFullIndex = i;
            }
        }
        if (leastFullIndex > 0) {
            deleteTuple(leastFullIndex);
        }
    }

    public long getWorstError() {
        long mostFullness = 0;
        for (int i = 1; i < this.numTuples - 1; i++) {
            long fullness = this.summary[i].g + this.summary[i].delta;
            if (fullness > mostFullness) {
                mostFullness = fullness;
            }
        }
        return mostFullness;
    }

    public long findMaxDelta() {
        long maxDelta = 0;
        for (int i = 0; i < this.numTuples; i++) {
            if (this.summary[i].delta > maxDelta) {
                maxDelta = this.summary[i].delta;
            }
        }
        return maxDelta;
    }

    public void compress(long maxDelta) {
        long[] bandBoundaries = computeBandBoundaries(maxDelta);
        for (int i = this.numTuples - 2; i >= 0; i--) {
            if (this.summary[i].delta >= this.summary[i + 1].delta) {
                int band = 0;
                while (this.summary[i].delta < bandBoundaries[band]) {
                    band++;
                }
                long belowBandThreshold = Long.MAX_VALUE;
                if (band > 0) {
                    belowBandThreshold = bandBoundaries[band - 1];
                }
                long mergeG = this.summary[i + 1].g + this.summary[i].g;
                int childI = i - 1;
                while (((mergeG + this.summary[i + 1].delta) < maxDelta)
                        && (childI >= 0)
                        && (this.summary[childI].delta >= belowBandThreshold)) {
                    mergeG += this.summary[childI].g;
                    childI--;
                }
                if (mergeG + this.summary[i + 1].delta < maxDelta) {
                    // merge
                    int numDeleted = i - childI;
                    this.summary[childI + 1] = new Tuple(this.summary[i + 1].v,
                            mergeG, this.summary[i + 1].delta);
                    // todo complete & test this multiple delete
                    System.arraycopy(this.summary, i + 2, this.summary,
                            childI + 2, this.numTuples - (i + 2));
                    for (int j = this.numTuples - numDeleted; j < this.numTuples; j++) {
                        this.summary[j] = null;
                    }
                    this.numTuples -= numDeleted;
                    i = childI + 1;
                }
            }
        }
    }

    public double getQuantile(double quant) {
        long r = (long) Math.ceil(quant * this.numObservations);
        long currRank = 0;
        for (int i = 0; i < this.numTuples - 1; i++) {
            currRank += this.summary[i].g;
            if (currRank + this.summary[i + 1].g > r) {
                return this.summary[i].v;
            }
        }
        return this.summary[this.numTuples - 1].v;
    }

    public long getTotalCount() {
        return this.numObservations;
    }

    public double getPropotionBelow(double cutpoint) {
        return (double) getCountBelow(cutpoint) / (double) this.numObservations;
    }

    public long getCountBelow(double cutpoint) {
        long rank = 0;
        for (int i = 0; i < this.numTuples; i++) {
            if (this.summary[i].v > cutpoint) {
                break;
            }
            rank += this.summary[i].g;
        }
        return rank;
    }

    public double[] getSuggestedCutpoints() {
        double[] cutpoints = new double[this.numTuples];
        for (int i = 0; i < this.numTuples; i++) {
            cutpoints[i] = this.summary[i].v;
        }
        return cutpoints;
    }

    protected int findIndexOfTupleGreaterThan(double val) {
        int high = this.numTuples, low = -1, probe;
        while (high - low > 1) {
            probe = (high + low) / 2;
            if (this.summary[probe].v > val) {
                high = probe;
            } else {
                low = probe;
            }
        }
        return high;
    }

    public static long[] computeBandBoundaries(long maxDelta) {
        ArrayList<Long> boundaryList = new ArrayList<Long>();
        boundaryList.add(new Long(maxDelta));
        int alpha = 1;
        while (true) {
            long boundary = (maxDelta - (2 << (alpha - 1)) - (maxDelta % (2 << (alpha - 1))));
            if (boundary >= 0) {
                boundaryList.add(new Long(boundary + 1));
            } else {
                break;
            }
            alpha++;
        }
        boundaryList.add(new Long(0));
        long[] boundaries = new long[boundaryList.size()];
        for (int i = 0; i < boundaries.length; i++) {
            boundaries[i] = boundaryList.get(i).longValue();
        }
        return boundaries;
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
