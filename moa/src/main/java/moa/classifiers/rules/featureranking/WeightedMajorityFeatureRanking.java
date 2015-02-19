package moa.classifiers.rules.featureranking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hamcrest.core.IsSame;

import com.github.javacliparser.FloatOption;

import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.featureranking.messages.MeritCheckMessage;
import moa.classifiers.rules.featureranking.messages.RuleExpandedMessage;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

public class WeightedMajorityFeatureRanking extends AbstractFeatureRanking implements FeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	

	protected DoubleVector attributeImportance;
	protected HashMap<ObservableMOAObject, RuleInformation> ruleInformation;
	
	public FloatOption meritThresholdOption = new FloatOption(
			"meritThreshold",
			'm',
			"Merit threshold value. If the merit of an input attribute is below the threshold its importance will decrease",
			0.01, 0.0, 1.0);
	

	public WeightedMajorityFeatureRanking() {
		super();
		this.ruleInformation =  new HashMap<ObservableMOAObject, RuleInformation>();
	}

	public void update(ObservableMOAObject o, Object arg) {
		
		if (arg instanceof MeritCheckMessage){
			
			RuleInformation ri=ruleInformation.get(o);
			if(ri==null)
			{
				ri=new RuleInformation();
				ruleInformation.put(o, ri);
			}

			MeritCheckMessage msg = (MeritCheckMessage) arg;
			
			//Get Learning attributes
			boolean [] attributesMask=msg.getLearningAttributes().clone(); //because it will be modified
			
			if(this.attributeImportance ==null){
				double [] init= new double[attributesMask.length];
				for (int i=0; i<attributesMask.length;i++)
					init[i]=1.0;
				this.attributeImportance = new DoubleVector(init);
			}
			
			//Cannot decrease importance the importance of attributes in the literals 
			Iterator<Integer> it=ri.getLiteralAttributes().iterator();
			while(it.hasNext())
				attributesMask[it.next()]=false;
			
			//Update for each learning attribute given the threshold value
			DoubleVector merits=msg.getMerits();
			int numLiterals=ri.getNumLiterals();
			double th=meritThresholdOption.getValue();

			for (int i=0; i<attributesMask.length;i++){
				if(merits.getValue(i)<th && attributesMask[i]){
					double currentValue=attributeImportance.getValue(i);
					attributeImportance.setValue(i, currentValue*(1+numLiterals)/(2+numLiterals));
					ri.accumulate(i,currentValue/(2+numLiterals));
				}
			}
		}
		else if (arg instanceof RuleExpandedMessage){
			RuleInformation ri=ruleInformation.get(o);
			RuleExpandedMessage msg=((RuleExpandedMessage)arg);
			int attribIndex=msg.getAttributeIndex();
			if(!msg.isSpecialization()){
				ri.addLiteralAttribute(attribIndex);
			}
			this.attributeImportance.addToValue(attribIndex, ri.getAccumulated(attribIndex));
			ri.resetDemerit(attribIndex);
		}
		else if (arg instanceof ChangeDetectedMessage) {
			RuleInformation ri=ruleInformation.get(o);
			this.attributeImportance.addValues(ri.getAccumulated());
			ruleInformation.remove(o);		
		}
		

	}

	@Override
	public DoubleVector getFeatureRankings() {
		DoubleVector normRankings=null;
		if(attributeImportance!=null){
			double total=0;
			for (int i=0; i<attributeImportance.numValues();i++)
				total+=attributeImportance.getValue(i);
			
			//
			normRankings=new DoubleVector();
			double[] aux= new double[attributeImportance.numValues()];
			for (int i=0; i<attributeImportance.numValues();i++)
				aux[i]=attributeImportance.getValue(i)/total;	
			normRankings= new DoubleVector(aux);
		}
		return normRankings;
		//return attributeImportance;
	}

	public DoubleVector getAccumulated() {
		DoubleVector accumulated= new DoubleVector();
		Iterator <Entry<ObservableMOAObject, RuleInformation>> it=this.ruleInformation.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<ObservableMOAObject, RuleInformation> pair = (Map.Entry<ObservableMOAObject, RuleInformation>)it.next();
	        accumulated.addValues(pair.getValue().getAccumulated());
	    }
		return accumulated;
	}
	
	
	/***********************************
	 * 
	 * Rule information class
	 * 
	 **********************************/
	public class RuleInformation{
		private DoubleVector accumulated;
		private int numLiterals;
		private List<Integer> literalAttributes= new LinkedList<Integer>();
		

		public RuleInformation() {
			accumulated=new DoubleVector();
			numLiterals=0;
		}

		public void addLiteralAttribute(int attribIndex) {
			literalAttributes.add(attribIndex);
			
		}

		public DoubleVector getAccumulated() {
			return accumulated;
		}
		
		public double getAccumulated(int attribIndex) {
			return accumulated.getValue(attribIndex);
		}
		
		public void accumulate(int attributeIndex, double value) {
			accumulated.addToValue(attributeIndex, value);
		}
		
		public void addNumLiterals(){
			numLiterals++;
		}

		public int getNumLiterals() {
			return numLiterals;
		}
		
		public void resetDemerit(int attribIndex) {
			accumulated.setValue(attribIndex, 0);
		}
		
		public List<Integer> getLiteralAttributes() {
			return literalAttributes;
		}


	
	}

}
