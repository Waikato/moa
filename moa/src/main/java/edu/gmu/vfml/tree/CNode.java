package edu.gmu.vfml.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * <p>A helper class for {@code weka.classifiers.trees.CVFDT}. Stores the nested
 * tree structure of attribute splits and associated Node statistics.</p>
 * 
 * @author ulman
 */
public class CNode extends Node
{
    private static final long serialVersionUID = 1L;

    /**
     * A map of subtrees made from splitting on alternative Attributes (instead of
     * splitting on the Attribute specified in this.attribute).
     */
    protected Map<Attribute, CNode> altNodes = new LinkedHashMap<Attribute, CNode>( );
    protected Map<Attribute, TestStats> altStats = new LinkedHashMap<Attribute, TestStats>( );

    /**
     * @see InstanceId
     */
    protected int id;

    /**
     * Number of instances until entering/exiting next test phase.
     */
    transient protected int testCount = 0;

    /**
     * The number of correctly classified test instances.
     */
    transient protected int testCorrectCount = 0;

    /**
     * If true, new data instances are not used to grow the tree. Instead, they are
     * used to compare the error rate of this Node to that of its subtrees.
     */
    transient protected boolean testMode = false;

    public CNode( Attribute[] attributes, Attribute classAttribute, int id )
    {
        super( attributes, classAttribute );
        this.id = id;
    }

    public CNode( Instances instances, Attribute classAttribute, int id )
    {
        super( instances, classAttribute );
        this.id = id;
    }

    public CNode( Instance instance, Attribute classAttribute, int id )
    {
        super( instance, classAttribute );
        this.id = id;
    }
    
    /**
     * Modified this Node to look like the provided node. Does not deep copy fields.
     */
    @Override
    public void copyNode( Node node )
    {
        super.copyNode( node );
        
        if ( node instanceof CNode )
        {
            CNode cnode = (CNode) node;
            
            this.id = cnode.id;
            this.altNodes = cnode.altNodes;
            this.altStats = cnode.altStats;
        }
    }

    @Override
    public CNode getLeafNode( Instance instance )
    {
        return ( CNode ) getLeafNode( this, instance );
    }

    @Override
    public CNode getSuccessor( int value )
    {
        if ( successors != null )
        {
            return ( CNode ) successors[value];
        }
        else
        {
            return null;
        }
    }

    public Collection<CNode> getAlternativeTrees( )
    {
        return altNodes.values( );
    }

    /**
     * Determines the class prediction of the tree rooted at this node for the given instance.
     * Compares that prediction against the true class value, and if they are equal, increments
     * the correct test counter for this node.
     * 
     * @param instance
     */
    public void testInstance( Instance instance )
    {
        // test this node
        double predicted = getLeafNode( instance ).getClassValue( );
        double actual = instance.classValue( );
        if ( predicted == actual )
        {
            this.testCorrectCount++;
        }
        
        // test all alternative nodes
        Iterator<Attribute> iter = altNodes.keySet( ).iterator( );
        while ( iter.hasNext( ) )
        {
            Attribute attribute = iter.next( );
            CNode alt = altNodes.get( attribute );
            TestStats stats = altStats.get( attribute );
            
            double altPredicted = alt.getLeafNode( instance ).getClassValue( );
            if ( altPredicted == actual )
            {
                stats.correct( );
            }
            else
            {
                stats.incorrect( );
            }
        }
    }

    public void incrementTestCount( int testInterval, int testDuration )
    {
        // check whether we should enter or exit test mode
        this.testCount++;
        if ( this.testMode )
        {
            if ( this.testCount > testDuration )
            {
                endTest( );
            }
        }
        else
        {
            if ( this.testCount > testInterval )
            {
                startTest( );
            }
        }
    }
    
    public boolean isTestMode( )
    {
        return this.testMode;
    }

    public int getTestCount( )
    {
        return this.testCount;
    }

    public double getTestError( )
    {
        return 1 - ( double ) this.testCorrectCount / ( double ) this.testCount;
    }

    public boolean doesAltNodeExist( int attributeIndex )
    {
        for ( Attribute altAttribute : altNodes.keySet( ) )
        {
            if ( altAttribute.index( ) == attributeIndex ) return true;
        }

        return false;
    }

