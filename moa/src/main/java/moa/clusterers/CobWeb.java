/*
 *    CobWeb.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *    @author Mark Hall (mhall@cs.waikato.ac.nz)
 * 
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
package moa.clusterers;

import java.io.Serializable;


import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.FastVector;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import weka.core.AttributeStats;
import weka.experiment.Stats;
import weka.filters.unsupervised.attribute.Add;

/**
 * Class implementing the Cobweb and Classit clustering algorithms.
 * See: http://en.wikipedia.org/wiki/Cobweb_%28clustering%29
 * 
 * Citation: D. Fisher (1987). 
 * Knowledge acquisition via incremental conceptual clustering. 
 * Machine Learning. 2(2):139-172.
 **/
public class CobWeb extends AbstractClusterer {

    private static final long serialVersionUID = 1L;
    public FloatOption acuityOption = new FloatOption("acuity",
            'a', "Acuity (minimum standard deviation)", 1.0, 0.0, 90.0);
    public FloatOption cutoffOption = new FloatOption("cutoff",
            'c', "Cutoff (minimum category utility)", 0.002, 0.0, 90.0); //0.01 * Cobweb.m_normal
    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random noise.", 1);	//42

    /**
     * Inner class handling node operations for Cobweb.
     *
     * @see Serializable
     */
    private class CNode implements Serializable {

        /** for serialization */
        static final long serialVersionUID = 3452097436933325631L;
        /**
         * Within cluster attribute statistics
         */
        private AttributeStats[] m_attStats;
        /**
         * Number of attributes
         */
        private int m_numAttributes;
        /**
         * Instances at this node
         */
        protected Instances m_clusterInstances = null;
        /**
         * Children of this node
         */
        private FastVector m_children = null;
        /**
         * Total instances at this node
         */
        private double m_totalInstances = 0.0;
        /**
         * Cluster number of this node
         */
        private int m_clusterNum = -1;

        /**
         * Creates an empty <code>CNode</code> instance.
         *
         * @param numAttributes the number of attributes in the data
         */
        public CNode(int numAttributes) {
            m_numAttributes = numAttributes;
        }

        /**
         * Creates a new leaf <code>CNode</code> instance.
         *
         * @param numAttributes the number of attributes in the data
         * @param leafInstance the instance to store at this leaf
         */
        public CNode(int numAttributes, Instance leafInstance) {
            this(numAttributes);
            if (m_clusterInstances == null) {
		//System.out.println(leafInstance.numAttributes()+"-"+leafInstance.value(0)+"-"+leafInstance.value(1)+"-"+leafInstance.value(2));
		//System.out.println(leafInstance.numAttributes()+"-"+leafInstance.attribute(0).type()+"-"+leafInstance.attribute(1).type()+"-"+leafInstance.attribute(2).type());
                m_clusterInstances = new Instances(leafInstance.dataset(), 1);
            }
            m_clusterInstances.add(leafInstance);
            updateStats(leafInstance, false);
        }

        /**
         * Adds an instance to this cluster.
         *
         * @param newInstance the instance to add
         */
        protected void addInstance(Instance newInstance) {
            // Add the instance to this cluster

            if (m_clusterInstances == null) {
                m_clusterInstances = new Instances(newInstance.dataset(), 1);
                m_clusterInstances.add(newInstance);
                updateStats(newInstance, false);
                return;
            } else if (m_children == null) {
                /* we are a leaf, so make our existing instance(s) into a child
                and then add the new instance as a child */
                m_children = new FastVector();
                CNode tempSubCluster = new CNode(m_numAttributes,
                        m_clusterInstances.instance(0));

                //	System.out.println("Dumping "+m_clusterInstances.numInstances());
                for (int i = 1; i < m_clusterInstances.numInstances(); i++) {
                    tempSubCluster.m_clusterInstances.add(m_clusterInstances.instance(i));
                    tempSubCluster.updateStats(m_clusterInstances.instance(i), false);
                }
                m_children = new FastVector();
                m_children.addElement(tempSubCluster);
                m_children.addElement(new CNode(m_numAttributes, newInstance));

                m_clusterInstances.add(newInstance);
                updateStats(newInstance, false);

                // here is where we check against cutoff (also check cutoff
                // in findHost)
                if (categoryUtility() < m_cutoff) {
                    //	  System.out.println("Cutting (leaf add) ");
                    m_children = null;
                }
                return;
            }

            // otherwise, find the best host for this instance
            CNode bestHost = findHost(newInstance, false);
            if (bestHost != null) {
                // now add to the best host
                bestHost.addInstance(newInstance);
            }
        }

