package moa.classifiers.rules.featureranking;

import java.util.HashMap;

import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.featureranking.messages.RuleExpandedMessage;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

public class BasicFeatureRanking extends AbstractFeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected DoubleVector attributeImportance;
	protected HashMap<ObservableMOAObject, RuleInformation> ruleInformation;


	public BasicFeatureRanking() {
		super();
		this.attributeImportance = new DoubleVector();
		this.ruleInformation= new HashMap<ObservableMOAObject, RuleInformation>();
	}

	public void update(ObservableMOAObject o, Object arg) {
		
		if (arg instanceof RuleExpandedMessage){
			
			RuleInformation ri=ruleInformation.get(o);
			if(ri==null)
			{
				ri=new RuleInformation();
				ruleInformation.put(o, ri);
			}
			RuleExpandedMessage msg=((RuleExpandedMessage)arg);
			int index=msg.getAttributeIndex();
			if(!msg.isSpecialization()){
				ri.addMerit(index,1.0);	
				this.attributeImportance.addToValue(index, 1.0);
			}
				
		}
		else if (arg instanceof ChangeDetectedMessage) {
			RuleInformation ri=ruleInformation.get(o);
			DoubleVector accumulatedMerit = ri.getAccumulatedMerit();
			attributeImportance.subtractValues(accumulatedMerit);
			ruleInformation.remove(o);
		}
		

	}


	

	@Override
	public DoubleVector getFeatureRankings() {
		/*DoubleVector normRankings=null;
		if(attributeImportance!=null){
			normRankings=new DoubleVector(attributeImportance.getArrayCopy());
			normRankings.normalize();
		}
		return normRankings;*/
		if(attributeImportance==null)
			return new DoubleVector();
		return attributeImportance;
	}
	

	
	public class RuleInformation{
		private DoubleVector accumulatedMerit;
		
		public RuleInformation() {
			accumulatedMerit= new DoubleVector();
		}

		
		public void addMerit(int index, double value) {
			accumulatedMerit.addToValue(index, value);
		}


		public DoubleVector getAccumulatedMerit(){
			return accumulatedMerit;
			
		}
	}


}
