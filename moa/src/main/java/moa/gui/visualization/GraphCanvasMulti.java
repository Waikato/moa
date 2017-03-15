/*
 *    GraphCanvasMulti.java
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import moa.evaluation.MeasureCollection;
import moa.streams.clustering.ClusterEvent;


/**
 * GraphCanvasMulti is very similar to GraphCanvas, but supports drawing
 * multiple curves on a regular GraphAxes by using a GraphMultiCurve.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas, GraphAxes, GraphMultiCurve
 */
public class GraphCanvasMulti extends JPanel {

	private static final long serialVersionUID = 1L;

	private MeasureCollection[] measures;

    private int measureSelected;

    private ArrayList<ClusterEvent> clusterEvents;

    private ArrayList<JLabel> eventLabelList;

    private GraphAxes axesPanel;

    private GraphMultiCurve curvePanel;

    private JPanel eventPanel;

    private int eventCounter;

    private static final int X_OFFSET_LEFT = 35;

    private static final int X_OFFSET_RIGHT = 5;

    private static final int Y_OFFSET_BOTTOM = 20;

    private static final int Y_OFFSET_TOP = 20; 
    
    private int[] processFrequencies;

    private int min_processFrequency;

    private double min_y_value;

    private double max_y_value;

    private double max_x_value;

    private double x_resolution; //how many pixels per 1px

//    private double y_resolution = 1;   //full min to max scale

    /**
     * Initialises a GraphCanvasMulti by constructing its GraphAxes,
     * GraphMultiCurve and EventPanel members as well as setting the initial
     * sizes.
     */
    public GraphCanvasMulti() {
        this.axesPanel = new GraphAxes();
        this.curvePanel = new GraphMultiCurve();
        this.eventPanel = new JPanel();

        this.curvePanel.setLocation(X_OFFSET_LEFT + 1, Y_OFFSET_TOP);
        this.eventPanel.setLocation(X_OFFSET_LEFT + 1, 0);
        this.eventPanel.setLayout(null);

        add(this.axesPanel);
        this.axesPanel.add(this.curvePanel);
        this.axesPanel.add(this.eventPanel);
        this.eventLabelList = new ArrayList<JLabel>();
        
        this.measureSelected = 0;
        this.eventCounter = 0;
        this.min_y_value = 0;
        this.max_y_value = 1;
        this.max_x_value = 250;
        this.x_resolution = 0.5;
        
        updateXResolution();
        updateYValues();
        updateSize();
    }

    /**
     * Scales the resolution on the x-axis by factor 2.
     * @param scaleUp will resolution be scaled up
     */
    public void scaleXResolution(boolean scaleUp) {
        if (scaleUp) {
            this.x_resolution *= 2;
        } else {
            this.x_resolution /= 2;
        }
        updateCanvas(true);
    }

    /**
     * Scales the resolution on the y-axis by factor 2.
     * CURRENTLY NOT IMPLEMENTED
     * @param scaleUp will resolution be scaled up
     */
    public void scaleYResolution(boolean scaleUp) {
//        if (scaleUp) {
//            y_resolution *= 2;
//        } else {
//            y_resolution /= 2;
//        }
//        updateCanvas(true);
    }

    /**
     * Returns the currently selected measure index.
     * @return currently selected measure index
     */
    public int getMeasureSelected() {
        return this.measureSelected;
    }
    
    /**
     * Returns the minimum process frequency.
     * @return minimum process frequency
     */
	public int getMinProcessFrequency() {
		return this.min_processFrequency;
	}
	
	/**
	 * Returns the list of registered process frequencies.
	 * @return list of registered process frequencies
	 */
	public int[] getProcessFrequencies() {
		return this.processFrequencies;
	}

	/**
	 * Sets the graph containing multiple curves.
	 * @param measures 			   information about the curves
	 * @param mSelect 			   currently selected measure
	 * @param processFrequencies   information about the process frequencies of
	 * 							   the curves
	 * @param min_processFrequency minimun process frequency
	 */
    public void setGraph(MeasureCollection[] measures, int mSelect, int[] processFrequencies, int min_processFrequency) {
        this.measures = measures;
        this.measureSelected = mSelect;
        this.processFrequencies = processFrequencies;
        this.min_processFrequency = min_processFrequency;
        this.axesPanel.setProcessFrequency(min_processFrequency);
        this.curvePanel.setProcessFrequency(min_processFrequency);
        this.curvePanel.setGraph(measures, mSelect, processFrequencies);
        this.updateCanvas(false);
    }

    /**
     * Updates the canvas. First, it checks if the minimum and maximum have
     * changed. If this is the case and/or the update is forced, the size is
     * recomputed and the panels repainted.
     * @param force enforce repainting
     */
    public void updateCanvas(boolean force) {

        //check for new min max values first so we know if we have to do some resizing
        if (updateMinMaxValues() || force) {
            int maxLabel = (int) Math.ceil(max_x_value / x_resolution / 500);
            int width = (int) (maxLabel * 500);
            setSize(width, getHeight());
            setPreferredSize(new Dimension(width, getHeight()));

            axesPanel.setXMaxValue(maxLabel);

            updateXResolution();
            updateYValues();
            updateSize();

            axesPanel.repaint();
        }

        //check for new events
        addEvents();

        //add the latest plot point through repaint
        //TODO: somehow realize incremental painting? (real heavyweight canvas: update())
        curvePanel.repaint();
    }

