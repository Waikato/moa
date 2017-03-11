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
import java.util.ArrayList;
import moa.evaluation.MeasureCollection;
import moa.streams.clustering.ClusterEvent;

/**
 * GraphMultiCurve draws several curves on a GraphCanvasMulti.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvasMulti, GraphCurve
 */
public class GraphMultiCurve extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;
	
//	private double min_value = 0;
    private double max_value = 1;
    private MeasureCollection[] measures;
    private int measureSelected = 0;

    private double x_resolution;
    
    private int[] processFrequencies;
    private int min_processFrequency;

    private ArrayList<ClusterEvent> clusterEvents;
    
    /**
     * Initialises a GraphMultiCurve by setting its layout.
     */
    protected GraphMultiCurve() {
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
     * @param min_processFrequency minimum process frequency
     */
    protected void setProcessFrequency(int min_processFrequency) {
        this.min_processFrequency = min_processFrequency;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        
        if (this.measures == null) { 
        	// no measures received yet -> nothing to paint
        	return; 
        }
        
        // paint all curves
        for (int i = 0; i < this.measures.length; i++) {
        	paintFullCurve(g, this.measures[i], this.measureSelected, this.processFrequencies[i], Color.BLACK);
        }
        
        paintEvents(g);
        
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
            if (m.getNumberOfValues(mSelect)==0) {
            	// no values of this measure available
            	return;
            }
            
            int height = getHeight();
            
            // compute the relation of minimum PF and current PF
//            double processFrequencyFactor = pf / this.min_processFrequency;

            int n = m.getNumberOfValues(mSelect);
            if(this.x_resolution > 1) 
                n = (int)(n / (int)this.x_resolution);
            int[] x = new int[n];
            int[] y = new int[n];

            for (int i = 0; i < n; i ++) {
                if(this.x_resolution > 1){
                    //we need to compress the values
                    double sum_y = 0;
                    int counter = 0;
                    for (int j = 0; j < this.x_resolution; j++) {
                        if((i)*this.x_resolution+j<m.getNumberOfValues(mSelect)){
                            sum_y+= m.getValue(mSelect,i);
                            counter++;
                        }
                        sum_y/=counter;
                    }
                    x[i] = (int) i;
                    y[i] = (int)(height-(sum_y/this.max_value)*height);
                }
                else{
                    //spreading one value
                    x[i] = (int)(i)*(int)(1/this.x_resolution)+(int)(1/this.x_resolution/2);
                    double value = m.getValue(mSelect,i);
                    if(Double.isNaN(value)){
                        // invalid value -> do not draw anything
                    	return;
                    }
                    y[i] = (int)(height-(value/this.max_value)*height);
                    
                }
            }
            g.setColor(color);
            g.drawPolyline(x, y, n);
    }

    /**
     * Draw events, visualised as a vertical line.
     * @param g graphics object
     */
    private void paintEvents(Graphics g){
       if(clusterEvents!=null){
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i < clusterEvents.size(); i++) {
                int x = (int)(clusterEvents.get(i).getTimestamp()/this.min_processFrequency/x_resolution);

                g.drawLine(x, 0, x, getHeight());
            }
        }
    }

    /**
     * Sets minimum and maximum y value.
     * @param min minimum y value
     * @param max maximum y value
     */
    protected void setYMinMaxValues(double min, double max){
//        min_value = min;
        max_value = max;
    }

    /**
     * Sets the list of occured cluster events.
     * @param clusterEvents cluster events
     */
    protected void setClusterEventsList(ArrayList<ClusterEvent> clusterEvents) {
        this.clusterEvents = clusterEvents;
    }

    /**
     * Sets the resolution on the x-axis
     * @param x_resolution resolution on the x-axis
     */
    protected void setXResolution(double x_resolution) {
        this.x_resolution = x_resolution;
    }
}
