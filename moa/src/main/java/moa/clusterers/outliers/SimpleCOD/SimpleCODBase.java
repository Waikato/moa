/*
 *    SimpleCODBase.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import moa.clusterers.outliers.SimpleCOD.ISBIndex.ISBNode;

public abstract class SimpleCODBase extends MyBaseOutlierDetector {    
    protected static class EventItem implements Comparable<EventItem> {
        public ISBNode node;
        public Long timeStamp;

        public EventItem(ISBNode node, Long timeStamp) {
            this.node = node;
            this.timeStamp = timeStamp;
        }

        @Override
        public int compareTo(EventItem t) {
            if (this.timeStamp > t.timeStamp) {
                return +1;
            } else if (this.timeStamp < t.timeStamp) {
                return -1;
            } else {
                if (this.node.id > t.node.id)
                    return +1;
                else if (this.node.id < t.node.id)
                    return -1;
            }
            return 0;
        }
    }
    
    protected static class EventQueue {
        public TreeSet<EventItem> setEvents;

        public EventQueue() {
            setEvents = new TreeSet<EventItem>();
        }
        
        public void Insert(ISBNode node, Long expTime) {
            setEvents.add(new EventItem(node, expTime));
        }
        
        public EventItem FindMin() {
            if (setEvents.size() > 0) {
                // events are sorted ascenting by expiration time
                return setEvents.first();
            }
            return null;
        }
        
        public EventItem ExtractMin() {
            EventItem e = FindMin();
            if (e != null) {
                setEvents.remove(e);
                return e;
            }
            return null;
        }
    }
    
    protected static final Long FIRST_OBJ_ID = 1L;
    
    // object identifier increments with each new data stream object
    protected Long objId;
    // list used to find expired nodes
    protected Vector<ISBNode> windowNodes; 
    protected EventQueue eventQueue;
    // index of objects
    protected ISBIndex ISB;
    protected int m_WindowSize;
    protected double m_radius;
    protected int m_k;
    public boolean bWarning = false;
    
    // statistics
    public int m_nBothInlierOutlier;
    public int m_nOnlyInlier;
    public int m_nOnlyOutlier;
    
    @Override
    public String getObjectInfo(Object obj) {
        if (obj == null) return null;
        
        ISBNode node = (ISBNode) obj;
        
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
    
    Long GetExpirationTime(ISBNode node) {
        return node.id + m_WindowSize;
    }
    
    void SaveOutlier(ISBNode node) {
        AddOutlier(new Outlier(node.inst, node.id, node));
        node.nOutlier++; // update statistics
    }
    
    void RemoveOutlier(ISBNode node) {
        RemoveOutlier(new Outlier(node.inst, node.id, node));
        node.nInlier++; // update statistics
    }
    
    @Override
    protected boolean IsNodeIdInWin(long id) {
        int toleranceStart = 1;
        Long start = GetWindowStart() - toleranceStart;
        if ( (start <= id) && (id <= GetWindowEnd()) )
            return true;
        else
            return false;
    }
    
    void AddNode(ISBNode node) {
        windowNodes.add(node);
    }
    
    void RemoveNode(ISBNode node) {
        windowNodes.remove(node);
        RemoveExpiredOutlier(new Outlier(node.inst, node.id, node)); // ### remove when expired?
        // update statistics
        UpdateStatistics(node);
    }
    
    void UpdateStatistics(ISBNode node) {
        if ((node.nInlier > 0) && (node.nOutlier > 0))
            m_nBothInlierOutlier++;
        else if (node.nInlier > 0)
            m_nOnlyInlier++;
        else
            m_nOnlyOutlier++;
    }
    
    ISBNode GetExpiredNode() {
        if (windowNodes.size() <= 0)
            return null;       
        // get oldest node
        ISBNode node = windowNodes.get(0);
        // check if node has expired
        if (node.id < GetWindowStart()) {
            return node;
        }        
        return null;
    }
    
    double GetEuclideanDist(ISBNode n1, ISBNode n2)
    {
        double diff;
        double sum = 0;
        int d = n1.obj.dimensions();
        for (int i = 0; i < d; i++) {
            diff = n1.obj.get(i) - n2.obj.get(i);
            sum += Math.pow(diff, 2);
        }
        return Math.sqrt(sum);
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
    
    public void PrintNodeSet(Set<ISBNode> set) {
        for (ISBNode n : set) {
            Print(n.id + " ");
        }
        Println("");
    }
    
    public void PrintNodeVector(Vector<ISBNode> vector) {
        for (ISBNode n : vector) {
            Print(n.id + " ");
        }
        Println("");
    }
    
    public void PrintNodeList(List<ISBNode> list) {
        for (ISBNode n : list) {
            Print(n.id + " ");
        }
        Println("");
    }
    
    public void PrintEventQueue() {
        Println("event queue: ");
        for (EventItem n : eventQueue.setEvents) {
            Printf("  id=%d, exp=%d\n", n.node.id, n.timeStamp);
        }
    }
    
    public void PrintISB() {
        Print("PD: ");
        for (ISBNode n : ISB.GetAllNodes()) {
            Print(n.id + " ");
        }
        Println("");
    }
}
