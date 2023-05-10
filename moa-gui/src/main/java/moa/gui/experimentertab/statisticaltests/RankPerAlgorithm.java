/*
 *    RankPerAlgorithm.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
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
package moa.gui.experimentertab.statisticaltests;

/**
 * This class contains each algorithm with its ranking.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class RankPerAlgorithm implements Comparable<RankPerAlgorithm> {

    public String algName;
    public double rank;

    /**
     * Constructor.
     *
     * @param algName
     * @param rank
     */
    public RankPerAlgorithm(String algName, double rank) {
        this.algName = algName;
        this.rank = rank;
    }

    @Override
    public int compareTo(RankPerAlgorithm r) {
        if (rank < r.rank) {
            return -1;
        }
        if (rank > r.rank) {
            return 1;
        }
        return 0;
    }

}
