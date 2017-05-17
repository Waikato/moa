package classifiers.thirdparty;
import static com.metsci.glimpse.util.logging.LoggerUtils.logWarning;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import edu.gmu.vfml.tree.CNode;
import edu.gmu.vfml.tree.InstanceId;
import edu.gmu.vfml.tree.Node;

/**
 * <!-- globalinfo-start -->
 * will be automatically replaced
 * <!-- globalinfo-end -->
 * 
 *  <!-- technical-bibtex-start -->
 * will be automatically replaced
 * <!-- technical-bibtex-end -->
 * 
 *  <!-- options-start -->
 * will be automatically replaced
 * <!-- options-end -->
 * 
 * @see weka.classifiers.trees.VFDT
 * @author ulman
 */

/**
public class CVFDT extends VFDT
{
    private static final Logger logger = Logger.getLogger( CVFDT.class.getName( ) );

    private static final long serialVersionUID = 1L;

    /**
     * Linked list of instances currently inside the CVFDT learning window.
     */
    protected LinkedList<InstanceId> window;

    /**
     * The maximum size of the window list.
     */
    protected int windowSize = 200000;

    /**
     * The number of data instances between rechecks of the validity of all
     * alternative trees. This is a global count on instances (see
     * splitValidityCounter).
     */
    protected int splitRecheckInterval = 20000;

    /**
     * Every altTestModeInterval instances, CNodes enter a test state where they
     * use the next altTestModeDuration instances to evaluate whether or not
     * to discard the current tree in favor of one of the Node's alternative trees.
     */
    protected int testInterval = 9000;
    protected int testDuration = 1000;

    transient protected int largestNodeId;
    transient protected int splitValidityCounter;

    public int getWindowSize( )
    {
        return windowSize;
    }

    public void setWindowSize( int windowSize )
    {
        this.windowSize = windowSize;
    }

    public int getSplitRecheckInterval( )
    {
        return splitRecheckInterval;
    }

    public void setSplitRecheckInterval( int splitRecheckInterval )
    {
        this.splitRecheckInterval = splitRecheckInterval;
    }

    public int getTestInterval( )
    {
        return testInterval;
    }

    public void setTestInterval( int testInterval )
    {
        this.testInterval = testInterval;
    }

    public int getTestDuration( )
    {
        return testDuration;
    }

    public void setTestDuration( int testDuration )
    {
        this.testDuration = testDuration;
    }

    /**
     * Lists the command line options available to this classifier.
     */
    @Override
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public Vector listOptionsVector( )
    {
        Vector list = super.listOptionsVector( );
        list.addElement( new Option( "\tWindow Size.\n", "W", 1, "-W <window size>" ) );
        list.addElement( new Option( "\tSplit Recheck Interval.\n", "F", 1, "-F <split recheck interval>" ) );
        list.addElement( new Option( "\tTest Mode Interval.\n", "I", 1, "-I <test mode interval>" ) );
        list.addElement( new Option( "\tTest Mode Duration.\n", "D", 1, "-D <test mode duration>" ) );
        return list;
    }

    /**
     * Parses a given list of options.
     * 
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions( String[] options ) throws Exception
    {
        super.setOptions( options );

        String windowSizeString = Utils.getOption( 'W', options );
        if ( !windowSizeString.isEmpty( ) )
        {
            windowSize = Integer.parseInt( windowSizeString );
        }

        String splitRecheckString = Utils.getOption( 'F', options );
        if ( !splitRecheckString.isEmpty( ) )
        {
            splitRecheckInterval = Integer.parseInt( splitRecheckString );
        }

        String altTestModeIntervalString = Utils.getOption( 'I', options );
        if ( !splitRecheckString.isEmpty( ) )
        {
            testInterval = Integer.parseInt( altTestModeIntervalString );
        }

        String altTestModeDurationString = Utils.getOption( 'D', options );
        if ( !splitRecheckString.isEmpty( ) )
        {
            testDuration = Integer.parseInt( altTestModeDurationString );
        }
    }

    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public List<String> getOptionsList( )
    {
        List<String> options = super.getOptionsList( );

        options.add( "-W" );
        options.add( String.valueOf( windowSize ) );

        options.add( "-F" );
        options.add( String.valueOf( splitRecheckInterval ) );

        options.add( "-I" );
        options.add( String.valueOf( testInterval ) );

        options.add( "-D" );
        options.add( String.valueOf( testDuration ) );

        return options;
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing 
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     * 
     * @return the technical information about this class
     */
    @Override
    public TechnicalInformation getTechnicalInformation( )
    {
        TechnicalInformation info = new TechnicalInformation( Type.ARTICLE );

        info.setValue( Field.AUTHOR, "Pedro Domingos, Laurie Spencer, and Geoff Hulten" );
        info.setValue( Field.YEAR, "2001" );
        info.setValue( Field.TITLE, "Mining Time-Changing Data Streams" );
        info.setValue( Field.JOURNAL, "Proceedings of the seventh ACM SIGKDD international conference on Knowledge discovery and data mining" );
        info.setValue( Field.SERIES, "KDD '01" );
        info.setValue( Field.ISBN, "1-58113-391-X" );
        info.setValue( Field.LOCATION, "Boston, Massachusetts, USA" );
        info.setValue( Field.PAGES, "97-106" );
        info.setValue( Field.URL, "http://dl.acm.org/citation.cfm?id=502529" );
        info.setValue( Field.PUBLISHER, "ACM" );

        return info;
    }

    @Override
    public CNode getRoot( )
    {
        return ( CNode ) root;
    }

    /**
     * Perform classifier initialization steps.
     */
    @Override
    public void initialize( Instances data ) throws Exception
    {
        super.initialize( data );

        this.window = new LinkedList<InstanceId>( );
        this.largestNodeId = 0;
        this.splitValidityCounter = 0;
    }

    public void addInstance( Instance instance )
    {
        try
        {
            // update the counts associated with this instance
            // unlike VFDT, we start at the root because we will reach multiple
            // leaf nodes in the various alternative trees
            traverseAndIncrementCounts( instance, getRoot( ) );

            // add the new instance to the window and remove old instance (if necessary)
            updateWindow( instance );

            // split nodes with attributes which have surpassed the hoeffding bound
            // and/or test the alternative subtrees for nodes in test mode
            traverseAndSplitOrTest( instance, getRoot( ) );

            // check whether new alternative nodes should be created
            if ( ++splitValidityCounter % splitRecheckInterval == 0 )
            {
                traverseAndCheckSplitValidity( instance, getRoot( ) );
            }
        }
        catch ( Exception e )
        {
            logWarning( logger, "Trouble processing instance.", e );
        }
    }

    @SuppressWarnings( "rawtypes" )
    protected void makeTree( Enumeration data )
    {
        while ( data.hasMoreElements( ) )
        {
            // retrieve the next data instance
            Instance instance = ( Instance ) data.nextElement( );

            addInstance( instance );
        }
    }
    
    @Override
    protected Node newNode( Instances instances )
    {
        return new CNode( instances, classAttribute, ++largestNodeId );
    }

    /**
     * Method for building an CVFDT tree.
     *
     * @param data the training data
     * @exception Exception if decision tree can't be built successfully
     */
    @Override
    protected void makeTree( Instances data ) throws Exception
    {
        makeTree( data.enumerateInstances( ) );
    }

    /**
     * Traverse the entire tree and determine if new alternative trees should be created.
     */
    protected void traverseAndCheckSplitValidity( Instance instance, CNode node )
    {
        // only check the validity of split for non-leaf node (i.e. nodes with splits)
        if ( node.getAttribute( ) != null )
        {
            // check the validity of the split on node.getAttribute() by
            // potentially creating a node with an alternative split
            recheckNodeSplit( instance, node );

            // traverse into all the alternative nodes
            for ( CNode alt : node.getAlternativeTrees( ) )
            {
                traverseAndCheckSplitValidity( instance, alt );
            }

            // descend into all child nodes
            int numValues = node.getAttribute( ).numValues( );
            for ( int attributeValue = 0; attributeValue < numValues; attributeValue++ )
            {
                CNode childNode = node.getSuccessor( attributeValue );
                traverseAndCheckSplitValidity( instance, childNode );
            }
        }
    }

    /**
     * In addition to incrementing the counts for the main tree,
     * increment the counts of any alternative trees being grown from this Node.
     */
    protected void traverseAndIncrementCounts( Instance instance, CNode node )
    {
        // increment the counts for this node
        // (unlike VFDT, statistics are kept for each data instance
        // at every node in the tree in order to continuously monitor
        // the validity of previous decisions)
        node.incrementCounts( instance );

        // traverse into all the alternative nodes
        for ( CNode alt : node.getAlternativeTrees( ) )
        {
            traverseAndIncrementCounts( instance, alt );
        }

        // if tree node is not a leaf node,
        // descend into the appropriate child node
        if ( node.getAttribute( ) != null )
        {
            int attributeValue = ( int ) instance.value( node.getAttribute( ) );
            CNode childNode = node.getSuccessor( attributeValue );
            traverseAndIncrementCounts( instance, childNode );
        }
    }

    /**
     * Called when an instance rolls off the window. Removes the instance
     * from the counts of each node
     */
    protected void traverseAndDecrementCounts( Instance instance, CNode node, int id )
    {
        // nodes with greater id than the instance id were created after the
        // instance arrived and do not have the instance data included in their counts
        if ( node.getId( ) <= id )
        {
            node.decrementCounts( instance );
        }

        // traverse into all the alternative nodes
        for ( CNode alt : node.getAlternativeTrees( ) )
        {
            traverseAndDecrementCounts( instance, alt, id );
        }

        // if the main tree node is not a leaf node,
        // descend into the appropriate child node
        if ( node.getAttribute( ) != null )
        {
            int attributeValue = ( int ) instance.value( node.getAttribute( ) );
            CNode childNode = node.getSuccessor( attributeValue );
            traverseAndDecrementCounts( instance, childNode, id );
        }
    }

    /**
     * <p>Traverses the main tree and alternative trees. If in test mode, classifies
     * the instance and increments the testCorrectCount if the classification
     * is correct.</p>
     * 
     * <p>If not in test mode, does nothing unless this is a leaf node and nMin
     * instances have been reached. At that point it checks for potential new
     * splits of the node.</p>
     * 
     * @param instance
     */
    protected void traverseAndSplitOrTest( Instance instance, CNode node )
    {
        node.incrementTestCount( testInterval, testDuration );

        // If we're in test mode, instead of considering splits, evaluate
        // the predicted class of this instance and compare it to the correct
        // classification then store whether or not it matches. Perform this
        // calculation for this node and each alternative node.
        if ( node.isTestMode( ) )
        {
            node.testInstance( instance );
        }

        // traverse into all the alternative nodes
        for ( CNode alt : node.getAlternativeTrees( ) )
        {
            traverseAndSplitOrTest( instance, alt );
        }

        // if tree node is not a leaf node,
        // descend into the appropriate child node
        if ( node.getAttribute( ) != null )
        {
            int attributeValue = ( int ) instance.value( node.getAttribute( ) );
            CNode childNode = node.getSuccessor( attributeValue );
            traverseAndSplitOrTest( instance, childNode );
        }
        // if we are not in test mode and the node is a leaf node and
        // the count is a multiple of nMin,
        // check to see whether we should split the node
        else if ( !node.isTestMode( ) && node.getCount( ) % nMin == 0 )
        {
            checkNodeSplit( instance, node );
        }
    }

    protected void updateWindow( Instance instance )
    {
        //XXX CVFDT Table 2 uses the largest ID *among the nodes that instance
        //XXX passes through* I think using the overall largest ID has the
        //XXX same affect, but I'm not 100% sure

        // add the new instance to the window
        // tag it with the id of the largest currently existing node
        window.addLast( new InstanceId( instance, largestNodeId ) );

        // drop the oldest instance from the window
        if ( window.size( ) > windowSize )
        {
            InstanceId oldInstanceId = window.removeFirst( );
            int oldId = oldInstanceId.getId( );

            // iterate through the tree (and all alternative trees) and decrement
            // counts if the node's id is less than or equal to oldId
            traverseAndDecrementCounts( instance, getRoot( ), oldId );
        }
    }

    @Override
    protected void splitNode( Node node, Attribute attribute, Instance instance )
    {
        ( ( CNode ) node ).split( attribute, instance, ++largestNodeId );
    }

    /**
     * Evaluates the attributes of an already split node to determine if
     * a new alternative tree should be created.
     * 
     * @see VFDT#checkNodeSplit(Instance, Node)
     */
    protected void recheckNodeSplit( Instance instance, CNode node )
    {
        // determine based on Hoeffding Bound whether to split node
        int firstIndex = 0;
        double firstValue = Double.MAX_VALUE;
        double secondValue = Double.MAX_VALUE;

        // loop through all the attributes, calculating information gains
        // and keeping the attributes with the two highest information gains
        for ( int attrIndex = 0; attrIndex < instance.numAttributes( ); attrIndex++ )
        {
            // don't consider the class attribute
            if ( attrIndex == classAttribute.index( ) ) continue;
            // don't consider the current split attribute
            if ( attrIndex == node.getAttribute( ).index( ) ) continue;

            Attribute attribute = instance.attribute( attrIndex );
            double value = computeEntropySum( node, attribute );

            if ( value < firstValue )
            {
                secondValue = firstValue;
                firstValue = value;
                firstIndex = attrIndex;
            }
            else if ( value < secondValue )
            {
                secondValue = value;
            }
        }

        // if the difference between the information gain of the two best attributes
        // has exceeded the Hoeffding bound (which will continually shrink as more
        // attributes are added to the node) then split on the best attribute 
        double hoeffdingBound = calculateHoeffdingBound( node );

        boolean alreadyExists = node.doesAltNodeExist( firstIndex );
        // split if there is a large enough entropy difference between the first/second place attributes
        boolean confident = secondValue - firstValue > hoeffdingBound;
        // or if the first/second attributes are so close that the hoeffding bound has decreased below
        // the tie threshold (in this case it really doesn't matter which attribute is chosen
        boolean tie = tieConfidence > hoeffdingBound && secondValue - firstValue >= tieConfidence / 2.0;

        if ( !alreadyExists && ( tie || confident ) )
        {
            Attribute attribute = instance.attribute( firstIndex );
            node.addAlternativeNode( instance, attribute, ++largestNodeId );
        }
    }
}
