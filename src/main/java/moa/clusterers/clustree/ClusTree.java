package moa.clusterers.clustree;

//import cluster.Cluster;
//import cluster.Clustering;
//import cluster.StreamClusterer;
import java.util.LinkedList;
import moa.cluster.CFCluster;
import moa.clusterers.clustree.util.*;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.core.Measurement;
import moa.options.IntOption;
import weka.core.Instance;

/**
 * A representation of a tree.
 * @author sanchez
 */
public class ClusTree extends AbstractClusterer{
    public IntOption timeWindowOption = new IntOption("timeWindow",
			't', "Rang of the window.", 1000);

    public IntOption maxHeightOption = new IntOption(
			"maxHeight", 'h',
			"The maximal height of the tree", 8);

    private static int INSERTIONS_BETWEEN_CLEANUPS = 10000;
    /**
     * The root node of the tree.
     */
    private Node root;
    // Information about the data represented in this tree.
    /**
     * Dimensionality of the data points managed by this tree.
     */
    private int numberDimensions;
    /**
     * Parameter for the weighting function use to weight the entries.
     */
    private double negLambda;
    /**
     * The current height of the tree. Should always be smaller than maxHeight.
     */
    private int height;
    /**
     * The maximal height of the tree.
     */
    private int maxHeight;
    /**
     * This variable is used to keep the inverse height that is stored in every
     * node correct.
     */
    private int numRootSplits;
    /**
     * The thresholf for the weighting of an Entry. An Entry is irrelevant, if
     * it is in a leaf and the weightedN of the data Cluster is smaller than
     * this threshold.
     * @see Entry#data
     */
    private double weightThreshold = 0.01;
    /**
     * Number of points inserted into the tree.
     */

    private int numberInsertions;
    private long timestamp;


