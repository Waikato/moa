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

package moa.gui.visualization;

/**
 *
 * @author mits
 */
class OutlierEvent implements Comparable<OutlierEvent> {
    public DataPoint point;
    public boolean outlier;
    public Long timestamp;

    public OutlierEvent(DataPoint point, boolean outlier, Long timestamp) {
        this.point = point;
        this.outlier = outlier;
        this.timestamp = timestamp;
    }
    
    @Override
    public int compareTo(OutlierEvent o) { 
        if (this.timestamp > o.timestamp)
            return 1;
        else if (this.timestamp < o.timestamp)
            return -1;
        else {
            if (this.point.timestamp > o.point.timestamp)
                return 1;
            else if (this.point.timestamp < o.point.timestamp)
                return -1;
        }
            
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return ( (this.timestamp == ((OutlierEvent) o).timestamp) && 
                 (this.point.timestamp == ((OutlierEvent) o).point.timestamp) );
    }
}
