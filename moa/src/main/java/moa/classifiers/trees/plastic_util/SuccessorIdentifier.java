package moa.classifiers.trees.plastic_util;

import moa.AbstractMOAObject;

import java.util.Objects;

class SuccessorIdentifier extends AbstractMOAObject implements Comparable<SuccessorIdentifier> {
    public static final double DEFAULT_NOMINAL_VALUE = -1.0;
    private final boolean isNumeric;
    private final Double selectorValue;
    private final Double referenceValue;
    private final boolean isLower;
    private int hashCode;

    public SuccessorIdentifier(SuccessorIdentifier other) {
        this.isLower = other.isLower;
        this.isNumeric = other.isNumeric;
        this.selectorValue = other.selectorValue;
        this.referenceValue = other.referenceValue;
        hashCode = toString().hashCode();
    }

    public SuccessorIdentifier(boolean isNumeric, Double referenceValue, Double selectorValue, boolean isLower) {
        this.isNumeric = isNumeric;
        this.isLower = isLower;
        this.selectorValue = selectorValue;

        if (referenceValue == null)
            this.referenceValue = selectorValue;
        else
            this.referenceValue = referenceValue;

        hashCode = toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() == o.getClass()) {
            SuccessorIdentifier that = (SuccessorIdentifier) o;
            boolean equal = isNumeric() == that.isNumeric();
            equal &= selectorValue.equals(that.getSelectorValue());
            equal &= referenceValue.equals(that.getReferencevalue());
            if (isNumeric) {
                equal &= isLower() == that.isLower();
            }
            return equal;
        }
        Double that = (Double) o;
        if (isNumeric)
            return containsNumericAttribute(that);
        else {
            boolean result = Objects.equals(selectorValue, that);
            if (!result) {
                // use the default successor if the reference value is not `that` and the selector value is the selectorvalue of the default successor
                result = Objects.equals(selectorValue, DEFAULT_NOMINAL_VALUE) && !Objects.equals(referenceValue, that);
            }
            return result;
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(SuccessorIdentifier other) {
        if (isNumeric != other.isNumeric)
            return 0;
        if (isNumeric) {
            if (!Objects.equals(referenceValue, other.referenceValue))
                return 0;
            if (isLower == other.isLower)
                return 0;
            return isLower ? -1 : 1;
        }
        else {
            if (selectorValue == null) // this can only happen in the case of a dummy split (which will be pruned after reordering)
                return 0;
            if (selectorValue == DEFAULT_NOMINAL_VALUE || other.selectorValue == DEFAULT_NOMINAL_VALUE)
                return referenceValue == DEFAULT_NOMINAL_VALUE ? 1 : -1;
            if (selectorValue.equals(other.selectorValue))
                return 0;
            if (selectorValue < other.selectorValue)
                return -1;
            return 1;
        }
    }

    public Double getSelectorValue() {
        return selectorValue;
    }

    public Double getReferencevalue() {
        return referenceValue;
    }

    private boolean matchesCategoricalValue(double attValue) {
        if (isNumeric)
            return false;
        return selectorValue == attValue;
    }

    private boolean containsNumericAttribute(double attValue) {
        if (!isNumeric)
            return false;
        return isLower ? attValue <= selectorValue : attValue > selectorValue;
    }

    public SuccessorIdentifier getOther() {
        if (!isNumeric)
            return null;
        return new SuccessorIdentifier(isNumeric, selectorValue, selectorValue, !isLower);
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    public boolean isLower() {
        if (!isNumeric)
            return false;
        return isLower;
    }

    public Double getValue() {
        return selectorValue;
    }

    public String toString() {
        if (isNumeric) {
            String s = "%b%f%f%b";
            return String.format(s, true, referenceValue, selectorValue, isLower);
        }
        else {
            String s = "%b%f%f";
            return String.format(s, false, referenceValue, selectorValue);
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
