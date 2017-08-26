package moa.classifiers.rules.featureranking;

import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

abstract public class AbstractFeatureRanking extends AbstractOptionHandler implements FeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}

	@Override
	abstract public void update(ObservableMOAObject o, Object arg);
	

	abstract public DoubleVector getFeatureRankings();
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
	
	}

}
