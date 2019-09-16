package com.yahoo.labs.samoa.instances;

import java.util.List;

public interface FeatureRelevanceResult {
	void addFeature(Attribute attr);

	void setFeatureRelevance(Attribute attr, double relevance);

	double getFeatureRelevance(Attribute attr);

	List<Attribute> getFeatures();

}
