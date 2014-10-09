/**
 * RandomRBFGeneratorEvents.java
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz) - RandomRBFGenerator 
 * 		   Timm Jansen (moa@cs.rwth-aachen.de) - Events
 * @editor Yunsu Kim
 * 
 * Last edited: 2013/06/02
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
package moa.streams.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.core.AutoExpandVector;
import moa.core.InstanceExample;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import moa.gui.visualization.DataPoint;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import moa.core.FastVector;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;


public class RandomRBFGeneratorEvents extends ClusteringStream {
    private transient Vector listeners;

    private static final long serialVersionUID = 1L;

    public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
                    'm', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption(
                    "instanceRandomSeed", 'i',
                    "Seed for random generation of instances.", 5);

    public IntOption numClusterOption = new IntOption("numCluster", 'K',
                    "The average number of centroids in the model.", 5, 1, Integer.MAX_VALUE);

    public IntOption numClusterRangeOption = new IntOption("numClusterRange", 'k',
                    "Deviation of the number of centroids in the model.", 3, 0, Integer.MAX_VALUE);

    public FloatOption kernelRadiiOption = new FloatOption("kernelRadius", 'R',
                    "The average radii of the centroids in the model.", 0.07, 0, 1);

    public FloatOption kernelRadiiRangeOption = new FloatOption("kernelRadiusRange", 'r',
                    "Deviation of average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption densityRangeOption = new FloatOption("densityRange", 'd',
                    "Offset of the average weight a cluster has. Value of 0 means all cluster " +
                    "contain the same amount of points.", 0, 0, 1);

    public IntOption speedOption = new IntOption("speed", 'V',
                    "Kernels move a predefined distance of 0.01 every X points", 500, 1, Integer.MAX_VALUE);

    public IntOption speedRangeOption = new IntOption("speedRange", 'v',
                    "Speed/Velocity point offset", 0, 0, Integer.MAX_VALUE);

    public FloatOption noiseLevelOption = new FloatOption("noiseLevel", 'N',
                    "Noise level", 0.1, 0, 1);

    public FlagOption noiseInClusterOption = new FlagOption("noiseInCluster", 'n',
                    "Allow noise to be placed within a cluster");

    public IntOption eventFrequencyOption = new IntOption("eventFrequency", 'E',
                    "Event frequency. Enable at least one of the events below and set numClusterRange!", 30000, 0, Integer.MAX_VALUE);

    public FlagOption eventMergeSplitOption = new FlagOption("eventMergeSplitOption", 'M',
                    "Enable merging and splitting of clusters. Set eventFrequency and numClusterRange!");

    public FlagOption eventDeleteCreateOption = new FlagOption("eventDeleteCreate", 'C',
    				"Enable emering and disapperaing of clusters. Set eventFrequency and numClusterRange!");

    
    private double merge_threshold = 0.7;
    private int kernelMovePointFrequency = 10;
    private double maxDistanceMoveThresholdByStep = 0.01;
    private int maxOverlapFitRuns = 50;
    private double eventFrequencyRange = 0;

    private boolean debug = false;

    private AutoExpandVector<GeneratorCluster> kernels;
    protected Random instanceRandom;
    protected InstancesHeader streamHeader;
    private int numGeneratedInstances;
    private int numActiveKernels;
    private int nextEventCounter;
    private int nextEventChoice = -1;
    private int clusterIdCounter;
    private GeneratorCluster mergeClusterA;
    private GeneratorCluster mergeClusterB;
    private boolean mergeKernelsOverlapping = false;



    private class GeneratorCluster implements Serializable{
        //TODO: points is redundant to microclusterpoints, we need to come 
        //up with a good strategy that microclusters get updated and 
        //rebuild if needed. Idea: Sort microclusterpoints by timestamp and let 
        // microclusterdecay hold the timestamp for when the last point in a 
        //microcluster gets kicked, then we rebuild... or maybe not... could be
        //same as searching for point to be kicked. more likely is we rebuild 
        //fewer times then insert.
        
        private static final long serialVersionUID = -6301649898961112942L;
        
        SphereCluster generator;
        int kill = -1;
        boolean merging = false;
        double[] moveVector;
        int totalMovementSteps;
        int currentMovementSteps;
        boolean isSplitting = false;

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
                double weight = avgWeight + (instanceRandom.nextBoolean()?-1:1)*avgWeight*densityRangeOption.getValue()*instanceRandom.nextDouble();
                generator.setWeight(weight);
                setDesitnation(null);
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
            setDesitnation(null);
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
                                setDesitnation(null);
                                break;
                            }
                        }
                    }
                    generator.setCenter(center);
                }
            }
            else{
                if(!merging){
                    setDesitnation(null);
                    isSplitting = false;
                }
            }
        }

        void setDesitnation(double[] destination){

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
            setMoveVector(v);
        }

        void setMoveVector(double[] vector){
        	//we are ignoring the steps, otherwise we have to change 
        	//speed of the kernels, do we want that?
            moveVector = vector;
            int speedInPoints  = speedOption.getValue();
            if(speedRangeOption.getValue() > 0)
                    speedInPoints +=(instanceRandom.nextBoolean()?-1:1)*instanceRandom.nextInt(speedRangeOption.getValue());
            if(speedInPoints  < 1) speedInPoints  = speedOption.getValue();


            double length = 0;
            for ( int d = 0; d < moveVector.length; d++ ) {
                length+=Math.pow(vector[d],2);
            }
            length = Math.sqrt(length);

            totalMovementSteps = (int)(length/(maxDistanceMoveThresholdByStep*kernelMovePointFrequency)*speedInPoints);
            for ( int d = 0; d < moveVector.length; d++ ) {
                moveVector[d]/=(double)totalMovementSteps;
            }


            currentMovementSteps = 0;
//            if(debug){
//                System.out.println("Setting new direction for C"+generator.getId()+": distance "
//                        +length+" in "+totalMovementSteps+" steps");
//            }
        }

        private String tryMerging(GeneratorCluster merge){
           String message = "";
           double overlapDegree = generator.overlapRadiusDegree(merge.generator);
           if(overlapDegree > merge_threshold){
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
                mergeKernelsOverlapping = false;
            }
           else{
	           if(overlapDegree > 0 && !mergeKernelsOverlapping){
	        	   mergeKernelsOverlapping = true;
	        	   message = "Merge overlapping started";
	           }
           }
           return message;
        }

        private String splitKernel(){
            isSplitting = true;
            //todo radius range
            double radius = kernelRadiiOption.getValue();
            double avgWeight = 1.0/numClusterOption.getValue();
            double weight = avgWeight + avgWeight*densityRangeOption.getValue()*instanceRandom.nextDouble();
            SphereCluster spcluster = null;

            double[] center = generator.getCenter();
            spcluster = new SphereCluster(center, radius, weight);

            if(spcluster !=null){
                GeneratorCluster gc = new GeneratorCluster(clusterIdCounter++, spcluster);
                gc.isSplitting = true;
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
        
        private String fadeOut(){
        	kill = decayHorizonOption.getValue();
        	generator.setWeight(0.0);
        	numActiveKernels--;
        	normalizeWeights();
        	return "Fading out C"+generator.getId();
        }
        
        
    }

    public RandomRBFGeneratorEvents() {
        noiseInClusterOption.set();
//        eventDeleteCreateOption.set();
//        eventMergeSplitOption.set();
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
            nextEventChoice = getNextEvent();
            numActiveKernels = 0;
            numGeneratedInstances = 0;
            clusterIdCounter = 0;
            mergeClusterA = mergeClusterB = null;
            kernels = new AutoExpandVector<GeneratorCluster>();

            initKernels();
    }
	
    protected void generateHeader() {	// 2013/06/02: Noise label
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.add(new Attribute("att" + (i + 1)));
        }
        
        ArrayList<String> classLabels = new ArrayList<String>();
        for (int i = 0; i < this.numClusterOption.getValue(); i++) {
            classLabels.add("class" + (i + 1));
        }
        if (noiseLevelOption.getValue() > 0) classLabels.add("noise");	// The last label = "noise"
        
        attributes.add(new Attribute("class", classLabels));
        streamHeader = new InstancesHeader(new Instances(getCLICreationString(InstanceStream.class), attributes, 0));
        streamHeader.setClassIndex(streamHeader.numAttributes() - 1);
    }

        
    protected void initKernels() {
        for (int i = 0; i < numClusterOption.getValue(); i++) {
            kernels.add(new GeneratorCluster(clusterIdCounter));
            numActiveKernels++;
            clusterIdCounter++;
        }
        normalizeWeights();
    }

    public InstanceExample nextInstance() {
        numGeneratedInstances++;
        eventScheduler();

        //make room for the classlabel
        double[] values_new = new double [numAttsOption.getValue()+1]; //+1
        double[] values = null;
        int clusterChoice = -1;

        if(instanceRandom.nextDouble() > noiseLevelOption.getValue()){
            clusterChoice = chooseWeightedElement();
            values = kernels.get(clusterChoice).generator.sample(instanceRandom).toDoubleArray();
        }
        else{
            //get ranodm noise point
            values = getNoisePoint();
        }

        if(Double.isNaN(values[0])){
            System.out.println("Instance corrupted:"+numGeneratedInstances);
        }
        //System.arraycopy(values, 0, values_new, 0, values.length);
        System.arraycopy(values, 0, values_new, 0, values.length);
        Instance inst = new DenseInstance(1.0, values_new);
        inst.setDataset(getHeader());
        if(clusterChoice == -1){
        	// 2013/06/02 (Yunsu Kim)
        	// Noise instance has the last class value instead of "-1"
        	// Preventing ArrayIndexOutOfBoundsException in WriteStreamToARFFFile
            inst.setClassValue(numClusterOption.getValue());
        }
        else{
            inst.setClassValue(kernels.get(clusterChoice).generator.getId());
            //Do we need micro cluster representation if have overlapping clusters?
            //if(!overlappingOption.isSet())
                kernels.get(clusterChoice).addInstance(inst);
        }
//        System.out.println(numGeneratedInstances+": Overlap is"+updateOverlaps());
        
        return new InstanceExample(inst);
    }


    public Clustering getGeneratingClusters(){
        Clustering clustering = new Clustering();
        for (int c = 0; c < kernels.size(); c++) {
            clustering.add(kernels.get(c).generator);
        }
        return clustering;
    }

    public Clustering getMicroClustering(){
        Clustering clustering = new Clustering();
        int id = 0;

            for (int c = 0; c < kernels.size(); c++) {
                for (int m = 0; m < kernels.get(c).microClusters.size(); m++) {
                    kernels.get(c).microClusters.get(m).setId(id);
                    kernels.get(c).microClusters.get(m).setGroundTruth(kernels.get(c).generator.getId());
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


        if(eventFrequencyOption.getValue() == 0){
            return;
        }

        String type ="";
        String message ="";
        boolean eventFinished = false;
        switch(nextEventChoice){
            case 0:
                if(numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()){
                    message = mergeKernels(nextEventCounter);
                    type = "Merge";
                }
                if(mergeClusterA==null && mergeClusterB==null && message.startsWith("Clusters merging")){
                	eventFinished = true;
                }                
            break;
            case 1:
                if(nextEventCounter<=0){
                    if(numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()){
                        type = "Split";
                        message = splitKernel();
                    }
                    eventFinished = true;
                }
            break;
            case 2:
                if(nextEventCounter<=0){
                	if(numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()){
                        message = fadeOut();
                        type = "Delete";
                    }
                	eventFinished = true;
                }
            break;
            case 3:
                if(nextEventCounter<=0){
                	if(numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()){
	                    message = fadeIn();
	                    type = "Create";
                	}
                	eventFinished = true;          	
                }
            break;

        }
        if (eventFinished){
                nextEventCounter = (int)(eventFrequencyOption.getValue()+(instanceRandom.nextBoolean()?-1:1)*eventFrequencyOption.getValue()*eventFrequencyRange*instanceRandom.nextDouble());
                nextEventChoice = getNextEvent();
                //System.out.println("Next event choice: "+nextEventChoice);
        }
        if(!message.isEmpty()){
        	message+=" (numKernels = "+numActiveKernels+" at "+numGeneratedInstances+")";
        	if(!type.equals("Merge") || message.startsWith("Clusters merging"))
        		fireClusterChange(numGeneratedInstances, type, message);
        }
    }
    
    private int getNextEvent() {
    	int choice = -1;
    	boolean lowerLimit = numActiveKernels <= numClusterOption.getValue() - numClusterRangeOption.getValue();
    	boolean upperLimit = numActiveKernels >= numClusterOption.getValue() + numClusterRangeOption.getValue();

    	if(!lowerLimit || !upperLimit){
	    	int mode = -1;
	    	if(eventDeleteCreateOption.isSet() && eventMergeSplitOption.isSet()){
	    		mode = instanceRandom.nextInt(2);
	    	}
	    	
			if(mode==0 || (mode==-1 && eventMergeSplitOption.isSet())){
				//have we reached a limit? if not free choice
				if(!lowerLimit && !upperLimit) 
					choice = instanceRandom.nextInt(2);
				else
					//we have a limit. if lower limit, choose split
					if(lowerLimit)
						choice = 1;
					//otherwise we reached upper level, choose merge
					else
						choice = 0;
			}
			
			if(mode==1 || (mode==-1 && eventDeleteCreateOption.isSet())){
				//have we reached a limit? if not free choice
				if(!lowerLimit && !upperLimit) 
					choice = instanceRandom.nextInt(2)+2;
				else
					//we have a limit. if lower limit, choose create
					if(lowerLimit)
						choice = 3;
					//otherwise we reached upper level, choose delete
					else
						choice = 2;
			}
    	}

    	
    	return choice;
    }

	private String fadeOut(){
	    int id = instanceRandom.nextInt(kernels.size());
	    while(kernels.get(id).kill!=-1)
	        id = instanceRandom.nextInt(kernels.size());
	
	    String message = kernels.get(id).fadeOut();
	    return message;
    }
    
    private String fadeIn(){
	    	GeneratorCluster gc = new GeneratorCluster(clusterIdCounter++);
	        kernels.add(gc);
	        numActiveKernels++;
	        normalizeWeights();
    	return "Creating new cluster";
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
    }

    private String mergeKernels(int steps){
        if(numActiveKernels >1 && ((mergeClusterA == null && mergeClusterB == null))){

        	//choose clusters to merge
        	double diseredDist = steps / speedOption.getValue() * maxDistanceMoveThresholdByStep;
        	double minDist = Double.MAX_VALUE;
//        	System.out.println("DisredDist:"+(2*diseredDist));
        	for(int i = 0; i < kernels.size(); i++){
        		for(int j = 0; j < i; j++){
            		if(kernels.get(i).kill!=-1 || kernels.get(j).kill!=-1){
            			continue;
            		}
            		else{
            			double kernelDist = kernels.get(i).generator.getCenterDistance(kernels.get(j).generator);
            			double d = kernelDist-2*diseredDist;
//            			System.out.println("Dist:"+i+" / "+j+" "+d);
            			if(Math.abs(d) < minDist && 
            					(minDist != Double.MAX_VALUE || d>0 || Math.abs(d) < 0.001)){
            				minDist = Math.abs(d);
            				mergeClusterA = kernels.get(i);
            				mergeClusterB = kernels.get(j);
            			}
            		}
        		}
        	}
        	
        	if(mergeClusterA!=null && mergeClusterB!=null){
	        	double[] merge_point = mergeClusterA.generator.getCenter();
	        	double[] v = mergeClusterA.generator.getDistanceVector(mergeClusterB.generator);
	        	for (int i = 0; i < v.length; i++) {
	        		merge_point[i]= merge_point[i]+v[i]*0.5;
				}
	
	            mergeClusterA.merging = true;
	            mergeClusterB.merging = true;
	            mergeClusterA.setDesitnation(merge_point);
	            mergeClusterB.setDesitnation(merge_point);
	            
	            if(debug){
	            	System.out.println("Center1"+Arrays.toString(mergeClusterA.generator.getCenter()));
		        	System.out.println("Center2"+Arrays.toString(mergeClusterB.generator.getCenter()));
		            System.out.println("Vector"+Arrays.toString(v));        	
	            	
	                System.out.println("Try to merge cluster "+mergeClusterA.generator.getId()+
	                        " into "+mergeClusterB.generator.getId()+
	                        " at "+Arrays.toString(merge_point)+
	                        " time "+numGeneratedInstances);
	            }
	            return "Init merge";
        	}
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

    }

    private double[] getNoisePoint(){
        double [] sample = new double [numAttsOption.getValue()];
        boolean incluster = true;
        int counter = 20;
        while(incluster){
            for (int j = 0; j < numAttsOption.getValue(); j++) {
                 sample[j] = instanceRandom.nextDouble();
            }
            incluster = false;
            if(!noiseInClusterOption.isSet() && counter > 0){
                counter--;
                for(int c = 0; c < kernels.size(); c++){
                    for(int m = 0; m < kernels.get(c).microClusters.size(); m++){
                        Instance inst = new DenseInstance(1, sample);
                        if(kernels.get(c).microClusters.get(m).getInclusionProbability(inst) > 0){
                            incluster = true;
                            break;
                        }
                    }
                    if(incluster)
                        break;
                }
            }
        }

//        double [] sample = new double [numAttsOption.getValue()];
//        for (int j = 0; j < numAttsOption.getValue(); j++) {
//             sample[j] = instanceRandom.nextDouble();
//        }
             
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
