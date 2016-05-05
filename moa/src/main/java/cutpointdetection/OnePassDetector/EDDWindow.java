package cutpointdetection.OnePassDetector;
 
import java.util.ArrayList;
/**
 *
 * @author sripirakas
 */
public class EDDWindow
{
	int                 i_blockSize;
	ArrayList<EDDBlock> arr_BlockIndex;
	int                 i_LastBlockIndex;
	int                 i_totalElements;
	double              d_total;

	public EDDWindow(int _iBlockSize)
	{	
		i_blockSize         = _iBlockSize;
                i_LastBlockIndex    = -1;
                i_totalElements     = 0;
                d_total             = 0;
		arr_BlockIndex      = new ArrayList<EDDBlock>();
	}
	public void add(double _dValue)
	{
		if((i_totalElements % i_blockSize) == 0)
		{
			arr_BlockIndex.add(new EDDBlock(i_blockSize));
			i_LastBlockIndex++;
		}
		arr_BlockIndex.get(i_LastBlockIndex).add(_dValue);
		i_totalElements++;
		d_total = d_total + _dValue;
	}
        public void add(double _dValue, boolean _isTested)
	{
		if((i_totalElements % i_blockSize) == 0)
		{
			arr_BlockIndex.add(new EDDBlock(i_blockSize, _isTested));
			i_LastBlockIndex++;
		}
		arr_BlockIndex.get(i_LastBlockIndex).add(_dValue);
		i_totalElements++;
		d_total = d_total + _dValue;
	}
        

	public double get(int _iIndex)
	{
		return arr_BlockIndex.get(_iIndex/i_blockSize).d_data[(_iIndex % i_blockSize)];

	}
	public void addAt(int _iIndex, double _dValue)
	{	
	    arr_BlockIndex.get(_iIndex/i_blockSize).addAt(_iIndex,_dValue);
	}
	public void removeFirstBlock()
	{
		d_total = d_total - arr_BlockIndex.get(0).d_total;
		arr_BlockIndex.remove(0);
		i_totalElements = i_totalElements - i_blockSize;
		i_LastBlockIndex--;
	}	
	
	public int getSize()
	{
		return i_totalElements;
	}	

	public double getTotal()
	{
		double dTotal = 0.0;
		for(int iIndex = 0; iIndex < arr_BlockIndex.size() ;iIndex++)
		{
			dTotal = dTotal + arr_BlockIndex.get(iIndex).d_total;
		}
                return dTotal;
	}

	public double getFirstBlockTotal()
	{
		return arr_BlockIndex.get(0).d_total;

	}
        
        public void removeAll()
        {
            arr_BlockIndex.clear();
            i_LastBlockIndex = -1;
            i_totalElements = 0;
            d_total = 0; 
        }    
        
        public void markLastAddedBlock()
        {
            if(arr_BlockIndex.size() > 0)
            {
                arr_BlockIndex.get(arr_BlockIndex.size() - 1).setTested(true);
            }
        }
        
        public int getNumOfTests()
        {
            int iNumTests = 0;
           // for(int iIndex = 0; iIndex < arr_BlockIndex.size(); iIndex++)
           // {
           //     if(arr_BlockIndex.get(iIndex).IsTested())
           //         iNumTests++;
           // }
            return iNumTests;
        }
        
        public void getDescription(StringBuilder sb, int indent) {
        }
}