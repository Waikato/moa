/*
 *    Node.java
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

public class Node implements Serializable {

	final int NUMBER_ENTRIES = 3;
    static int INSERTIONS_BETWEEN_CLEANUPS = 10000;
    
    /**
     * The children of this node.
     */
    private Entry[] entries;
    // Information about the state in the tree.
    /**
     * The depth at which this <code>Node</code> is in the tree.
     */
    private int level;

    /**
     * Initialze a normal node, which is not fake.
     * @param numberDimensions The dimensionality of the data points it
     * manipulates.
     * @param level The INVERSE level at which this node hangs.
     */
    public Node(int numberDimensions, int level) {
        this.level = level;

        this.entries = new Entry[NUMBER_ENTRIES];
        // Generate all entries when we generate a Node.
        // That no entry can be null makes it much easier to handle.
        for (int i = 0; i < NUMBER_ENTRIES; i++) {
            entries[i] = new Entry(numberDimensions);
            entries[i].setNode(this);
        }
    }

    /**
     * Initialiazes a node which is a fake root depending on the given
     * <code>boolean</code>.
     * @param numberDimensions The dimensionality of the data points it
     * manipulates.
     * @param level The level at which this node hangs.
     * @param fakeRoot A parameter the says if the node is to be fake or not.
     */
    protected Node(int numberDimensions, int numberClasses, int level,
            boolean fakeRoot) {

        this.level = level;

        this.entries = new Entry[NUMBER_ENTRIES];
        // Generate all entries when we generate a Node.
        // That no entry can be null makes it much easier to handle.
        for (int i = 0; i < NUMBER_ENTRIES; i++) {
            entries[i] = new Entry(numberDimensions);
        }
    }
    
    /**
     * USED FOR EM_TOP_DOWN BULK LOADING
     * @param numberDimensions
     * @param level
     * @param argEntries
     */
    public Node(int numberDimensions, int level, Entry[] argEntries) {
        this.level = level;

        this.entries = new Entry[NUMBER_ENTRIES];
        // Generate all entries when we generate a Node.
        // That no entry can be null makes it much easier to handle.
        for (int i = 0; i < NUMBER_ENTRIES; i++) {
            entries[i] = argEntries[i];
        }
    }


    /**
     * Checks if this node is a leaf. A node is a leaf when none of the entries
     * in the node have children.
     * @return <code>true</code> if the node is leaf, <code>false</code>
     * otherwise.
     */
    protected boolean isLeaf() {

        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            if (entry.getChild() != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the neareast <code>Entry</code> to the given <code>Cluster</code>.
     * The distance is minimized over <code>Entry.calcDistance(Cluster)</code>.
     * @param buffer The cluster to which the distance has to be compared.
     * @return The <code>Entry</code> with minimal distance to the given
     * cluster.
     * @see ClusKernel
     * @see Entry#calcDistance(ClusKernel)
     */
    public Entry nearestEntry(ClusKernel buffer) {

        // TODO: (Fernando) Adapt the nearestEntry(...) function to the new algorithm.

        Entry res = entries[0];
        double min = res.calcDistance(buffer);
        for (int i = 1; i < entries.length; i++) {
            Entry entry = entries[i];

            if (entry.isEmpty()) {
                break;
            }

            double distance = entry.calcDistance(buffer);
            if (distance < min) {
                min = distance;
                res = entry;
            }
        }

        return res;
    }

    /**
     * Return the nearest entry to the given one. The
     * <code>calcDistance(Entry)</code> function is find the one with the
     * shortest distance in this node to the given one.
     * @param newEntry The entry to which the entry with the minimal distance
     * to it is calculated.
     * @return The entry with the minimal distance to the given one.
     */
    protected Entry nearestEntry(Entry newEntry) {
        assert (!this.entries[0].isEmpty());

        Entry res = entries[0];
        double min = res.calcDistance(newEntry);
        for (int i = 1; i < entries.length; i++) {

            if (this.entries[i].isEmpty()) {
                break;
            }

            Entry entry = entries[i];
            double distance = entry.calcDistance(newEntry);
            if (distance < min) {
                min = distance;
                res = entry;
            }
        }

        return res;
    }

    /**
     * Return the number of free <code>Entry</code>s in this node.
     * @return The number of free <code>Entry</code>s in this node.
     * @see Entry
     */
    protected int numFreeEntries() {
        int res = 0;
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            if (entry.isEmpty()) {
                res++;
            }
        }

        assert (NUMBER_ENTRIES == entries.length);
        return res;
    }

    /**
     * Add a new <code>Entry</code> to this node. If there is no space left a
     * <code>NoFreeEntryException</code> is thrown.
     * @param newEntry The <code>Entry</code> to be added.
     * @throws RuntimeException Is thrown when there is no space left in
     * the node for the new entry.
     */
    public void addEntry(Entry newEntry, long currentTime){
    	newEntry.setNode(this);
        int freePosition = getNextEmptyPosition();
        entries[freePosition].initializeEntry(newEntry, currentTime);
    }

    /**
     * Returns the position of the next free Entry.
     * @return The position of the next free Entry.
     * @throws RuntimeException Is thrown when there is no free entry left in
     * the node.
     */
    private int getNextEmptyPosition(){
        int counter;
        for (counter = 0; counter < entries.length; counter++) {
            Entry e = entries[counter];
            if (e.isEmpty()) {
                break;
            }
        }

        if (counter == entries.length) {
            throw new RuntimeException("Entry added to a node which is already full.");
        }

        return counter;
    }

    /**
     * If there exists an entry, whose relevance is under the threshold given
     * as a parameter to the tree, this entry is returned. Otherwise
     * <code>null</code> is returned.
     * @return An irrelevant <code>Entry</code> if there exists one,
     * <code>null</code> otherwise.
     * @see Entry
     * @see Entry#isIrrelevant(double) 
     */
    protected Entry getIrrelevantEntry(double threshold) {
        for (int i = 0; i < this.entries.length; i++) {
            Entry entry = this.entries[i];
            if (entry.isIrrelevant(threshold)) {
                return entry;
            }
        }

        return null;
    }

    /**
     * Return an array with references to the children of this node. These are
     * not copies, that means side effects are possible!
     * @return An array with references to the children of this node.
     * @see Entry
     */
    public Entry[] getEntries() {
        return entries;
    }

    /**
     * Return the level number in the node. This is not the real level. For the
     * real level one has to call <code>getLevel(Tree tree)</code>.
     * @return The raw level of the node.
     * @see #getLevel(ClusTree)
     */
    protected int getRawLevel() {
        return level;
    }

    /**
     * Returns the level at which this node is in the tree. If a tree is passed
     * to which the node does not belonged a value is returned, but it is
     * gibberish.
     * @param tree The tree to which this node belongs.
     * @return The level at which this node hangs.
     */
    protected int getLevel(ClusTree tree) {
        int numRootSplits = tree.getNumRootSplits();
        return numRootSplits - this.getRawLevel();
    }

    /**
     * Clear this Node, which means that the noiseBuffer is cleared, that
     * <code>shallowClear</code> is called upon all the entries of the node,
     * that the split counter is set to zero and the node is set to not be a
     * fake root. Notice that the level stays the same after calling this
     * function.
     * @see ClusKernel#clear()
     * @see Entry#shallowClear()
     */
    // Level stays the same.
    protected void clear() {
        for (int i = 0; i < NUMBER_ENTRIES; i++) {
            entries[i].shallowClear();
        }
    }

    /**
     * Merge the two entries at the given position. The entries are reordered in
     * the <code>entries</code> array so that the non-empty entries are still
     * at the beginning.
     * @param pos1 The position of the first entry to be merged. This position
     * has to be smaller than the the second position.
     * @param pos2 The position of the second entry to be merged. This position
     * has to be greater than the the first position.
     */
    protected void mergeEntries(int pos1, int pos2) {
        assert (this.numFreeEntries() == 0);
        assert (pos1 < pos2);

        this.entries[pos1].mergeWith(this.entries[pos2]);

        for (int i = pos2; i < entries.length - 1; i++) {
            entries[i] = entries[i + 1];
        }
        entries[entries.length - 1].clear();
    }

    protected void makeOlder(long currentTime, double negLambda) {
        for (int i = 0; i < this.entries.length; i++) {
            Entry entry = this.entries[i];

            if (entry.isEmpty()) {
                break;
            }

            entry.makeOlder(currentTime, negLambda);
        }
    }
}
