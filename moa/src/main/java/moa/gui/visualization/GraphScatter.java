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
    
    private static final int DOT_SIZE = 6;
	
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
    	
    	this.budgets = new ArrayList<Double>();
    	this.budgets.add(0.0);
    	this.budgets.add(0.5);
    	this.budgets.add(1.0);
    	
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
     * TODO javadoc
     * @param g
     * @param ms
     * @param mSelect
     * @param color
     */
    private void scatter(Graphics g, MeasureCollection ms, int mSelect, Color color){ 	
    	int n = this.budgets.size();
    	double[] values = new double[]{0.5,0.0,1.0};
    	
    	if (n == 0) {
    		// no budgets initialised yet
    		return;
    	}
    	
//    	if (n != ms.length) {
//    		// number of measure collections does not match number of budgets
//    		// TODO this case should not occur and can be removed later on
//    		System.err.println("Budget size does not match measure collection size.");
//    		return;
//    	}

    	int height = getHeight();
    	int width = getWidth();
    	
    	int[] x = new int[n];
        int[] y = new int[n];
        
        for (int i = 0; i < n; i++) {
    		x[i] = (int) (width * this.budgets.get(i));
//    		double value = ms[i].getLastValue(mSelect);  
    		double value = values[i];
            if(Double.isNaN(value)){
            	// no result for this budget yet
                continue;
            }
            y[i] = (int)(height - (value / this.max_value) * height);       
        }
        
        g.setColor(color);
        for (int i = 0; i < n; i++) {
        	g.fillOval(x[i] - DOT_SIZE/2, y[i] - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);
        }
    }
    
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
 