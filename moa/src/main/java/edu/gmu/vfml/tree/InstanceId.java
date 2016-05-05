package edu.gmu.vfml.tree;

import weka.core.Instance;

/**
 * <p>A wrapper class around a {@code weka.core.Instance} which stores the largest
 * id among the currently existing CNodes at the time the data was passed to
 * CVFDT. This is used to remove the instance data from the Node counts when
 * the instance rolls off the window.</p>
 * 
 * <p>Note: This class is not thread safe.</p>
 * 
 * @see weka.classifiers.trees.CVFDT
 * @author ulman
 */
public class InstanceId
{
    protected Instance instance;
    protected int id;

    public InstanceId( Instance instance, int id )
    {
        this.instance = instance;
        this.id = id;
    }

    public Instance getInstance( )
    {
        return instance;
    }

    public int getId( )
    {
        return id;
    }
}
