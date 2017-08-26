/*
 *    MeritFeatureRanking.java
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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.featureranking.messages.MeritCheckMessage;
import moa.classifiers.rules.featureranking.messages.RuleExpandedMessage;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

/**
 * Merit Feature Ranking method
 * João Duarte, João Gama,Feature ranking in hoeffding algorithms for regression. SAC 2017: 836-841
 */


public class MeritFeatureRanking extends AbstractFeatureRanking implements FeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	

	protected DoubleVector attributeImportance;
	protected HashMap<ObservableMOAObject, RuleInformation> ruleInformation;
	


	public MeritFeatureRanking() {
		super();
		this.attributeImportance = new DoubleVector();
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
			DoubleVector merits=ri.getCurrent();
			if(!ri.isFirstAfterExpansion())
				this.attributeImportance.subtractValues(merits);
			MeritCheckMessage msg = (MeritCheckMessage) arg;
			ri.updateCurrent(msg.getMerits());
			merits=ri.getCurrent();
			this.attributeImportance.addValues(merits);
		}
		else if (arg instanceof RuleExpandedMessage){
			RuleInformation ri=ruleInformation.get(o);
			if(!((RuleExpandedMessage)arg).isSpecialization())
				ri.addNumLiterals();
		}
		else if (arg instanceof ChangeDetectedMessage) {
			RuleInformation ri=ruleInformation.get(o);
			this.attributeImportance.subtractValues(ri.getAccumulated());
			ruleInformation.remove(o);		
		}
		

	}

	@Override
	public DoubleVector getFeatureRankings() {
		/*DoubleVector normRankings=null;
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
		*/
		
		if(attributeImportance==null)
			return new DoubleVector();
		return attributeImportance;
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
	
	
	/*
	 * Rule information class
	 */
	public class RuleInformation{
		private DoubleVector accumulated;
		private DoubleVector current;
		//private HashMap<Integer, Integer> literalAttributes;
		private boolean isFirstAfterExpansion;
		private int numLiterals;
		
		public RuleInformation() {
			//literalAttributes=new HashMap<Integer, Integer>();
			isFirstAfterExpansion=false;
			accumulated=new DoubleVector();
			current=new DoubleVector();
			numLiterals=0;
		}

		public DoubleVector getAccumulated() {
			return accumulated;
		}
		
		public DoubleVector getCurrent() {
			return current;
		}

		public void updateCurrent(DoubleVector merits){
			DoubleVector newMerits=new DoubleVector(merits);
			if(!isFirstAfterExpansion){
				accumulated.subtractValues(current);
			}
			newMerits.scaleValues(1.0/(1+numLiterals));
			accumulated.addValues(newMerits);
			current=newMerits;
			isFirstAfterExpansion=false;
		}

		public void addNumLiterals() {
			/*boolean contains=false;
			Iterator<Integer> it=literalAttributes.iterator();
			while (it.hasNext() && !contains){
				if(it.next()==attribIndex)
					contains=true;
			}
			if(!contains){
				literalAttributes.add(attribIndex);
			}*/
			this.numLiterals++;
			isFirstAfterExpansion=true;
		}

		public boolean isFirstAfterExpansion() {
			return isFirstAfterExpansion;
		}
	
	}

}
