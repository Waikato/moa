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
 * TODO javadoc
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas
 */
public class GraphCanvasMulti extends JPanel {

	private static final long serialVersionUID = 1L;

	private MeasureCollection[] measures;

    int measureSelected = 0;

    private ArrayList<ClusterEvent> clusterEvents;

    private ArrayList<JLabel> eventLabelList;

    private GraphAxes axesPanel;

    private GraphMultiCurve curvePanel;

    private JPanel eventPanel;

    private int eventCounter = 0;

    private final int x_offset_left = 35;

    private final int x_offset_right = 5;

    private final int y_offset_bottom = 20;

    private final int y_offset_top = 20;
    
    private int[] processFrequencies;

    private int min_processFrequency;

    //default values to start with;
    private double min_y_value = 0;

    private double max_y_value = 1;

    private double max_x_value = 250;

    private double x_resolution = 0.5; //how many pixels per 1px

//    private double y_resolution = 1;   //full min to max scale

    public GraphCanvasMulti() {
        addComponents();
        eventLabelList = new ArrayList<JLabel>();
        
        updateXResolution();
        updateYValues();
        updateSize();
    }

    public void scaleXResolution(boolean scaleUp) {
        if (scaleUp) {
            x_resolution *= 2;
        } else {
            x_resolution /= 2;
        }
        updateCanvas(true);
    }

    public void scaleYResolution(boolean scaleUp) {
//        if (scaleUp) {
//            y_resolution *= 2;
//        } else {
//            y_resolution /= 2;
//        }
//        updateCanvas(true);
    }

    public int getMeasureSelected() {
        return this.measureSelected;
    }
    
	public int getMinProcessFrequency() {
		return this.min_processFrequency;
	}
	
	public int[] getProcessFrequencies() {
		return this.processFrequencies;
	}

    public void setGraph(MeasureCollection[] measures, int mSelect, int[] processFrequencies, int min_processFrequency) {
        this.measures = measures;
        this.measureSelected = mSelect;
        this.processFrequencies = processFrequencies;
        this.min_processFrequency = min_processFrequency;
        this.axesPanel.setProcessFrequency(min_processFrequency);
        this.curvePanel.setProcessFrequency(min_processFrequency);
        this.curvePanel.setGraph(measures, mSelect, processFrequencies);
        this.updateCanvas();
    }

    public void updateCanvas() {
        updateCanvas(false);
    }

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

    //returns true when values have changed
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

    private void updateXResolution() {
        axesPanel.setXResolution(x_resolution);
        curvePanel.setXResolution(x_resolution);
    }

    private void updateYValues() {
        axesPanel.setYMinMaxValues(min_y_value, max_y_value);
        curvePanel.setYMinMaxValues(min_y_value, max_y_value);
    }

    private void updateSize() {
        axesPanel.setSize(getWidth(), getHeight());
        curvePanel.setSize(getWidth() - x_offset_left - x_offset_right, getHeight() - y_offset_bottom - y_offset_top);
        eventPanel.setSize(getWidth() - x_offset_left - x_offset_right, y_offset_top);

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

    //check if there are any new events in the event list and add them to the plot
    private void addEvents() {
        if (clusterEvents != null && clusterEvents.size() > eventCounter) {
            ClusterEvent ev = clusterEvents.get(eventCounter);
            eventCounter++;
            JLabel eventMarker = new JLabel(ev.getType().substring(0, 1));

            eventMarker.setPreferredSize(new Dimension(20, y_offset_top));
            eventMarker.setSize(new Dimension(20, y_offset_top));
            eventMarker.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            int x = (int) (ev.getTimestamp() / this.min_processFrequency / x_resolution);

            eventMarker.setLocation(x - 10, 0);
            eventMarker.setToolTipText(ev.getType() + " at " + ev.getTimestamp() + ": " + ev.getMessage());
            eventPanel.add(eventMarker);
            eventLabelList.add(eventMarker);
            eventPanel.repaint();
        }
    }

    //check if there are any new events in the event list and add them to the plot
    public void forceAddEvents() {
        if (clusterEvents != null) {
            eventPanel.removeAll();
            for (int i = 0; i < clusterEvents.size(); i++) {

                ClusterEvent ev = clusterEvents.get(i);
                JLabel eventMarker = new JLabel(ev.getType().substring(0, 1));

                eventMarker.setPreferredSize(new Dimension(20, y_offset_top));
                eventMarker.setSize(new Dimension(20, y_offset_top));
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
    protected void paintComponent(Graphics g) {
        //needed in case parent component gets resized
        //TODO doesn't fully work yet when reducing height
        updateSize();
        super.paintComponent(g);
    }

    private void addComponents() {

        axesPanel = new GraphAxes();
        curvePanel = new GraphMultiCurve();
        eventPanel = new JPanel();

        curvePanel.setLocation(x_offset_left + 1, y_offset_top);
        eventPanel.setLocation(x_offset_left + 1, 0);
        eventPanel.setLayout(null);

        add(axesPanel);
        axesPanel.add(curvePanel);
        axesPanel.add(eventPanel);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    public void setClusterEventsList(ArrayList<ClusterEvent> clusterEvents) {
        this.clusterEvents = clusterEvents;
        curvePanel.setClusterEventsList(clusterEvents);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
