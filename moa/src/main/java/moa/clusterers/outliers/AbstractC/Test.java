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

package moa.clusterers.outliers.AbstractC;

import moa.streams.clustering.RandomRBFGeneratorEvents;
import weka.core.Instance;

public class Test {    
    public static void main(String[] args) throws Exception 
    {
        //if (true) return;
        
        int numInstances = 10000;        
        //moa.streams.ArffFileStream stream = new ArffFileStream("./datasets/debug_1.txt", -1);
        RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
        stream.prepareForUse();
        
        AbstractC myOutlierDetector= new AbstractC();
        /*myOutlierDetector.fractionOption.setValue(0.2);
        myOutlierDetector.radiusOption.setValue(3);
        myOutlierDetector.windowSizeOption.setValue(12);*/
        myOutlierDetector.setModelContext(stream.getHeader());
        myOutlierDetector.prepareForUse();  
        
        Long tmStart = System.currentTimeMillis();
        
        int numberSamples = 0;  
        int w = myOutlierDetector.windowSizeOption.getValue();      
        while (stream.hasMoreInstances() && (numberSamples < numInstances)) {
            Instance newInst = stream.nextInstance();
            myOutlierDetector.processNewInstanceImpl(newInst);            
            numberSamples++;
            if (numberSamples % 100 == 0) {
                //System.out.println("Processed " + numberSamples + " stream objects.");  
            }
            if ((numberSamples % (w / 2)) == 0) {
                //myOutlierDetector.PrintOutliers();
            }
        }        
        // myOutlierDetector.PrintOutliers();
        System.out.println("Total time = " + (System.currentTimeMillis() - tmStart) + " ms");
    }
}