        /**
         * Temporarily adds a new instance to each of this nodes children
         * in turn and computes the category utility.
         *
         * @param newInstance the new instance to evaluate
         * @return an array of category utility values---the result of considering
         * each child in turn as a host for the new instance
         * @throws Exception if an error occurs
         */
        private double[] cuScoresForChildren(Instance newInstance) {
            //throws Exception {
            // look for a host in existing children
            double[] categoryUtils = new double[m_children.size()];

            // look for a home for this instance in the existing children
            for (int i = 0; i < m_children.size(); i++) {
                CNode temp = (CNode) m_children.elementAt(i);
                // tentitively add the new instance to this child
                temp.updateStats(newInstance, false);
                categoryUtils[i] = categoryUtility();

                // remove the new instance from this child
                temp.updateStats(newInstance, true);
            }
            return categoryUtils;
        }

        private double cuScoreForBestTwoMerged(CNode merged,
                CNode a, CNode b,
                Instance newInstance) {//throws Exception {

            double mergedCU = -Double.MAX_VALUE;
            // consider merging the best and second
            // best.
            merged.m_clusterInstances = new Instances(m_clusterInstances, 1);

            merged.addChildNode(a);
            merged.addChildNode(b);
            merged.updateStats(newInstance, false); // add new instance to stats
            // remove the best and second best nodes
            m_children.removeElementAt(m_children.indexOf(a));
            m_children.removeElementAt(m_children.indexOf(b));
            m_children.addElement(merged);
            mergedCU = categoryUtility();
            // restore the status quo
            merged.updateStats(newInstance, true);
            m_children.removeElementAt(m_children.indexOf(merged));
            m_children.addElement(a);
            m_children.addElement(b);
            return mergedCU;
        }

