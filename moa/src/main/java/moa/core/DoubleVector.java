/*
 *    DoubleVector.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.core;

import moa.AbstractMOAObject;

/**
 * Vector of double numbers with some utilities.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class DoubleVector extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    /** The underlying storage of the vector's values. */
    protected double[] array;

    /** Creates an empty DoubleVector. After creation, the length if this DoubleVector is 0. */
    public DoubleVector() {
        this.array = new double[0];
    }

    /**
     * Creates an initialised DoubleVector. The values of the input array are copied into the new
     * DoubleVector.
     *
     * @param toCopy the array to copy the numbers from
     */
    public DoubleVector(double[] toCopy) {
        this.array = new double[toCopy.length];
        System.arraycopy(toCopy, 0, this.array, 0, toCopy.length);
    }

    /**
     * Duplicates a DoubleVector. The values of the input DoubleVector are copied into the new
     * object.
     *
     * @param toCopy the DoubleVector to copy the values from
     */
    public DoubleVector(DoubleVector toCopy) {
        this(toCopy.getArrayRef());
    }

    /**
     * How long this DoubleVector is.
     *
     * @return the length of this DoubleVector
     */
    public int numValues() {
        return this.array.length;
    }

    /**
     * Replaces the value at the specified position. If the DoubleVector is smaller than the
     * required index, it grows to accommodate it.
     *
     * @param i the index of the value to replace
     * @param v the number to store at the specified position
     */
    public void setValue(int i, double v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] = v;
    }

    /**
     * Adds a number to one of the DoubleVector's values. If the position is greater than this
     * DoubleVector's size, the vector grows to accommodate it and the number is inserted.
     *
     * @param i the position where to perform the operation
     * @param v the number to add
     */
    public void addToValue(int i, double v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] += v;
    }

    /**
     * Adds element-wise the values of a DoubleVector. If both vectors are not the same length, the
     * shorter one behaves as if padded with zeros to match the longer one.
     *
     * @param toAdd a DoubleVector of numbers to add
     */
    public void addValues(DoubleVector toAdd) {
        addValues(toAdd.getArrayRef());
    }

    /**
     * Adds element-wise the values of an array. If both containers are not the same length, the
     * shorter one behaves as if padded with zeros to match the longer one.
     *
     * @param toAdd an array of numbers to add
     */
    public void addValues(double[] toAdd) {
        if (toAdd.length > this.array.length) {
            setArrayLength(toAdd.length);
        }
        for (int i = 0; i < toAdd.length; i++) {
            this.array[i] += toAdd[i];
        }
    }

    /**
     * Subtracts element-wise the values of a DoubleVector. If both vectors are not the same length,
     * the shorter one behaves as if padded with zeros to match the longer one.
     *
     * @param toSubtract a DoubleVector of numbers to subtract
     */
    public void subtractValues(DoubleVector toSubtract) {
        subtractValues(toSubtract.getArrayRef());
    }

    /**
     * Subtracts element-wise the values of an array. If both containers are not the same length,
     * the shorter one behaves as if padded with zeros to match the longer one.
     *
     * @param toSubtract an array of numbers to subtract
     */
    public void subtractValues(double[] toSubtract) {
        if (toSubtract.length > this.array.length) {
            setArrayLength(toSubtract.length);
        }
        for (int i = 0; i < toSubtract.length; i++) {
            this.array[i] -= toSubtract[i];
        }
    }

    /**
     * Adds a number to all values of this DoubleVector.
     *
     * @param toAdd the number to add
     */
    public void addToValues(double toAdd) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] + toAdd;
        }
    }

    /**
     * Multiplies all numbers in this DoubleVector by a factor.
     *
     * @param multiplier the factor to multiply by
     */
    public void scaleValues(double multiplier) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] * multiplier;
        }
    }

    /**
     * Returns the element at the specified position. Returns 0 for values outside of the range
     * (index ≥ 0 || index &lt; numValues).
     *
     * @param i the index of the element to return
     * @return the value at the specified index
     */
    public double getValue(int i) {
        return ((i >= 0) && (i < this.array.length)) ? this.array[i] : 0.0;
    }

    /**
     * Sums the numbers in this DoubleVector.
     *
     * @return the addition of the values
     */
    public double sumOfValues() {
        double sum = 0.0;
        for (double element : this.array) {
            sum += element;
        }
        return sum;
    }

    /**
     * Sums the magnitudes of the numbers in this DoubleVector.
     *
     * @return the addition of the absolute values
     */
    public double sumOfAbsoluteValues() {
        double sum = 0.0;
        for (double element : this.array) {
            sum += (element > 0.0) ? element : -element;
        }
        return sum;
    }

    /**
     * Finds the position of the greatest number. If this DoubleVector is empty, returns -1.
     *
     * @return the position of the greatest value or -1
     */
    public int maxIndex() {
        int max = -1;
        for (int i = 0; i < this.array.length; i++) {
            if ((max < 0) || (this.array[i] > this.array[max])) {
                max = i;
            }
        }
        return max;
    }

    /**
     * Rescales the values to be between -1 and 1. The numbers in this DoubleVector are scaled so
     * that the sum of their absolute values equals 1.
     */
    public void normalize() {
        scaleValues(1.0 / sumOfAbsoluteValues());
    }

    /**
     * Counts the number of values different from zero.
     *
     * @return a count of values that are not 0
     */
    public int numNonZeroEntries() {
        int count = 0;
        for (double element : this.array) {
            if (element != 0.0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds the smallest number in this DoubleVector.
     *
     * @return the smallest value of the DoubleVector. 0 if this DoubleVector is empty.
     */
    public double minWeight() {
        if (this.array.length > 0) {
            double min = this.array[0];
            for (int i = 1; i < this.array.length; i++) {
                if (this.array[i] < min) {
                    min = this.array[i];
                }
            }
            return min;
        }
        return 0.0;
    }

    /**
     * Creates a copy of the underlying array of this DoubleVector.
     *
     * @return an array containing the values of this DoubleVector
     */
    public double[] getArrayCopy() {
        double[] aCopy = new double[this.array.length];
        System.arraycopy(this.array, 0, aCopy, 0, this.array.length);
        return aCopy;
    }

    /**
     * Gives the underlying array of this DoubleVector.
     *
     * @return the array containing the values
     */
    public double[] getArrayRef() {
        return this.array;
    }

    /**
     * Sets the size of the DoubleVector. If the new size is less than the current size, all values
     * at index <code>l</code> and greater are discarded.
     *
     * @param l The new size of this DoubleVector. Must be positive.
     */
    protected void setArrayLength(int l) {
        double[] newArray = new double[l];
        int numToCopy = this.array.length;
        if (numToCopy > l) {
            numToCopy = l;
        }
        System.arraycopy(this.array, 0, newArray, 0, numToCopy);
        this.array = newArray;
    }

    /**
     * Represents the content of this DoubleVector.
     *
     * @param out the StringBuilder to build the representation in
     */
    public void getSingleLineDescription(StringBuilder out) {
        getSingleLineDescription(out, numValues());
    }

    /**
     * Represents the <code>numValues</code> first entries in this DoubleVector. Padded with zeros
     * at the end if it exceeds the length of this DoubleVector.
     *
     * @param out the StringBuilder to build the representation in
     * @param numValues how many numbers to show
     */
    public void getSingleLineDescription(StringBuilder out, int numValues) {
        out.append("{");
        for (int i = 0; i < numValues; i++) {
            if (i > 0) {
                out.append("|");
            }
            out.append(StringUtils.doubleToString(getValue(i), 3));
        }
        out.append("}");
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        getSingleLineDescription(sb);
    }
}
