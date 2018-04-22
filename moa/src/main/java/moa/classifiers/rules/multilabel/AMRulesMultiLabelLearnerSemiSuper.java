/*
 *    AMRulesMultiLabelLearnerSemiSuper.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author R. Sousa, J. Gama
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

package moa.classifiers.rules.multilabel;

import java.util.ListIterator;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.MultiLabelLearner;
import moa.classifiers.MultiTargetLearnerSemiSupervised;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.rules.core.anomalydetection.AnomalyDetector;
import moa.classifiers.rules.core.anomalydetection.OddsRatioScore;
import moa.classifiers.rules.featureranking.FeatureRanking;
import moa.classifiers.rules.featureranking.NoFeatureRanking;
import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.multilabel.attributeclassobservers.NominalStatisticsObserver;
import moa.classifiers.rules.multilabel.attributeclassobservers.NumericStatisticsObserver;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.MultiLabelRuleSet;
import moa.classifiers.rules.multilabel.core.ObserverMOAObject;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.classifiers.rules.multilabel.core.voting.ErrorWeightedVoteMultiLabel;
import moa.classifiers.rules.multilabel.errormeasurers.MultiLabelErrorMeasurer;
import moa.classifiers.rules.multilabel.inputselectors.InputAttributesSelector;
import moa.classifiers.rules.multilabel.inputselectors.SelectAllInputs;
import moa.classifiers.rules.multilabel.instancetransformers.NoInstanceTransformation;
import moa.classifiers.rules.multilabel.outputselectors.OutputAttributesSelector;
import moa.classifiers.rules.multilabel.outputselectors.SelectAllOutputs;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.options.ClassOption;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;
import java.util.LinkedList;
import java.util.List;
import moa.core.Example;
import moa.learners.Learner;
import moa.learners.LearnerSemiSupervised;


/**
* Semi-supervised method for online multi-target regression.
* This method uses the AMRules as learner and it based on probabilistic method very similar to 
* anomaly detection.
* This method measures predicts the benefit of a unlabeled example to the models(using only the input information) to the 
* model. If this benefit is higher than <scoreThreshold> then the algorithm predicts an output and 
* artificially labels the example and use it for training.  


The algorithm is fully explained in the following publication: 

*Sousa R., Gama J. (2016) Online Semi-supervised Learning for Multi-target Regression 
*in Data Streams Using AMRules. In: Boström H., Knobbe A., Soares C., Papapetrou P. (eds)
*Advances in Intelligent Data Analysis XV. IDA 2016. Lecture Notes in Computer Science, 
*vol 9897. Springer.

* @author RSousa
* @version $Revision: 2 $
*/




public abstract class AMRulesMultiLabelLearnerSemiSuper extends AbstractMultiLabelLearner implements MultiTargetLearnerSemiSupervised { 

    private static final long serialVersionUID = 1L;
    protected MultiLabelRuleSet ruleSet;
    protected MultiLabelRule defaultRule;
    protected int ruleNumberID=1;
    protected double[] statistics;
    protected ObserverMOAObject observer;

    //
    public FloatOption splitConfidenceOption = new FloatOption("splitConfidence",
                        'c',"Hoeffding Bound Parameter. The allowable error in split decision, values closer to 0 will take longer to decide.",0.0000001, 0.0, 1.0);
    public FloatOption tieThresholdOption = new FloatOption("tieThreshold",
                        't', "Hoeffding Bound Parameter. Threshold below which a split will be forced to break ties.",0.05, 0.0, 1.0);
    public IntOption gracePeriodOption = new IntOption("gracePeriod",
                        'g', "Hoeffding Bound Parameter. The number of instances a leaf should observe between split attempts.",200, 1, Integer.MAX_VALUE);   
    public ClassOption learnerOption;
    public FlagOption unorderedRulesOption = new FlagOption("setUnorderedRulesOn",
                        'U',"unorderedRules.");
    public FlagOption dropOldRuleAfterExpansionOption = new FlagOption("dropOldRuleAfterExpansion",
                        'D',"Drop old rule if it expanded (by default the rule is kept for the set of outputs not selected for expansion.)");
    public ClassOption changeDetector;

    public ClassOption anomalyDetector = new ClassOption("anomalyDetector",
                        'A', "Anomaly Detector.", AnomalyDetector.class,OddsRatioScore.class.getName());
    public ClassOption splitCriterionOption;

    public ClassOption errorMeasurerOption;

    public ClassOption weightedVoteOption;

