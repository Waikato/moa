/*
 *    SimpleCOD.java
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

import java.util.Vector;
import moa.clusterers.outliers.SimpleCOD.ISBIndex.ISBNode;
import moa.clusterers.outliers.SimpleCOD.ISBIndex.ISBSearchResult;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;


////The algorithm is described in 
// M. Kontaki, A. Gounaris, A. N. Papadopoulos, K. Tsichlas, and Y. Manolopoulos. 
//Continuous monitoring of distance-based outliers over data streams.
//In ICDE, pages 135–146, 2011.

public class SimpleCOD extends SimpleCODBase {
    public FloatOption radiusOption = new FloatOption("radius", 'r', "Search radius.", 0.1);
    public IntOption kOption = new IntOption("k", 't', "Parameter k.", 50);
    
    public SimpleCOD()
    {
        // System.out.println("SimpleCOD: created");
    }
    
    @Override
    public void Init() {   
        super.Init();
        
        m_WindowSize = windowSizeOption.getValue();
        m_radius = radiusOption.getValue();
        m_k = kOption.getValue();
                
        Println("Init SimpleCOD:");
        Println("   window_size: " + m_WindowSize);
        Println("   radius: " + m_radius);
        Println("   k: " + m_k);
        
        //bTrace = true;
        //bWarning = true;
        
        objId = FIRST_OBJ_ID; // init object identifier
        // create nodes list of window
        windowNodes = new Vector<ISBNode>();
        // create ISB
        ISB = new ISBIndex(m_radius, m_k);
        // create event queue
        eventQueue = new EventQueue();
        
        // init statistics
        m_nBothInlierOutlier = 0;
        m_nOnlyInlier = 0;
        m_nOnlyOutlier = 0;
    }
    
    void ProcessNewNode(ISBNode nodeNew, boolean bNewNode) {
        if (bTrace) { Print("ProcessNewNode: "); PrintNode(nodeNew); }        
        
        if (bTrace) Println("Perform R range query");    
        nRangeQueriesExecuted++;
        Vector<ISBSearchResult> resultNodes;
        resultNodes = ISB.RangeSearch(nodeNew, m_radius);
        for (ISBSearchResult sr : resultNodes) {
            double distance = sr.distance;
            ISBNode q = sr.node;
            if ( (nodeNew != q) && (distance <= m_radius) ) {  
                if (bTrace) Println("nodeNew has neighbor q.id " + q.id);  
                nodeNew.AddPrecNeigh(q);
                q.count_after++;
                
                if (q.bOutlier) {
                    int count = q.CountPrecNeighs(GetWindowStart()) + q.count_after; 
                    if (count >= m_k) {
                        if (bTrace) Println("q.id " + q.id + " is now an inlier");  
                        q.bOutlier = false;
                        RemoveOutlier(q);
                        // insert q to event queue
                        ISBNode nodeMinExp = q.GetMinPrecNeigh(GetWindowStart());
                        AddToEventQueue(q, nodeMinExp);
                    }
                }
            }
        }
        
        if (bTrace) Println("Check if nodeNew is an inlier or outlier"); 
        int count = nodeNew.CountPrecNeighs(GetWindowStart()) + nodeNew.count_after;
        if (count >= m_k) {
            if (bTrace) Println("nodeNew is an inlier");   
            nodeNew.bOutlier = false;
            RemoveOutlier(nodeNew); // updates statistics
            // insert nodeNew to event queue
            ISBNode nodeMinExp = nodeNew.GetMinPrecNeigh(GetWindowStart());
            AddToEventQueue(nodeNew, nodeMinExp);
        } else {
            if (bTrace) Println("nodeNew is an outlier");
            nodeNew.bOutlier = true;
            SaveOutlier(nodeNew);
        }
    }

    void AddToEventQueue(ISBNode x, ISBNode nodeMinExp) {
        if (bTrace) Println("AddToEventQueue x.id: " + x.id); 
        if (nodeMinExp != null) {
            Long expTime = GetExpirationTime(nodeMinExp);
            eventQueue.Insert(x, expTime);
            if (bTrace) {
                Print("x.nn_before: "); PrintNodeList(x.Get_nn_before());
                Println("nodeMinExp: " + nodeMinExp.id + ", expTime = " + expTime);
                PrintEventQueue();
            }
        } else {
            if (bWarning) Println("AddToEventQueue: Cannot add x.id: " + x.id + " to event queue (nn_before is empty, count_after=" + x.count_after + ")"); 
        }
    }
    
    void ProcessEventQueue(ISBNode nodeExpired) {
        EventItem e = eventQueue.FindMin();
        while ((e != null) && (e.timeStamp <= GetWindowEnd())) {
            e = eventQueue.ExtractMin();
            ISBNode x = e.node;
            if (bTrace) Println("Process event queue: check node x: " + x.id);
            // node x must be in window
            if (IsNodeIdInWin(x.id)) {
                // remove nodeExpired from x.nn_before
                x.RemovePrecNeigh(nodeExpired);
                // get amount of neighbors of x
                int count = x.count_after + x.CountPrecNeighs(GetWindowStart());
                if (count < m_k) {
                    if (bTrace) Println("x is an outlier");
                    x.bOutlier = true;
                    SaveOutlier(x);
                } else {
                    if (bTrace) Println("x is an inlier, add to event queue");
                    x.bOutlier = false;
                    // get oldest preceding neighbor of x
                    ISBNode nodeMinExp = x.GetMinPrecNeigh(GetWindowStart());
                    // add x to event queue
                    AddToEventQueue(x, nodeMinExp);
                }
            } else {
                if (bWarning) Println("Process event queue: node x.id: " + x.id + " has expired!");
            }
            e = eventQueue.FindMin();
        }
    }
    
    void ProcessExpiredNode(ISBNode nodeExpired) { 
        if (nodeExpired != null) {
            if (bTrace) Println("\nnodeExpired: " + nodeExpired.id);
            ISB.Remove(nodeExpired); // remove nodeExpired from index
            RemoveNode(nodeExpired);
            ProcessEventQueue(nodeExpired);
        }
    }
    
    @Override
    protected void ProcessNewStreamObj(Instance inst)
    {                
        if (bShowProgress) ShowProgress("Processed " + (objId-1) + " stream objects.");       
        // PrintInstance(inst);
        
        double[] values = getInstanceValues(inst);
        StreamObj obj = new StreamObj(values);
        
        if (bTrace) Println("\n- - - - - - - - - - - -\n");

        // create new ISB node
        ISBNode nodeNew = new ISBNode(inst, obj, objId);
        if (bTrace) { Print("New node: "); PrintNode(nodeNew); }
        
        objId++; // update object identifier (slide window)
        
        AddNode(nodeNew); // add nodeNew to window
        if (bTrace) PrintWindow();
        
        // insert new node to ISB index
        ISB.Insert(nodeNew);
        
        ProcessNewNode(nodeNew, true);
        ProcessExpiredNode(GetExpiredNode());
        
        if (bTrace) {
            PrintOutliers();
            PrintISB();
        }
    }
}
