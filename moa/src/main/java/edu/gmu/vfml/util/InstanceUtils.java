package edu.gmu.vfml.util;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import edu.gmu.vfml.tree.InstanceId;

public class InstanceUtils
{
    public static InstanceId wrapInstance( Instance instance, int id )
    {
        return new InstanceId( instance, id );
    }

    public static Attribute[] getAttributes( Instances instances )
    {
        return getAttributes( instances.instance( 0 ) );
    }

    public static Attribute[] getAttributes( Instance instance )
    {
        int numAttributes = instance.numAttributes( );
        Attribute[] attributes = new Attribute[numAttributes];

        for ( int i = 0; i < numAttributes; i++ )
        {
            attributes[i] = instance.attribute( i );
        }

        return attributes;
    }
}
