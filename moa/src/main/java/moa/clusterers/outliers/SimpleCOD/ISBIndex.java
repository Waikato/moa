/*
 *    ISBIndex.java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.yahoo.labs.samoa.instances.Instance;

public class ISBIndex {    
    public static class ISBNode implements Comparable<ISBNode> {        
        public Instance inst;
        public StreamObj obj;
        public Long id;
        public boolean bOutlier;
        public int count_after;
        private ArrayList<ISBNode> nn_before;
        
        // statistics
        public int nOutlier;
        public int nInlier;
        

        public ISBNode(Instance inst, StreamObj obj, Long id) {
            this.inst = inst;
            this.obj  = obj;
            this.id   = id;
            
            this.bOutlier    = false;
            this.count_after = 1;
            this.nn_before   = new ArrayList<ISBNode>();
            
            // init statistics
            nOutlier = 0;
            nInlier  = 0;
        }  
        
        @Override
        public int compareTo(ISBNode t) {
            if (this.id > t.id)
                return +1;
            else if (this.id < t.id)
                return -1;
            return 0;
        }
        
        public void AddPrecNeigh(ISBNode node) {
            int pos = Collections.binarySearch(nn_before, node);
            if (pos < 0) {
                // item does not exist, so add it to the right position
                nn_before.add(-(pos + 1), node);
            }
        }
        
        public void RemovePrecNeigh(ISBNode node) {
            int pos = Collections.binarySearch(nn_before, node);
            if (pos >= 0) {
                // item exists
                nn_before.remove(pos);
            }
        }
        
        public ISBNode GetMinPrecNeigh(Long sinceId) {
            if (nn_before.size() > 0) {                
                int startPos;
                ISBNode dummy = new ISBNode(null, null, sinceId);
                
                int pos = Collections.binarySearch(nn_before, dummy);
                if (pos < 0) {
                    // item does not exist, should insert at position startPos
                    startPos = -(pos + 1);
                } else {
                    // item exists at startPos
                    startPos = pos;
                }
                
                if (startPos < nn_before.size()) {
                    return nn_before.get(startPos);
                }
            }
            return null;
        }
        
        public int CountPrecNeighs(Long sinceId) {
            if (nn_before.size() > 0) {
                // get number of neighs with id >= sinceId
                int startPos;
                ISBNode dummy = new ISBNode(null, null, sinceId);
                int pos = Collections.binarySearch(nn_before, dummy);
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
        
        public List<ISBNode> Get_nn_before() {
            return nn_before;
        }
    }
    
    MyMTree mtree;
    Map<Integer, Set<ISBNode>> mapNodes;
    double m_radius;
    int m_k; // k nearest neighbors
    
    public ISBIndex(double radius, int k) {
        mtree = new MyMTree();
        mapNodes = new HashMap<Integer, Set<ISBNode>>();
        m_radius = radius;
        m_k = k;
    }
    
    Vector<ISBNode> GetAllNodes() {
        Vector<ISBNode> v = new Vector<ISBNode>(); 
        Iterator it = mapNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Set<ISBNode> setNodes = (Set<ISBNode>) pairs.getValue();
            for (ISBNode n : setNodes) {
                v.add(n);
            }
        }  
        return v;
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
