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
package moa.classifiers.rules.multilabel.attributeclassobservers;

import moa.AbstractMOAObject;

import com.github.javacliparser.StringUtils;

/**
 * Vector of float numbers with some utilities.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class SingleVector extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected float[] array;

    public SingleVector() {
        this.array = new float[0];
    }

    public SingleVector(float[] toCopy) {
        this.array = new float[toCopy.length];
        System.arraycopy(toCopy, 0, this.array, 0, toCopy.length);
    }

    
    public SingleVector(SingleVector toCopy) {
        this(toCopy.getArrayRef());
    }


	public SingleVector(double[] arrayRef) {
		this.array=new float[arrayRef.length];
		for(int i=0; i<arrayRef.length;i++)
			this.array[i]=(float)arrayRef[i];
	}

	public int numValues() {
        return this.array.length;
    }

    public void setValue(int i, float v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] = v;
    }

    public void addToValue(int i, float v) {
        if (i >= this.array.length) {
            setArrayLength(i + 1);
        }
        this.array[i] += v;
    }

    public void addValues(SingleVector toAdd) {
        addValues(toAdd.getArrayRef());
    }

    public void addValues(float[] toAdd) {
        if (toAdd.length > this.array.length) {
            setArrayLength(toAdd.length);
        }
        for (int i = 0; i < toAdd.length; i++) {
            this.array[i] += toAdd[i];
        }
    }

    public void subtractValues(SingleVector toSubtract) {
        subtractValues(toSubtract.getArrayRef());
    }

    public void subtractValues(float[] toSubtract) {
        if (toSubtract.length > this.array.length) {
            setArrayLength(toSubtract.length);
        }
        for (int i = 0; i < toSubtract.length; i++) {
            this.array[i] -= toSubtract[i];
        }
    }

    public void addToValues(float toAdd) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] + toAdd;
        }
    }

    public void scaleValues(float multiplier) {
        for (int i = 0; i < this.array.length; i++) {
            this.array[i] = this.array[i] * multiplier;
        }
    }

    // returns 0.0 for values outside of range
    public float getValue(int i) {
        return ((i >= 0) && (i < this.array.length)) ? this.array[i] : 0.0f;
    }

    public float sumOfValues() {
        float sum = 0.0f;
        for (float element : this.array) {
            sum += element;
        }
        return sum;
    }

    public int maxIndex() {
        int max = -1;
        for (int i = 0; i < this.array.length; i++) {
            if ((max < 0) || (this.array[i] > this.array[max])) {
                max = i;
            }
        }
        return max;
    }

    public void normalize() {
        scaleValues(1.0f / sumOfValues());
    }

    public int numNonZeroEntries() {
        int count = 0;
        for (float element : this.array) {
            if (element != 0.0) {
                count++;
            }
        }
        return count;
    }

    public float minWeight() {
        if (this.array.length > 0) {
            float min = this.array[0];
            for (int i = 1; i < this.array.length; i++) {
                if (this.array[i] < min) {
                    min = this.array[i];
                }
            }
            return min;
        }
        return 0.0f;
    }

    public float[] getArrayCopy() {
        float[] aCopy = new float[this.array.length];
        System.arraycopy(this.array, 0, aCopy, 0, this.array.length);
        return aCopy;
    }

    public float[] getArrayRef() {
        return this.array;
    }

    protected void setArrayLength(int l) {
        float[] newArray = new float[l];
        int numToCopy = this.array.length;
        if (numToCopy > l) {
            numToCopy = l;
        }
        System.arraycopy(this.array, 0, newArray, 0, numToCopy);
        this.array = newArray;
    }

    public void getSingleLineDescription(StringBuilder out) {
        getSingleLineDescription(out, numValues());
    }

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
