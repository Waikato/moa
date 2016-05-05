package cutpointdetection.OnePassDetector;

/**
 * Write a description of class Reservoir here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Reservoir
{
    // instance variables - replace the example below with your own
    private int i_size;
    private double d_total;
    private int i_blockSize;
    private EDDWindow e_Set;
    
    private int i_MAX_SIZE;

    /**
     * Constructor for objects of class Reservoir
     */
    public Reservoir(int _iSize, int _iBlockSize)
    {
        // initialise instance variables
        i_MAX_SIZE = _iSize;
        d_total = 0;
        i_blockSize = _iBlockSize;
        
        e_Set = new EDDWindow(i_blockSize);
    }

    /**
     * An example of a method - replace this comment with your own
     * 
     * @param  y   a sample parameter for a method
     * @return     the sum of x and y 
     */
    public double getSampleMean()
    {
        return d_total/i_size;
    }
    public void addElement(double _dValue)
    {
        try
        {
        if(i_size < i_MAX_SIZE)
        {
            e_Set.add(new Double(_dValue));
            d_total = d_total+_dValue;
            i_size++;
        }
        else
        {
            int irIndex = (int)(Math.random()*i_blockSize);
            if(irIndex < i_MAX_SIZE)
            {
                d_total = d_total- e_Set.get(irIndex);
                e_Set.addAt(irIndex,_dValue);
                d_total = d_total + _dValue;
            }
        }
        
        //System.out.println("Sample size :"+e_Set.getSize());
    }
    catch(Exception e)
    {
        System.out.println("2 Exception"+e); 
    }
                
    }
    public double get(int _iIndex)
    {
        return e_Set.get(_iIndex);
    }
    
    public int getSize()
    {
        return i_size;
    }
    public void clear()
    {
        e_Set.removeAll();
        d_total = 0;
        i_size=0;
    }
    public double getTotal()
    {
        return d_total;
    }
    public void copy(Reservoir _oSource)
    {
        //e_Set.removeAll();
        for(int iIndex=0;iIndex < _oSource.getSize();iIndex++)
        {            
            //System.out.println("Size :"+e_Set.getSize()+" "+_oSource.get(iIndex));
            addElement(_oSource.get(iIndex));
        }
        _oSource.clear();
    }
    public int getNumOfTests()
    {
        return e_Set.getNumOfTests();
    }
}
