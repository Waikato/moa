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
 * GraphMultiCurve draws several curves on a GraphCanvasMulti.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvasMulti, GraphCurve
 */
public class GraphMultiCurve extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;
	
    private double max_value = 1;
    private MeasureCollection[] measures;
    private int measureSelected = 0;

    private double x_resolution;
    
    private int[] processFrequencies;
//    private int min_processFrequency;
    
    /**
     * Initialises a GraphMultiCurve by setting its layout.
     */
    protected GraphMultiCurve() {
    	
    	this.max_value = 1;
    	this.measureSelected = 0;
    	
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
     * Updates the measure collection information and repaints the curves.
     * @param measures 			 information about the curves
     * @param mSelect 		 	 currently selected measure
     * @param processFrequencies information about the process frequencies of
     * 							 the curves
     */
    protected void setGraph(MeasureCollection[] measures, int mSelect, int[] processFrequencies){
       this.measures = measures;
       this.measureSelected = mSelect;
       this.processFrequencies = processFrequencies;
       repaint();
    }
    
    /**
     * Sets the minimum process frequency, which determines the x-axis on the
     * GraphAxes. Curves with a process frequency have to be painted
     * compressed/stretched.
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
        
        g.setColor(Color.BLACK);
        
        // paint all curves
        for (int i = 0; i < this.measures.length; i++) {
        	paintFullCurve(g, this.measures[i], this.measureSelected, this.processFrequencies[i], Color.BLACK);
        }
        
    }

    /**
     * Draws a single curve on the canvas.
     * @param g 	  graphics object
     * @param m 	  curve information
     * @param mSelect currently selected measure
     * @param pf 	  process frequency of the curve
     * @param color   colour the curve will be drawn in
     */
    private void paintFullCurve(Graphics g, MeasureCollection m, int mSelect, int pf, Color color){
            if (m.getNumberOfValues(mSelect) == 0) {
            	// no values of this measure available
            	return;
            }
            
            int height = getHeight();
            
//          // compute the relation of minimum PF and current PF
//          double processFrequencyFactor = pf / this.min_processFrequency;

            int n = m.getNumberOfValues(mSelect);

            int[] x = new int[n];
            int[] y = new int[n];

            for (int i = 0; i < n; i ++) {
            	x[i] = (int) (i / x_resolution);
            	y[i] = (int)(height-(m.getValue(mSelect, i)/this.max_value)*height);
            }
            g.setColor(color);
            g.drawPolyline(x, y, n);
    }

    /**
     * Sets minimum and maximum y value.
     * @param min minimum y value
     * @param max maximum y value
     */
    protected void setYMaxValue(double max){
        this.max_value = max;
    }

    /**
     * Sets the resolution on the x-axis
     * @param x_resolution resolution on the x-axis
     */
    protected void setXResolution(double x_resolution) {
        this.x_resolution = x_resolution;
    }

}
