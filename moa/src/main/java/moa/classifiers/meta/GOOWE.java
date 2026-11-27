/*
 *    GOOWE.java
 *    Copyright (C) 2018 Bilkent University, Ankara, Turkey
 *    @author Hamed Bonab (rezanejad.hamed@gmail.com)
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

//package moa.classifiers.meta;

import Jama.Matrix;
import com.github.javacliparser.IntOption;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;

/**
 * The version of this implementation is used for generating the results for: 
 * Hamed R. Bonab, and Fazli Can. "GOOWE: Geometrically Optimum and Online-Weighted Ensemble Classifier for Evolving Data Streams"
 * ACM Transactions on Knowledge Discovery from Data (TKDD), 2(12), March 2018
 * @author Hamed R. Bonab
 * Date: 09 June 2018
 */

public class GOOWE extends AbstractClassifier{
    
    // options 
    /**
     * Type of classifier to use as a component classifier.
     */
    public ClassOption baseLearnerOption = new ClassOption("learner", 'l', "Classifier to train.", Classifier.class, 
			"trees.HoeffdingTree -e 2000000 -g 100 -c 0.01");

    /**
     * Number of component classifiers.
     */ 
    public IntOption numOfHypoOption = new IntOption("memberCount", 'n',
                    "The maximum number of classifiers in an ensemble.", 10, 1, Integer.MAX_VALUE);
    
    /**
    * Number of class labels
    */ 
    public IntOption numOfClassLabelsOption = new IntOption("classCount", 'c',
                    "The number of class labels for classification, for example binary classfication is 2", 2, 1, Integer.MAX_VALUE);
    
    /**
     * Window size.
     * this specifies if no change in this period of time happens we should train new hypo and compare it with the existing ones
     */
    public IntOption windowSizeOption = new IntOption("windowSize", 'w',
                    "The window size used for classifier creation and evaluation.", 500, 1, Integer.MAX_VALUE);
    
    
    public Classifier[] hypo; // array of classifiers in ensemble
    public double[] glob_weight;  // weights of each classifier in an ensemble
    
    InstanceList window;
    int numOfClasses;
    int num_proccessed_instance;
    int numOfHypo;
    int curNumOfHypo; //number of hypothesis till now     
    int candidateIndex;
    int fixedWindowPeriod;
    
