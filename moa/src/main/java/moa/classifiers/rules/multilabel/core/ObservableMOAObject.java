/*
 *    ObservableMOAObject.java
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

package moa.classifiers.rules.multilabel.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import moa.AbstractMOAObject;
import moa.classifiers.rules.featureranking.messages.FeatureRankingMessage;

//For allowing the implementation of observer pattern for feature ranking in RandomAMRules
abstract public class ObservableMOAObject extends AbstractMOAObject {

	/**
	 * 
	 */
	List<ObserverMOAObject> observers= new LinkedList<ObserverMOAObject>();
	private static final long serialVersionUID = 1L;
	
	abstract public void addObserver(ObserverMOAObject o);
	
	public void notifyAll(FeatureRankingMessage message){
		Iterator<ObserverMOAObject> it= observers.iterator();
		while(it.hasNext()){
			it.next().update(this, message);
		}
 		
	}
	
	public void notify(ObserverMOAObject o,FeatureRankingMessage message){
		o.update(this, message);
	}
	
	//abstract public void receiveFeedback(ObserverMOAObject o, Object feedback);

	
}
