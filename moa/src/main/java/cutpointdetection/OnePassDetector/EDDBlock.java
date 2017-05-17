package cutpointdetection.OnePassDetector;
 
/**
 *
 * @author sripirakas
 */
public class EDDBlock
    {
	public  double  d_data[];
	public  double  d_total;
	private int     i_LastIndex;
        private boolean b_IsTested;
	

	EDDBlock(int _iLength)
	{
		d_data = new double[_iLength];
                d_total     = 0.0;
                i_LastIndex = 0;
                b_IsTested = false;
                for(int iIndex=0;iIndex < d_data.length;iIndex++)
                {
                    d_data[iIndex] = -1;
                }
	}
        EDDBlock(int _iLength, boolean _isTested)
	{
		d_data = new double[_iLength];
                d_total     = 0.0;
                i_LastIndex = 0;
                b_IsTested = _isTested;
                for(int iIndex=0;iIndex < d_data.length;iIndex++)
                {
                    d_data[iIndex] = -1;
                }
	}
        

	public void add(double _dValue)
	{
		if(i_LastIndex < d_data.length)
		{
			d_data[i_LastIndex] = _dValue;
			d_total = d_total + _dValue;
			i_LastIndex++;
		}
		else
                {
                    System.out.println("ERROR in adding to EDDBlock. Last Index :"+i_LastIndex + " Total :"+d_total+" Array Length :"+d_data.length);
                    System.exit(2);		
                }
	}
	public void addAt(int _iIndex, double _dNewValue)
	{
	    d_total = d_total - d_data[_iIndex];
	    d_total = d_total + _dNewValue;
	    d_data[_iIndex] = _dNewValue;
	}
        public void setTested(boolean _isTested)
        {
            b_IsTested = _isTested;
        }
        public boolean IsTested()
        {
            return b_IsTested;
        }
       
        public void getDescription(StringBuilder sb, int indent) {
    }
}