    /**
     * Computes the maximum values of the registered measure collections and
     * updates the member values accordingly.
     * @return true, if the values have changed
     */
    private boolean updateMinMaxValues() {
    	
    	if (this.measures == null) {
    		// no measures received -> no values can have changed
    		return false;
    	}
    	
        double min_y_value_new = this.min_y_value;
        double max_y_value_new = this.max_y_value;
        double max_x_value_new = this.max_x_value;
        
        for (int i = 0; i < this.measures.length; i++) {
        	MeasureCollection m = this.measures[i];
        	if (m == null) { continue; } // TODO check if this is necessary
        	
        	min_y_value_new = Math.min(min_y_value_new, m.getMinValue(measureSelected));
        	max_y_value_new = Math.max(max_y_value_new, m.getMaxValue(measureSelected));
        	//TODO this might be computable outside the loop. See GraphCanvas
        	max_x_value_new = Math.max(max_x_value_new, m.getNumberOfValues(measureSelected));
        }

        //resizing needed?
        if (max_x_value_new != max_x_value || max_y_value_new != max_y_value || min_y_value_new != min_y_value) {
            min_y_value = min_y_value_new;
            max_y_value = max_y_value_new;
            max_x_value = max_x_value_new;
            return true;
        }
        return false;
    }

    /**
     * Updates the x resolution.
     */
    private void updateXResolution() {
        axesPanel.setXResolution(x_resolution);
        curvePanel.setXResolution(x_resolution);
    }

    /**
     * Updates the y values of the axes and curve panel.
     */
    private void updateYValues() {
        axesPanel.setYMinMaxValues(min_y_value, max_y_value);
        curvePanel.setYMinMaxValues(min_y_value, max_y_value);
    }

    /**
     * Updates the size of the axes, curve and event panel. Recomputes the
     * event locations if necessary.
     */
    private void updateSize() {
        axesPanel.setSize(getWidth(), getHeight());
        curvePanel.setSize(getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT, getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP);
        eventPanel.setSize(getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT, Y_OFFSET_TOP);

        if (clusterEvents != null) {
            //update Label positions
            for (int i = 0; i < clusterEvents.size(); i++) {
                int x = (int) (clusterEvents.get(i).getTimestamp() / this.min_processFrequency / x_resolution);
                if (i < eventLabelList.size()) {
                    eventLabelList.get(i).setLocation(x - 10, 0);
                }
            }
        }
    }

    /**
     * Checks if there are any new events in the event list and add them to the plot
     */
    private void addEvents() {
        if (clusterEvents != null && clusterEvents.size() > eventCounter) {
            ClusterEvent ev = clusterEvents.get(eventCounter);
            eventCounter++;
            JLabel eventMarker = new JLabel(ev.getType().substring(0, 1));

            eventMarker.setPreferredSize(new Dimension(20, Y_OFFSET_TOP));
            eventMarker.setSize(new Dimension(20, Y_OFFSET_TOP));
            eventMarker.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            int x = (int) (ev.getTimestamp() / this.min_processFrequency / x_resolution);

            eventMarker.setLocation(x - 10, 0);
            eventMarker.setToolTipText(ev.getType() + " at " + ev.getTimestamp() + ": " + ev.getMessage());
            eventPanel.add(eventMarker);
            eventLabelList.add(eventMarker);
            eventPanel.repaint();
        }
    }

    /**
     * Checks if there are any new events in the event list and add them to the plot
     */
    public void forceAddEvents() {
        if (clusterEvents != null) {
            eventPanel.removeAll();
            for (int i = 0; i < clusterEvents.size(); i++) {

                ClusterEvent ev = clusterEvents.get(i);
                JLabel eventMarker = new JLabel(ev.getType().substring(0, 1));

                eventMarker.setPreferredSize(new Dimension(20, Y_OFFSET_TOP));
                eventMarker.setSize(new Dimension(20, Y_OFFSET_TOP));
                eventMarker.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                int x = (int) (ev.getTimestamp() / this.min_processFrequency / x_resolution);

                eventMarker.setLocation(x - 10, 0);
                eventMarker.setToolTipText(ev.getType() + " at " + ev.getTimestamp() + ": " + ev.getMessage());
                eventPanel.add(eventMarker);
                eventLabelList.add(eventMarker);
                eventPanel.repaint();
            }
        }
    }
    
    @Override
    protected void paintChildren(Graphics g) {
    	updateSize();
    	super.paintChildren(g);
    }

    /**
     * Sets the list of cluster events.
     * @param clusterEvents list of cluster events
     */
    public void setClusterEventsList(ArrayList<ClusterEvent> clusterEvents) {
        this.clusterEvents = clusterEvents;
        curvePanel.setClusterEventsList(clusterEvents);
    }
}
