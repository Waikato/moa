/*
 *    Test.java
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

package moa.clusterers.outliers.SimpleCOD;

import moa.streams.ArffFileStream;
import com.yahoo.labs.samoa.instances.Instance;


public class Test {    
    public static void main(String[] args) throws Exception 
    {
        //if (true) return;
        
        int numInstances = 30;
        moa.streams.ArffFileStream stream = new ArffFileStream("./datasets/debug_1.txt", -1);
        //RandomRBFGeneratorEvents stream = new RandomRBFGeneratorEvents();
        stream.prepareForUse();
        
        SimpleCOD myOutlierDetector= new SimpleCOD();
        myOutlierDetector.kOption.setValue(3);
        myOutlierDetector.radiusOption.setValue(5);
        myOutlierDetector.windowSizeOption.setValue(6);
        myOutlierDetector.setModelContext(stream.getHeader());
        myOutlierDetector.prepareForUse(); 
        
        Long tmStart = System.currentTimeMillis();
        
        int numberSamples = 0;   
        int w = myOutlierDetector.windowSizeOption.getValue();
        while (stream.hasMoreInstances() && (numberSamples < numInstances)) {
            Instance newInst = stream.nextInstance().getData();
            myOutlierDetector.processNewInstanceImpl(newInst);            
            numberSamples++;
        }        
        //myOutlierDetector.PrintOutliers();
        System.out.println("Total time = " + (System.currentTimeMillis() - tmStart) + " ms");
    }
}
