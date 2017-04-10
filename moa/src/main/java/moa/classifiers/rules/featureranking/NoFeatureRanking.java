package moa.classifiers.rules.featureranking;

import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

public class NoFeatureRanking extends AbstractFeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NoFeatureRanking() {
		super();
	}

	public void update(ObservableMOAObject o, Object arg) {
		
	}


	

	@Override
	public DoubleVector getFeatureRankings() {
		return null;
	}
	
}
