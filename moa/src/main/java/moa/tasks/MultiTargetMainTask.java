/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.tasks;

import java.util.ArrayList;
import moa.streams.InstanceStream;
import moa.streams.clustering.ClusterEvent;

/**
 *
 * @author albert
 */
public abstract class MultiTargetMainTask extends MainTask {

    protected ArrayList<ClusterEvent> events;

    protected void setEventsList(ArrayList<ClusterEvent> events) {
        this.events = events;
    }
    
    public ArrayList<ClusterEvent> getEventsList() {
        return this.events;
    }
    
}
