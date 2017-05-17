package edu.gmu.vfml.tree;

/**
 * A helper class used to keep track of test statistics for alternative trees in CVFDT.
 * 
 * @see CNode
 * @author ulman
 */
public class TestStats
{
    private int totalCount = 0;
    private int correctCount = 0;
    
    /**
     * Only valid for alternative nodes. Stores the previous best error difference
     * between the main tree and the alternative node. Used for pruning unpromising
     * alternative trees.
     */
    // start with infinitely bad error
    private double bestErrorDiff = -Double.MAX_VALUE;

    /**
     * A flag set when a new alternative node is first created.
     * Necessary because the test interval is based on per-node counts and the
     * alternative node creation interval is based on the overall instance count.
     * Therefore, a new alternative node may get created in the middle of the test
     * phase for a node. This node shouldn't be included in the test (since it
     * doesn't have a full set of instances).
     */
    private boolean isNew = false;
    
    public TestStats( )
    {
        
    }
    
    public double getBestError( )
    {
        return bestErrorDiff;
    }

    public void setBestError( double bestError )
    {
        this.bestErrorDiff = bestError;
    }
    
    public boolean isNew( )
    {
        return this.isNew;
    }
    
    public void setNew( boolean isNew )
    {
        this.isNew = isNew;
    }
    
    public void reset( )
    {
        this.totalCount = 0;
        this.correctCount = 0;
        
        // mark all the alternative nodes as not new so they will
        // participate in the next round of testing
        this.isNew = false;
    }
    
    public void correct( )
    {
        this.totalCount++;
        this.correctCount++;
    }
    
    public void incorrect( )
    {
        this.totalCount++;
    }
    
    public double getError( )
    {
        return 1.0 - (double) correctCount / (double) totalCount;
    }
}
