/*
 *    ApproxSTORM.java
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

package moa.clusterers.outliers.Angiulli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import moa.clusterers.outliers.Angiulli.ISBIndex.ISBNode;
import moa.clusterers.outliers.Angiulli.ISBIndex.ISBSearchResult;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;


public class ApproxSTORM extends STORMBase {
    public class ISBNodeAppr extends ISBNode {
        public Long count_after, count_before;
        public double fract_before;
        
        public ISBNodeAppr(Instance inst, StreamObj obj, Long id, int k) {
            super(inst, obj, id);
            m_k = k;
            count_after = 0L;
            count_before = 0L;
            fract_before = 0;
        }
    }
    
    public FloatOption radiusOption = new FloatOption("radius", 'r', "Search radius.", 0.1);
    public IntOption kOption = new IntOption("k", 't', "Parameter k.", 50);
    public IntOption queryFreqOption = new IntOption("queryFreq", 'q', "Query frequency.", 1);
    public FloatOption pOption = new FloatOption("p", 'p', "Parameter p.", 0.1);
    
    Set<ISBNode> safe_inliers; // list of safe inliers
    int m_FractWindowSize;  
    Random m_Random;
    
    @Override
    public String getObjectInfo(Object obj) {
        if (obj == null) return null;
        
        ISBNodeAppr node = (ISBNodeAppr) obj;
        
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
        infoTitle.add("count_before");
        infoValue.add(String.format("%d", node.count_before));
        
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
    
    public ApproxSTORM()
    {
        // System.out.println("DistanceOutliersAppr: created");
    }
    
    @Override
    public void Init() {    
        super.Init();
              
        m_WindowSize = windowSizeOption.getValue();
        m_radius = radiusOption.getValue();
        m_k = kOption.getValue();
        m_QueryFreq = queryFreqOption.getValue();
        m_FractWindowSize = (int) (pOption.getValue() * m_WindowSize);
                
        Println("Init DistanceOutliersAppr:");
        Println("   window_size: " + m_WindowSize);
        Println("   radius: " + m_radius);
        Println("   k: " + m_k);
        Println("   query_freq: " + m_QueryFreq);
        
        m_Random = new Random();
        
        objId = FIRST_OBJ_ID; // init object identifier
        // create fifo
        windowNodes = new Vector<ISBNode>();
        // create ISB
        ISB = new ISBIndex(m_radius, m_k);
        // create safe_inliers list
        safe_inliers = new HashSet<ISBNode>();
        
        // init statistics
        m_nBothInlierOutlier = 0;
        m_nOnlyInlier = 0;
        m_nOnlyOutlier = 0;
    }
    
    void AddSafeInlier(ISBNode node) {
        safe_inliers.add(node);
    }
    
    ISBNode GetSafeInlier(int idx) {
        ISBNode node = null;
        Iterator it = safe_inliers.iterator();
        while (idx >= 0) {
            node = (ISBNodeAppr)it.next();
            idx--;
        }
        return node;
    }
    
    boolean IsSafeInlier(ISBNodeAppr node) {
        return node.count_after >= m_k;
    }
    
    void PrintSafeInliers() {
        Print("Safe inliers: ");
        ISBNode node;
        Iterator it = safe_inliers.iterator();
        while (it.hasNext()) {
            node = (ISBNode) it.next();
            Print(node.id + " ");
        }
        Println(" ");
    }
    
    void RemoveNode(ISBNode node) {
        // remove node from ISB
        ISB.Remove(node);   
        // remove from fifo
        windowNodes.remove(node);
        // remove node from safe_inliers
        safe_inliers.remove(node);
        // remove from outliers
        RemoveExpiredOutlier(new Outlier(node.inst, node.id, node));
        // update statistics
        UpdateStatistics(node);
    }
    
    void RemoveSafeInlier(ISBNode node) {
        // remove node from ISB
        ISB.Remove(node);   
        // remove node from safe_inliers
        safe_inliers.remove(node);
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

        // delete a node if it has expired
        DeleteExpiredNode();

        // create new ISB node
        ISBNodeAppr nodeNew = new ISBNodeAppr(inst, obj, objId, m_k);
        if (bTrace) {
            Print("New obj: ");
            PrintNode(nodeNew);
        }
        
        // update object identifier
        objId++;
        
        // init nodeNew
        nodeNew.count_after = 1L;
        nodeNew.count_before = 0L;

        // perform range query search
        if (bTrace) Println("Perform range query seach:");
        nRangeQueriesExecuted++;
        Vector<ISBIndex.ISBSearchResult> nodes = ISB.RangeSearch(nodeNew, m_radius);

        // process each returned node
        int nSafeInliers;
        Long count_si_before = 0L;
        for (ISBSearchResult res : nodes) {
            ISBNodeAppr n = (ISBNodeAppr) res.node;
            if (bTrace) {
                Printf("   Found at d=%.2f: ", res.distance);
                PrintNode(res.node);
            }

            n.count_after++;

            if (IsSafeInlier(n)) {
                if (bTrace) Println("   Safe inlier: id=" + n.id);
                AddSafeInlier(n);
                count_si_before++;
            }

            nSafeInliers = safe_inliers.size();
            if (nSafeInliers > m_FractWindowSize) {
                // get a random safe inlier: 0 <= idx < nSafeInliers
                int idx = m_Random.nextInt(nSafeInliers);
                ISBNode si = GetSafeInlier(idx);
                if (bTrace) Println("   Remove random safe inlier: id=" + si.id);
                // remove node from ISB and safe-inliers-list
                RemoveSafeInlier(si);
            }

            nodeNew.count_before++;
        }

        // Set fract_before of curr_node which is determined as the ratio 
        // between the number of preceding neighbors of curr_node in ISB 
        // which are safe inliers and the total number of safe inliers in 
        // ISB, at the arrival time of curr_node.

        nSafeInliers = safe_inliers.size();
        if (nSafeInliers > 0) {
            nodeNew.fract_before = (double)count_si_before / (double)nSafeInliers;
        }
        else {
            if (bTrace) Println("Set fract before: no safe inliers yet, set 0.");
            nodeNew.fract_before = 0;
        }

        if (bTrace) {
            Println("Node: ");
            Println("   count_after=" + nodeNew.count_after);
            Println("   count_before=" + nodeNew.count_before);
            Printf("   fract_before=%.3f\n", nodeNew.fract_before);
            Println("Insert new node to ISB.");
        }
        
        // insert node to ISB
        ISB.Insert(nodeNew);
        
        // insert node at window
        windowNodes.add(nodeNew);

        if (bTrace) {
            PrintWindow();
            PrintSafeInliers();
        }
        
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
        ISBNodeAppr node;
        // process each node in the ISB (also in window)
        for (int i = 0; i < windowNodes.size(); i++) {
            node = (ISBNodeAppr) windowNodes.get(i);
            if (bTrace) {
                Print("   Process node: ");
                PrintNode(node);
            }
            UpdateNodeType(node);            
        }
    }
    
    void UpdateNodeType(ISBNodeAppr node) {
        double succ_neighs, prec_neighs;
        
        // get number of succeeding neighbors
        succ_neighs = node.count_after;
        if (bTrace) Println("      succ_neighs: " + succ_neighs);

        // get number of preceding neighbors
        prec_neighs = node.fract_before * (double)Math.abs((node.id + m_WindowSize) - GetWindowEnd());
        if (bTrace) Println("      prec_neighs: " + prec_neighs);

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
    
    void UpdateNodeStatistics(ISBNodeAppr node) {
        double succ_neighs = node.count_after;
        double prec_neighs = node.fract_before * (double)Math.abs((node.id + m_WindowSize) - GetWindowEnd());
        if (succ_neighs + prec_neighs < m_k) {
            node.nOutlier++; // update statistics
        } else {
            node.nInlier++; // update statistics
        }
    }
}