        /**
         * Finds a host for the new instance in this nodes children. Also
         * considers merging the two best hosts and splitting the best host.
         *
         * @param newInstance the instance to find a host for
         * @param structureFrozen true if the instance is not to be added to
         * the tree and instead the best potential host is to be returned
         * @return the best host
         * @throws Exception if an error occurs
         */
        private CNode findHost(Instance newInstance,
                boolean structureFrozen) {//throws Exception {

            if (!structureFrozen) {
                updateStats(newInstance, false);
            }

            // look for a host in existing children and also consider as a new leaf
            double[] categoryUtils = cuScoresForChildren(newInstance);

            // make a temporary new leaf for this instance and get CU
            CNode newLeaf = new CNode(m_numAttributes, newInstance);
            m_children.addElement(newLeaf);
            double bestHostCU = categoryUtility();
            CNode finalBestHost = newLeaf;

            // remove new leaf when seaching for best and second best nodes to
            // consider for merging and splitting
            m_children.removeElementAt(m_children.size() - 1);

            // now determine the best host (and the second best)
            int best = 0;
            int secondBest = 0;
            for (int i = 0; i < categoryUtils.length; i++) {
                if (categoryUtils[i] > categoryUtils[secondBest]) {
                    if (categoryUtils[i] > categoryUtils[best]) {
                        secondBest = best;
                        best = i;
                    } else {
                        secondBest = i;
                    }
                }
            }

            CNode a = (CNode) m_children.elementAt(best);
            CNode b = (CNode) m_children.elementAt(secondBest);
            if (categoryUtils[best] > bestHostCU) {
                bestHostCU = categoryUtils[best];
                finalBestHost = a;
                //	System.out.println("Node is best");
            }

            if (structureFrozen) {
                if (finalBestHost == newLeaf) {
                    return null; // *this* node is the best host
                } else {
                    return finalBestHost;
                }
            }

            double mergedCU = -Double.MAX_VALUE;
            CNode merged = new CNode(m_numAttributes);
            if (a != b) {
                mergedCU = cuScoreForBestTwoMerged(merged, a, b, newInstance);

                if (mergedCU > bestHostCU) {
                    bestHostCU = mergedCU;
                    finalBestHost = merged;
                }
            }

            // Consider splitting the best
            double splitCU = -Double.MAX_VALUE;
            double splitBestChildCU = -Double.MAX_VALUE;
            double splitPlusNewLeafCU = -Double.MAX_VALUE;
            double splitPlusMergeBestTwoCU = -Double.MAX_VALUE;
            if (a.m_children != null) {
                FastVector tempChildren = new FastVector();

                for (int i = 0; i < m_children.size(); i++) {
                    CNode existingChild = (CNode) m_children.elementAt(i);
                    if (existingChild != a) {
                        tempChildren.addElement(existingChild);
                    }
                }
                for (int i = 0; i < a.m_children.size(); i++) {
                    CNode promotedChild = (CNode) a.m_children.elementAt(i);
                    tempChildren.addElement(promotedChild);
                }
                // also add the new leaf
                tempChildren.addElement(newLeaf);

                FastVector saveStatusQuo = m_children;
                m_children = tempChildren;
                splitPlusNewLeafCU = categoryUtility(); // split + new leaf
                // remove the new leaf
                tempChildren.removeElementAt(tempChildren.size() - 1);
                // now look for best and second best
                categoryUtils = cuScoresForChildren(newInstance);

                // now determine the best host (and the second best)
                best = 0;
                secondBest = 0;
                for (int i = 0; i < categoryUtils.length; i++) {
                    if (categoryUtils[i] > categoryUtils[secondBest]) {
                        if (categoryUtils[i] > categoryUtils[best]) {
                            secondBest = best;
                            best = i;
                        } else {
                            secondBest = i;
                        }
                    }
                }
                CNode sa = (CNode) m_children.elementAt(best);
                CNode sb = (CNode) m_children.elementAt(secondBest);
                splitBestChildCU = categoryUtils[best];

                // now merge best and second best
                CNode mergedSplitChildren = new CNode(m_numAttributes);
                if (sa != sb) {
                    splitPlusMergeBestTwoCU =
                            cuScoreForBestTwoMerged(mergedSplitChildren, sa, sb, newInstance);
                }
                splitCU = (splitBestChildCU > splitPlusNewLeafCU)
                        ? splitBestChildCU : splitPlusNewLeafCU;
                splitCU = (splitCU > splitPlusMergeBestTwoCU)
                        ? splitCU : splitPlusMergeBestTwoCU;

                if (splitCU > bestHostCU) {
                    bestHostCU = splitCU;
                    finalBestHost = this;
                    //	  tempChildren.removeElementAt(tempChildren.size()-1);
                } else {
                    // restore the status quo
                    m_children = saveStatusQuo;
                }
            }

            if (finalBestHost != this) {
                // can commit the instance to the set of instances at this node
                m_clusterInstances.add(newInstance);
            } else {
                m_numberSplits++;
            }

            if (finalBestHost == merged) {
                m_numberMerges++;
                m_children.removeElementAt(m_children.indexOf(a));
                m_children.removeElementAt(m_children.indexOf(b));
                m_children.addElement(merged);
            }

            if (finalBestHost == newLeaf) {
                finalBestHost = new CNode(m_numAttributes);
                m_children.addElement(finalBestHost);
            }

            if (bestHostCU < m_cutoff) {
                if (finalBestHost == this) {
                    // splitting was the best, but since we are cutting all children
                    // recursion is aborted and we still need to add the instance
                    // to the set of instances at this node
                    m_clusterInstances.add(newInstance);
                }
                m_children = null;
                finalBestHost = null;
            }

            if (finalBestHost == this) {
                // splitting is still the best, so downdate the stats as
                // we'll be recursively calling on this node
                updateStats(newInstance, true);
            }

            return finalBestHost;
        }

        /**
         * Adds the supplied node as a child of this node. All of the child's
         * instances are added to this nodes instances
         *
         * @param child the child to add
         */
        protected void addChildNode(CNode child) {
            for (int i = 0; i < child.m_clusterInstances.numInstances(); i++) {
                Instance temp = child.m_clusterInstances.instance(i);
                m_clusterInstances.add(temp);
                updateStats(temp, false);
            }

            if (m_children == null) {
                m_children = new FastVector();
            }
            m_children.addElement(child);
        }