    public ClassOption numericObserverOption = new ClassOption("numericObserver",
                        'y', "Numeric observer.", NumericStatisticsObserver.class,"MultiLabelBSTree");
    public ClassOption nominalObserverOption = new ClassOption("nominalObserver",
			'z', "Nominal observer.", 
			NominalStatisticsObserver.class,
			"MultiLabelNominalAttributeObserver");
    public IntOption VerbosityOption = new IntOption(
			"verbosity",
			'v',
			"Output Verbosity Control Level. 1 (Less) to 5 (More)",
			1, 1, 5);
    public ClassOption outputSelectorOption = new ClassOption("outputSelector",
			'O', "Output attributes selector", 
			OutputAttributesSelector.class,
			SelectAllOutputs.class.getName());
    public ClassOption inputSelectorOption = new ClassOption("inputSelector",
			'I', "Input attributes selector", 
			InputAttributesSelector.class,
			SelectAllInputs.class.getName());
    public IntOption randomSeedOption = new IntOption("randomSeedOption",
			'r', "randomSeedOption", 
			1,Integer.MIN_VALUE, Integer.MAX_VALUE);	
    public ClassOption featureRankingOption = new ClassOption("featureRanking",
			'F', "Feature ranking algorithm.", 
			FeatureRanking.class,
			NoFeatureRanking.class.getName());
    public FloatOption scoreThreshold = new FloatOption("scoreThreshold",
                        'h', "Initial dataset (%)", 1);
    public IntOption  slidingWindowSize= new IntOption("slidingWindowSize",
                        'W', "slidingWindowSize", 1000);
    public IntOption  slidingWindowStep= new IntOption("slidingWindowStep",
                        'j', "slidingWindowStep", 1);
    
    private int nAttributes=0;
	
    protected double attributesPercentage;

    public double getAttributesPercentage() {
        return attributesPercentage;
    }

    public void setAttributesPercentage(double attributesPercentage) {
        this.attributesPercentage = attributesPercentage;
    }

    public AMRulesMultiLabelLearnerSemiSuper() {
        super();
        super.randomSeedOption=this.randomSeedOption;
        attributesPercentage=100;
    }

    public AMRulesMultiLabelLearnerSemiSuper(double attributesPercentage) {
        this();
        this.attributesPercentage=attributesPercentage;
    }


    @Override
    public Prediction getPredictionForInstance(MultiLabelInstance inst) {
        ErrorWeightedVoteMultiLabel vote=getVotes(inst);
        if(vote!=null)	
            return vote.getPrediction();
        else
            return null;
    }

    /**
    * getVotes extension of the instance method getVotesForInstance 
    * in moa.classifier.java
    * returns the prediction of the instance.
    */
    public ErrorWeightedVoteMultiLabel getVotes(MultiLabelInstance instance) {

        ErrorWeightedVoteMultiLabel errorWeightedVote=newErrorWeightedVote(); 
        //int numberOfRulesCovering = 0;
        VerboseToConsole(instance); // Verbose to console Dataset name.
        
        for (MultiLabelRule rule : ruleSet) {
            
            if (rule.isCovering(instance) == true){
                //numberOfRulesCovering++;
 
                Prediction vote=rule.getPredictionForInstance(instance);
                 
                if (vote!=null){ //should only happen for first instance
                    double [] errors= rule.getCurrentErrors();
                    if(errors==null) //if errors==null, rule has seen no predictions since expansion: return maximum error, since prediction is not reliable
                        errors=defaultRuleErrors(vote);
                    debug("Rule No"+ rule.getRuleNumberID() + " Vote: " + vote.toString() + " Error: " + errors + " Y: " + instance.classValue(),3); //predictionValueForThisRule);
                    errorWeightedVote.addVote(vote,errors);
                }
                
                if (VerbosityOption.getValue()>1){
                    System.out.print("Rule " + rule.getRuleNumberID()+ ": ");
                    for (int i=0; i< instance.numOutputAttributes() ; i++){
                        System.out.print(" " + vote.getVotes(i)[0]);
                    }
                    System.out.print("\n");
                }
 
                if (!this.unorderedRulesOption.isSet()) { // Ordered Rules Option.
                     break; // Only one rule cover the instance.
                }
            }
        }

        
        
        if(!errorWeightedVote.coversAllOutputs()) {
            //Complete Prediction (fill missing outputs with default)
            Prediction vote=errorWeightedVote.getPrediction();
                
            if (vote==null){  //use default rule
                vote = new MultiLabelPrediction(instance.numberOutputTargets());
            }
                
            Prediction defaultVote=defaultRule.getPredictionForInstance(instance);
            if(defaultVote!=null){
                
                double [] defaultErrors= defaultRule.getCurrentErrors();
                    
                if(defaultErrors==null)
                    defaultErrors=defaultRuleErrors(defaultVote);
                    
                double [] fixErrors=new double[vote.numOutputAttributes()];
                Prediction fixVote= new MultiLabelPrediction(vote.numOutputAttributes());
                for (int i=0; i<vote.numOutputAttributes(); i++){
                    if(!vote.hasVotesForAttribute(i)){
                        fixVote.setVotes(i, defaultVote.getVotes(i));
                        fixErrors[i]=defaultErrors[i];
                    }
                }
                errorWeightedVote.addVote(fixVote,fixErrors);
                debug("Default Rule Vote " + defaultVote.toString() + "\n Error " + defaultErrors + "  Y: " + instance,3);
            }
        } 	
        errorWeightedVote.computeWeightedVote();
            
        return errorWeightedVote;
    }

