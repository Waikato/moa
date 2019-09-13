package moa.classifiers.core.attributeclassobservers;

import moa.options.AbstractOptionHandler;

public abstract class AbstractAttributeClassObserver extends AbstractOptionHandler implements AttributeClassObserver {

	@Override
	public void observeAttributeValue(double attVal, double value, double weight) {
		if (this instanceof NumericAttributeClassObserver) 
			this.observeAttributeTarget(attVal, value);
		else if (this instanceof DiscreteAttributeClassObserver) {
			this.observeAttributeClass(attVal, (int) value, weight);
		}
	}

}
