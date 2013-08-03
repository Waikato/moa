/*
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

package moa.clusterers.outliers.MCOD;

import java.util.ArrayList;
import moa.clusterers.outliers.MCOD.ISBIndex.ISBNode;
import moa.clusterers.utils.mtree.DistanceFunctions.EuclideanCoordinate;

public class MicroCluster implements EuclideanCoordinate, Comparable<MicroCluster> {
    public ISBNode mcc;
    public ArrayList<ISBNode> nodes;

    public MicroCluster(ISBNode mcc) {
        this.mcc = mcc;
        nodes = new ArrayList<ISBNode>();
        AddNode(mcc);
    }
    
    public void AddNode(ISBNode node) {
        if (node != null)
            nodes.add(node);
    }
    
    public void RemoveNode(ISBNode node) {
        if (node != null)
            nodes.remove(node);
    }
    
    public int GetNodesCount() {
        return nodes.size();
    }

    @Override
    public int dimensions() {
        return mcc.obj.dimensions();
    }

    @Override
    public double get(int index) {
        return mcc.obj.get(index);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MicroCluster) {
            MicroCluster that = (MicroCluster) obj;
            if (this.dimensions() != that.dimensions()) {
                return false;
            }
            for (int i = 0; i < this.dimensions(); i++) {
                if (this.mcc.obj.get(i) != that.mcc.obj.get(i)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(MicroCluster that) {
        int dimensions = Math.min(this.dimensions(), that.dimensions());
        for (int i = 0; i < dimensions; i++) {
            double v1 = this.mcc.obj.get(i);
            double v2 = that.mcc.obj.get(i);
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