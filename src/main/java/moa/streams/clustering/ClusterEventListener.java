package moa.streams.clustering;


import java.util.EventListener;

/** A contract between a SunEvent source and
  *   listener classes
  */
public interface ClusterEventListener extends EventListener {
  /** Called whenever the sun changes position
    *   in a SunEvent source object
    */
  public void changeCluster(ClusterEvent e);
}

