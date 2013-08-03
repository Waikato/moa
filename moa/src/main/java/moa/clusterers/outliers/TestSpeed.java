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

package moa.clusterers.outliers;

import moa.clusterers.outliers.AbstractC.AbstractC;
import moa.clusterers.outliers.Angiulli.ExactSTORM;
import moa.clusterers.outliers.MCOD.MCOD;
import moa.clusterers.outliers.SimpleCOD.SimpleCOD;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import weka.core.Instance;

public class TestSpeed {    
    public static void main(String[] args) throws Exception 
    {        
        int numInstances = 2000;        
        RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
        stream.prepareForUse();
        
        SimpleCOD scod = new SimpleCOD();   
        MCOD mcod = new MCOD();        
        ExactSTORM angiulli = new ExactSTORM();   
        //DistanceOutliersAppr angiulli = new DistanceOutliersAppr(); 
        AbstractC abstractC = new AbstractC();        
        
        angiulli.queryFreqOption.setValue(1);
        
        scod.setModelContext(stream.getHeader());
        scod.prepareForUse(); 
        
        mcod.setModelContext(stream.getHeader());
        mcod.prepareForUse(); 
        
        angiulli.setModelContext(stream.getHeader());
        angiulli.prepareForUse(); 
        
        abstractC.setModelContext(stream.getHeader());
        abstractC.prepareForUse();
        
        Long tmStart = System.currentTimeMillis();
        
        int numberSamples = 0;     
        while (stream.hasMoreInstances() && (numberSamples < numInstances)) {               
            Instance newInst = stream.nextInstance();
            
            //scod.processNewInstanceImpl(newInst);  
            mcod.processNewInstanceImpl(newInst);  
            //angiulli.processNewInstanceImpl(newInst);  
            //abstractC.processNewInstanceImpl(newInst); 
            
            numberSamples++;
        }      
        
        System.out.println("Total time = " + (System.currentTimeMillis() - tmStart) + " ms");
    }
}
