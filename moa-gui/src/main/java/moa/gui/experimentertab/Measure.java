/*
 *    Measure.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
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
package moa.gui.experimentertab;

import moa.core.DoubleVector;

/**
 * This class determines the value of each measure for each algorithm
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class Measure {

    private String name;
    
    private String fileName;

    private Double value;

    private DoubleVector values = new DoubleVector();

    private Double std;

    private boolean type;

    private int index;

    /**
     * Measure Constructor
     * @param name
     * @param type
     * @param index
     */
    public Measure(String name,String filename, boolean type, int index) {
        this.name = name;
        this.fileName = filename;
        this.type = type;
        this.index = index;
        this.value = 0.0;
        this.std = 0.0;
    }
   
    /**
     *
     * @return the name of measure
     */
    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Sets the name of measure
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return the value of measure
     */
    public Double getValue() {
        return value;
    }

    /**
     * Sets the value of measure
     * @param value
     */
    public void setValue(Double value) {
        this.value = value;
    }

    /**
     * Returns the standard deviation
     * @return the standard deviation
     */
    public Double getStd() {
        return std;
    }

    /**
     * Sets the standard deviation
     * @param std
     */
    public void setStd(Double std) {
        this.std = std;
    }

    /**
     * Returns the type of measure
     * @return the type of measure
     */
    public boolean isType() {
        return type;
    }

    /**
     * Sets the type of measure
     * @param type
     */
    public void setType(boolean type) {
        this.type = type;
    }

    /**
     * Returns the index of measure
     * @return the index of measure
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the index of measure
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     *
     * @return values
     */
    public DoubleVector getValues() {
        return values;
    }

    /**
     *
     * @param values
     */
    public void setValues(DoubleVector values) {
        this.values = (DoubleVector) values.copy();
    }

    /**
     * Calculates the value of measure
     * @param values
     */
    public void computeValue(DoubleVector values) {
        if (this.isType()) {
            setValues(values);
            double sumDif = 0.0;
            this.value = this.values.sumOfValues() / (double) values.numValues();
            for (int i = 0; i < this.values.numValues(); i++) {
                double dif = this.values.getValue(i) - this.value;
                sumDif += Math.pow(dif, 2);
            }
            sumDif = sumDif / this.values.numValues();
            this.std = Math.sqrt(sumDif);
        }

    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone(); 
    }

}