    /*
    * Returns the estimate error for each output of a rule
    * Should be used when rule.getCurrentErrors() returns null
    */
    protected double[] defaultRuleErrors(Prediction vote) {
        double [] errors=new double[vote.numOutputAttributes()];
        for(int i=0; i<vote.numOutputAttributes(); i++){
            if(vote.hasVotesForAttribute(i))
                errors[i]=Double.MAX_VALUE;
            }
        return errors;
    }

    @Override
    public boolean isRandomizable(){
        return true;
    }


    /**
    * AMRules Algorithm.
    * Method for updating (training) the AMRules model using a new instance
    */

    private double numChangesDetected;      //Just for statistics 
    private double numAnomaliesDetected;    //Just for statistics 
    private double numInstances;            //Just for statistics
    private FeatureRanking featureRanking;
    public Prediction prediction;
    public int numberTotalExamples;
    public boolean hasModel=false;
    boolean isUnlabeled;
    public AnomalyDetector anomalyDetector2;
    

    @Override
    public void trainOnInstanceImpl(MultiLabelInstance instance) {
 
        if(nAttributes==0)
            nAttributes=instance.numInputAttributes();
        
        numInstances+=instance.weight();
        debug("Train",3);
        debug("NÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âº instance "+numInstances + " - " + instance.toString(),3);
        
        boolean rulesCoveringInstance = false;
        ListIterator<MultiLabelRule> ruleIterator= this.ruleSet.listIterator();

        numberTotalExamples++;
        
        prediction= new MultiLabelPrediction(instance.numOutputAttributes());
        boolean isUnlabeledRejected=true; 

        isUnlabeled=false;
        for(int m=0; m < instance.numOutputAttributes() ; m++){
            if(instance.valueOutputAttribute(m)==Double.NEGATIVE_INFINITY){ 
                isUnlabeled=true;
                break;
            } 
        }

        if(numberTotalExamples==1){
        	anomalyDetector2= new OddsRatioScore(); 
        }
        

        //----------------------------------------------------------------------
        while (ruleIterator.hasNext()) { 
            MultiLabelRule rule = ruleIterator.next();
            if (rule.isCovering(instance) == true) {
                rulesCoveringInstance = true;
                if (!rule.updateAnomalyDetection(instance) ) {
                    prediction=rule.getPredictionForInstance(instance);
                    if( isUnlabeled ){
                        if(rule.getAnomalyScore() >= scoreThreshold.getValue() ){
                            for( int a=0 ; a<instance.numOutputAttributes() ; a++)
                                instance.setClassValue(a,prediction.getVote(a,0));
                            isUnlabeledRejected=false; //Unlabeled rejected
                        }
                        else
                            continue;
                    }

                    //----------------------------------------------------------
                    if (rule.updateChangeDetection(instance)) {
                        debug("I) Drift Detected. Exa. : " +  numInstances + " (" + rule.getWeightSeenSinceExpansion() +") Remove Rule: " +rule.getRuleNumberID(),1);
                        ruleIterator.remove();                         //Remove a regra 
                        rule.notifyAll(new ChangeDetectedMessage());   //Rule expansion event			
                        this.numChangesDetected += instance.weight();  //Just for statistics 
                    } else {
                        
                        rule.trainOnInstance(instance); //<<<<<<<<<<<<
                        if (rule.getWeightSeenSinceExpansion()  % gracePeriodOption.getValue() == 0.0) { 
                            if (rule.tryToExpand(splitConfidenceOption.getValue(), tieThresholdOption.getValue()) ) {
                                
                                MultiLabelRule otherMultiLabelRule = rule.getNewRuleFromOtherOutputs(); //Need to be outside to make sure other rules are cleaned
                                
                                if(!dropOldRuleAfterExpansionOption.isSet() && rule.hasNewRuleFromOtherOutputs()){
                                    rule.clearOtherOutputs();
                                    otherMultiLabelRule.setRuleNumberID(++ruleNumberID);
                                    setRuleOptions(otherMultiLabelRule);
                                    ruleIterator.add(otherMultiLabelRule);
                                    if(observer!=null)
                                        otherMultiLabelRule.addObserver(observer);
                                }
                                setRuleOptions(rule);
                                debug("Rule Expanded:",2);
                                debug(rule.toString(),2);
                            }	
                        }
                    }
                }
                else {
                    debug("Anomaly Detected: " + numInstances + " Rule: " +rule.getRuleNumberID() ,1);
                    numAnomaliesDetected+=instance.weight();//Just for statistics
                }
  
                // Testa se  ordered ou unordered
                if (!unorderedRulesOption.isSet()) 
                    break;
            }

        }

        //----------------------------------------------------------------------

        if (rulesCoveringInstance == false){
            
            isUnlabeledRejected=false;
            
            for(int m=0; m < instance.numOutputAttributes() ; m++){
                if(instance.valueOutputAttribute(m)==Double.NEGATIVE_INFINITY ){
                    
                    prediction=defaultRule.getPredictionForInstance(instance);
                            
                    for( int a=0 ; a<instance.numOutputAttributes() ; a++){
                        instance.setClassValue(a,prediction.getVote(a,0)); 
                    }
                    break;
                }            
             }
  
            defaultRule.trainOnInstance(instance);

            if (defaultRule.getWeightSeenSinceExpansion() % this.gracePeriodOption.getValue() == 0.0) {
                debug("Nr. examples "+defaultRule.getWeightSeenSinceExpansion(), 4);
                if ( defaultRule.tryToExpand(this.splitConfidenceOption.getValue(), this.tieThresholdOption.getValue()) == true) {
                    MultiLabelRule newDefaultRule=defaultRule.getNewRuleFromOtherBranch();
                    newDefaultRule.setRuleNumberID(++ruleNumberID);
                    setRuleOptions(newDefaultRule);
                    setRuleOptions(defaultRule);
                    ruleSet.add(defaultRule);
                    debug("Default rule expanded! New Rule:",2);
                    debug(defaultRule.toString(),2);
                    debug("New default rule:", 3);	
                    debug(newDefaultRule.toString(),3);
                    defaultRule=newDefaultRule;
                    if(observer!=null)
                        defaultRule.addObserver(observer);
                }
            }
        }
        //----------------------------------------------------------------------
    }   