        /**
         * Computes the utility of all children with respect to this node
         *
         * @return the category utility of the children with respect to this node.
         * @throws Exception if there are no children
         */
        protected double categoryUtility() {// {throws Exception {

            // if (m_children == null) {
            //throw new Exception("categoryUtility: No children!");
            // }

            double totalCU = 0;

            for (int i = 0; i < m_children.size(); i++) {
                CNode child = (CNode) m_children.elementAt(i);
                totalCU += categoryUtilityChild(child);
            }

            totalCU /= (double) m_children.size();
            return totalCU;
        }

        /**
         * Computes the utility of a single child with respect to this node
         *
         * @param child the child for which to compute the utility
         * @return the utility of the child with respect to this node
         * @throws Exception if something goes wrong
         */
        protected double categoryUtilityChild(CNode child) {//throws Exception {

            double sum = 0;
            for (int i = 0; i < m_numAttributes; i++) {
                if (m_clusterInstances.attribute(i).isNominal()) {
                    for (int j = 0;
                            j < m_clusterInstances.attribute(i).numValues(); j++) {
                        double x = child.getProbability(i, j);
                        double y = getProbability(i, j);
                        sum += (x * x) - (y * y);
                    }
                } else {
                    // numeric attribute
                    sum += ((m_normal / child.getStandardDev(i))
                            - (m_normal / getStandardDev(i)));

                }
            }
            return (child.m_totalInstances / m_totalInstances) * sum;
        }

        /**
         * Returns the probability of a value of a nominal attribute in this node
         *
         * @param attIndex the index of the attribute
         * @param valueIndex the index of the value of the attribute
         * @return the probability
         * @throws Exception if the requested attribute is not nominal
         */
        protected double getProbability(int attIndex, int valueIndex) {
            //throws Exception {

            //  if (!m_clusterInstances.attribute(attIndex).isNominal()) {
            //throw new Exception("getProbability: attribute is not nominal");
            //  }

            if (m_attStats[attIndex].totalCount <= 0) {
                return 0;
            }

            return (double) m_attStats[attIndex].nominalCounts[valueIndex]
                    / (double) m_attStats[attIndex].totalCount;
        }

        /**
         * Returns the standard deviation of a numeric attribute
         *
         * @param attIndex the index of the attribute
         * @return the standard deviation
         * @throws Exception if an error occurs
         */
        protected double getStandardDev(int attIndex) { //throws Exception {
            //  if (!m_clusterInstances.attribute(attIndex).isNumeric()) {
            //throw new Exception("getStandardDev: attribute is not numeric");
            // }

            m_attStats[attIndex].numericStats.calculateDerived();
            double stdDev = m_attStats[attIndex].numericStats.stdDev;
            if (Double.isNaN(stdDev) || Double.isInfinite(stdDev)) {
                return m_acuity;
            }

            return Math.max(m_acuity, stdDev);
        }

        /**
         * Update attribute stats using the supplied instance.
         *
         * @param updateInstance the instance for updating
         * @param delete true if the values of the supplied instance are
         * to be removed from the statistics
         */
        protected void updateStats(Instance updateInstance,
                boolean delete) {

            if (m_attStats == null) {
                m_attStats = new AttributeStats[m_numAttributes];
                for (int i = 0; i < m_numAttributes; i++) {
                    m_attStats[i] = new AttributeStats();
                    if (m_clusterInstances.attribute(i).isNominal()) {
                        m_attStats[i].nominalCounts =
                                new int[m_clusterInstances.attribute(i).numValues()];
                    } else {
                        m_attStats[i].numericStats = new Stats();
                    }
                }
            }
            for (int i = 0; i < m_numAttributes; i++) {
                if (!updateInstance.isMissing(i)) {
                    double value = updateInstance.value(i);
                    if (m_clusterInstances.attribute(i).isNominal()) {
                        m_attStats[i].nominalCounts[(int) value] += (delete)
                                ? (-1.0 * updateInstance.weight())
                                : updateInstance.weight();
                        m_attStats[i].totalCount += (delete)
                                ? (-1.0 * updateInstance.weight())
                                : updateInstance.weight();
                    } else {
                        if (delete) {
                            m_attStats[i].numericStats.subtract(value,
                                    updateInstance.weight());
                        } else {
                            m_attStats[i].numericStats.add(value, updateInstance.weight());
                        }
                    }
                }
            }
            m_totalInstances += (delete)
                    ? (-1.0 * updateInstance.weight())
                    : (updateInstance.weight());
        }

