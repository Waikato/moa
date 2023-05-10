/*
 *    AbstractGraphPlot.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package moa.gui.visualization;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/**
 * AbstractGraphPlot is an abstract class defining the structure of a Plot
 * class.
 *
 * This class is partially based on GraphCurve.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCurve
 */
public abstract class AbstractGraphPlot extends JPanel{

	private static final long serialVersionUID = 1L;

	protected MeasureCollection[] measures;
	protected MeasureCollection[] measureStds;
    protected int measureSelected;

    protected double min_x_value;
    protected double max_x_value;
    protected double max_y_value;

    protected double lower_x_value;
    protected double upper_x_value;
    protected double upper_y_value;

    protected double x_resolution;

    protected Color[] colors;

    protected boolean isStandardDeviationPainted;

	public AbstractGraphPlot() {
	    this.measureSelected = 0;
	    this.min_x_value = 0;
		this.max_x_value = 1;
		this.max_y_value = 1;
		this.lower_x_value = 0;
		this.upper_x_value = 1;
		this.upper_y_value = 1;

		this.isStandardDeviationPainted = false;

    	setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
	}

	/**
	 * Sets the graph by updating the measures and currently measure index.
	 * This method should not be directly called, but may be used by subclasses
	 * to save space.
	 * @param measures measure information
	 * @param stds standard deviation of the measures
	 * @param colors color encoding for the plots
	 */
	protected void setGraph(MeasureCollection[] measures, MeasureCollection[] stds, Color[] colors) {
		this.measures = measures;
		this.measureStds = stds;
	    this.colors = colors;
	    repaint();
	}

    /**
     * Sets the currently selected measure index.
     *
     * @param selected
     * 			  new selected measure
     */
    public void setMeasureSelected(int selected) {
        this.measureSelected = selected;
    }

    /**
     * Sets minimum x value.
     * @param min minimum x value
     */
    protected void setMinXValue(double min){
        this.min_x_value = min;
    }

    /**
     * Sets maximum x value.
     * @param max maximum x value
     */
    protected void setMaxXValue(double max){
        this.max_x_value = max;
    }

    /**
     * Sets maximum y value.
     * @param max maximum y value
     */
    protected void setMaxYValue(double max){
        this.max_y_value = max;
    }

    /**
     * Sets the lower value for the x-axis.
     * 
     * @param value
     *            lower x value
     */
    protected void setLowerXValue(double value) {
        this.lower_x_value = value;
    }

    /**
     * Sets the upper value for the x-axis.
     * 
     * @param value
     *            upper x value
     */
    protected void setUpperXValue(double value) {
    	this.upper_x_value = value;
    }

    /**
     * Sets the upper value for the y-axis.
     * 
     * @param value
     *            upper y value
     */
    protected void setUpperYValue(double value) {
    	this.upper_y_value = value;
    }

    /**
     * Sets the resolution on the x-axis
     * @param x_resolution resolution on the x-axis
     */
    protected void setXResolution(double x_resolution) {
        this.x_resolution = x_resolution;
    }

    /**
     * Sets the value for isStandardDeviationPainted.
     * @param b new setting for isStandardDeviationPainted
     */
    protected void setStandardDeviationPainted(boolean b) {
        this.isStandardDeviationPainted = b;
    }

    protected void paintStandardDeviation(Graphics g, int len, int x, int y) {
        // draw vertical line
        g.drawLine(x, y-len, x, y+len);
        // draw horizontal lines
        g.drawLine(x-5, y-len, x+5, y-len);
        g.drawLine(x-5, y+len, x+5, y+len);
    }
}
