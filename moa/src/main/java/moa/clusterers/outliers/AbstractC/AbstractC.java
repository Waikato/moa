/*
 *    AbstractC.java
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

package moa.clusterers.outliers.AbstractC;

import java.util.Vector;
import moa.clusterers.outliers.AbstractC.ISBIndex.ISBNode;
import moa.clusterers.outliers.AbstractC.ISBIndex.ISBSearchResult;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

//The algorithm is presented in:
//D. Yang, E. Rundensteiner, and M. Ward.
//Neighbor-based pattern detection for windows over streaming data. 
//In EDBT, pages 529–540, 2009


public class AbstractC extends AbstractCBase {    
    public FloatOption radiusOption = new FloatOption("radius", 'r', "Search radius.", 0.1);
    //public FloatOption fractionOption = new FloatOption("fraction", 'f', "Parameter fraction.", 0.05);
    public IntOption kOption = new IntOption("k", 't', "Parameter k.", 50);
    public FlagOption waitWinFullOption = new FlagOption("waitWinFull", 'a', "Output outliers when windows is full.");
    
    public AbstractC()
    {
        // System.out.println("AbstractC: created");
    }
    
    @Override
    public void Init() {   
        super.Init();
        
        m_WindowSize = windowSizeOption.getValue();
        m_radius = radiusOption.getValue();
        //m_Fraction = fractionOption.getValue();
        m_Fraction = (double)kOption.getValue() / (double)m_WindowSize;
                
        Println("Init AbstractC:");
        Println("   window_size: " + m_WindowSize);
        Println("   radius: " + m_radius);
        Println("   Fraction: " + m_Fraction);
        Println("   (Fraction * window_size: " + String.format("%.2f", m_Fraction * m_WindowSize) + ")");
        
        //bTrace = true;
        bWarning = true;
        
        objId = FIRST_OBJ_ID; // init object identifier
        // create fifo
        windowNodes = new Vector<ISBNode>();
        // create ISB
        ISB = new ISBIndex(m_radius, m_Fraction);
        
        // init statistics
        m_nBothInlierOutlier = 0;
        m_nOnlyInlier = 0;
        m_nOnlyOutlier = 0;
    }
    
    void UpdateNeighbors(ISBNode n, ISBNode q) {
        if (n == q) return;
        if (bTrace) Println("UpdateNeighbors: n.id: " + n.id + ", q.id: " + q.id);
        
        int len = q.lt_cnt.size();
        for (int i = 0; i < len; i++) {
            // n.lt_cnt++
            n.lt_cnt.set(i, n.lt_cnt.get(i) + 1);
            // q.lt_cnt++
            q.lt_cnt.set(i, q.lt_cnt.get(i) + 1);
        }
    }
    
    void OutputPatterns() {
        if (bTrace) Println("OutputPatterns");
        
        double thr = m_Fraction * m_WindowSize;
        
        for (ISBNode node : windowNodes) {
            if (node.lt_cnt.size() > 0) {
                if (IsWinFull() || !waitWinFullOption.isSet()) {
                    if (node.lt_cnt.get(0) < thr) {
                        SaveOutlier(node);
                    } else {
                        RemoveOutlier(node);
                    }
                }
                node.lt_cnt.remove(0);
            } else {
                if (bWarning) Println("OutputPatterns: " + node.id + ".lt_cnt is empty!");
            }
        }
    }
    
    @Override
    protected void ProcessNewStreamObj(Instance inst)
    {        
        if (bShowProgress) ShowProgress("Processed " + (objId - 1) + " stream objects.");       
        // PrintInstance(inst);
        
        double[] values = getInstanceValues(inst);
        StreamObj obj = new StreamObj(values);
        
        // process new data stream object
        if (bTrace) Println("\n- - - - - - - - - - - -\n");
        
        // create new ISB node
        ISBNode nodeNew = new ISBNode(inst, obj, objId);
        if (bTrace) { Print("New node: "); PrintNode(nodeNew); }
        
        objId++; // update object identifier (slide window)
        
        // purge expired node
        ISBNode nodeExpired = GetExpiredNode();
        if (nodeExpired != null) {
            // purge nodeExpired
            if (bTrace) { Print("nodeExpired: "); PrintNode(nodeExpired); }
            RemoveNode(nodeExpired);
        }
        
        // initialize nodeNew.lt_cnt
        if (bTrace) Println("initialize nodeNew");
        for (int i = 0; i < m_WindowSize; i++) {
            nodeNew.lt_cnt.add(1);
        }        
        AddNode(nodeNew); // add nodeNew to window and index
        if (bTrace) PrintWindow();
        
        // perform range query search
        if (bTrace) Println("Perform range query seach");
        nRangeQueriesExecuted++;
        Vector<ISBIndex.ISBSearchResult> neighbors = ISB.RangeSearch(nodeNew, m_radius);

        // process each returned node
        for (ISBSearchResult res : neighbors) {
            ISBNode node = res.node;
            UpdateNeighbors(nodeNew, node);
        }
        
        OutputPatterns();
        
        if (bTrace) {            
            PrintOutliers();
            for (ISBNode node : windowNodes) {
                Print(node.id + ".lt_count: "); Print_lt_cnt(node.lt_cnt);
            }
        }
    }
}
