/*
 *    ALTaskTextViewerPanel.java
 *    Original Work: Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/*
 * TODO the x axis is fixed to maximum b=1, but it may be less, e.g. if we only look
 * at b=0.01 to b=0.2. so we should consider this. I propose to first set it to
 * 1 and then, when the run is started, update it to the max value. afterwards
 * we dont need resizing of the x axis anymore.
 */
/*
 * TODO rescaling of y axis (and x axis too)
 */

/**
 * A BudgetGraphCanvas is a live graph showing the performance per budget as a
 * scatter plot.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas
 */
public class BudgetGraphCanvas extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private MeasureCollection[] measure0;

    private MeasureCollection[] measure1;
    
    private int measureSelected;
	
	private BudgetGraphAxes axesPanel;
	
	private GraphScatter scatterPanel;
	
    private static final int X_OFFSET_LEFT = 35;

    private static final int X_OFFSET_RIGHT = 5;

    private static final int Y_OFFSET_BOTTOM = 20;

    private static final int Y_OFFSET_TOP = 20;  

//    private double y_resolution;   //full min to max scale
    
    //default values to start with;
//    private double min_y_value;
//
//    private double max_y_value;
//
//    // TODO probably change to accurate value
//    private double max_x_value;

	/**
	 * Constructor. Initialises the class variables and panels.
	 */
    public BudgetGraphCanvas() {
    	
    	this.measure0 = null;
    	this.measure1 = null;
    	this.measureSelected = 0;
    	
//    	this.y_resolution = 1;
//    	
//    	this.min_y_value = 0;
//    	this.max_y_value = 1;
//    	this.max_x_value = 1;
    	
    	this.axesPanel = new BudgetGraphAxes();
    	this.scatterPanel = new GraphScatter();

    	this.scatterPanel.setLocation(X_OFFSET_LEFT, Y_OFFSET_TOP);

        this.add(axesPanel);
        this.axesPanel.add(scatterPanel);
    }

    /**
	 * Scales the resolution on the y-axis either up or down by factor 2.
	 * @param scaleUp  scale resolution up or down
     */
    // TODO this is currently not needed. check out the actionsPerformed stuff in ALTaskText...
    // and consider using this one.
    public void scaleYResolution(boolean scaleUp) {
    	return;
//        if (scaleUp) {
//            y_resolution *= 2;
//        } else {
//            y_resolution /= 2;
//        }
//        updateCanvas(true);
    }
    
    /**
     * Updates the graph.
     * @param measure0  list of measure collections, one for each budget
     * @param measure1  list of measure collections, one for each budget
     * @param mSelect   currently selected measure
     */
    public void setGraph(MeasureCollection[] measure0, MeasureCollection[] measure1, int mSelect) {
        this.measure0 = measure0;
        this.measure1 = measure1;
        this.measureSelected = mSelect;

        this.scatterPanel.setGraph(measure0, measure1, mSelect);
        updateCanvas();
    }
    
    /**
     * Updates the canvas. Calls {@link #updateCanvas(boolean) updateCanvas(false)}.
     */
    public void updateCanvas() {
        updateCanvas(false);
    }

    /**
     * Updates the canvas. Checks if newly obtained values require resetting
     * the axes. Repaints the scatter plot. 
     * @param force  enforce repainting the axes.
     */
    public void updateCanvas(boolean force) {

        //check for new min max values first so we know if we have to do some resizing
//        if (updateMinMaxValues() || force) {
//        	//TODO we probably don't need this anymore, the steps in the budget are quite fixed
//            int maxLabel = (int) Math.ceil(max_x_value / 500);
//            int width = (int) (maxLabel * 500);
//            setSize(width, getHeight());
//            setPreferredSize(new Dimension(width, getHeight()));
//
//            updateYValues();
//            updateSize();
//
//            axesPanel.repaint();
//        }

        //add the latest plot point through repaint
        scatterPanel.repaint();
    }
    
    /**
     * Update minimum and maximum value of y, as well as maximum value of x.
     * @return true if values have changed, false otherwise
     */
    //TODO check out resizing of x-axis
//    private boolean updateMinMaxValues() {
//        double min_y_value_new = min_y_value;
//        double max_y_value_new = max_y_value;
//
//        if (measure0 != null && measure1 != null) {
//            min_y_value_new = Math.min(measure0.getMinValue(measureSelected), measure1.getMinValue(measureSelected));
//            max_y_value_new = Math.max(measure0.getMaxValue(measureSelected), measure1.getMaxValue(measureSelected));
//        } else {
//            if (measure0 != null) {
//                min_y_value_new = measure0.getMinValue(measureSelected);
//                max_y_value_new = measure0.getMaxValue(measureSelected);
//            }
//        }
//
//        //resizing needed?
//        if (max_y_value_new != max_y_value || min_y_value_new != min_y_value) {
//            min_y_value = min_y_value_new;
//            max_y_value = max_y_value_new;
//            return true;
//        }
//        return false;
//    }

    /**
     * Updates the y values.
     */
//    private void updateYValues() {
//    		System.out.println("Setting the max value to " + max_y_value);
//  	    axesPanel.setYMaxValue(max_y_value);
//	        scatterPanel.setYMinMaxValues(min_y_value, max_y_value);
//    }
    
    /**
     * Updates the size.
     */
    private void updateSize() {
        axesPanel.setSize(getWidth(), getHeight());

        scatterPanel.setSize(getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT, getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        //needed in case parent component gets resized
        //TODO doesn't fully work yet when reducing height
        updateSize();
        super.paintComponent(g);
    }
}
