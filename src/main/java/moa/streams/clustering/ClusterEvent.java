/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.streams.clustering;

import java.util.EventObject;


/**
 *
 * @author jansen
 */
public class ClusterEvent extends EventObject {

  private String type;
  private String message;
  private long timestamp;

  public ClusterEvent(Object source, long timestamp, String type, String message) {
    super(source);
    this.type = type;
    this.message = message;
    this.timestamp = timestamp;
  }

  public String getMessage(){
      return message;
  }

  public long getTimestamp(){
      return timestamp;
  }

  public String getType(){
      return type;
  }
}
