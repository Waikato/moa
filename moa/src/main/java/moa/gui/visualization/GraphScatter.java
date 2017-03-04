/*
 *    GraphScatter.java
 *    Copyright (C) Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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
package moa.gui.visualization;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/**
 * GraphScatter plots a scatter graph on a BudgetGraphCanvas.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCurve
 */
public class GraphScatter extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private MeasureCollection measure0;
    private MeasureCollection measure1;
    
    private int measureSelected;
    
    private double max_value;
    
    private ArrayList<Double> budgets;
	
	/**
	 * Constructor. Initialises class variables and sets the layout.
	 */
    public GraphScatter() {
    	this.max_value = 1;
    	this.measure0 = null;
    	this.measure1 = null;
    	this.measureSelected = 0;
    	
    	budgets = new ArrayList<Double>();
    	budgets.add(0.0);
    	budgets.add(0.5);
    	budgets.add(1.0);
    	
        setOpaque(false);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
        	layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 1000, Short.MAX_VALUE));
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 300, Short.MAX_VALUE));
    }
    
    /**
     * Updates the measure values and repaints the graph.
     * @param measure0  first measure collection
     * @param measure1  second measure collection
     * @param selection  currently selected measure
     */
    public void setGraph(MeasureCollection measure0, MeasureCollection measure1, int selection){
        this.measure0 = measure0;
        this.measure1 = measure1;
        this.measureSelected = selection;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);

        if(measure0!=null && measure1!=null){
                scatter(g, this.measure0, this.measureSelected, Color.red);
                scatter(g, this.measure1, this.measureSelected, Color.blue);
        }
        else{
            if(measure0!=null){
                scatter(g, this.measure0, this.measureSelected, Color.red);
            }
        }
    }
    
    /**
     * Plots a scatter graph onto the panel. If a measure value is corrupted
     * (i.e. NaN), do not draw anything. If needed, compress or stretch the
     * values.
     * @param g        Graphics instance
     * @param m        MeasureCollection to be drawn
     * @param mSelect  currently selected measure
     * @param color    color the scatter plot is drawn with
     */
    private void scatter(Graphics g, MeasureCollection m, int mSelect, Color color){
    	return;}
//    	if (this.budgets.size() == 0) {
//    		// no results are existing yet
//    		return;
//    	}
//
//    	int height = getHeight();
//    	int n = this.budgets.size();
//    	
//    	int[] x = new int[n];
//        int[] y = new int[n];
//        
//        for (int i = 0; i < n; i++) {
////            if(x_resolution > 1){
////                // compress values
////                double sum_y = 0;
////                int counter = 0;
////                for (int j = 0; j < x_resolution; j++) {
////                    if((i * x_resolution) + j < m.getNumberOfValues(mSelect)){
////                        sum_y += m.getValue(mSelect, i);
////                        counter++;
////                    }
////                    sum_y /= counter;
////                }
////                x[i] = i;
////                y[i] = (int)(height - (sum_y / this.max_value) * height);
////            }
////            else{
//                // spreading one value
////                x[i] = i * (int)(1 / x_resolution) + (int)(1 / x_resolution / 2);
//                double value = m.getValue(mSelect,i);
//                if(Double.isNaN(value)){
//                	// invalid entry. do not draw anything
//                    return;
//                }
//                y[i] = (int)(height - (value / this.max_value) * height);       
//            }
//        }
//
//        g.setColor(color);
//        for (int i = 0; i < n; i++) {
//        	g.fillOval(x[i], y[i], 1, 1);
//        }
//    }
    
    /**
     * Sets the min and max y-value of this instance. 
     * TODO check about min value. its currently not used, so either drop it or use it
     * @param min  minimum y-value
     * @param max  maximum y-value
     */
    public void setYMinMaxValues(double min, double max){
//        min_value = min;
        max_value = max;
    }
}
 