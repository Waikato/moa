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

import java.util.ArrayList;
import java.util.Vector;
import moa.clusterers.outliers.AbstractC.ISBIndex.ISBNode;
import moa.clusterers.outliers.MyBaseOutlierDetector;

public abstract class AbstractCBase extends MyBaseOutlierDetector {    
    protected static final Long FIRST_OBJ_ID = 1L;
    
    // object identifier increments with each new data stream object
    protected Long objId;
    // list used to find expired nodes
    protected Vector<ISBNode> windowNodes; 
    protected ISBIndex ISB;
    protected int m_WindowSize;
    protected double m_radius;
    protected double m_Fraction;
    protected boolean bWarning = false;
    
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
    
    void AddNode(ISBNode node) {
        windowNodes.add(node);
        ISB.Insert(node);
    }
    
    void RemoveNode(ISBNode node) {
        windowNodes.remove(node);
        ISB.Remove(node);
        RemoveExpiredOutlier(new Outlier(node.inst, node.id, node)); // ### remove when expired?
        // update statistics
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
    
    public void Print_lt_cnt(ArrayList<Integer> lt_cnt) {
        for (int i = 0; i < lt_cnt.size(); i++) {
            Print("(" + i + ": " + lt_cnt.get(i) + ") ");
        }
        Println("");
    }
}
