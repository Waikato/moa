package moa.classifiers.rules.featureranking;

import moa.classifiers.rules.multilabel.core.ObserverMOAObject;
import moa.core.DoubleVector;

public interface FeatureRanking extends ObserverMOAObject{
	public DoubleVector getFeatureRankings();
}
