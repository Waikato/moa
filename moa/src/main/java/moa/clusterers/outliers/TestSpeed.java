/*
 *    TestSpeed.java
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

package moa.clusterers.outliers;

import moa.clusterers.outliers.AbstractC.AbstractC;
import moa.clusterers.outliers.Angiulli.ExactSTORM;
import moa.clusterers.outliers.MCOD.MCOD;
import moa.clusterers.outliers.SimpleCOD.SimpleCOD;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import com.yahoo.labs.samoa.instances.Instance;

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
            Instance newInst = stream.nextInstance().getData();
            
            //scod.processNewInstanceImpl(newInst);  
            mcod.processNewInstanceImpl(newInst);  
            //angiulli.processNewInstanceImpl(newInst);  
            //abstractC.processNewInstanceImpl(newInst); 
            
            numberSamples++;
        }      
        
        System.out.println("Total time = " + (System.currentTimeMillis() - tmStart) + " ms");
    }
}