        /**
         * Recursively assigns numbers to the nodes in the tree.
         *
         * @param cl_num an <code>int[]</code> value
         * @throws Exception if an error occurs
         */
        private void assignClusterNums(int[] cl_num) { //throws Exception {
            // if (m_children != null && m_children.size() < 2) {
            //throw new Exception("assignClusterNums: tree not built correctly!");
            // }

            m_clusterNum = cl_num[0];
            cl_num[0]++;
            if (m_children != null) {
                for (int i = 0; i < m_children.size(); i++) {
                    CNode child = (CNode) m_children.elementAt(i);
                    child.assignClusterNums(cl_num);
                }
            }
        }

        /**
         * Recursively build a string representation of the Cobweb tree
         *
         * @param depth depth of this node in the tree
         * @param text holds the string representation
         */
        protected void dumpTree(int depth, StringBuffer text) {

            if (depth == 0) {
                determineNumberOfClusters();
            }

            if (m_children == null) {
                text.append("\n");
                for (int j = 0; j < depth; j++) {
                    text.append("|   ");
                }
                text.append("leaf " + m_clusterNum + " ["
                        + m_clusterInstances.numInstances() + "]");
            } else {
                for (int i = 0; i < m_children.size(); i++) {
                    text.append("\n");
                    for (int j = 0; j < depth; j++) {
                        text.append("|   ");
                    }
                    text.append("node " + m_clusterNum + " ["
                            + m_clusterInstances.numInstances()
                            + "]");
                    ((CNode) m_children.elementAt(i)).dumpTree(depth + 1, text);
                }
            }
        }

        /**
         * Returns the instances at this node as a string. Appends the cluster
         * number of the child that each instance belongs to.
         *
         * @return a <code>String</code> value
         * @throws Exception if an error occurs
         */
        protected String dumpData() { //throws Exception {
            if (m_children == null) {
                return m_clusterInstances.toString();
            }

            // construct instances string with cluster numbers attached
            CNode tempNode = new CNode(m_numAttributes);
            tempNode.m_clusterInstances = new Instances(m_clusterInstances, 1);
            for (int i = 0; i < m_children.size(); i++) {
                tempNode.addChildNode((CNode) m_children.elementAt(i));
            }
            Instances tempInst = tempNode.m_clusterInstances;
            tempNode = null;

            Add af = new Add();
            af.setAttributeName("Cluster");
            String labels = "";
            for (int i = 0; i < m_children.size(); i++) {
                CNode temp = (CNode) m_children.elementAt(i);
                labels += ("C" + temp.m_clusterNum);
                if (i < m_children.size() - 1) {
                    labels += ",";
                }
            }
            af.setNominalLabels(labels);
            //af.setInputFormat(tempInst);
            //tempInst = Filter.useFilter(tempInst, af);
            tempInst.setRelationName("Cluster " + m_clusterNum);

            int z = 0;
            for (int i = 0; i < m_children.size(); i++) {
                CNode temp = (CNode) m_children.elementAt(i);
                for (int j = 0; j < temp.m_clusterInstances.numInstances(); j++) {
                    tempInst.instance(z).setValue(m_numAttributes, (double) i);
                    z++;
                }
            }
            return tempInst.toString();
        }

        /**
         * Recursively generate the graph string for the Cobweb tree.
         *
         * @param text holds the graph string
         * @throws Exception if generation fails
         */
        protected void graphTree(StringBuffer text) { //throws Exception {

            text.append("N" + m_clusterNum
                    + " [label=\"" + ((m_children == null)
                    ? "leaf " : "node ")
                    + m_clusterNum + " "
                    + " (" + m_clusterInstances.numInstances()
                    + ")\" "
                    + ((m_children == null)
                    ? "shape=box style=filled " : "")
                    + (m_saveInstances
                    ? "data =\n" + dumpData() + "\n,\n"
                    : "")
                    + "]\n");
            if (m_children != null) {
                for (int i = 0; i < m_children.size(); i++) {
                    CNode temp = (CNode) m_children.elementAt(i);
                    text.append("N" + m_clusterNum
                            + "->"
                            + "N" + temp.m_clusterNum
                            + "\n");
                }

                for (int i = 0; i < m_children.size(); i++) {
                    CNode temp = (CNode) m_children.elementAt(i);
                    temp.graphTree(text);
                }
            }
        }

