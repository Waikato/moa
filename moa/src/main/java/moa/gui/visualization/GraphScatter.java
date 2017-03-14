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
	
	private MeasureCollection[] measures;
    
    private int measureSelected;
    
    private double max_value;
	
	/**
	 * Constructor. Initialises class variables and sets the layout.
	 */
    public GraphScatter() {
    	this.max_value = 1;
    	this.measureSelected = 0;

        setOpaque(false);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
        	layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 1000, Short.MAX_VALUE));
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 300, Short.MAX_VALUE));
    }
    
    /**
     * Updates the measure values and repaints the scatter plot.
     * @param measures  list of measure collections, one for each task
     * @param selection currently selected measure
     */
    public void setGraph(MeasureCollection[] measures, int mSelect){
        this.measures = measures;
        this.measureSelected = mSelect;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
    	
        if (this.measures == null) { 
        	// no measures received yet -> nothing to paint
        	return; 
        }

        g.setColor(Color.BLACK);
        
        // scatter current budgets
        for (int i = 0; i < this.measures.length; i++) {
        	this.scatter(g, this.measures[i]);
        }
    }
    
    /**
     * Paint a dot onto the panel.
     * @param g graphics object
     * @param m MeasureCollection containing the data
     */
    private void scatter(Graphics g, MeasureCollection m){
    	System.out.println(m.getLastValue(6));

    	int height = getHeight();
    	int width = getWidth();
    	
    	int x = (int) (width * m.getLastValue(6));
   		double value = m.getLastValue(this.measureSelected);  
        if(Double.isNaN(value)){
        	// no result for this budget yet
            return;
        }
        int y = (int)(height - (value / this.max_value) * height); 
        
        System.out.println(x + " " + y);

    	g.fillOval(x - DOT_SIZE/2, y - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);
    }
    
    /**
     * Sets the min and max y-value of this instance. 
     * TODO check about min value. its currently not used, so either drop it or use it
     * @param min  minimum y-value
     * @param max  maximum y-value
     */
    public void setYMinMaxValues(double min, double max){
//        min_value = min;
        this.max_value = max;
    }
}
 