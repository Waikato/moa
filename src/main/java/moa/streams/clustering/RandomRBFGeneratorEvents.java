/*
 *    RandomRBFGenerator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.streams.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.core.AutoExpandVector;
import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.gui.visualization.DataPoint;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class RandomRBFGeneratorEvents extends ClusteringStream {
    private transient Vector listeners;

    private static final long serialVersionUID = 1L;

    public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
                    'm', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption(
                    "instanceRandomSeed", 'i',
                    "Seed for random generation of instances.", 1);

    public IntOption numClusterOption = new IntOption("numCluster", 'K',
                    "The average number of centroids in the model.", 4, 1, Integer.MAX_VALUE);

    public IntOption numClusterRangeOption = new IntOption("numClusterRange", 'k',
                    "Deviation of the number of centroids in the model.", 3, 1, Integer.MAX_VALUE);

    public FloatOption kernelRadiiOption = new FloatOption("kernelRadius", 'R',
                    "The average radii of the centroids in the model.", 0.05, 0, 1);

    public FloatOption kernelRadiiRangeOption = new FloatOption("kernelRadiusRange", 'r',
                    "Deviation of average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption densityRangeOption = new FloatOption("densityRange", 'd',
                    "Offset of the average weight a cluster has. Value of 0 means all cluster " +
                    "contain the same amount of points.", 0, 0, 1);

    public IntOption speedOption = new IntOption("speed", 'V',
                    "Kernels move a predefined distance of 0.01 every X points", 100, 1, Integer.MAX_VALUE);

    public IntOption speedRangeOption = new IntOption("speedRange", 'v',
                    "Speed/Velocity point offset", 0, 0, Integer.MAX_VALUE);

    public FloatOption noiseLevelOption = new FloatOption("noiseLevel", 'N',
                    "Noise level", 0.1, 0, 1);

    public IntOption eventFrequencyOption = new IntOption("eventFrequency", 'E',
                    "Event frequency", 15000, 0, Integer.MAX_VALUE);

    public FloatOption eventMergeWeightOption = new FloatOption("eventMergeWeight", 'M',
                    "", 0.5, 0, 1);

    public FloatOption eventSplitWeightOption = new FloatOption("eventSplitWeight", 'P',
                    "Influences the probablity of SplitClusterChange events relative to the total sum of all event-weights." +
                    "SplitClusterChange Events will split a cluster into two clusters.", 0.5, 0, 1);

//    public FloatOption eventSizeWeightOption = new FloatOption("eventSizeWeight", 'S',
//                    "Influences the probablity of SizeClusterChange events relative to the total sum of all event-weights." +
//                    "SizeClusterChange Events will increase/decrease the clusters radius.", 0.5, 0, 1);
//
//    public FloatOption eventDensityWeightOption = new FloatOption("eventDensityWeight", 'D',
//                    "Influences the probablity of DensityClusterChange events relative to the total sum of all event-weights." +
//                    "DensityClusterChange Events will increase/decrease the amount of points contained by a cluster.", 0.5, 0, 1);


    private double merge_threshold = 0.7;
    private int kernelMovePointFrequency = 10;
    private double maxDistanceMoveThresholdByStep = 0.01;
    private int maxOverlapFitRuns = 50;
    private double eventFrequencyRange = 0.25;
    //double test = (2.0/5.0) + (2.0/5.0) - 0.6;

    private boolean debug = true;

    private AutoExpandVector<GeneratorCluster> kernels;
    protected Random instanceRandom;
    protected InstancesHeader streamHeader;
    private int numGeneratedInstances;
    private int numActiveKernels;
    private int nextEventCounter;
    private int nextEventChoice;
    private int clusterIdCounter;
    private GeneratorCluster mergeClusterA;
    private GeneratorCluster mergeClusterB;



    private class GeneratorCluster{
        //TODO: points is redundant to microclusterpoints, we need to come 
        //up with a good strategie that microclusters get updated and 
        //rebuild if needed. Idea: Sort microclusterpoints by timestamp and let 
        // microclusterdecay hold the timestamp for when the last point in a 
        //micro cluster gets kicked then we rebuild... or maybe not... could be
        //same as searching for point to be kicked. more likely is we rebuild 
        //fewer times then insert.
        
        SphereCluster generator;
        int kill = -1;
        boolean merging = false;
        double[] moveVector;
        int totalMovementSteps;
        int currentMovementSteps;

        LinkedList<DataPoint> points = new LinkedList<DataPoint>();
        ArrayList<SphereCluster> microClusters = new ArrayList<SphereCluster>();
        ArrayList<ArrayList<DataPoint>> microClustersPoints = new ArrayList();
        ArrayList<Integer> microClustersDecay = new ArrayList();


        public GeneratorCluster(int label) {
            boolean outofbounds = true;
            int tryCounter = 0;
            while(outofbounds && tryCounter < maxOverlapFitRuns){
                tryCounter++;
                outofbounds = false;
                double[] center = new double [numAttsOption.getValue()];
                double radius = kernelRadiiOption.getValue()+(instanceRandom.nextBoolean()?-1:1)*kernelRadiiRangeOption.getValue()*instanceRandom.nextDouble();
                while(radius <= 0){
                    radius = kernelRadiiOption.getValue()+(instanceRandom.nextBoolean()?-1:1)*kernelRadiiRangeOption.getValue()*instanceRandom.nextDouble();
                }
                for (int j = 0; j < numAttsOption.getValue(); j++) {
                     center[j] = instanceRandom.nextDouble();
                     if(center[j]- radius < 0 || center[j] + radius > 1){
                        outofbounds = true;
                        break;
                     }
                }
                generator = new SphereCluster(center, radius);
            }
            if(tryCounter < maxOverlapFitRuns){
                generator.setId(label);
                double avgWeight = 1.0/numClusterOption.getValue();
                double weight = avgWeight + avgWeight*densityRangeOption.getValue()*instanceRandom.nextDouble();
                generator.setWeight(weight);
                setDesitnation(null, 0);
            }
            else{
                generator = null;
                kill = 0;
                System.out.println("Tried "+maxOverlapFitRuns+" times to create kernel. Reduce average radii." );
            }
        }

        public GeneratorCluster(int label, SphereCluster cluster) {
            this.generator = cluster;
            cluster.setId(label);
            setDesitnation(null, 0);
        }

        public int getWorkID(){
            for(int c = 0; c < kernels.size(); c++){
                if(kernels.get(c).equals(this))
                    return c;
            }
            return -1;
        }

        private void updateKernel(){
            if(kill == 0){
                kernels.remove(this);
            }
            if(kill > 0){
                kill--;
            }
            //we could be lot more precise if we would keep track of timestamps of points
            //then we could remove all old points and rebuild the cluster on up to date point base
            //BUT worse the effort??? so far we just want to avoid overlap with this, so its more
            //konservative as needed. Only needs to change when we need a thighter representation
            for (int m = 0; m < microClusters.size(); m++) {
                if(numGeneratedInstances-microClustersDecay.get(m) > decayHorizonOption.getValue()){
                    microClusters.remove(m);
                    microClustersPoints.remove(m);
                    microClustersDecay.remove(m);
                }
            }

            if(!points.isEmpty() && numGeneratedInstances-points.getFirst().getTimestamp() >= decayHorizonOption.getValue()){
//                if(debug)
//                    System.out.println("Cleaning up macro cluster "+generator.getId());
                points.removeFirst();
            }

        }

        private void addInstance(Instance instance){
            DataPoint point = new DataPoint(instance, numGeneratedInstances);
            points.add(point);
            
            int minMicroIndex = -1;
            double minHullDist = Double.MAX_VALUE;
            boolean inserted = false;
            //we favour more recently build clusters so we can remove earlier cluster sooner
            for (int m = microClusters.size()-1; m >=0 ; m--) {
                SphereCluster micro = microClusters.get(m);
                double hulldist = micro.getCenterDistance(point)-micro.getRadius();
                //point fits into existing cluster
                if(hulldist <= 0){
                    microClustersPoints.get(m).add(point);
                    microClustersDecay.set(m, numGeneratedInstances);
                    inserted = true;
                    break;
                }
                //if not, check if its at least the closest cluster
                else{
                    if(hulldist < minHullDist){
                        minMicroIndex = m;
                        minHullDist = hulldist;
                    }
                }
            }
            //Reseting index choice for alternative cluster building
            int alt = 1;
            if(alt == 1)
                minMicroIndex = -1;
            if(!inserted){
                //add to closest cluster and expand cluster
                if(minMicroIndex!=-1){
                    microClustersPoints.get(minMicroIndex).add(point);
                    //we should keep the miniball instances and just check in
                    //new points instead of rebuilding the whole thing
                    SphereCluster s = new SphereCluster(microClustersPoints.get(minMicroIndex),numAttsOption.getValue());
                    //check if current microcluster is bigger then generating cluster
                    if(s.getRadius() > generator.getRadius()){
                        //remove previously added point
                        microClustersPoints.get(minMicroIndex).remove(microClustersPoints.get(minMicroIndex).size()-1);
                        minMicroIndex = -1;
                    }
                    else{
                        microClusters.set(minMicroIndex, s);
                        microClustersDecay.set(minMicroIndex, numGeneratedInstances);
                    }
                }
                //minMicroIndex might have been reset above
                //create new micro cluster
                if(minMicroIndex == -1){
                    ArrayList<DataPoint> microPoints = new ArrayList<DataPoint>();
                    microPoints.add(point);
                    SphereCluster s;
                    if(alt == 0)
                        s = new SphereCluster(microPoints,numAttsOption.getValue());
                    else
                        s = new SphereCluster(generator.getCenter(),generator.getRadius(),1);

                    microClusters.add(s);
                    microClustersPoints.add(microPoints);
                    microClustersDecay.add(numGeneratedInstances);
                    int id = 0;
                    while(id < kernels.size()){
                        if(kernels.get(id) == this)
                            break;
                        id++;
                    }
                    s.setGroundTruth(id);
                }
            }

        }


        private void move(){
            if(currentMovementSteps < totalMovementSteps){
                currentMovementSteps++;
                if( moveVector == null){
                    return;
                }
                else{
                    double[] center = generator.getCenter();
                    boolean outofbounds = true;
                    while(outofbounds){
                        double radius = generator.getRadius();
                        outofbounds = false;
                        center = generator.getCenter();
                        for ( int d = 0; d < center.length; d++ ) {
                            center[d]+= moveVector[d];
                            if(center[d]- radius < 0 || center[d] + radius > 1){
                                outofbounds = true;
                                setDesitnation(null, 0);
                                break;
                            }
                        }
                    }
                    generator.setCenter(center);
                }
            }
            else{
                if(!merging){
                    setDesitnation(null, 0);
                }
            }
        }

        void setDesitnation(double[] destination, int steps){

            if(destination == null){
                destination = new double [numAttsOption.getValue()];
                for (int j = 0; j < numAttsOption.getValue(); j++) {
                     destination[j] = instanceRandom.nextDouble();
                }
            }
            double[] center = generator.getCenter();
            int dim = center.length;

            double[] v = new double[dim];

            for ( int d = 0; d < dim; d++ ) {
                v[d]=destination[d]-center[d];
            }
            setMoveVector(v, steps);
        }

        void setMoveVector(double[] vector, int steps){
            moveVector = vector;
            int speedInPoints  = speedOption.getValue();
            if(speedRangeOption.getValue() > 0)
                    speedInPoints +=(instanceRandom.nextBoolean()?-1:1)*instanceRandom.nextInt(speedRangeOption.getValue());
            if(speedInPoints  < 1) speedInPoints  = speedOption.getValue();


            double length = 0;
            for ( int d = 0; d < moveVector.length; d++ ) {
                length+=Math.pow(vector[d],2);
            }

            totalMovementSteps = (int)(length/maxDistanceMoveThresholdByStep*speedInPoints);
            for ( int d = 0; d < moveVector.length; d++ ) {
                moveVector[d]/=(double)totalMovementSteps;
            }

            currentMovementSteps = 0;
//            if(debug){
//                System.out.println("Setting new direction for C"+generator.getId()+": distance "
//                        +Math.sqrt(length)+" in "+totalMovementSteps+" steps");
//            }
        }

        private String tryMerging(GeneratorCluster merge){
           String message = "";
           if(generator.overlapRadiusDegree(merge.generator) > merge_threshold){
                SphereCluster mcluster = merge.generator;
                double radius = Math.max(generator.getRadius(), mcluster.getRadius());
                generator.combine(mcluster);

//                //adjust radius, get bigger and bigger with high dim data
                generator.setRadius(radius);
//                double[] center = generator.getCenter();
//                double[] mcenter = mcluster.getCenter();
//                double weight = generator.getWeight();
//                double mweight = generator.getWeight();
////                for (int i = 0; i < center.length; i++) {
////                    center[i] = (center[i] * weight + mcenter[i] * mweight) / (mweight + weight);
////                }
//                generator.setWeight(weight + mweight);
                message  = "Clusters merging: "+mergeClusterB.generator.getId()+" into "+mergeClusterA.generator.getId();

                //clean up and restet merging stuff
                //mark kernel so it gets killed when it doesn't contain any more instances
                merge.kill = decayHorizonOption.getValue();
                //set weight to 0 so no new instances will be created in the cluster
                mcluster.setWeight(0.0);
                normalizeWeights();
                numActiveKernels--;
                mergeClusterB = mergeClusterA = null;
                merging = false;
            }
            return message;
        }

        private String splitKernel(){
            
            //todo radius range
            double radius = kernelRadiiOption.getValue();
            double avgWeight = 1.0/numClusterOption.getValue();
            double weight = avgWeight + avgWeight*densityRangeOption.getValue()*instanceRandom.nextDouble();
            SphereCluster spcluster = null;

            double[] center = generator.getCenter();
            spcluster = new SphereCluster(center, radius, weight);

            if(spcluster !=null){
                GeneratorCluster gc = new GeneratorCluster(clusterIdCounter++, spcluster);
                kernels.add(gc);
                normalizeWeights();
                numActiveKernels++;
                return "Split from Kernel "+generator.getId();
            }
            else{
                System.out.println("Tried to split new kernel from C"+generator.getId()+
                        ". Not enough room for new cluster, decrease average radii, number of clusters or enable overlap.");
                return "";
            }

        }
    }

    public RandomRBFGeneratorEvents() {

    }

    public InstancesHeader getHeader() {
            return streamHeader;
    }

    public long estimatedRemainingInstances() {
            return -1;
    }

    public boolean hasMoreInstances() {
            return true;
    }

    public boolean isRestartable() {
            return true;
    }

    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing random RBF...", -1.0);
        generateHeader();
        restart();
    }

    public void restart() {
            instanceRandom = new Random(instanceRandomSeedOption.getValue());
            nextEventCounter = eventFrequencyOption.getValue();
            nextEventChoice = instanceRandom.nextInt(2);
            numActiveKernels = 0;
            numGeneratedInstances = 0;
            clusterIdCounter = 0;
            mergeClusterA = mergeClusterB = null;
            kernels = new AutoExpandVector<GeneratorCluster>();

            initKernels();
    }
	
    protected void generateHeader() {
            FastVector attributes = new FastVector();
            for (int i = 0; i < this.numAttsOption.getValue(); i++) {
                    attributes.addElement(new Attribute("att" + (i + 1)));
            }
            FastVector classLabels = new FastVector();
            for (int i = 0; i < this.numClusterOption.getValue(); i++) {
                    classLabels.addElement("class" + (i + 1));
            }
            attributes.addElement(new Attribute("class", classLabels));
            streamHeader = new InstancesHeader(new Instances(
                            getCLICreationString(InstanceStream.class), attributes, 0));
            streamHeader.setClassIndex(streamHeader.numAttributes()-1);
    }

        
    protected void initKernels() {
        for (int i = 0; i < numClusterOption.getValue(); i++) {
            kernels.add(new GeneratorCluster(clusterIdCounter));
            numActiveKernels++;
            clusterIdCounter++;
        }
        normalizeWeights();
        //updateOverlaps();
    }

    public Instance nextInstance() {
        numGeneratedInstances++;
        eventScheduler();

        //make room for thge classlabel
        double[] values_new = new double [numAttsOption.getValue()+1];
        double[] values = null;
        int clusterChoice = -1;

        if(instanceRandom.nextDouble() > noiseLevelOption.getValue()){
            clusterChoice = chooseWeightedElement();
            values = kernels.get(clusterChoice).generator.sample(instanceRandom).toDoubleArray();
        }
        else{
            //get ranodm noise point
            values = getNewSample();
        }

        if(Double.isNaN(values[0])){
            System.out.println("Instance corrupted:"+numGeneratedInstances);
        }
        System.arraycopy(values, 0, values_new, 0, values.length);

        Instance inst = new DenseInstance(1.0, values_new);
        inst.setDataset(getHeader());
        if(clusterChoice == -1){
            inst.setClassValue(-1);
        }
        else{
            inst.setClassValue(kernels.get(clusterChoice).generator.getId());
            //Do we need micro cluster representation if have overlapping clusters?
            //if(!overlappingOption.isSet())
                kernels.get(clusterChoice).addInstance(inst);
        }
//        System.out.println(numGeneratedInstances+": Overlap is"+updateOverlaps());
        
        return inst;
    }


    public Clustering getGeneratingClusters(){
        Clustering clustering = new Clustering();
        for (int c = 0; c < kernels.size(); c++) {
            clustering.add(kernels.get(c).generator);
        }
        return clustering;
    }

    public Clustering getClustering(){
        Clustering clustering = new Clustering();
        int id = 0;

            for (int c = 0; c < kernels.size(); c++) {
                for (int m = 0; m < kernels.get(c).microClusters.size(); m++) {
                    kernels.get(c).microClusters.get(m).setId(id);
                    clustering.add(kernels.get(c).microClusters.get(m));
                    id++;
                }
            }

        //System.out.println("numMicroKernels "+clustering.size());
        return clustering;
    }

/**************************** EVENTS ******************************************/
    private void eventScheduler(){
        for ( int i = 0; i < kernels.size(); i++ ) {
            kernels.get(i).updateKernel();
        }

        nextEventCounter--;
        //only move kernels every 10 points, performance reasons????
        //should this be randomized as well???
        if(nextEventCounter%kernelMovePointFrequency == 0){
            //move kernels
            for ( int i = 0; i < kernels.size(); i++ ) {
                kernels.get(i).move();
                //overlapControl();
            }
        }

        String type ="";
        String message ="";
        switch(nextEventChoice){
            case 0:
                if(nextEventCounter<=0){
                    if(numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()){
                        type = "Split";
                        message = splitKernel();
                        message+=" -> numKernels = "+numActiveKernels;
                    }
                    else{
                        nextEventChoice=-1;
                    }
                }
            break;
            case 1:
                if(numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()){
                    message = mergeKernels(false);
                    type = "Merge";
                    if(!message.equals(""))
                        message+=" -> numKernels = "+numActiveKernels;
                }
                else{
                    nextEventChoice=-1;
                }
            break;
            case 2:
                if(nextEventCounter<=0){
                    message = changeWeight(true);
                    type = "Increase Weight";
                }
            break;
            case 3:
                if(nextEventCounter<=0){
                    message = changeWeight(false);
                    type = "Decrease Weight";
                }
            break;
            case 4:
                if(nextEventCounter<=0){
                    message = changeRadius(true);
                    type = "Increase Radius";
                }
            break;
            case 5:
                if(nextEventCounter<=0){
                    message = changeRadius(false);
                    type = "Decrease Radius";
                }
            break;
        }
        if ((nextEventCounter <= 0 &&!message.isEmpty()) || nextEventChoice==-1){
                nextEventCounter = (int)(eventFrequencyOption.getValue()+(instanceRandom.nextBoolean()?-1:1)*eventFrequencyOption.getValue()*eventFrequencyRange*instanceRandom.nextDouble());
                nextEventChoice = instanceRandom.nextInt(2);
        }
        if(!message.isEmpty())
            fireClusterChange(numGeneratedInstances, type, message);
    }

    private String changeWeight(boolean increase){
        double changeRate = 0.1;
        int id = instanceRandom.nextInt(kernels.size());
        while(kernels.get(id).kill!=-1)
            id = instanceRandom.nextInt(kernels.size());

        int sign = 1;
        if(!increase)
            sign = -1;
        double weight_old = kernels.get(id).generator.getWeight();
        double delta = sign*numActiveKernels*instanceRandom.nextDouble()*changeRate;
        kernels.get(id).generator.setWeight(weight_old+delta);

        normalizeWeights();

        String message;
        if(increase)
            message = "Increase ";
        else
            message = "Decrease ";
        message+=" weight on Cluster "+id+" from "+weight_old+" to "+(weight_old+delta);
        return message;


    }

    private String changeRadius(boolean increase){
        double maxChangeRate = 0.1;
        int id = instanceRandom.nextInt(kernels.size());
        while(kernels.get(id).kill!=-1)
            id = instanceRandom.nextInt(kernels.size());

        int sign = 1;
        if(!increase)
            sign = -1;

        double r_old = kernels.get(id).generator.getRadius();
        double r_new =r_old+sign*r_old*instanceRandom.nextDouble()*maxChangeRate;
        if(r_new >= 0.5) return "Radius to big";
        kernels.get(id).generator.setRadius(r_new);
        
        String message;
        if(increase)
            message = "Increase ";
        else
            message = "Decrease ";
        message+=" radius on Cluster "+id+" from "+r_old+" to "+r_new;
        return message;
    }

    private String splitKernel(){
        int id = instanceRandom.nextInt(kernels.size());
        while(kernels.get(id).kill!=-1)
            id = instanceRandom.nextInt(kernels.size());

        String message = kernels.get(id).splitKernel();

        return message;
        //TODO generateHeader(); does that do anything? Ref on dataset in instances?
    }

    private String mergeKernels(boolean reset){
        if(numActiveKernels >1 && ((mergeClusterA == null && mergeClusterB == null) || reset)){
//            if(reset){
//                System.out.println("Reset merging, wasn't possible to merge C"+mergeClusterA+" and C"+mergeClusterB);
//                if(mergeClusterA!=-1)
//                    kernels.get(mergeClusterA).merging = false;
//                if(mergeClusterA!=-1)
//                    kernels.get(mergeClusterB).merging = false;
//                mergeClusterA = mergeClusterB = -1;
//
//            }
            //choose clusters to merge
            mergeClusterA = kernels.get(instanceRandom.nextInt(kernels.size()));
            while(mergeClusterA.kill!=-1)
                mergeClusterA = kernels.get(instanceRandom.nextInt(kernels.size()));

            mergeClusterB = mergeClusterA;
            while(mergeClusterB == mergeClusterA || mergeClusterB.kill!=-1){
                mergeClusterB = kernels.get(instanceRandom.nextInt(kernels.size()));
            }
            boolean outofbound = true;
            double[] merge_point = new double [numAttsOption.getValue()];
            double maxradius = Math.max(mergeClusterA.generator.getRadius(),
                                            mergeClusterB.generator.getRadius());

            int counter = maxOverlapFitRuns;
            while(outofbound && counter > 0){
                counter--;
                outofbound = false;
                for (int j = 0; j < numAttsOption.getValue(); j++) {
                     merge_point[j] = instanceRandom.nextDouble();
                     if(merge_point[j]- maxradius < 0 || merge_point[j] + maxradius > 1){
                        outofbound = true;
                        break;
                     }
                }
            }
            if(counter <= 0)
                return "";

            mergeClusterA.merging = true;
            mergeClusterB.merging = true;
            mergeClusterA.setDesitnation(merge_point,nextEventCounter);
            mergeClusterB.setDesitnation(merge_point,nextEventCounter);
            if(debug)
                System.out.println("Try to merge cluster "+mergeClusterA.getWorkID()+
                        " into "+mergeClusterB.getWorkID()+
                        " at "+Arrays.toString(merge_point)+
                        " time "+numGeneratedInstances);

            return "";
        }

        if(mergeClusterA != null && mergeClusterB != null){

            //movekernels will move the kernels close to each other,
            //we just need to check and merge here if they are close enough
            return mergeClusterA.tryMerging(mergeClusterB);
        }

        return "";
    }