	/**
         * Recursively build a clustering representation of the Cobweb tree
         *
         * @param depth depth of this node in the tree
         * @param clustering holds the Clustering representation
         */
        protected void computeTreeClustering(int depth, Clustering clustering) {

            if (depth == 0) {
                determineNumberOfClusters();
            }

            if (m_children == null) {
		//Append Cluster
                /*text.append("\n");
                for (int j = 0; j < depth; j++) {
                    text.append("|   ");
                }
                text.append("leaf " + m_clusterNum + " ["
                        + m_clusterInstances.numInstances() + "]");
		clustering.add(SphereCluster(this.coordinates, .05, m_clusterInstances.numInstances()));*/
	            if (depth == 0) {
	    		    double [] centroidCoordinates = new double[m_clusterInstances.numAttributes()];
			    for (int j = 0; j < m_clusterInstances.numAttributes()-1; j++) {						
				centroidCoordinates[j] = m_clusterInstances.meanOrMode(j);	
			    }
			    clustering.add(new SphereCluster(centroidCoordinates, .05, m_clusterInstances.numInstances()));
	            }
            } else {
                for (int i = 0; i < m_children.size(); i++) {
                    /*text.append("\n");
                    for (int j = 0; j < depth; j++) {
                        text.append("|   ");
                    }
                    text.append("node " + m_clusterNum + " ["
                            + m_clusterInstances.numInstances()
                            + "]");*/
    		    double [] centroidCoordinates = new double[m_clusterInstances.numAttributes()];
		    for (int j = 0; j < m_clusterInstances.numAttributes()-1; j++) {						
			centroidCoordinates[j] = m_clusterInstances.meanOrMode(j);	
		    }
		    clustering.add(new SphereCluster(centroidCoordinates, .05, m_clusterInstances.numInstances()));
                    ((CNode) m_children.elementAt(i)).computeTreeClustering(depth + 1, clustering);
                }
            }
        }
    }
    /**
     * Normal constant.
     */
    protected static final double m_normal = 1.0 / (2 * Math.sqrt(Math.PI));
    /**
     * Acuity (minimum standard deviation).
     */
    protected double m_acuity = 1.0;
    /**
     * Cutoff (minimum category utility).
     */
    protected double m_cutoff = 0.002;//0.01 * Cobweb.m_normal;
    /**
     * Holds the root of the Cobweb tree.
     */
    protected CNode m_cobwebTree = null;
    /**
     * Number of clusters (nodes in the tree). Must never be queried directly,
     * only via the method numberOfClusters(). Otherwise it's not guaranteed that
     * it contains the correct value.
     *
     * @see #numberOfClusters()
     * @see #m_numberOfClustersDetermined
     */
    protected int m_numberOfClusters = -1;
    /** whether the number of clusters was already determined */
    protected boolean m_numberOfClustersDetermined = false;
    /** the number of splits that happened */
    protected int m_numberSplits;
    /** the number of merges that happened */
    protected int m_numberMerges;
    /**
     * Output instances in graph representation of Cobweb tree (Allows
     * instances at nodes in the tree to be visualized in the Explorer).
     */
    protected boolean m_saveInstances = false;
    @SuppressWarnings("hiding")
    public static final String classifierPurposeString = "Cobweb and Classit clustering algorithms: it always compares the best host, adding a new leaf, merging the two best hosts, and splitting the best host when considering where to place a new instance..";

    @Override
    public void resetLearningImpl() {
        setAcuity(this.acuityOption.getValue());
        setCutoff(this.cutoffOption.getValue());
        m_numberOfClusters = -1;
        m_cobwebTree = null;
        m_numberSplits = 0;
        m_numberMerges = 0;
    }

    /**
     * Adds an instance to the clusterer.
     *
     * @param newInstance the instance to be added
     * @throws Exception 	if something goes wrong
     */
    // public void updateClusterer(Instance newInstance) throws Exception {
    @Override
    public void trainOnInstanceImpl(Instance newInstance) { //throws Exception {
        m_numberOfClustersDetermined = false;

        if (m_cobwebTree == null) {
            m_cobwebTree = new CNode(newInstance.numAttributes(), newInstance);
        } else {
            m_cobwebTree.addInstance(newInstance);
        }
    }

