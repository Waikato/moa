/*
 *    GraphMultiCurve.java
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
import moa.evaluation.MeasureCollection;

/**
 * GraphMultiCurve is an an implementation of AbstractGraphPlot that draws
 * several curves on a Canvas.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphPlot
 */
public class GraphMultiCurve extends AbstractGraphPlot {

	private static final long serialVersionUID = 1L;

//    private int[] processFrequencies;
//    private int min_processFrequency;

    /**
     * Updates the measure collection information and repaints the curves.
     * @param measures 			 information about the curves
     * @param measureStds		 standard deviation of the measures
     * @param processFrequencies information about the process frequencies of
     * 							 the curves
     * @param colors			 color encodings of the curves
     */
    protected void setGraph(MeasureCollection[] measures, MeasureCollection[] measureStds, int[] processFrequencies, Color[] colors){
//    	this.processFrequencies = processFrequencies;   
    	super.setGraph(measures, measureStds, colors);
    }

    /**
     * Sets the minimum process frequency, which may be used to stretch or
     * compress curves.
     * NOTE this is currently not implemented
     * @param min_processFrequency minimum process frequency
     */
    protected void setProcessFrequency(int min_processFrequency) {
//        this.min_processFrequency = min_processFrequency;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (this.measures == null) { 
        	// no measures received yet -> nothing to paint
        	return; 
        }

        // paint all curves
        for (int i = 0; i < this.measures.length; i++) {
        	paintFullCurve(g, i);
        }

    }

    /**
     * Draws a single curve on the canvas.
     * @param g 	  the Graphics context in which to paint
	 * @param i       index of the currently selected measure
     */
    private void paintFullCurve(Graphics g, int i){
            if (this.measures[i].getNumberOfValues(this.measureSelected) == 0) {
            	// no values of this measure available
            	return;
            }

            g.setColor(this.colors[i]);

            int height = getHeight();

//          // compute the relation of minimum PF and current PF
//          double processFrequencyFactor = pf / this.min_processFrequency;

            int n = this.measures[i].getNumberOfValues(this.measureSelected);

            int[] x = new int[n];
            int[] y = new int[n];

            for (int j = 0; j < n; j++) {
            	x[j] = (int) (j * x_resolution);
            	y[j] = (int)(height-(this.measures[i].getValue(this.measureSelected, j)/this.upper_y_value)*height);

            	if (this.isStandardDeviationPainted) {
            	    int len = (int) ((this.measureStds[i].getValue(this.measureSelected, j)/this.upper_y_value)*height);
            	    paintStandardDeviation(g, len, x[j], y[j]);
            	}
            }

            g.drawPolyline(x, y, n);
    }

}
