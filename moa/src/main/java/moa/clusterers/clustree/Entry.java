/*
 *    Entry.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Sanchez Villaamil (moa@cs.rwth-aachen.de)
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
package moa.clusterers.clustree;

import java.io.Serializable;

public class Entry implements Serializable {

    /**
     * The actual entry data.
     */
    public ClusKernel data;
    /**
     * The buffer of this entry. It can also be seen as the buffer of the child
     * node, it is here just to simplify the insertion recuersion.
     */
    private ClusKernel buffer;
    /**
     * A reference to the next node in the tree. <code>null</code> if we are
     * at a leaf, or this is an entry is part of a lying <code>Node</code>.
     */
    private Node child;
    /**
     *	A reference to the Entry's parent Entry
     */
    private Entry parentEntry;
    /**
     *	A reference to the Node, that contains this Entry
     */
    private Node node;
	/**
     * Last time this entry was changed.
     */
    private long timestamp;
    /**
     * The timestamp to be used when no operation has yet been done on this
     * entry.
     * @see #timestamp
     */
    private static final long defaultTimestamp = 0;

    /**
     * Constructor for the entry. To be used when we want to create an empty
     * entry. Notice that the timestamp will be set to zero, since there is no
     * reason to know when an empty entry was generated.
     * @param numberDimensions The dimensionality of the data point in tree
     * where this entry is used.
     */
    public Entry(int numberDimensions) {
        this.data = new ClusKernel(numberDimensions);
        this.buffer = new ClusKernel(numberDimensions);
        this.child = null;
        this.timestamp = Entry.defaultTimestamp;
    }

    /**
     * Constructor that creates an <code>Entry</code> that points to the given
     * node. The values of <code>data</code> will be calculated for this.
     * @param numberDimensions The dimensionality of the node.
     * @param node The node to which the new <code>Entry</code> should point.
     * @param currentTime The timestamp for the moment where this Entry was
     * was generated.
     * @see Node
     * @see #data
     */
    protected Entry(int numberDimensions,
            Node node, long currentTime, Entry parentEntry, Node containerNode) {
        this(numberDimensions);
        this.child = node;
        this.parentEntry = parentEntry;
        this.node = containerNode;
        Entry[] entries = node.getEntries();
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            entry.setParentEntry(this);
            if (entry.isEmpty()) {
                break;
            }

            this.add(entry);
        }

        this.timestamp = currentTime;
    }



    /**
     * Constructuctor that creates an <code>Entry</code> with an empty buffer
     * and the <code>data</code> given by the <code>Kernel</code>.
     * @param numberDimensions The dimensionality of the information in the
     * cluster.
     * @param cluster The cluster from which the information is to be extracted.
     * @param currentTime The timestamp for the moment where this Entry was
     * was generated.
     * @see ClusKernel
     * @see #data
     */
    public Entry(int numberDimensions, ClusKernel cluster, long currentTime) {
        this(numberDimensions);
        this.data.add(cluster);
        this.timestamp = currentTime;
    }
    /**
     * extended constructor with containerNode and parentEntry
     * @param numberDimensions
     * @param cluster
     * @param currentTime
     * @param parentEntry
     * @param containerNode
     */
    protected Entry(int numberDimensions, ClusKernel cluster, long currentTime, Entry parentEntry, Node containerNode) {
        this(numberDimensions);
        this.parentEntry = parentEntry;
        this.data.add(cluster);
        this.node = containerNode;
        this.timestamp = currentTime;
    }
    /**
     * Copy constructor. Everythin is copied, including the child.
     * @param other
     */
    protected Entry(Entry other) {
    	this.parentEntry = other.parentEntry;
    	this.node = other.node;
        this.buffer = new ClusKernel(other.buffer);
        this.data = new ClusKernel(other.data);
        this.timestamp = other.timestamp;
        this.child = other.child;
    	if (other.getChild()!=null)
	    	for (Entry e : other.getChild().getEntries()){
	    		e.setParentEntry(this);
	    	}
    }

    public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	/**
     * Clear the Entry. All points in the buffer and in the data cluster are
     * lost, the connection to the child is lost and the timestamp is set to
     * the default value.
     */
    protected void clear() {
        this.data.clear();
        this.buffer.clear();
        this.child = null;
        this.timestamp = Entry.defaultTimestamp;
    }

    /**
     * Clear the <code>data</code> and the <code>buffer Custer</code> in this
     * entry. This function does not clear the child of this <code>Entry</code>.
     * @see #data
     * @see #buffer
     * @see ClusKernel
     */
    protected void shallowClear() {
        this.buffer.clear();
        this.data.clear();
    }

    /**
     * Calculates the distance to the data in this entry.
     * @param cluster The Kernel cluster to which the distance is to be
     * calculated.
     * @return The distance to the data <code>Kernel</code> in this
     * <code>Entry</code>
     * @see ClusKernel
     * @see #data
     */
    protected double calcDistance(ClusKernel cluster) {
        return data.calcDistance(cluster);
    }

    /**
     * Calculates the distance to the data in this entry of the data in the
     * given entry.
     * @param other The <code>Entry</code> to which the distance is to be
     * calculated.
     * @return The distance to the data <code>Kernel</code> in this
     * <code>Entry</code> of the data <code>Kernel</code> in the other
     * <code>Entry</code>.
     * @see ClusKernel
     * @see #data
     */
    public double calcDistance(Entry other) {
        return this.getData().calcDistance(other.getData());
    }

    /**
     * When this entry is empty, give it it's first values. It makes sense to
     * have this operation separated from the aggregation, because the
     * aggregation first weights the values in <code>data</code> and
     * <code>Kernel</code>, which makes no sense in an empty entry.
     * @param other The entry with the information to be used to initialize
     * this entry.
     * @param currentTime The time at which this is happening.
     */
    protected void initializeEntry(Entry other, long currentTime) {
        assert (this.isEmpty());
        assert (other.getBuffer().isEmpty());
        this.data.add(other.data);
        this.timestamp = currentTime;
        this.child = other.child;
        if (child!=null){
        	for (Entry e : child.getEntries()){
        		e.setParentEntry(this);
        	}
        }
    }

    /**
     * Add the data cluster of another entry to the data cluster of this entry.
     * By using this function the timestamp does not get updated, nor does this
     * entry get older.
     * @param other The entry of which the data cluster should be added to
     * the local data cluster.
     * @see #data
     * @see ClusKernel#add(ClusKernel)
     */
    public void add(Entry other) {
        this.data.add(other.data);
    }

    /**
     * Aggregate the <code>data</code> in the <code>Kernel</code> of the other
     * <code>Entry</code>.
     * @param other The <code>Entry</code> to be aggregated.
     * @see #data
     * @see ClusKernel
     */
    protected void aggregateEntry(Entry other, long currentTime,
            double negLambda) {
        this.data.aggregate(other.data, currentTime - this.timestamp,
                negLambda);
        this.timestamp = currentTime;
    }

    /**
     * Aggregate the given <code>Kernel</code> to the <code>data</code> cluster
     * of this entry.
     * @param otherData The <code>Entry</code> to be aggregated.
     * @see #data
     * @see ClusKernel
     */
    protected void aggregateCluster(ClusKernel otherData, long currentTime,
            double negLambda) {
        this.getData().aggregate(otherData, currentTime - this.timestamp,
                negLambda);
        this.timestamp = currentTime;
    }

    /**
     * Aggregate the given <code>Kernel</code> to the <code>buffer</code>
     * cluster of this entry.
     * @param pointToInsert The cluster to aggregate to the buffer.
     * @param currentTime The time at which the aggregation occurs.
     * @param negLambda A parameter needed to weight the current state of the
     * buffer.
     */
    protected void aggregateToBuffer(ClusKernel pointToInsert, long currentTime,
            double negLambda) {
        ClusKernel currentBuffer = this.getBuffer();
        currentBuffer.aggregate(pointToInsert, currentTime - this.timestamp,
                negLambda);
        this.timestamp = currentTime;
    }

    /**
     * Merge this entry witht the given <code>Entry</code>. This adds the data 
     * cluster of the given Entry to the data cluster of this entry and sets the 
     * timestamp to the newest one of the the two entries.
     * @param other The entry from which the data cluster is added.
     * @see ClusKernel#add(ClusKernel)
     */
    protected void mergeWith(Entry other) {
        // We should only merge entries in leafs, and leafes should have empty
        // buffers.
        assert (this.child == null);
        assert (other.child == null);
        assert (other.buffer.isEmpty());

        this.data.add(other.data);
        if (this.timestamp < other.timestamp) {
            this.timestamp = other.timestamp;
        }
    }

    /**
     * Getter for the buffer. It is the real object, that means side effects are
     * possible!
     * @return A reference to the buffer in this entry.
     */
    protected ClusKernel getBuffer() {
        return buffer;
    }

    /**
     * Return the reference to the child of this <code>Entry</code> to navigate
     * in the tree.
     * @return A reference to the child of this <code>Entry</code>
     */
    public Node getChild() {
        return child;
    }

    /**
     * Getter for the data. It is the real object, that means side effects are
     * possible!
     * @return A reference to the data <code>Kernel</code> in this entry.
     * @see ClusKernel
     */
    protected ClusKernel getData() {
        return data;
    }
    public Entry getParentEntry() {
		return parentEntry;
	}

	public void setParentEntry(Entry parent) {
		this.parentEntry = parent;
	}

    /**
     * Setter for the child in this entry. Use to build the tree.
     * @param child The <code>Node</code> that should be a child of this
     * <code>Entry</code>
     * @see Node
     */
    public void setChild(Node child) {
        this.child = child;
    }

    /**
     * Return the current timestamp.
     * @return The current timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Clear the buffer in this entry and return a copy. No side effects are
     * possible (given that the copy constructor of <code>Kernel</code> makes
     * a deep copy).
     * @return A copy of the buffer.
     */
    protected ClusKernel emptyBuffer(long currentTime, double negLambda) {
        this.buffer.makeOlder(currentTime - this.timestamp, negLambda);
        ClusKernel bufferCopy = new ClusKernel(this.buffer);
        this.buffer.clear();
        return bufferCopy;
    }

    /**
     * Check if this <code>Entry</code> is empty or not. An <code>Entry</code>
     * is empty if the <code>data Kernel</code> is empty, since then the buffer
     * has to be empty.
     * @return <code>true</code> if the data cluster has no data points,
     * <code>false</code> otherwise.
     */
    protected boolean isEmpty() {
        // Assert that if the data cluster is empty, the buffer cluster is
        // empty too.
        assert ((this.data.isEmpty() && this.buffer.isEmpty())
                || !this.data.isEmpty());

        return this.data.isEmpty();
    }

    /**
     * Overwrites the LS, SS and weightedN in the data cluster of this
     * <code>Entry</code> to the values of the data cluster in the given
     * <code>Entry</code>, but adds N and classCount of the cluster in the given
     * Entry to the data cluster in this one. This function is useful when the
     * weight of an entry becomes to small, and we want to forget the
     * information of the old points.
     * @param newEntry The cluster that should overwrite the information.
     */
    protected void overwriteOldEntry(Entry newEntry) {
        assert (this.getBuffer().isEmpty());
        assert (newEntry.getBuffer().isEmpty());
        this.data.overwriteOldCluster(newEntry.data);
        newEntry.setParentEntry(this.parentEntry);
        if (newEntry.getChild()!=null)
        for (Entry e : newEntry.getChild().getEntries())
        	e.setParentEntry(this);
        //this.setParentEntry(newEntry.getParentEntry());
        this.child=newEntry.child;
    }

    /**
     * This functions reads every entry in the child node and calculates the
     * corresponding <code>data Kernel</code>. Timestamps are not changed.
     * @see #data
     * @see ClusKernel
     */
    protected void recalculateData() {
        Node currentChild = this.getChild();
        if (currentChild != null) {
            ClusKernel currentData = this.getData();
            currentData.clear();
            Entry[] entries = currentChild.getEntries();
            for (int i = 0; i < entries.length; i++) {
                currentData.add(entries[i].getData());
            }
        } else {
            this.clear();
        }
    }

    /**
     * Returns true if this entry is irrelevant with respecto the given
     * threshold. This is done by comparing the weighted N of this Entry to
     * the threshold, if it is smaller, than the entry is deemed to be
     * irrelevant.
     * @param threshold The threshold under which entries at leafs can be
     * erased.
     * @return True if this entry is deemed irrelevant, false otherwise.
     */
    protected boolean isIrrelevant(double threshold) {
        return this.getData().getWeight() < threshold;
    }

    /**
     * Ages this entrie's data AND buffer according to the given
     * time and aging constant.
     * @param currentTime   the current time
     * @param negLambda	    the aging constant
     */
    protected void makeOlder(long currentTime, double negLambda) {
//        assert (currentTime > this.timestamp) : "currentTime : "
//                + currentTime + ", this.timestamp: " + this.timestamp;

        long diff = currentTime - this.timestamp;
        this.buffer.makeOlder(diff, negLambda);
        this.data.makeOlder(diff, negLambda);
        this.timestamp = currentTime;
    }

}
