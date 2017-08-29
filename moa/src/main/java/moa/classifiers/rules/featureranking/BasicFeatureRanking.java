/*
 *    BasicFeatureRanking.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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

package moa.classifiers.rules.featureranking;

import java.util.HashMap;

import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.featureranking.messages.RuleExpandedMessage;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

/**
 * Basic Feature Ranking method
 * João Duarte, João Gama,Feature ranking in hoeffding algorithms for regression. SAC 2017: 836-841
 */


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
