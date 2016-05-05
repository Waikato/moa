package edu.gmu.vfml.tree;

import static edu.gmu.vfml.util.InstanceUtils.getAttributes;

import java.io.Serializable;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * <p>A helper class for {@code weka.classifiers.trees.VFDT}. Stores the nested
 * tree structure of attribute splits and associated Node statistics.</p>
 * 
 * <p>VFDT does not store entire training instances, only sufficient statistics
 * necessary to calculate Hoeffding bound and decide when to split nodes
 * and on which attributes to make the split.</p>
 * 
 * <p>Counts stores per-Node count values (in lieu of storing the entire set
 * of instances used at each Node.</p>
 */
public class Node implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** The node's successors. */
    protected Node[] successors;

    /** Attribute used for splitting. */
    protected Attribute attribute;

    /** Class value if node is leaf. */
    protected double classValue;

    /** Number of instances corresponding to classValue.
     *  This is equal to classCounts[classAttribute.index()]. */
    protected int classCount;

    // fields copied from VFDT
    protected Attribute classAttribute;

    // counts indexed by [attribute][value][class]
    protected transient int[][][] counts;
    protected transient int[] classCounts;
    protected transient int totalCount;

    public Node( Attribute[] attributes, Attribute classAttribute )
    {
        this.classAttribute = classAttribute;

        int attributeCount = attributes.length;
        this.classCounts = new int[classAttribute.numValues( )];
        this.counts = new int[attributeCount][][];
        for ( int i = 0; i < attributeCount; i++ )
        {
            Attribute attribute = attributes[i];
            int[][] attributeCounts = new int[attribute.numValues( )][];
            this.counts[i] = attributeCounts;

            for ( int j = 0; j < attribute.numValues( ); j++ )
            {
                attributeCounts[j] = new int[classAttribute.numValues( )];
            }
        }
    }

    public Node( Instances instances, Attribute classAttribute )
    {
        this( getAttributes( instances ), classAttribute );
    }

    public Node( Instance instance, Attribute classAttribute )
    {
        this( getAttributes( instance ), classAttribute );
    }
    
    /**
     * Modified this Node to look like the provided node. Does not deep copy fields.
     */
    public void copyNode( Node node )
    {
        this.successors = node.successors;
        this.attribute = node.attribute;
        this.classAttribute = node.classAttribute;
        this.classValue = node.classValue;
        this.classCount = node.classCount;
        this.counts = node.counts;
        this.classCount = node.classCount;
        this.totalCount = node.totalCount;
    }

    public int getTreeSize( )
    {
        if ( successors != null )
        {
            int count = 0;
            for ( Node node : successors )
            {
                count += node.getTreeSize( );
            }

            // add one for this node
            return count + 1;
        }
        else
        {
            return 1;
        }
    }
    
    public Node getSuccessor( int value )
    {
        if ( successors != null )
        {
            return successors[value];
        }
        else
        {
            return null;
        }
    }

    /**
     * @see #getLeafNode(Node, Instance)
     */
    public Node getLeafNode( Instance instance )
    {
        return getLeafNode( this, instance );
    }

    /**
     * Traverses the node tree for the provided instance and returns the leaf node
     * associated with the provided instance.
     * 
     * @param instance the instance to be classified
     * @return the leaf node for the instance
     * @see weka.classifiers.trees.Id3#classifyInstance(Instance)
     */
    protected Node getLeafNode( Node node, Instance instance )
    {
        // this is a leaf node, so return this node
        if ( node.getAttribute( ) == null )
        {
            return node;
        }
        // this is an internal node, move to the next child based on the m_Attribute for this node
        else
        {
            int attributeValue = ( int ) instance.value( node.getAttribute( ) );
            Node childNode = node.getSuccessor( attributeValue );
            return getLeafNode( childNode, instance );
        }
    }

    public Attribute getClassAttribute( )
    {
        return classAttribute;
    }

    public Attribute getAttribute( )
    {
        return attribute;
    }

    public double getClassValue( )
    {
        return classValue;
    }

    public void split( Attribute attribute, Instance instance )
    {
        this.successors = new Node[attribute.numValues( )];
        this.attribute = attribute;

        for ( int valueIndex = 0; valueIndex < attribute.numValues( ); valueIndex++ )
        {
            this.successors[valueIndex] = new Node( instance, classAttribute );
        }
    }

    public int getNumClasses( )
    {
        return this.classAttribute.numValues( );
    }

    /**
     * @return the total number of instances in this Node
     */
    public int getCount( )
    {
        return totalCount;
    }

    /**
     * @param classIndex the class to get counts for
     * @return the total number of instances for the provided class
     */
    public int getCount( int classIndex )
    {
        return classCounts[classIndex];
    }

    /**
    * @param attribute the attribute to get a count for
    * @param valueIndex the value of the attribute
    * @return the total number of instances with the provided attribute value
    */
    public int getCount( Attribute attribute, int valueIndex )
    {
        int attributeIndex = attribute.index( );

        int count = 0;
        for ( int classIndex = 0; classIndex < classAttribute.numValues( ); classIndex++ )
        {
            count += getCount( attributeIndex, valueIndex, classIndex );
        }

        return count;
    }

    public int getCount( Attribute attribute, int valueIndex, int classIndex )
    {
        return getCount( attribute.index( ), valueIndex, classIndex );
    }

    /**
     * @param classIndex
     * @param attributeIndex
     * @param valueIndex
     * @return the number of instances with the provided class and attribute value
     */
    public int getCount( int attributeIndex, int valueIndex, int classIndex )
    {
        return counts[attributeIndex][valueIndex][classIndex];
    }

    public void incrementCounts( Instance instance )
    {
        adjustCounts( instance, 1 );
    }

    public void decrementCounts( Instance instance )
    {
        adjustCounts( instance, -1 );
    }

    public void adjustCounts( Instance instance, int amount )
    {
        //XXX assumes nominal class
        int instanceClassValue = ( int ) instance.classValue( );

        adjustTotalCount( amount );
        adjustClassCount( instanceClassValue, amount );

        for ( int i = 0; i < instance.numAttributes( ); i++ )
        {
            Attribute attribute = instance.attribute( i );
            adjustCount( attribute, instance, amount );
        }

        // update classValue and classCount
        int instanceClassCount = getCount( instanceClassValue );

        // if we incremented, and
        // if the count of the class we just added is greater than the current
        // largest count, it becomes the new classification for this node
        if ( amount > 0 && instanceClassCount > classCount )
        {
            classCount = instanceClassCount;
            classValue = instance.value( classAttribute );
        }
        // if we decremented the current leading class, make sure it's
        // still the leading class
        else if ( amount < 0 && instanceClassValue == classValue )
        {
            updateClass( );
        }
    }

    protected void updateClass( )
    {
        int maxCount = 0;
        int maxIndex = 0;
        for ( int i = 0; i < classCounts.length; i++ )
        {
            int count = classCounts[i];
            if ( count > maxCount )
            {
                maxCount = count;
                maxIndex = i;
            }
        }

        classCount = maxCount;
        //XXX assumes nominal class
        classValue = maxIndex;
    }

    protected void adjustTotalCount( int amount )
    {
        totalCount += amount;
    }

    protected void adjustClassCount( int classIndex, int amount )
    {
        classCounts[classIndex] += amount;
    }

    protected void adjustCount( Attribute attribute, Instance instance, int amount )
    {
        int attributeIndex = attribute.index( );
        int classValue = ( int ) instance.value( classAttribute );
        int attributeValue = ( int ) instance.value( attribute );
        adjustCount( attributeIndex, attributeValue, classValue, amount );
    }

    protected void adjustCount( int attributeIndex, int valueIndex, int classIndex, int amount )
    {
        counts[attributeIndex][valueIndex][classIndex] += amount;
    }
    
    /**
     * Prints the decision tree using the private toString method from below.
     *
     * @return a textual description of the classifier
     */
    public String toString( )
    {
        return toString( this, 0 );
    }

    /**
     * Outputs a tree at a certain level.
     *
     * @param level the level at which the tree is to be printed
     * @return the tree as string at the given level
     */
    protected String toString( Node node, int level )
    {

        StringBuffer text = new StringBuffer( );

        if ( node.getAttribute( ) == null )
        {
            text.append( ": " + node.getText( ) );
        }
        else
        {
            for ( int j = 0; j < node.getAttribute( ).numValues( ); j++ )
            {
                text.append( "\n" );
                for ( int i = 0; i < level; i++ )
                {
                    text.append( "|  " );
                }
                text.append( node.getText( j ) );
                text.append( toString( node.getSuccessor( j ), level + 1 ) );
            }
        }
        return text.toString( );
    }
    
    protected String getText( )
    {
        return getClassAttribute( ).value( ( int ) getClassValue( ) );
    }
    
    protected String getText( int attributeIndex )
    {
        if ( getAttribute( ) != null )
        {
            return getAttribute( ).name( ) + " = " + getAttribute( ).value( attributeIndex );
        }
        else
        {
            return getText( );
        }
    }
}