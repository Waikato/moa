package moa.learners.predictors.rules;

import moa.learners.predictors.MultiTargetRegressor;

public class AMRulesRegressor extends AMRulesMultiTargetRegressor implements MultiTargetRegressor {

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
