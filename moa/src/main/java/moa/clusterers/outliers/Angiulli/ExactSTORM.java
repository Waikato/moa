/*
 *    ExactSTORM.java
 *    Copyright (C) 2013 Aristotle University of Thessaloniki, Greece
 *    @author D. Georgiadis, A. Gounaris, A. Papadopoulos, K. Tsichlas, Y. Manolopoulos
 *
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
package moa.clusterers.outliers.Angiulli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import moa.clusterers.outliers.Angiulli.ISBIndex.ISBNode;
import moa.clusterers.outliers.Angiulli.ISBIndex.ISBSearchResult;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;


// The algorithm is presented in "Distance-based outlier queries in data streams: the novel task and algorithms.
//Data Mining and Knowledge Discovery, 20(2):290–324,2010.


public class ExactSTORM extends STORMBase {    
    public class ISBNodeExact extends ISBNode {
        public int count_after;
        // nn_before:
        //   A list that needs O(logn) time for ordered insertion and search.
        //   It must be able to perform a search in the list using e.g. <=.
        private ArrayList<Long> nn_before;
        
        public ISBNodeExact(Instance inst, StreamObj obj, Long id, int k) {
            super(inst, obj, id);
            m_k = k;
            count_after = 0;
            nn_before = new ArrayList<Long>();
        }
        
        public void AddPrecNeigh(Long id) {
            int pos = Collections.binarySearch(nn_before, id);
            if (pos < 0) {
                // item does not exist, so add it to the right position
                nn_before.add(-(pos + 1), id);
            }
        }
        
        public int CountPrecNeighs(Long sinceId) {
            if (nn_before.size() > 0) {
                // get number of neighs with id >= sinceId
                int startPos;
                int pos = Collections.binarySearch(nn_before, sinceId);
                if (pos < 0) {
                    // item does not exist, should insert at position startPos
                    startPos = -(pos + 1);
                } else {
                    // item exists at startPos
                    startPos = pos;
                }
                
                if (startPos < nn_before.size()) {
                    return nn_before.size() - startPos;
                }
            }
            return 0;
        }
        
        public void PrintPrecNeighs() {
            Print("      nn_before: ");
            Iterator it = nn_before.iterator();
            while (it.hasNext()) {
                Print((Long)it.next() + " ");
            }
            Println(" ");
        }
    }
    
    public FloatOption radiusOption = new FloatOption("radius", 'r', "Search radius.", 0.1);
    public IntOption kOption = new IntOption("k", 't', "Parameter k.", 50);
    public IntOption queryFreqOption = new IntOption("queryFreq", 'q', "Query frequency.", 1);
    
    public ExactSTORM()
    {
        // System.out.println("DistanceOutliersExact: created");
    }
    
    @Override
    public String getObjectInfo(Object obj) {
        if (obj == null) return null;
        
        ISBNodeExact node = (ISBNodeExact) obj;
        
        ArrayList<String> infoTitle = new ArrayList<String>();
        ArrayList<String> infoValue = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        // show node position
        for (int i = 0; i < node.obj.dimensions(); i++) {
            infoTitle.add("Dim" + (i+1));
            infoValue.add(String.format("%.3f", node.obj.get(i)));
        }
        
        // show node properties
        infoTitle.add("id");
        infoValue.add(String.format("%d", node.id));
        infoTitle.add("count_after");
        infoValue.add(String.format("%d", node.count_after));
        infoTitle.add("|nn_before|");
        infoValue.add(String.format("%d", node.CountPrecNeighs(GetWindowStart())));
        
        sb.append("<html>");
        sb.append("<table>");
        int i = 0;
        while(i < infoTitle.size() && i < infoValue.size()){
            sb.append("<tr><td><b>"+infoTitle.get(i)+":</b></td><td>"+infoValue.get(i)+"</td></tr>");
            i++;
        }
        sb.append("</table>");

        
        sb.append("</html>");
        return sb.toString();
    }
    
    @Override
    public void Init() {   
        super.Init();
        
        m_WindowSize = windowSizeOption.getValue();
        m_radius = radiusOption.getValue();
        m_k = kOption.getValue();
        m_QueryFreq = queryFreqOption.getValue();
                
        Println("Init DistanceOutliersExact:");
        Println("   window_size: " + m_WindowSize);
        Println("   radius: " + m_radius);
        Println("   k: " + m_k);
        Println("   query_freq: " + m_QueryFreq);
        
        objId = FIRST_OBJ_ID; // init object identifier
        // create fifo
        windowNodes = new Vector<ISBNode>();
        // create ISB
        ISB = new ISBIndex(m_radius, m_k);
        
        // init statistics
        m_nBothInlierOutlier = 0;
        m_nOnlyInlier = 0;
        m_nOnlyOutlier = 0;
    }
    
    void RemoveNode(ISBNode node) {
        // remove node from ISB
        ISB.Remove(node);   
        // remove from fifo
        windowNodes.remove(node);
        // remove from outliers
        RemoveExpiredOutlier(new Outlier(node.inst, node.id, node));
        // update statistics
        UpdateStatistics(node);
    }
    
    void DeleteExpiredNode() {
        if (windowNodes.size() <= 0)
            return;        
       
        // get oldest node
        ISBNode node = windowNodes.get(0);
        // check if node has expired
        if (node.id < GetWindowStart()) {
            if (bTrace) {
                Print("Delete expired node: ");
                PrintNode(node);
            }
            // remove node
            RemoveNode(node);
        }
    }
    
    @Override
    protected void ProcessNewStreamObj(Instance inst)
    {        
        if (bShowProgress) ShowProgress("Processed " + objId + " stream objects.");       
        // PrintInstance(inst);
        
        double[] values = getInstanceValues(inst);
        StreamObj obj = new StreamObj(values);
        
        if (bTrace) Println("\n- - - - - - - - - - - -\n");

        // create new ISB node
        ISBNodeExact nodeNew = new ISBNodeExact(inst, obj, objId, m_k);
        if (bTrace) {
            Print("New obj: ");
            PrintNode(nodeNew);
        }
        // update object identifier
        objId++;
        
        // delete a node if it has expired
        DeleteExpiredNode();
        
        // init nodeNew
        nodeNew.count_after = 1;

        // perform range query search
        if (bTrace) Println("Perform range query seach:");
        nRangeQueriesExecuted++;
        Vector<ISBIndex.ISBSearchResult> nodes = ISB.RangeSearch(nodeNew, m_radius);

        // process each returned node
        for (ISBSearchResult res : nodes) {
            ISBNodeExact n = (ISBNodeExact) res.node;
            if (bTrace)  {
                Printf("   Found at d=%.2f: ", res.distance);
                PrintNode(res.node);
            }
            
            n.count_after++;            
            nodeNew.AddPrecNeigh(res.node.id);
        }

        if (bTrace) Println("Insert new node to ISB.");
        ISB.Insert(nodeNew);
        
        // insert node at window
        windowNodes.add(nodeNew);
        if (bTrace) PrintWindow();
        
        if (CanSearch()) {
            // invoke query function to detect outliers
            SearchOutliers();
        } else {
            // update statistics outlierness of new node
            UpdateNodeStatistics(nodeNew);
        }
    }
    
    void SearchOutliers() {
        if (bTrace) Println("Invoke query: ");
        ISBNodeExact node;
        // process each node in the ISB (also in window)
        for (int i = 0; i < windowNodes.size(); i++) {
            node = (ISBNodeExact) windowNodes.get(i);
            if (bTrace) {
                Print("   Process node: ");
                PrintNode(node);
            }
            UpdateNodeType(node);        
        }
    }
    
    void UpdateNodeType(ISBNodeExact node) {
        int succ_neighs, prec_neighs;
        
        // get number of succeeding neighbors
        succ_neighs = node.count_after;
        if (bTrace) Println("      succ_neighs: " + succ_neighs);

        // get number of preceding neighbors with
        // non-expired objects determined
        prec_neighs = node.CountPrecNeighs(GetWindowStart());
        if (bTrace) {
            Println("      GetWindowStart(): " + GetWindowStart());
            node.PrintPrecNeighs();
            Println("      prec_neighs: " + prec_neighs);
        }

        // check if node is an outlier
        if (succ_neighs + prec_neighs < m_k) {
            SaveOutlier(node);
            if (bTrace) {
                Print("*** Outlier: ");
                PrintNode(node);
            }
        } else {
            RemoveOutlier(node);
        }
    }
    
    void UpdateNodeStatistics(ISBNodeExact node) {
        int succ_neighs = node.count_after;
        int prec_neighs = node.CountPrecNeighs(GetWindowStart());
        if (succ_neighs + prec_neighs < m_k) {
            node.nOutlier++; // update statistics
        } else {
            node.nInlier++; // update statistics
        }
    }
}
