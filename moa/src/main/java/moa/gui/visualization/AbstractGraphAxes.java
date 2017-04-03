/*
 *    AbstractGraphAxes.java
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
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.JPanel;

/**
 * AbstractGraphAxes is an abstract class offering functionality to draw axes.
 * 
 * This class is partially based on GraphAxes.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphAxes
 */
public abstract class AbstractGraphAxes extends JPanel {

    private static final long serialVersionUID = 1L;
    protected static final int X_OFFSET_LEFT = 35;
    protected static final int X_OFFSET_RIGHT = 15;
    protected static final int Y_OFFSET_BOTTOM = 20;
    protected static final int Y_OFFSET_TOP = 20;

    protected int width;
    protected int height;

    protected double x_resolution;
    protected double y_resolution;

    protected double min_x_value;
    protected double max_x_value;
    protected double max_y_value;

    protected double lower_x_value;
    protected double upper_x_value;
    protected double upper_y_value;

    /**
     * Initialises a AbstractGraphAxes by setting the initial values and the
     * layout.
     */
    public AbstractGraphAxes() {
        this.min_x_value = 0;
        this.max_x_value = 1;
        this.max_y_value = 1;
        this.lower_x_value = 0;
        this.upper_x_value = 1;
        this.upper_y_value = 1;

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 300, Short.MAX_VALUE));
    }

    /**
     * Sets the x resolution.
     * 
     * @param resolution
     *            new x resolution
     */
    public void setXResolution(double resolution) {
        x_resolution = resolution;
    }

    /**
     * Sets the y resolution
     * 
     * @param resolution
     *            new y resolution
     */
    public void setYResolution(double resolution) {
        y_resolution = resolution;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // compute "true" width/height without offsets
        height = getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP;
        width = getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT;

        g.setColor(new Color(236, 233, 216));
        g.fillRect(0, 0, getWidth(), getHeight());

        // draw background
        g.setColor(Color.WHITE);
        g.fillRect(X_OFFSET_LEFT, Y_OFFSET_TOP, width, height);

        g.setFont(new Font("Tahoma", 0, 11));

        xAxis(g);
        yAxis(g);
    }

    /**
     * Draws the x axis, containing of the axis line and the labels.
     * 
     * @param g
     *            the Graphics context in which to paint
     */
    protected void xAxis(Graphics g) {
        g.setColor(Color.BLACK);

        // x-axis line
        g.drawLine(X_OFFSET_LEFT, calcY(0), width + X_OFFSET_LEFT, calcY(0));

        drawXLabels(g);
    }

    /**
     * Draws the x labels onto the x axis. Must be overridden by subclasses.
     * 
     * @param g
     *            the Graphics context in which to paint
     */
    protected abstract void drawXLabels(Graphics g);

    /**
     * Draws the y axis, containing og the axis line, the horizontal helping
     * line and the labels.
     * 
     * @param g
     *            the Graphics context in which to paint
     */
    private void yAxis(Graphics g) {
        // y-axis
        g.setColor(Color.BLACK);
        g.drawLine(X_OFFSET_LEFT, calcY(0), X_OFFSET_LEFT, Y_OFFSET_TOP);

        // center horizontal line
        g.setColor(new Color(220, 220, 220));
        g.drawLine(X_OFFSET_LEFT, height / 2 + Y_OFFSET_TOP, getWidth(),
                height / 2 + Y_OFFSET_TOP);

        g.setColor(Color.BLACK);

        // y-achsis markers + labels
        DecimalFormat d = new DecimalFormat("0.00");

        double numLabels = Math.min(Math.pow(2, y_resolution), 32);
        /*
         * technically, this is numLabels-1, but as we're iterating 0 <= i <=
         * numLabels, we need the extra label. Also don't draw more than 32
         * labels.
         */

        for (int i = 0; i <= numLabels; i++) {
            double fraction = i / numLabels;
            double value = fraction * upper_y_value;
            g.drawString(d.format(value), 1,
                    (int) ((1 - fraction) * height) + Y_OFFSET_TOP + 5);
            g.drawLine(X_OFFSET_LEFT - 5,
                    (int) ((1 - fraction) * height) + Y_OFFSET_TOP,
                    X_OFFSET_LEFT,
                    (int) ((1 - fraction) * height) + Y_OFFSET_TOP);
        }

    }
    
    /**
     * Sets the minimum x value
     * 
     * @param min
     *            minimum x value
     */
    public void setMinXValue(double min) {
        this.min_x_value = min;
    }

    /**
     * Sets the maximum x value
     * 
     * @param max
     *            maximum x value
     */
    public void setMaxXValue(double max) {
        this.max_x_value = max;
    }
    
    /**
     * Sets the maximum y value
     * 
     * @param max
     *            maximum y value
     */
    public void setMaxYValue(double max) {
        this.max_y_value = max;
    }
    
    /**
     * Sets the lower value for the x-axis.
     * 
     * @param value
     *            lower x value
     */
    public void setLowerXValue(double value) {
        this.lower_x_value = value;
    }

    /**
     * Sets the upper value for the x-axis.
     * 
     * @param value
     *            upper x value
     */
    public void setUpperXValue(double value) {
        this.upper_x_value = value;
    }

    /**
     * Sets the upper value for the y-axis.
     * 
     * @param value
     *            upper y value
     */
    public void setUpperYValue(double value) {
        this.upper_y_value = value;
    }

    /**
     * Calculates the position on the y axis for a given value.
     * 
     * @param value
     *            value the y position has to be computed for
     * @return position on the y axis
     */
    private int calcY(double value) {
        return (int) (height - (value / max_y_value) * height) + Y_OFFSET_TOP;
    }

}