    @Override
    public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);
    }
    
    
    @Override
    public void resetLearningImpl() {
        numOfClasses = numOfClassLabelsOption.getValue();
        numOfHypo = numOfHypoOption.getValue();
        fixedWindowPeriod = windowSizeOption.getValue();
        window = new InstanceList(fixedWindowPeriod);
        this.num_proccessed_instance = 0;
        this.curNumOfHypo = 0;
        this.hypo = new Classifier[numOfHypo+1];
        glob_weight = new double[numOfHypo];
        for(int i=0; i< hypo.length ; i++){
            hypo[i] = (Classifier) getPreparedClassOption(this.baseLearnerOption);
            hypo[i].resetLearning();
            hypo[i].prepareForUse();
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance instnc) {
             
        double[][] votes = new double[curNumOfHypo][numOfClasses];
        for(int i=0; i<curNumOfHypo ; i++){
            double[] vote = normalizeVotes(hypo[i].getVotesForInstance(instnc));
            System.arraycopy(vote, 0, votes[i], 0, vote.length);            
        }
        
        if(num_proccessed_instance<fixedWindowPeriod)
            votes = null;
        
        window.add(instnc, votes);
        this.num_proccessed_instance++;
                
        if(this.num_proccessed_instance % fixedWindowPeriod == 0){ //chunk is full
            processChunk();
        }
        
    }
    
    /**
     * Performs the normalization operations explained in the paper
     * @param votes a vector of votes obtained from a ensemble
     * @return the normalized votes.
     */
    public double[] normalizeVotes(double[] votes){
        double[] newVotes = new double[numOfClasses];
        //check if all values are zero
        boolean allZero = true;
        for(int i=0; i<votes.length; i++){
            if(votes[i]>0)
                allZero=false;
        }
        
        if(allZero){ // all the votes are equal to zero
            double equalVote = 1.0/numOfClasses;
            for(int i=0; i<numOfClasses; i++){
                newVotes[i]=equalVote;
            }
        }else{ // votes are not equal to zero
            double sum=0;
            for(int i=0; i<votes.length; i++){
                sum+=votes[i];
            }
            for(int i=0; i<votes.length; i++){
                newVotes[i]=(votes[i]/sum);
            }
        }
        return newVotes;                
    }
    
    
    /**
     * process a new given chunk of instances called when the chunk is full
     */
    private void processChunk() {
        //train new classifier on this new chunk
        for(int i=fixedWindowPeriod; i>0; i--){
            hypo[numOfHypo].trainOnInstance(window.getIns(i-1));
        }
        
        // weight and train new and rest classifiers 
        if(curNumOfHypo==0) { //there is no one
            candidateIndex = curNumOfHypo;
            hypo[candidateIndex] = (Classifier) hypo[numOfHypo].copy();
            glob_weight[candidateIndex] = 1.0;
            curNumOfHypo++;
            
        } else if (curNumOfHypo < numOfHypo) { //still has space 
            
            candidateIndex = curNumOfHypo;
            hypo[candidateIndex] = (Classifier) hypo[numOfHypo].copy();
            glob_weight[candidateIndex] = 1.0;
            double[] newWights = window.getWeight();
            System.arraycopy(newWights, 0, glob_weight, 0, newWights.length);
            curNumOfHypo++;
            
        } else { // is full
            
            glob_weight = window.getWeight();
            //find minimum weight
            candidateIndex = 0;
            for(int i=1; i<glob_weight.length ; i++){
                if(glob_weight[i]<glob_weight[candidateIndex])
                    candidateIndex = i;
            }
            //substitutue
            hypo[candidateIndex] = (Classifier) hypo[numOfHypo].copy();
            glob_weight[candidateIndex] = 1.0;
        }

        //  train the rest of classifiers 
        for(int i=0;i<curNumOfHypo;i++){            
            //if(i==candidateIndex) // do not train candidate hypo again
            //    continue;            
            for(int j=0; j<fixedWindowPeriod; j++){
                hypo[i].trainOnInstance(window.getIns(j));
            }
        }
        
        hypo[numOfHypo].resetLearning();
    }
    
    
    @Override
    public boolean correctlyClassifies(Instance inst) {
        int expectedClass = Utils.maxIndex(getVotesForInstance(inst));
        int realClass = (int) inst.classValue();        
        
        return expectedClass == realClass;
    }
    
    /**
    * Vote aggregation operation to combine votes of different classifiers in the GOOWE ensemble
    * @param instnc a test example 
    * @return aggregated votes 
    */
    @Override
    public double[] getVotesForInstance(Instance instnc) {
        DoubleVector combinedVote = new DoubleVector();        
        double[] hypo_weight = glob_weight; 
        
        for (int i = 0; i < curNumOfHypo; i++) {
            DoubleVector vote = new DoubleVector(hypo[i].getVotesForInstance(instnc));                        
            if (vote.sumOfValues() > 0.0) {
                vote.normalize();
                vote.scaleValues(hypo_weight[i]);
                combinedVote.addValues(vote);
            }
        }        
        return combinedVote.getArrayRef();
    }
    
    
   
    @Override
    public boolean isRandomizable() {
        return true;
    }   
    
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getModelDescription(StringBuilder sb, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
   
    
    /**
    * A class for handling equation solving according to GOOWE paper Aw=d to find weights
    */
    private class InstanceList {
        int capacity; // fix window size
        int num_of_inst; // till now how many instances are there 
        int num_of_hypo;
        Instance[] list;
        VoteNode[] voteList;

        int not_unique;
        int unique;

        public InstanceList(int capacity) {

            not_unique = 0;
            unique = 0;

            this.capacity = capacity;
            num_of_inst = 0;
            num_of_hypo = 0;

            list = new Instance[capacity];
            voteList = new VoteNode[capacity];
        }

        public void add(Instance ins, double[][] votes) {

            for (int i = (list.length - 1); i > 0; i--) {
                list[i] = list[i - 1];
                voteList[i] = voteList[i - 1];
            }
            list[0] = (Instance) ins.copy();
            voteList[0] = new VoteNode(votes, (int) ins.classValue());

            if (votes == null) {
                num_of_hypo = 0;
            } else {
                num_of_hypo = votes.length;
            }

            if (num_of_inst < capacity) {
                num_of_inst++;
            }
        }

        public Instance getIns(int index) {
            return list[index];
        }

        public VoteNode getVote(int index) {
            return voteList[index];
        }

        public boolean isReady() {
            return num_of_inst >= capacity && num_of_hypo >= 2;
        }

        // calculate weights for current instances
        public double[] getWeight() {

            double[] weights;
            if (!this.isReady()) {
                weights = new double[num_of_hypo];
                for (int i = 0; i < num_of_hypo; i++) {
                    weights[i] = 1.0;
                }
                return weights;
            }

            double[][] A = new double[num_of_hypo][num_of_hypo];
            double[] D = new double[num_of_hypo];

            for (int i = 0; i < capacity; i++) {
                for (int q = 0; q < num_of_hypo; q++) {
                    for (int j = 0; j < num_of_hypo; j++) {
                        A[q][j] += (voteList[i].getAt())[q][j];
                    }
                    D[q] += (voteList[i].getDt())[q];
                }
            }

            // get optimum weights by solving the linear equation
            weights = matrixSolver(A, D);

            //normalizing with "standart normalization"
            double min = weights[0];
            double max = weights[0];
            for (int i = 1; i < weights.length; i++) {
                if (weights[i] < min) {
                    min = weights[i];
                }
                if (weights[i] > max) {
                    max = weights[i];
                }
            }
            for (int i = 0; i < weights.length; i++) {
                weights[i] = ((weights[i] - min) / (max - min));
            }

            return weights;

        }

        private double[] matrixSolver(double[][] a, double[] d) {
            //preparing matrix objects for JAMA package
            double[][] di = new double[d.length][1];
            for (int i = 0; i < d.length; i++) {
                di[i][0] = d[i];
            }
            Matrix A = new Matrix(a);
            Matrix D = new Matrix(di);
            double[][] res;

            //solve equation and change the result to sensible weight vector 
            double[] w = new double[d.length];
            try {
                Matrix x = A.solve(D);
                res = x.getArray();
                for (int i = 0; i < w.length; i++) {
                    w[i] = res[i][0];
                }
                unique++;
            } catch (RuntimeException e) {
                //System.out.println("Not a unique solution!!!! " + GOOWTester.index);
                for (int i = 0; i < w.length; i++) {
                    w[i] = 1.0;
                }
                not_unique++;
            }

            return w;
        }

        // votes for an Instance produced by different classifiers 
        private class VoteNode {

            private final double[][] At; // matrix A for instance It
            private final double[] Dt;   // matrix D for instance It
            private final int num_of_cur_hypos;
            private final int num_of_classes;

            //private double[][] votes;
            public VoteNode(double[][] votes, int classIndex) { //class index should start from zero
                //this.votes = votes;
                if (votes == null) {
                    num_of_cur_hypos = 0;
                    num_of_classes = 0;
                } else {
                    num_of_cur_hypos = votes.length;
                    num_of_classes = votes[0].length;
                }

                At = new double[num_of_cur_hypos][num_of_cur_hypos];
                Dt = new double[num_of_cur_hypos];

                for (int i = 0; i < num_of_cur_hypos; i++) {
                    for (int j = i; j < num_of_cur_hypos; j++) {
                        double ss = 0;
                        for (int k = 0; k < num_of_classes; k++) {
                            ss += (votes[i][k] * votes[j][k]);
                        }
                        At[i][j] = ss;
                        At[j][i] = ss;
                    }
                    Dt[i] = votes[i][classIndex];
                }
            }

            public double[][] getAt() {
                return At;
            }

            public double[] getDt() {
                return Dt;
            }

            public boolean ensembleIsReady() {
                return num_of_cur_hypos >= 2;
            }

        }// end of VoteNode

    }
    
}