    public boolean doesAltNodeExist( Attribute attribute )
    {
        return doesAltNodeExist( attribute.index( ) );
    }

    public void addAlternativeNode( Instance instance, Attribute attribute, int newId )
    {
        // create the alternative node and immediately split it on the new attribute
        CNode node = new CNode( instance, classAttribute, newId );
        node.split( attribute, instance, newId );
        
        TestStats stats = new TestStats( );
        
        // the new alternative node should not be tested if this CNode is currently
        // in test mode (wait until the next test mode)
        if ( isTestMode( ) )
        {
            stats.setNew( true );
        }

        altStats.put( attribute, stats );
        altNodes.put( attribute, node );
    }

    /**
     * Called when enough data instances have been seen that it is time to end test mode.
     */
    protected void endTest( )
    {
        Attribute bestAttribute = null;
        CNode bestAlt = null;
        double bestErrorDiff = 0;
        double mainError = getTestError( );
        Iterator<Attribute> iter = altNodes.keySet( ).iterator( );
        while ( iter.hasNext( ) )
        {
            Attribute attribute = iter.next( );
            CNode alt = altNodes.get( attribute );
            TestStats stats = altStats.get( attribute );
            
            // if an alternative tree was created while we were in
            // test mode, it will not yet be in test mode and will
            // not have collected enough examples yet to evaluate it properly
            // so skip it until the next test mode
            if ( !stats.isNew( ) )
            {
                double altError = stats.getError( );
                double errorDiff = mainError - altError;

                // if the error difference improved, record the new improved value
                if ( errorDiff > stats.getBestError( ) )
                {
                    stats.setBestError( errorDiff );
                }
                // if the error difference decreased by 1% below the current
                // best, then drop the alternative node
                else if ( errorDiff < stats.getBestError( ) * 1.01 )
                {
                    //TODO having two maps is awkward (it's only done because we sometimes
                    //     want lists of just CNodes)
                    iter.remove( );
                    altStats.remove( attribute );
                }

                // remember the alternative node with the best error
                if ( bestErrorDiff < errorDiff )
                {
                    bestAttribute = attribute;
                    bestErrorDiff = errorDiff;
                    bestAlt = alt;
                }
            }

            stats.reset( );
        }

        // one of the alternative trees is better than the current tree!
        // replace this node with the alternative node
        if ( bestAlt != null )
        {
            this.copyNode( bestAlt );
            // remove the alternative node which was promoted
            // from the list of alternative nodes
            this.altNodes.remove( bestAttribute );
            this.altStats.remove( bestAttribute );
        }

        this.testCorrectCount = 0;
        this.testCount = 0;
        this.testMode = false;
    }

    /**
     * Called when enough data instances have been seen that it is time to enter test mode.
     */
    protected void startTest( )
    {
        this.testCorrectCount = 0;
        this.testCount = 0;

        // if there are no alternative nodes to test, don't enter test mode (wait another
        // testInterval instances then check again)
        this.testMode = !this.altNodes.isEmpty( );
    }

    /**
     * Like {@code Node#split(Attribute, Instance)}, but creates CNodes and
     * assigns the specified id to the Node.
     */
    public void split( Attribute attribute, Instance instance, int id )
    {
        this.successors = new CNode[attribute.numValues( )];
        this.attribute = attribute;

        for ( int valueIndex = 0; valueIndex < attribute.numValues( ); valueIndex++ )
        {
            this.successors[valueIndex] = new CNode( instance, classAttribute, id );
        }
    }

    /**
     * @see InstanceId
     */
    public int getId( )
    {
        return id;
    }
    
    @Override
    protected String getText( )
    {
        if ( altNodes.isEmpty( ) )
        {
            return String.format( "id %d %s", id, super.getText( ) );
        }
        else
        {
            return String.format( "id %d %s (%d alt)", id, super.getText( ), altNodes.size( ) );
        }
    }
    
    @Override
    protected String getText( int attributeIndex )
    {
        if ( altNodes.isEmpty( ) )
        {
            return String.format( "id %d %s", id, super.getText( attributeIndex ) );
        }
        else
        {
            return String.format( "id %d %s (%d alt)", id, super.getText( attributeIndex ), altNodes.size( ) );
        }
    }
}
