package moa.classifiers.rules;

import moa.classifiers.Regressor;
import moa.classifiers.rules.multilabel.AMRulesMultiTargetRegressor;

public class AMRulesRegressor extends AMRulesMultiTargetRegressor implements
		Regressor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AMRulesRegressor() {
		super();
		changeDetector.setValueViaCLIString("PageHinkleyDM -d 0.05 -l 35.0");
		anomalyDetector.setValueViaCLIString("OddsRatioScore");
	}

}
