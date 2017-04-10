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
