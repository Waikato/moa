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

import java.util.*;
import weka.core.Instance;

public class ISBIndex {    
    public static class ISBNode {
        public Instance inst;
        public StreamObj obj;
        public Long id;
        public ArrayList<Integer> lt_cnt;
        
        // statistics
        public int nOutlier;
        public int nInlier;

        public ISBNode(Instance inst, StreamObj obj, Long id) {
            this.inst = inst;
            this.obj = obj;
            this.id = id;
            lt_cnt = new ArrayList<Integer>();
            
            // init statistics
            nOutlier = 0;
            nInlier  = 0;
        }
    }
    
    MyMTree mtree;
    Map<Integer, Set<ISBNode>> mapNodes;
    double m_radius;
    double m_Fraction;
    
    public ISBIndex(double radius, double fra) {
        mtree = new MyMTree();
        mapNodes = new HashMap<Integer, Set<ISBNode>>();
        m_radius = radius;
        m_Fraction = fra;
    }
    
    public static class ISBSearchResult {
        public ISBNode node;
        public double distance;
        
        public ISBSearchResult(ISBNode n, double distance) {
            this.node = n;
            this.distance = distance;
        }
    }
    
    public Vector<ISBSearchResult> RangeSearch(ISBNode node, double radius) {
        Vector<ISBSearchResult> results = new Vector<ISBSearchResult>();
        // execute range search at mtree
        StreamObj obj;
        double d;
        MyMTree.Query query = mtree.getNearestByRange(node.obj, radius);
        for (MyMTree.ResultItem q : query) {
            // get next obj found within range
            obj = q.data;
            // get distance of obj from query
            d = q.distance;
            // get all nodes referencing obj
            Vector<ISBNode> nodes = MapGetNodes(obj);
            for (int i = 0; i < nodes.size(); i++)
                results.add(new ISBSearchResult(nodes.get(i), d));
        }        
        return results;
    }
    
    public void Insert(ISBNode node) {
        // insert object of node at mtree
        mtree.add(node.obj);
        // insert node at map
        MapInsert(node);    
    }
    
    public void Remove(ISBNode node) {
        // remove from map
        MapDelete(node);
        // check if stream object at mtree is still being referenced
        if (MapCountObjRefs(node.obj) <= 0) {
            // delete stream object from mtree
            mtree.remove(node.obj);
        }
    }
    
    Vector<ISBNode> MapGetNodes(StreamObj obj) {
        int h = obj.hashCode();
        Vector<ISBNode> v = new Vector<ISBNode>();
        if (mapNodes.containsKey(h)) {
            Set<ISBNode> s = mapNodes.get(h);
            ISBNode node;
            Iterator<ISBNode> i = s.iterator();
            while (i.hasNext()) {
                node = i.next();
                if (node.obj.equals(obj))
                    v.add(node);
            }
        }
        return v;
    }
    
    int MapCountObjRefs(StreamObj obj) {
        int h = obj.hashCode();
        int iCount = 0;
        if (mapNodes.containsKey(h)) {
            Set<ISBNode> s = mapNodes.get(h);
            ISBNode n;
            Iterator<ISBNode> i = s.iterator();
            while (i.hasNext()) {
                n = i.next();
                if (n.obj.equals(obj))
                    iCount++;
            }
        }
        return iCount;
    }
    
    void MapInsert(ISBNode node) {
        int h = node.obj.hashCode();
        Set<ISBNode> s;
        if (mapNodes.containsKey(h)) {
            s = mapNodes.get(h);
            s.add(node);
        }
        else {
            s = new HashSet<ISBNode>();
            s.add(node);
            mapNodes.put(h, s);
        }
    }
    
    void MapDelete(ISBNode node) {
        int h = node.obj.hashCode();
        if (mapNodes.containsKey(h)) {
            Set<ISBNode> s = mapNodes.get(h);
            s.remove(node);
            if (s.isEmpty()) { // ### added
                mapNodes.remove(h);
            }
        }
    }    
}
