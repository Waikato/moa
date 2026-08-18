/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.experimentertab.tasks;

import moa.streams.clustering.ClusterEvent;

import java.util.ArrayList;

/**
 * @author albert
 */
public abstract class ConceptDriftMainTask extends ExperimenterTask {

    protected ArrayList<ClusterEvent> events;

    protected void setEventsList(ArrayList<ClusterEvent> events) {
        this.events = events;
    }

    public ArrayList<ClusterEvent> getEventsList() {
        return this.events;
    }
}
