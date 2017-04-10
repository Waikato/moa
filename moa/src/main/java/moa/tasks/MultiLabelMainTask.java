/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import moa.streams.clustering.ClusterEvent;

import java.util.ArrayList;

/**
 *
 * @author albert
 */
public abstract class MultiLabelMainTask extends MainTask {

    protected ArrayList<ClusterEvent> events;

    protected void setEventsList(ArrayList<ClusterEvent> events) {
        this.events = events;
    }
    
    public ArrayList<ClusterEvent> getEventsList() {
        return this.events;
    }
    
}
