/*
 *    WeightedMajorityFeatureRanking.java
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
import java.util.LinkedList;
import java.util.List;

import moa.classifiers.rules.featureranking.messages.ChangeDetectedMessage;
import moa.classifiers.rules.featureranking.messages.MeritCheckMessage;
import moa.classifiers.rules.featureranking.messages.RuleExpandedMessage;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

import com.github.javacliparser.FloatOption;

/**
 * Weighted Majority Feature Ranking method
 * João Duarte, João Gama,Feature ranking in hoeffding algorithms for regression. SAC 2017: 836-841
 */




public class WeightedMajorityFeatureRanking extends AbstractFeatureRanking implements FeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	

	protected double [] attributeImportance;
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

			MeritCheckMessage msg = (MeritCheckMessage) arg;
			//Get Learning attributes
			boolean [] attributesMask=msg.getLearningAttributes().clone(); //because it will be modified
			
			int numAttributes=attributesMask.length;
			
			if(this.attributeImportance ==null){
				this.attributeImportance= new double[numAttributes];
				for (int i=0; i<attributesMask.length;i++)
					attributeImportance[i]=1.0;
			}
			int numRules=ruleInformation.size();
			RuleInformation ri=ruleInformation.get(o);
			//Merit check of a new rule
			if(ri==null)
			{
				ri=new RuleInformation(numAttributes);
				ruleInformation.put(o, ri);
				//new
				for (int i=0; i<numAttributes; i++)
					attributeImportance[i]=(attributeImportance[i]*numRules+1)/(numRules+1);
			}
			//Merit check of an existing rule
			else{
				double [] old=ri.getAttributesImportance().clone(); //it may change after update
				List<Integer> updated=ri.update(msg.getMerits().getArrayRef(), attributesMask, meritThresholdOption.getValue());
				
				Iterator<Integer> it = updated.iterator();
				while(it.hasNext()){
					int attIndex=it.next();

					this.attributeImportance[attIndex]=(attributeImportance[attIndex]*numRules-
							old[attIndex]+ri.getAttributeImportance(attIndex))/numRules;
				}
			}			
		}
		//Rule expanded
		else if (arg instanceof RuleExpandedMessage){
			int numRules=ruleInformation.size();
			RuleInformation ri=ruleInformation.get(o);
			RuleExpandedMessage msg=((RuleExpandedMessage)arg);
			int attIndex=msg.getAttributeIndex();

			double oldValue=ri.getAttributeImportance(attIndex);
			this.attributeImportance[attIndex]=(attributeImportance[attIndex]*numRules-oldValue+1)/numRules;
			ri.addLiteralAttribute(attIndex);
			
		}
		//Rule will be removed
		else if (arg instanceof ChangeDetectedMessage) {
			RuleInformation ri=ruleInformation.get(o);
			int numRules=ruleInformation.size();
			double [] attribImportance=ri.getAttributesImportance();
			for (int i=0; i<this.attributeImportance.length; i++)
				attributeImportance[i]=(attributeImportance[i]*numRules-attribImportance[i])/(numRules-1);
			ruleInformation.remove(o);		
		}
		

	}

	@Override
	public DoubleVector getFeatureRankings() {
		/*DoubleVector normRankings=new DoubleVector();
		if(attributeImportance!=null){
			double total=0;
			for (int i=0; i<attributeImportance.numValues();i++)
				total+=attributeImportance.getValue(i);
			
			//
			double[] aux= new double[attributeImportance.numValues()];
			for (int i=0; i<attributeImportance.numValues();i++)
				aux[i]=attributeImportance.getValue(i)/total;	
			normRankings.addValues(aux);
		}
		return normRankings;*/
		if(attributeImportance==null)
			return new DoubleVector();
		return new DoubleVector(attributeImportance);
	}
	
	
	/***********************************
	 * 
	 * Rule information class
	 * 
	 **********************************/
	public class RuleInformation{
		private double [] attributeImportance;
		private double depth;
		private List<Integer> literalAttributes= new LinkedList<Integer>();
		

		public RuleInformation(int numAttributes) {
			attributeImportance=new double [numAttributes];
			depth=0;
			for(int i=0; i<numAttributes; i++)
				attributeImportance[i]=1.0;
		}

		public void addLiteralAttribute(int attribIndex) {
			//it means rule expanded. depth always increases. 
			//However, the number of literals may be inferior
			depth++;
			attributeImportance[attribIndex]=1.0;
			if(!literalAttributes.contains(attribIndex))
				literalAttributes.add(attribIndex);
		}

		public double [] getAttributesImportance() {
			return attributeImportance;
		}
		
		public double getAttributeImportance(int attribIndex) {
			return attributeImportance[attribIndex];
		}
		
		
		/*
		 * Update the attributes importance if the merit is below the giver threshold
		 * and the attribute is being considered (attributesMask[i]==true)
		 * Returns the list of attributes whose merit had change
		 */
		public List<Integer> update(double [] merits, boolean [] attributesMask, double threshold){
			boolean [] attributesMaskAux=attributesMask.clone();
			Iterator<Integer> it=literalAttributes.iterator();
			while(it.hasNext())
				attributesMaskAux[it.next()]=false;
			
			List<Integer> updated=new LinkedList<Integer>();
			for (int i=0; i<attributesMaskAux.length;i++){
				if(merits[i]<threshold && attributesMaskAux[i]){
					attributeImportance[i]*=(1+depth)/(2+depth);
					updated.add(i);
				}
			}
			return updated;
		}
				
	
	}

}
