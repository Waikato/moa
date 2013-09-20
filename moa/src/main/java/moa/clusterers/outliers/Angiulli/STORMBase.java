/*
 *    STORMBase.java
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

import java.util.Vector;
import moa.clusterers.outliers.Angiulli.ISBIndex.ISBNode;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import com.github.javacliparser.FlagOption;

public abstract class STORMBase extends MyBaseOutlierDetector {   
    public FlagOption waitWinFullOption = new FlagOption("waitWinFull", 'a', "Output outliers when windows is full.");
     
    protected static final Long FIRST_OBJ_ID = 1L;
    
    // object identifier increments with each new data stream object
    protected Long objId;
    // list used to find expired nodes
    protected Vector<ISBNode> windowNodes; 
    protected ISBIndex ISB;
    protected int m_WindowSize;
    protected double m_radius;
    protected int m_k;
    // perform a query every m_QueryFreq objects
    protected int m_QueryFreq; 
    
    // statistics
    public int m_nBothInlierOutlier;
    public int m_nOnlyInlier;
    public int m_nOnlyOutlier;
    
    @Override
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Statistics:\n\n");
        
        // get counters of expired nodes
        int nBothInlierOutlier = m_nBothInlierOutlier;
        int nOnlyInlier = m_nOnlyInlier;
        int nOnlyOutlier = m_nOnlyOutlier;
        
        // add counters of non expired nodes
        for (ISBNode node : windowNodes) {
            if ((node.nInlier > 0) && (node.nOutlier > 0))
                nBothInlierOutlier++;
            else if (node.nInlier > 0)
                nOnlyInlier++;
            else
                nOnlyOutlier++;
        }
        
        int sum = nBothInlierOutlier + nOnlyInlier + nOnlyOutlier;
        if (sum > 0) {
            sb.append(String.format("  Nodes always inlier: %d (%.1f%%)\n", nOnlyInlier, (100 * nOnlyInlier) / (double)sum));
            sb.append(String.format("  Nodes always outlier: %d (%.1f%%)\n", nOnlyOutlier, (100 * nOnlyOutlier) / (double)sum));
            sb.append(String.format("  Nodes both inlier and outlier: %d (%.1f%%)\n", nBothInlierOutlier, (100 * nBothInlierOutlier) / (double)sum));
            
            sb.append("  (Sum: " + sum + ")\n");
        }
        
        sb.append("\n  Total range queries: " + nRangeQueriesExecuted + "\n");
        sb.append("  Max memory usage: " + iMaxMemUsage + " MB\n");
        sb.append("  Total process time: " + String.format("%.2f ms", nTotalRunTime / 1000.0) + "\n");
        
        return sb.toString();
    }
    
    Long GetWindowEnd() {
        return objId - 1;
    }
    
    Long GetWindowStart() {
        Long x = GetWindowEnd() - m_WindowSize + 1;
        if (x < FIRST_OBJ_ID) 
            x = FIRST_OBJ_ID;
        return x;
    }
    
    boolean IsWinFull() {
        return (GetWindowEnd() >= FIRST_OBJ_ID + m_WindowSize - 1);
    }
    
    boolean CanSearch() {
        if (IsWinFull()  || !waitWinFullOption.isSet()) {
            if ((GetWindowEnd() - FIRST_OBJ_ID + 1) % m_QueryFreq == 0) {
                // perform query every m_QueryFreq objects
                return true;
            }
        }
        return false;
    }
    
    void SaveOutlier(ISBNode node) {
        AddOutlier(new Outlier(node.inst, node.id, node));
        node.nOutlier++; // update statistics
    }
    
    void RemoveOutlier(ISBNode node) {
        RemoveOutlier(new Outlier(node.inst, node.id, node));
        node.nInlier++; // update statistics
    }
    
    protected void UpdateStatistics(ISBNode node) {
        if ((node.nInlier > 0) && (node.nOutlier > 0))
            m_nBothInlierOutlier++;
        else if (node.nInlier > 0)
            m_nOnlyInlier++;
        else
            m_nOnlyOutlier++;
    }
    
    @Override
    protected boolean IsNodeIdInWin(long id) {
        if ((GetWindowStart() <= id) && (id <= GetWindowEnd()) )
            return true;
        else
            return false;
    }
    
    void PrintWindow() {
        Println("Window [" + GetWindowStart() + "-" + GetWindowEnd() + "]: ");
        ISBNode node;
        for (int i = 0; i < windowNodes.size(); i++) {
            node = windowNodes.get(i);
            Print("   Node: ");
            PrintNode(node);
        }
    }
    
    void PrintNode(ISBNode n) {
        Print("id=" + n.id + " (");
        int dim = n.obj.dimensions();
        for (int d = 0; d < dim; d++) {
            Print(Double.toString(n.obj.get(d)));
            if (d < dim - 1)
                Print(", ");
        }
        Println(")");
    }
}
