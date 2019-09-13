/*
 *    PValuePerTwoAlgorithm.java
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

import java.util.ArrayList;

/**
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class PValuePerTwoAlgorithm {

    public String algName1;

    public String algName2;

    public double PValue;

    /**
     * Costructor.
     * @param algName1
     * @param algName2
     * @param PValue
     */
    public PValuePerTwoAlgorithm(String algName1, String algName2, double PValue) {
        this.algName1 = algName1;
        this.algName2 = algName2;
        this.PValue = PValue;
    }

    /**
     *
     * @param PValue
     * @return
     */
    public  boolean isSignicativeBetterThan(double PValue){
        return this.PValue >= PValue;
    }

    /**
     *
     * @param pvalues
     * @param name1
     * @param name2
     * @return
     */
    public static int getIndex(ArrayList<PValuePerTwoAlgorithm> pvalues, String name1, String name2){
           for(int i = 0; i < pvalues.size(); i++){
               if(pvalues.get(i).algName1.equals(name1)==true && pvalues.get(i).algName2.equals(name2)==true
                       || pvalues.get(i).algName1.equals(name2)==true && pvalues.get(i).algName2.equals(name1)==true)
                   return i;
                   
           }
        return -1;   
    }
}
