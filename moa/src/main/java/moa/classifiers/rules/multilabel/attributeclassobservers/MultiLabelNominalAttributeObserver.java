/*
 *    MultiLabelNominalAttributeObserver.java
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

package moa.classifiers.rules.multilabel.attributeclassobservers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import moa.classifiers.rules.core.NominalRulePredicate;
import moa.classifiers.rules.core.Utils;
import moa.classifiers.rules.multilabel.core.AttributeExpansionSuggestion;
import moa.classifiers.rules.multilabel.core.splitcriteria.MultiLabelSplitCriterion;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.FlagOption;

/**
 * Function for determination of splitting points for nominal variables
 */



public class MultiLabelNominalAttributeObserver extends AbstractOptionHandler
		implements NominalStatisticsObserver {



	/**
	 * 
	 */
	protected FlagOption addUndeclaredValuesOption = new FlagOption("addUndeclaredValues", 'a',
			"Add to the domain values not declared? If false, the value is not used.");
	protected HashMap<Integer, DoubleVector[]> statisticsByNominalValue;
	
	
	private static final long serialVersionUID = 1L;

	@Override
	public void observeAttribute(double inputAttributeValue,
			DoubleVector[] observedStatistics) {
		if (statisticsByNominalValue==null){
			statisticsByNominalValue=new HashMap<Integer, DoubleVector[]>();
		}
		DoubleVector[] current=statisticsByNominalValue.get((int)inputAttributeValue);
		if(current==null)
			current=copyOfStatistics(observedStatistics);
		else
			addStatistics(current,observedStatistics);
		
		statisticsByNominalValue.put((int)inputAttributeValue, current);

	}


	@Override
	public AttributeExpansionSuggestion getBestEvaluatedSplitSuggestion(
			MultiLabelSplitCriterion criterion, DoubleVector[] preSplitStatistics,
			int inputAttributeIndex) {
		
		double bestSuggestionMerit=-Double.MAX_VALUE;
		AttributeExpansionSuggestion bestSuggestiong=null;
		
		Iterator<Entry<Integer, DoubleVector[]>> it = statisticsByNominalValue.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<Integer, DoubleVector[]> pair = it.next();
	    	int splitValue=pair.getKey();
	    	DoubleVector[] statistics=pair.getValue();
	    	
	    	DoubleVector[][] resultingStatistics=new DoubleVector [statistics.length][2];
			for (int i=0; i<statistics.length; i++){
				resultingStatistics[i]= new DoubleVector[2];
				resultingStatistics[i][0]=statistics[i];
				resultingStatistics[i][1]=new DoubleVector(preSplitStatistics[i]);
				resultingStatistics[i][1].subtractValues(statistics[i]);
			}
			
	    	double merit=criterion.getMeritOfSplit(preSplitStatistics, resultingStatistics);
	    	if(merit>bestSuggestionMerit){
	    		bestSuggestionMerit=merit;
	    		bestSuggestiong=new AttributeExpansionSuggestion(new NominalRulePredicate(inputAttributeIndex,splitValue,true), Utils.copy(resultingStatistics), merit);
	    	}
	    }

		return bestSuggestiong;
	}


	private void addStatistics(DoubleVector[] current,
			DoubleVector[] observedStatistics) {
		for(int i=0;i<current.length;i++){
			current[i].addValues(observedStatistics[i]);
		}
	}

	private DoubleVector[] copyOfStatistics(DoubleVector[] statistics) {
		DoubleVector[] copy= new DoubleVector[statistics.length];
		for(int i=0;i<statistics.length;i++){
			copy[i]= new DoubleVector(statistics[i]);
		}
		return copy;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {

	}

}