	/**
	 * print GUI evaluate model	
	 */
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		Measurement[] m=null;
		Measurement[] mNoFeatureRanking=new Measurement[]{
				new Measurement("anomaly detections", this.numAnomaliesDetected),
				new Measurement("change detections", this.numChangesDetected), 
				new Measurement("rules (number)", this.ruleSet.size()+1),
				new Measurement("Avg #inputs/rule", getAverageInputs()),
				new Measurement("Avg #outputs/rule", getAverageOutputs())}; 
		
		if(featureRanking instanceof NoFeatureRanking){
			m=mNoFeatureRanking;
		}
		else{
			m=new Measurement[mNoFeatureRanking.length+this.nAttributes];
			for(int i=0; i<mNoFeatureRanking.length; i++)
				m[i]=mNoFeatureRanking[i];
			DoubleVector rankings=this.featureRanking.getFeatureRankings();
			for(int i=0; i<this.nAttributes; i++)
				m[i+mNoFeatureRanking.length]=new Measurement("Attribute" + i, rankings.getValue(i));
		}
		return m;
	}

	protected double getAverageInputs() {
		double avg=0;
		int ct=0;
		if(ruleSet.size()>0){
			for (MultiLabelRule r : ruleSet){
				int [] aux=r.getInputsCovered();
				if(aux!=null){
					avg+=aux.length;
					ct++;
				}
			}
		}
		int [] aux=defaultRule.getInputsCovered();
		if(aux!=null){
			avg+=aux.length;
			ct++;
		}
		if(ct>0)
			avg/=ct;
		return avg;
	}

	protected double getAverageOutputs() {
		double avg=0;
		int ct=0;
		if(ruleSet.size()>0){
			for (MultiLabelRule r : ruleSet){
				int [] aux=r.getOutputsCovered();
				if(aux!=null){
					avg+=aux.length;
					ct++;
				}
			}
		}
		int [] aux=defaultRule.getOutputsCovered();
		if(aux!=null){
			avg+=aux.length;
			ct++;
		}
		if(ct>0)
			avg/=ct;
		return avg;
	}

	/**
	 * print GUI learn model	
	 */
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		if(!this.unorderedRulesOption.isSet()){
			StringUtils.appendIndented(out, indent, "Method Ordered");
			StringUtils.appendNewline(out);
		}else{
			StringUtils.appendIndented(out, indent, "Method Unordered");
			StringUtils.appendNewline(out);
		}
		StringUtils.appendIndented(out, indent, "Number of Rules: " + (this.ruleSet.size()+1));
		StringUtils.appendNewline(out);

		StringUtils.appendIndented(out, indent, "Default rule :");
		this.defaultRule.getDescription(out, indent);
		StringUtils.appendNewline(out);

		StringUtils.appendIndented(out, indent, "Rules in ruleSet:");
		StringUtils.appendNewline(out);
		for (MultiLabelRule rule: ruleSet) {
			rule.getDescription(out, indent);
			StringUtils.appendNewline(out);
		}
	}

	/**
	 * Print to console
	 * @param string
	 */
	protected void debug(String string, int level) {
		if (VerbosityOption.getValue()>=level){
			System.out.println(string); 
		}
	}

	protected void VerboseToConsole(MultiLabelInstance inst) {
		if(VerbosityOption.getValue()>=5){	
			System.out.println(); 
			System.out.println("I) Dataset: "+inst.dataset().getRelationName()); 

			if(!this.unorderedRulesOption.isSet()){ 
				System.out.println("I) Method Ordered");
			}else{
				System.out.println("I) Method Unordered");
			}
		}    	
	}

	public void PrintRuleSet() {    
		debug("Default rule :",2);
		debug(this.defaultRule.toString(),2);

		debug("Rules in ruleSet:",2);
		for (MultiLabelRule rule: ruleSet) {
			debug(rule.toString(),2);
		}
	}


	@Override
	public void resetLearningImpl() {
            
		defaultRule=newDefaultRule();
		this.classifierRandom.setSeed(this.randomSeed);
		MultiLabelLearner l = (MultiLabelLearner)((MultiLabelLearner)getPreparedClassOption(learnerOption)).copy();

                l.setRandomSeed(this.randomSeed);
                
		l.resetLearning();
		defaultRule.setLearner(l);
		defaultRule.setInstanceTransformer(new NoInstanceTransformation());
		setRuleOptions(defaultRule);
		ruleSet = new MultiLabelRuleSet();
		ruleNumberID=1;
		statistics=null;
		this.featureRanking=(FeatureRanking) getPreparedClassOption(this.featureRankingOption);
		setObserver(featureRanking);
                
	}


	protected void setRuleOptions(MultiLabelRule rule){
		rule.setSplitCriterion((MultiLabelSplitCriterion)((MultiLabelSplitCriterion)getPreparedClassOption(splitCriterionOption)).copy());
		rule.setChangeDetector((ChangeDetector)((ChangeDetector)getPreparedClassOption(changeDetector)).copy());
		rule.setAnomalyDetector((AnomalyDetector)((AnomalyDetector)getPreparedClassOption(anomalyDetector)).copy());
		rule.setNumericObserverOption((NumericStatisticsObserver)((NumericStatisticsObserver)getPreparedClassOption(numericObserverOption)).copy());
		rule.setNominalObserverOption((NominalStatisticsObserver)((NominalStatisticsObserver)getPreparedClassOption(nominalObserverOption)).copy());
		rule.setErrorMeasurer((MultiLabelErrorMeasurer)((MultiLabelErrorMeasurer)getPreparedClassOption(errorMeasurerOption)).copy());
		rule.setOutputAttributesSelector((OutputAttributesSelector)((OutputAttributesSelector)getPreparedClassOption(outputSelectorOption)).copy());
		rule.setRandomGenerator(this.classifierRandom);
		rule.setAttributesPercentage(this.attributesPercentage);
		rule.setInputAttributesSelector((InputAttributesSelector)((InputAttributesSelector)getPreparedClassOption(inputSelectorOption)).copy());
	}

	abstract protected MultiLabelRule newDefaultRule();

	public ErrorWeightedVoteMultiLabel newErrorWeightedVote(){
		return (ErrorWeightedVoteMultiLabel)((ErrorWeightedVoteMultiLabel) getPreparedClassOption(weightedVoteOption)).copy();
	}



	public void setRandomSeed(int randomSeed){
		super.setRandomSeed(randomSeed);
		this.classifierRandom.setSeed(randomSeed);
	}
	
	public void setObserver(ObserverMOAObject observer){
		this.observer=observer;
		this.defaultRule.addObserver(observer);
	}
        
        public Prediction getTrainingPrediction(){
                return prediction;
        }

}