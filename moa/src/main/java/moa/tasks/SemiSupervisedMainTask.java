package moa.tasks;

import moa.streams.clustering.ClusterEvent;

import java.util.ArrayList;

/**
 *
 */
public abstract class SemiSupervisedMainTask extends MainTask {

    private static final long serialVersionUID = 1L;

    protected ArrayList<ClusterEvent> events;

    protected void setEventsList(ArrayList<ClusterEvent> events) {
        this.events = events;
    }

    public ArrayList<ClusterEvent> getEventsList() {
        return this.events;
    }

}