    @Override
    public void resetLearningImpl() {
        negLambda = (1.0 / (double) timeWindowOption.getValue())
                * (Math.log(weightThreshold) / Math.log(2));
        maxHeight = maxHeightOption.getValue();
        numberDimensions = -1;
        root = null;
        timestamp = 0;
        height = 0;
        numRootSplits = 0;
        numberInsertions = 0;

    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    public boolean isRandomizable() {
        return false;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    public double[] getVotesForInstance(Instance inst) {
        return null;
    }

    @Override
    public boolean implementsMicroClusterer() {
        return true;
    }



    @Override
    public void trainOnInstanceImpl(Instance instance) {
        timestamp++;
        
        //TODO check if instance contains label
        if(root == null){
            numberDimensions = instance.numAttributes();
            root = new Node(numberDimensions, 0);
        }
        else{
            if(numberDimensions!=instance.numAttributes())
                System.out.println("Wrong dimensionality, expected:"+numberDimensions+ "found:"+instance.numAttributes());
        }

        ClusKernel newPointAsKernel = new ClusKernel(instance.toDoubleArray(), numberDimensions);
        insert(newPointAsKernel, new SimpleBudget(1000),timestamp);
    }


    /**
     * Insert a new point in the <code>Tree</code>. The point should be 
     * represented as a cluster with a single data point(i.e. N = 1). A
     * <code>Budget</code> class is also given, which is informed of the number
     * of operation the tree does, and informs the tree when it does not have
     * time left and should stop the insertion.
     * @param newPoint The point to be inserted.
     * @param budget The budget and statistics recollector for the insertion.
     * @param timestamp The moment at which this point is inserted.
     * @see Kernel
     * @see Budget
     */
    public void insert(ClusKernel newPoint, Budget budget, long timestamp) {

        Entry rootEntry = new Entry(this.numberDimensions,
                root, timestamp);
        ClusKernel carriedBuffer = new ClusKernel(this.numberDimensions);
        Entry toInsertHere = insert(newPoint, carriedBuffer, root, rootEntry,
                budget, timestamp);

        if (toInsertHere != null) {
            this.numRootSplits++;
            this.height += this.height < this.maxHeight ? 1 : 0;

            Node newRoot = new Node(this.numberDimensions,
                    toInsertHere.getChild().getRawLevel() + 1);
            newRoot.addEntry(rootEntry, timestamp);
            newRoot.addEntry(toInsertHere, timestamp);
            this.root = newRoot;
        }

        this.numberInsertions++;
        if (this.numberInsertions % INSERTIONS_BETWEEN_CLEANUPS == 0) {
            cleanUp(this.root, 0);
        }

    }

    // TODO: Expand all function that work on entries to work with the Budget.
    private Entry insert(ClusKernel pointToInsert, ClusKernel carriedBuffer,
            Node currentNode, Entry parentEntry, Budget budget, long timestamp) {
        assert (currentNode != null);
        assert (currentNode.isLeaf()
                || currentNode.getEntries()[0].getChild() != null);

        currentNode.makeOlder(timestamp, this.negLambda);

        // This variable will be changed from to null to an actual reference
        // in the following if-else block if we have to insert something here,
        // either because this is a leaf, or because of split propagation.
        Entry toInsertHere = null;

        if (currentNode.isLeaf()) {
            // At the end of the function the entry will be inserted.
            toInsertHere = new Entry(this.numberDimensions,
                    pointToInsert, timestamp);
        } else {

            Entry bestEntry = currentNode.nearestEntry(pointToInsert);
            bestEntry.aggregateCluster(pointToInsert, timestamp,
                    this.negLambda);

            boolean isCarriedBufferEmpty = carriedBuffer.isEmpty();

            Entry bestBufferEntry = null;
            if (!isCarriedBufferEmpty) {
                bestBufferEntry = currentNode.nearestEntry(carriedBuffer);
                bestBufferEntry.aggregateCluster(carriedBuffer, timestamp,
                        this.negLambda);
            }

            if (!budget.hasMoreTime()) {
                bestEntry.aggregateToBuffer(pointToInsert, timestamp,
                        this.negLambda);
                if (!isCarriedBufferEmpty) {
                    bestBufferEntry.aggregateToBuffer(carriedBuffer,
                            timestamp, this.negLambda);
                }
                return null;
            }

            // If the way of the buffer differs from the way of the point to
            // be inserted, leave the buffer here.
            if (!isCarriedBufferEmpty && (bestEntry != bestBufferEntry)) {
                bestBufferEntry.aggregateToBuffer(carriedBuffer, timestamp,
                        this.negLambda);
                carriedBuffer.clear();
            }
            // Take the buffer of the best entry for the point to be inserted
            // along.
            ClusKernel takeAlongBuffer = bestEntry.emptyBuffer(timestamp,
                    this.negLambda);
            carriedBuffer.add(takeAlongBuffer);

            // Recursive call.
            toInsertHere = insert(pointToInsert, carriedBuffer,
                    bestEntry.getChild(), bestEntry, budget, timestamp);
        }

        // If the above block has a new Entry for this place insert it.
        if (toInsertHere != null) {
            return this.insertHere(toInsertHere, currentNode, parentEntry,
                    carriedBuffer, budget, timestamp);
        }

        // If nothing else needs to be done in all the above levels
        // return null to signalize it.
        return null;
    }

    // XXX: Document the insertion when the final implementation is done.
    private Entry insertHere(Entry newEntry, Node currentNode,
            Entry parentEntry, ClusKernel carriedBuffer, Budget budget,
            long timestamp) {

        int numFreeEntries = currentNode.numFreeEntries();

        // Insert the buffer that we carry.
        if (!carriedBuffer.isEmpty()) {
            Entry bufferEntry = new Entry(this.numberDimensions,
                    carriedBuffer, timestamp);

            if (numFreeEntries <= 1) {
                // Distance from buffer to entries.
                Entry nearestEntryToCarriedBuffer =
                        currentNode.nearestEntry(newEntry);
                double distanceNearestEntryToBuffer =
                        nearestEntryToCarriedBuffer.calcDistance(newEntry);

                // Distance between buffer and point to insert.
                double distanceBufferNewEntry =
                        newEntry.calcDistance(carriedBuffer);

                // Best distance between Entrys in the Node.
                BestMergeInNode bestMergeInNode =
                        calculateBestMergeInNode(currentNode);

                // See what the minimal distance is and do the correspoding
                // action.
                if (distanceNearestEntryToBuffer <= distanceBufferNewEntry
                        && distanceNearestEntryToBuffer <= bestMergeInNode.distance) {
                    // Aggregate buffer entry to nearest entry in node.
                    nearestEntryToCarriedBuffer.aggregateEntry(bufferEntry,
                            timestamp, this.negLambda);
                } else if (distanceBufferNewEntry <= distanceNearestEntryToBuffer
                        && distanceBufferNewEntry <= bestMergeInNode.distance) {
                    newEntry.mergeWith(bufferEntry);
                } else {
                    currentNode.mergeEntries(bestMergeInNode.entryPos1,
                            bestMergeInNode.entryPos2);
                    currentNode.addEntry(bufferEntry, timestamp);
                }

            } else {
                assert (currentNode.isLeaf());
                currentNode.addEntry(bufferEntry, timestamp);
            }
        }

        // Normally the insertion of the carries buffer does not change the
        // number of free entries, but in case of future changes we calculate
        // the number again.
        numFreeEntries = currentNode.numFreeEntries();

        // Search for an Entry with a weight under the threshold.
        Entry irrelevantEntry = currentNode.getIrrelevantEntry(this.weightThreshold);
        if (currentNode.isLeaf() && irrelevantEntry != null) {
            irrelevantEntry.overwriteOldEntry(newEntry);
        } else if (numFreeEntries >= 1) {
            currentNode.addEntry(newEntry, timestamp);
        } else {
            if (currentNode.isLeaf() && (this.hasMaximalSize()
                    || !budget.hasMoreTime())) {
                mergeEntryWithoutSplit(currentNode, newEntry,
                        timestamp);
            } else {
                // We have to split.
                return split(newEntry, currentNode, parentEntry, timestamp);
            }
        }

        return null;
    }

    /**
     * Inserts an <code>Entry</code> into a <code>Node</code> without inducing
     * a split.
     * @param node The node at which the entry is to be inserted.
     * @param newEntry The entry to be inserted.
     * @param timestamp The moment at which this occurs.
     */
    private void mergeEntryWithoutSplit(Node node,
            Entry newEntry, long timestamp) {

        Entry nearestEntryToCarriedBuffer =
                node.nearestEntry(newEntry);
        double distanceNearestEntryToBuffer =
                nearestEntryToCarriedBuffer.calcDistance(newEntry);

        BestMergeInNode bestMergeInNode =
                calculateBestMergeInNode(node);

        if (distanceNearestEntryToBuffer < bestMergeInNode.distance) {
            nearestEntryToCarriedBuffer.aggregateEntry(newEntry, timestamp,
                    this.negLambda);
        } else {
            node.mergeEntries(bestMergeInNode.entryPos1,
                    bestMergeInNode.entryPos2);
            node.addEntry(newEntry, timestamp);
        }
    }

    /**
     * Calculates the best merge possible between two nodes in a node. This
     * means that the pair with the smallest distance is found.
     * @param node The node in which these two entries have to be found.
     * @return An object which encodes the two position of the entries with the
     * smallest distance in the node and the distance between them.
     * @see BestMergeInNode
     * @see Entry#calcDistance(tree.Entry) 
     */
    private BestMergeInNode calculateBestMergeInNode(Node node) {
        assert (node.numFreeEntries() == 0);

        Entry[] entries = node.getEntries();

        int toMerge1 = -1;
        int toMerge2 = -1;
        double distanceBetweenMergeEntries = Double.NaN;

        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < entries.length; i++) {
            Entry e1 = entries[i];
            for (int j = i + 1; j < entries.length; j++) {
                Entry e2 = entries[j];
                double distance = e1.calcDistance(e2);
                if (distance < minDistance) {
                    toMerge1 = i;
                    toMerge2 = j;
                    distanceBetweenMergeEntries = distance;
                }
            }
        }

        assert (toMerge1 != -1 && toMerge2 != -1);
        if (Double.isNaN(distanceBetweenMergeEntries)) {
            throw new RuntimeException("The minimal distance between two "
                    + "Entrys in a Node was Double.MAX_VAUE. That can hardly "
                    + "be right.");
        }

        return new BestMergeInNode(toMerge1, toMerge2,
                distanceBetweenMergeEntries);
    }

    private boolean hasMaximalSize() {
        // TODO: Improve hasMaximalSize(). For now it just works somehow for testing.
        return this.height == this.maxHeight;
    }

    /**
     * Performs a (2,2) split on the given node with the given entry. This
     * implementation only works if the nodes have three entries each. The split
     * will generate two new nodes. One of them will be put where the old node
     * was, and for the other a new <code>Entry</code> will be generated and
     * returned.
     * @param newEntry The entry to be added to the node.
     * @param node The node that is going to be splitted.
     * @param parentEntry The entry in the tree that points at the node that
     * is going to be splitted.
     * @param timestamp The moment at which this split occurs.
     * @return An entry which points at the second node created in the split.
     * This entry has to be introduced later in the tree.
     */
    private Entry split(Entry newEntry, Node node, Entry parentEntry,
            long timestamp) {
        // The implemented split function only works in trees where node
        // have three entries.
        // Splitting only makes sense on full nodes.
        assert (node.numFreeEntries() == 0);
        assert (parentEntry.getChild() == node);

        // All the entries we have to separate in two nodes.
        Entry[] allEntries = new Entry[4];
        Entry[] nodeEntries = node.getEntries();
        for (int i = 0; i < nodeEntries.length; i++) {
            allEntries[i] = new Entry(nodeEntries[i]);
        }
        allEntries[3] = newEntry;

        // Clear the given node, since we are going to refill it later.
        node = new Node(this.numberDimensions, node.getRawLevel());

        // Calculate the distance of all the possible pairings, since we want
        // to do a (2,2) split.
        double select01 = allEntries[0].calcDistance(allEntries[1])
                + allEntries[2].calcDistance(allEntries[3]);

        double select02 = allEntries[0].calcDistance(allEntries[2])
                + allEntries[1].calcDistance(allEntries[3]);

        double select03 = allEntries[0].calcDistance(allEntries[3])
                + allEntries[1].calcDistance(allEntries[2]);

        // See which of the pairings is minimal and distribute the entries
        // accordingly.
        Node residualNode = new Node(this.numberDimensions,
                node.getRawLevel());
        if (select01 < select02) {
            if (select01 < select03) {//select01 smallest
                node.addEntry(allEntries[0], timestamp);
                node.addEntry(allEntries[1], timestamp);
                residualNode.addEntry(allEntries[2], timestamp);
                residualNode.addEntry(allEntries[3], timestamp);
            } else {//select03 smallest
                node.addEntry(allEntries[0], timestamp);
                node.addEntry(allEntries[3], timestamp);
                residualNode.addEntry(allEntries[1], timestamp);
                residualNode.addEntry(allEntries[2], timestamp);
            }
        } else {
            if (select02 < select03) {//select02 smallest
                node.addEntry(allEntries[0], timestamp);
                node.addEntry(allEntries[2], timestamp);
                residualNode.addEntry(allEntries[1], timestamp);
                residualNode.addEntry(allEntries[3], timestamp);
            } else {//select03 smallest
                node.addEntry(allEntries[0], timestamp);
                node.addEntry(allEntries[3], timestamp);
                residualNode.addEntry(allEntries[1], timestamp);
                residualNode.addEntry(allEntries[2], timestamp);
            }
        }

        // Set the other node into the tree.
        parentEntry.setChild(node);
        parentEntry.recalculateData();

        // Generate a new entry for the residual node.
        Entry residualEntry = new Entry(this.numberDimensions,
                residualNode, timestamp);

        return residualEntry;
    }

    /**
     * Return the number of time the tree has grown in size. If the tree grows
     * and is then cutted from a certain depth, it also counts.
     * @return The number of times the root node was splitted.
     */
    public int getNumRootSplits() {
        return numRootSplits;
    }

    /**
     * Return the current height of the tree. This should never be greater than
     * <code>maxHeight</code>.
     * @return The height of the tree.
     * @see #maxHeight
     */
    public int getHeight() {
        assert (height <= maxHeight);
        return height;
    }

    private void cleanUp(Node currentNode, int level) {
        if (currentNode == null) {
            return;
        }

        Entry[] entries = currentNode.getEntries();
        if (level == this.maxHeight) {
            for (int i = 0; i < entries.length; i++) {
                Entry e = entries[i];
                e.setChild(null);
            }
        } else {
            for (int i = 0; i < entries.length; i++) {
                Entry e = entries[i];
                cleanUp(e.getChild(), level + 1);
            }
        }
    }

    /**
     * @param currentTime The current time
     * @return The kernels at the leaf level as a clustering
     */

    @Override
    public Clustering getMicroClusteringResult() {
        return getClustering(timestamp, -1);
    }

    @Override
    public Clustering getClusteringResult() {
        return null;
    }


    /**
     * @param currentTime The current time
     * @return The kernels at the given level as a clustering.
     */
    public Clustering getClustering(long currentTime, int targetLevel) {
        if (root == null) {
            return null;
        }

        Clustering clusters = new Clustering();
        LinkedList<Node> queue = new LinkedList<Node>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node current = queue.remove();

            int currentLevel = current.getLevel(this);
            boolean isLeaf = (current.isLeaf() && currentLevel <= maxHeight)
                    || currentLevel == maxHeight;

            if (currentLevel == targetLevel
                    || (targetLevel == - 1 && isLeaf)) {
                assert (currentLevel <= maxHeight);

                Entry[] entries = current.getEntries();
                for (int i = 0; i < entries.length; i++) {
                    Entry entry = entries[i];
                    if (entry == null || entry.isEmpty()) {
                        continue;
                    }

                    long diff = currentTime - entry.getTimestamp();
                    ClusKernel gaussKernel = new ClusKernel(entry.getData());

                    if (diff > 0) {
                        gaussKernel.makeOlder(diff, negLambda);
                    }

                    clusters.add(gaussKernel);
                }
            } else if (!current.isLeaf()) {
                Entry[] entries = current.getEntries();
                for (int i = 0; i < entries.length; i++) {
                    Entry entry = entries[i];

                    if (entry.isEmpty()) {
                        continue;
                    }

                    if (entry.isIrrelevant(weightThreshold)) {
                        continue;
                    }

                    queue.add(entry.getChild());
                }
            }
        }

        return clusters;
    }



    /**************************************************************************
     * LOCAL CLASSES
     **************************************************************************/
    /**
     * A class to code the return value of searching the smallest merge in a
     * node.
     */
    class BestMergeInNode {

        /**
         * The position of the first entry in the array of the node.
         */
        public int entryPos1;
        /**
         * The position of the second entry in the array of the node.
         */
        public int entryPos2;
        /**
         * The distance between the two entries.
         */
        public double distance;

        /**
         * The constructor of this return value. It will automatically make
         * sure that the first position is the smaller one of the two.
         * @param pos1 One of the position.
         * @param pos2 One of the position.
         * @param distance The distance between the entries at these positions.
         */
        public BestMergeInNode(int pos1, int pos2,
                double distance) {
            assert (pos1 != pos2);

            this.distance = distance;

            if (pos1 < pos2) {
                this.entryPos1 = pos1;
                this.entryPos2 = pos2;
            } else {
                this.entryPos1 = pos2;
                this.entryPos2 = pos1;
            }
        }
    }

}