    /**
     * Classifies a given instance.
     *
     * @param instance the instance to be assigned to a cluster
     * @return the number of the assigned cluster as an interger
     * if the class is enumerated, otherwise the predicted value
     * @throws Exception if instance could not be classified
     * successfully
     */
    public double[] getVotesForInstance(Instance instance) {
        //public int clusterInstance(Instance instance) {//throws Exception {
        CNode host = m_cobwebTree;
        CNode temp = null;

        determineNumberOfClusters();

        if (this.m_numberOfClusters < 1) {
            return (new double[0]);
        }
        double[] ret = new double[this.m_numberOfClusters];

        do {
            if (host.m_children == null) {
                temp = null;
                break;
            }

            host.updateStats(instance, false);
            temp = host.findHost(instance, true);
            host.updateStats(instance, true);

            if (temp != null) {
                host = temp;
            }
        } while (temp != null);

        ret[host.m_clusterNum] = 1.0;
        return ret;
    }

    /**
     * determines the number of clusters if necessary
     *
     * @see #m_numberOfClusters
     * @see #m_numberOfClustersDetermined
     */
    protected void determineNumberOfClusters() {
        if (!m_numberOfClustersDetermined
                && (m_cobwebTree != null)) {
            int[] numClusts = new int[1];
            numClusts[0] = 0;
            //  try {
            m_cobwebTree.assignClusterNums(numClusts);
            // }
            // catch (Exception e) {
//	e.printStackTrace();
//	numClusts[0] = 0;
            // }
            m_numberOfClusters = numClusts[0];

            m_numberOfClustersDetermined = true;
        }
    }

    /**
     * Returns the number of clusters.
     *
     * @return the number of clusters
     */
    public int numberOfClusters() {
        determineNumberOfClusters();
        return m_numberOfClusters;
    }

    {
//		return this.observedClassDistribution.getArrayCopy();
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        StringBuffer text = new StringBuffer();
        if (m_cobwebTree == null) {
            StringUtils.appendIndented(out, indent, "Cobweb hasn't been built yet!");
            StringUtils.appendNewline(out);
        } else {
            m_cobwebTree.dumpTree(0, text);
            StringUtils.appendIndented(out, indent, "CobWeb - ");
            out.append("Number of merges: "
                    + m_numberMerges + "\nNumber of splits: "
                    + m_numberSplits + "\nNumber of clusters: "
                    + numberOfClusters() + "\n" + text.toString());
            StringUtils.appendNewline(out);
        }
    }

    public boolean isRandomizable() {
        return false;
    }

    /**
     * Generates the graph string of the Cobweb tree
     *
     * @return a <code>String</code> value
     * @throws Exception if an error occurs
     */
    public String graph() {// throws Exception {
        StringBuffer text = new StringBuffer();

        text.append("digraph CobwebTree {\n");
        m_cobwebTree.graphTree(text);
        text.append("}\n");
        return text.toString();
    }

    /**
     * set the acuity.
     * @param a the acuity value
     */
    public void setAcuity(double a) {
        m_acuity = a;
    }

    /**
     * get the acuity value
     * @return the acuity
     */
    public double getAcuity() {
        return m_acuity;
    }

    /**
     * set the cutoff
     * @param c the cutof
     */
    public void setCutoff(double c) {
        m_cutoff = c;
    }

    /**
     * get the cutoff
     * @return the cutoff
     */
    public double getCutoff() {
        return m_cutoff;
    }

    /**
     * Get the value of saveInstances.
     *
     * @return Value of saveInstances.
     */
    public boolean getSaveInstanceData() {

        return m_saveInstances;
    }

    /**
     * Set the value of saveInstances.
     *
     * @param newsaveInstances Value to assign to saveInstances.
     */
    public void setSaveInstanceData(boolean newsaveInstances) {

        m_saveInstances = newsaveInstances;
    }

    public Clustering getClusteringResult() {
        //throw new UnsupportedOperationException("Not supported yet.");
	Clustering result = new Clustering();
        if (m_cobwebTree == null) {
            //StringUtils.appendIndented(out, indent, "Cobweb hasn't been built yet!");
            //StringUtils.appendNewline(out);
        } else {
            m_cobwebTree.computeTreeClustering(0,result);
	    System.out.println("After Number of clusters: "+numberOfClusters() );    
	}
	System.out.println("Number of clusters: "+result.size());
	return result; 
    }


}
     


