package com.yahoo.labs.samoa.instances;

import java.util.List;

public interface FeatureRelevanceResult {
	public void addFeature(Attribute attr);
	
	public void setFeatureRelevance(Attribute attr, double relevance);
	
	public double getFeatureRelevance(Attribute attr);
	
	public List<Attribute> getFeatures();

}