/************************* TOOLS **************************************/

    public void getDescription(StringBuilder sb, int indent) {
            // TODO Auto-generated method stub

    }

    private double[] getNewSample(){
        double [] sample = new double [numAttsOption.getValue()];
        for (int j = 0; j < numAttsOption.getValue(); j++) {
             sample[j] = instanceRandom.nextDouble();
        }
        return sample;
    }

     private int chooseWeightedElement() {
        double r = instanceRandom.nextDouble();

        // Determine index of choosen element
        int i = 0;
        while (r > 0.0) {
            r -= kernels.get(i).generator.getWeight();
            i++;
        }
        --i;	// Overcounted once
        //System.out.println(i);
        return i;
    }

    private void normalizeWeights(){
        double sumWeights = 0.0;
        for (int i = 0; i < kernels.size(); i++) {
            sumWeights+=kernels.get(i).generator.getWeight();
        }
        for (int i = 0; i < kernels.size(); i++) {
            kernels.get(i).generator.setWeight(kernels.get(i).generator.getWeight()/sumWeights);
        }
    }



 /*************** EVENT Listener *********************/
 // should go into the superclass of the generator, create new one for cluster streams?
  
  /** Add a listener */
  synchronized public void addClusterChangeListener(ClusterEventListener l) {
    if (listeners == null)
      listeners = new Vector();
    listeners.addElement(l);
  }

  /** Remove a listener */
  synchronized public void removeClusterChangeListener(ClusterEventListener l) {
    if (listeners == null)
      listeners = new Vector();
    listeners.removeElement(l);
  }

  /** Fire a ClusterChangeEvent to all registered listeners */
  protected void fireClusterChange(long timestamp, String type, String message) {
    // if we have no listeners, do nothing...
    if (listeners != null && !listeners.isEmpty()) {
      // create the event object to send
      ClusterEvent event =
        new ClusterEvent(this, timestamp, type , message);

      // make a copy of the listener list in case
      //   anyone adds/removes listeners
      Vector targets;
      synchronized (this) {
        targets = (Vector) listeners.clone();
      }

      // walk through the listener list and
      //   call the sunMoved method in each
      Enumeration e = targets.elements();
      while (e.hasMoreElements()) {
        ClusterEventListener l = (ClusterEventListener) e.nextElement();
        l.changeCluster(event);

      }
    }
  }

    @Override
    public String getPurposeString() {
            return "Generates a random radial basis function stream.";
    }


    public String getParameterString(){
        return "";
    }




}
