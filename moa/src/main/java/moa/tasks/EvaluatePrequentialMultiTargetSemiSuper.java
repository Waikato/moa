/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import moa.classifiers.MultiTargetRegressor;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.EWMAClassificationPerformanceEvaluator;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.MultiTargetPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.learners.Learner;
import moa.learners.LearnerSemiSupervised;
import moa.classifiers.MultiTargetLearnerSemiSupervised;
import moa.classifiers.MultiLabelLearner;

import moa.options.ClassOption;
import moa.streams.ExampleStream;
import moa.streams.MultiTargetInstanceStream;
import static moa.tasks.MainTask.INSTANCES_BETWEEN_MONITOR_UPDATES;

import java.util.Random;

import moa.classifiers.rules.multilabel.AMRulesMultiLabelLearnerSemiSuper;
//import moa.classifiers.rules.multilabel.AMRulesMultiLabelLearnerSemiSuperDualPerturb;

import java.util.LinkedList;
import java.util.List;
import java.io.*;

/**
 *
 * @author RSousa
 */

/*
 *    EvaluatePrequentialMultiTarget.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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


public class EvaluatePrequentialMultiTargetSemiSuper extends MultiTargetMainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 
                'l',"Learner to train.", MultiTargetLearnerSemiSupervised.class, "moa.classifiers.rules.multilabel.AMRulesMultiTargetRegressorSemiSuper");
    public ClassOption streamOption = new ClassOption("stream", 
                's',"Stream to learn from.", MultiTargetInstanceStream.class,"MultiTargetArffFileStream");
    public ClassOption evaluatorOption = new ClassOption("evaluator", 
                'e',"Classification performance evaluation method.",MultiTargetPerformanceEvaluator.class,"BasicMultiTargetPerformanceEvaluator");
    public IntOption instanceLimitOption = new IntOption("instanceLimit", 
                'i',"Maximum number of instances to test/train on  (-1 = no limit).",100000000, -1, Integer.MAX_VALUE);
    public IntOption timeLimitOption = new IntOption("timeLimit", 
                't',"Maximum number of seconds to test/train for (-1 = no limit).", -1,-1, Integer.MAX_VALUE);
    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
                'f',"How many instances between samples of the learning performance.",100000, 0, Integer.MAX_VALUE);
    public IntOption memCheckFrequencyOption = new IntOption("memCheckFrequency", 
                'q',"How many instances between memory bound checks.", 100000, 0,Integer.MAX_VALUE);
    public FileOption dumpFileOption = new FileOption("dumpFile", 
                'd',"File to append intermediate csv results to.", null, "csv", true);
    public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 
                'o',"File to append output predictions to.", null, "pred", true);
    public IntOption widthOption = new IntOption("width",
                'w', "Size of Window", 1000); //New for prequential method DEPRECATED
    public FloatOption alphaOption = new FloatOption("alpha",
                'a', "Fading factor or exponential smoothing factor", .01);
    public FloatOption unlabeledPercentage = new FloatOption("WithoutTarget",
                'z', "Without target percentage(%)", 50);
    public FloatOption dbInitialModelPercentage = new FloatOption("DBPercent",'D', "Initial dataset (%)", 30);
    public IntOption runSeed = new IntOption("Seed",
                'r', "Number of predictions",1);
    public IntOption slidingWindowSize = new IntOption("slidingWindowSize",
                'W', "slidingWindowSize",1000);
    public IntOption slidingWindowStep = new IntOption("slidingWindowStep",
                'j', "slidingWindowStep",1);
    
    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        
        MultiTargetLearnerSemiSupervised learner = (MultiTargetLearnerSemiSupervised) getPreparedClassOption(this.learnerOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        LearningCurve learningCurve = new LearningCurve("learning evaluation instances");

        
        //New for prequential methods
        if (evaluator instanceof WindowClassificationPerformanceEvaluator) {
            //((WindowClassificationPerformanceEvaluator) evaluator).setWindowWidth(widthOption.getValue());
            if (widthOption.getValue() != 1000) {
                System.out.println("DEPRECATED! Use EvaluatePrequential -e (WindowClassificationPerformanceEvaluator -w " + widthOption.getValue() + ")");
                 return learningCurve;
            }
        }
        if (evaluator instanceof EWMAClassificationPerformanceEvaluator) {
            //((EWMAClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
            if (alphaOption.getValue() != .01) {
                System.out.println("DEPRECATED! Use EvaluatePrequential -e (EWMAClassificationPerformanceEvaluator -a " + alphaOption.getValue() + ")");
                return learningCurve;
            }
        }
        if (evaluator instanceof FadingFactorClassificationPerformanceEvaluator) {
            //((FadingFactorClassificationPerformanceEvaluator) evaluator).setalpha(alphaOption.getValue());
            if (alphaOption.getValue() != .01) {
                System.out.println("DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a " + alphaOption.getValue() + ")");
                return learningCurve;
            }
        }
        
        //End New for prequential methods
        learner.setModelContext(stream.getHeader());
        int maxInstances = this.instanceLimitOption.getValue();
        long instancesProcessed = 0;
        int maxSeconds = this.timeLimitOption.getValue();
        int secondsElapsed = 0;
        monitor.setCurrentActivity("Evaluating learner...", -1.0);

        
        File dumpFile = this.dumpFileOption.getFile();
        PrintStream immediateResultStream = null;
        if (dumpFile != null) {
            try {
                if (dumpFile.exists()) {
                    immediateResultStream = new PrintStream(new FileOutputStream(dumpFile, true), true);
                } else {
                    immediateResultStream = new PrintStream(new FileOutputStream(dumpFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unable to open immediate result file: " + dumpFile, ex);
            }
        }
        
        //File for output predictions
        File outputPredictionFile = this.outputPredictionFileOption.getFile();
        PrintStream outputPredictionResultStream = null;
        if (outputPredictionFile != null) {
            try {
                if (outputPredictionFile.exists()) {
                    outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile, true), true);
                } else {
                    outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unable to open prediction result file: " + outputPredictionFile, ex);
            }
        }
        
        boolean firstDump = true;
        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        long lastEvaluateStartTime = evaluateStartTime;
        double RAMHours = 0.0;
        
        
        
        //======================================================================
        Random randomGenerator1 = new Random(runSeed.getValue());   //Examples scrambler
        Random randomGenerator2 = new Random(1);                    //Labeled/Unlabeled selector
        List<Double> slidingWindow = new LinkedList<Double>();
        
        //Compute stream size
        int StrmDtSz=0;
        while (stream.hasMoreInstances()==true){
            stream.nextInstance();
            StrmDtSz++;
        }

        //System.out.print("EvaluateMultiTargetSemiUnsupervised: StrmDtSz " + StrmDtSz +"\n");
        
        Example [] streamData = new Example[StrmDtSz];
        int[] randIndex= new int[StrmDtSz];
        
        int i=0;
        stream.restart();
        while (stream.hasMoreInstances()){
            streamData[i]=stream.nextInstance();
            i++;
        }
         
        for (int ri=0; ri < StrmDtSz ; ri++)
            randIndex[ri] = ri;
 
        int randomIndex; // the randomly selected index each time through the loop
        int randomValue; // the value at nums[randomIndex] each time through the loop
        
        //Randomize order of examples
        if( runSeed.getValue()> 0 ){
            for( int ri = 0; ri < randIndex.length; ++ri){
                randomIndex = randomGenerator1.nextInt(randIndex.length);
                randomValue = randIndex[randomIndex];
                randIndex[randomIndex] = randIndex[ri];
                randIndex[ri] = randomValue;
            } 
        }
        
        
        //TRAIN  initial Model 
        //================================================================= 
        double errorAllSum=0;
        int examplesCounter=0;
        Example trainInst=streamData[randIndex[0]]; 
        Example testInst= (Example) trainInst;
        Instance inst= (Instance) testInst.getData();
        
        //System.out.format("Test Start at %d \n",(int)(dbInitialModelPercentage.getValue()/100*StrmDtSz));

        while( examplesCounter < dbInitialModelPercentage.getValue()/100*StrmDtSz ){
        	
            trainInst =streamData[randIndex[examplesCounter]]; 
            testInst = (Example) trainInst;
            inst= (Instance) testInst.getData();
            examplesCounter++;
            learner.trainOnInstance(trainInst);
            
            if(examplesCounter>1){
                Prediction trainPrediction =learner.getTrainingPrediction();
                double sumDenominator=0;double sumNumerator=0;
                for( int m=0 ; m<inst.numOutputAttributes() ; m++){
                    //sumNumerator+=Math.pow( inst.valueOutputAttribute(m) - learner.prediction.getVote(m,0) , 2 );
                    sumNumerator+=Math.pow( inst.valueOutputAttribute(m) - trainPrediction.getVote(m,0) , 2 );
                    sumDenominator+=Math.pow( inst.valueOutputAttribute(m) , 2 );
                }
                errorAllSum+=Math.sqrt(sumNumerator/sumDenominator);
                
                slidingWindow.add(Math.sqrt(sumNumerator/sumDenominator));
                if(slidingWindow.size()==slidingWindowSize.getValue()+1)
                    slidingWindow.remove(0);
            }
            
            if( examplesCounter % slidingWindowStep.getValue() == 0 ){
                //System.out.format(" %.4f" + " ",(double)errorAllSum/(double)examplesCounter);
                double windowMean=0;
                for(int j=0; j<slidingWindow.size() ; j++)
                    windowMean+=slidingWindow.get(j);
                //System.out.format(" %.4f" + "\n",windowMean/slidingWindow.size());
            }
        }

        double [] exampleOutputs= new double[inst.numOutputAttributes()];

        
        
        
        //TEST 
        //======================================================================
        while (examplesCounter<StrmDtSz-1) {
            
            trainInst =streamData[randIndex[examplesCounter]];
            testInst = (Example) trainInst;
            inst= (Instance) testInst.getData();

            Prediction prediction = learner.getPredictionForInstance(testInst);
            evaluator.addResult(testInst,prediction);
            
            
            /*for(int m=0; m < inst.numOutputAttributes() ; m++){
                exampleOutputs[m]=inst.valueOutputAttribute(m);
            }*/
            
            //Labeled or unlabeled imposition
            if( randomGenerator2.nextDouble() <= unlabeledPercentage.getValue()/100 ){ 
                for(int m=0; m < inst.numOutputAttributes() ; m++){
                    inst.setClassValue(m,Double.NEGATIVE_INFINITY); //Double.NaN
                }
            }
               
            examplesCounter++;
            learner.trainOnInstance(trainInst);

            //MONITORING 
            //======================================================================
            instancesProcessed++;
            if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
                    ) {   //|| stream.hasMoreInstances() == false
                long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
                double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
                double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
                RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours
                RAMHours += RAMHoursIncrement;
                lastEvaluateStartTime = evaluateTime;
                learningCurve.insertEntry(new LearningEvaluation(
                        new Measurement[]{
                            new Measurement(
                            "learning evaluation instances",
                            instancesProcessed),
                            new Measurement(
                            "evaluation time ("
                            + (preciseCPUTiming ? "cpu "
                            : "") + "seconds)",
                            time),
                            new Measurement(
                            "model cost (RAM-Hours)",
                            RAMHours)
                        },
                        evaluator, learner));

                if (immediateResultStream != null) {
                    if (firstDump) {
                        immediateResultStream.println(learningCurve.headerToString());
                        firstDump = false;
                    }
                    immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
                    immediateResultStream.flush();
                }
            }
            if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                if (monitor.taskShouldAbort()) {
                    return null;
                }
                long estimatedRemainingInstances = stream.estimatedRemainingInstances();
                if (maxInstances > 0) {
                    long maxRemaining = maxInstances - instancesProcessed;
                    if ((estimatedRemainingInstances < 0)
                            || (maxRemaining < estimatedRemainingInstances)) {
                        estimatedRemainingInstances = maxRemaining;
                    }
                }
                monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
                        : (double) instancesProcessed
                        / (double) (instancesProcessed + estimatedRemainingInstances));
                if (monitor.resultPreviewRequested()) {
                    monitor.setLatestResultPreview(learningCurve.copy());
                }
                secondsElapsed = (int) TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                        - evaluateStartTime);
            }
            
            
        }

        //|| stream.hasMoreInstances() == false
        long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
        double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
        double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
        RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours
        RAMHours += RAMHoursIncrement;
        lastEvaluateStartTime = evaluateTime;
        learningCurve.insertEntry(new LearningEvaluation(
                new Measurement[]{
                    new Measurement(
                    "learning evaluation instances",
                    instancesProcessed),
                    new Measurement(
                    "evaluation time ("
                    + (preciseCPUTiming ? "cpu "
                    : "") + "seconds)",
                    time),
                    new Measurement(
                    "model cost (RAM-Hours)",
                    RAMHours)
                },
                evaluator, learner));

        if (immediateResultStream != null) {
            if (firstDump) {
                immediateResultStream.println(learningCurve.headerToString());
                firstDump = false;
            }
            immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
            immediateResultStream.flush();
        }

        StringBuilder sb= new StringBuilder();
        learner.getDescription(sb, 0);
        System.out.println(sb.toString());
        if (immediateResultStream != null) {
            immediateResultStream.close();
        }
        if (outputPredictionResultStream != null) {
            outputPredictionResultStream.close();
        }

        return learningCurve;
    }
}


