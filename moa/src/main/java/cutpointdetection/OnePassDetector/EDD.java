package cutpointdetection.OnePassDetector;
import java.util.ArrayList;

import cutpointdetection.CutPointDetector;

public class EDD implements CutPointDetector
{
    public static double[] d_ErrorValues      = null;    
    public static boolean b_IsCalculated = false;
    
    private Reservoir rightReservoir = null;
    private Reservoir leftReservoir = null;
    private ArrayList<Integer> arr_UniqueNumbers = null;    
    
    //parameters
    private double  d_sigLevel          = 0.01;
    private int     i_blockSize         = 100;
    private int     i_sampleSize        = i_blockSize;
    private int     i_slidingWindowSize; //if < 0 then no sliding
    private int     i_inequality        = BERNSTEIN;
    private int     i_reportingType     = DRIFT_ONLY;
    private int     i_errorCorrection   = SERIES;

    //variables
    private int     i_instanceCounter   = 0;
    private double  d_setskMean         = 0.0;
    private double  d_setsjMean         = 0.0;
    private int     i_blockCounter      = 0;
    private double  d_variance          = 0.0;
    private boolean b_IsDrift           = false;
    private double  d_Total             = 0.0;
    
    //Sir
    private int m=0;
    
    private int     i_numInstances;
    
    private Epsilon o_Epsilon           = null;
    
    private double  d_minStdevProbability   = Double.MAX_VALUE;
    private double  d_minProbability        = Double.MAX_VALUE;
    private double  d_minStdev              = Double.MAX_VALUE;
    

    public final static int BERNSTEIN   = 1;
    public final static int ADWINBOUND  = 2;
    public final static int HOEFFDING = 3;

    public final static int DRIFT_ONLY          = 0;
    public final static int WARNING_ONLY        = 1;
    public final static int DRIFT_WARNING_ONLY  = 2;
    public static final int ALL                 = 3;
    
    public final static  int DRIFT         = 0;
    public final static  int WARNING        = 1;
    public final static  int HOMOGENEOUS    = 2;
    public final static  int INTERNAL_DRIFT = 3;
    
    
    public final static int BONFERRONI = 0;
    public final static int NOCOORECTION = 1;
    public final static int SERIES = 2;
    
    //
    private double pWeight = 0.0;
    private int TTL = 25;
    
    public double getWeight()
    {
	return pWeight;
    }
    
    public void setWeight(double value)
    {
	this.pWeight = value;
    }
    
    //
     
//    public EDD(double _dsigLevel, int _iBlockSize, int _iSlidingWindowSize,  int _iInequality)
//    {
//        //Parameters
//        d_sigLevel          = _dsigLevel;
//        i_blockSize         = _iBlockSize;
//        i_sampleSize        = _iBlockSize;
//        i_slidingWindowSize = _iSlidingWindowSize;
//        i_inequality        = _iInequality;
//
//        i_reportingType     = DRIFT_ONLY;
//
//        //Variables
//        i_instanceCounter   = 0;
//        i_blockCounter      = 0;
//        d_variance          = 0;
//        b_IsWarning         = false;
//        b_IsDrift           = false;
//        d_Total             = 0.0;
//        d_ErrorValues       = new double[i_slidingWindowSize * 2];
//        o_Epsilon           = new Epsilon();
//        d_warningSigLevel   = 0.05;
//               
//        //Data Structures
//        
//        leftReservoir = new Reservoir(i_blockSize);
//        rightReservoir = new Reservoir(i_blockSize);
//        arr_UniqueNumbers   = new ArrayList<Integer>();
//                
//        fillErrorValues();        
//    }

    public EDD(double _dsigLevel, int _iBlockSize, int _iInequality, int _iErrorCorrection)
    {
        //Parameters
        d_sigLevel          = _dsigLevel;
        i_blockSize         = _iBlockSize;
        i_sampleSize        = _iBlockSize;
        //i_slidingWindowSize = _iSlidingWindowSize; //Sliding window size is automaticall set as 1/delta
        i_inequality        = _iInequality;
        i_errorCorrection   = _iErrorCorrection;

        i_reportingType     = DRIFT_WARNING_ONLY;

        //Variables
        i_instanceCounter   = 0;
        i_blockCounter      = 0;
        d_variance          = 0;
        b_IsDrift           = false;
        d_Total             = 0.0;
        //d_ErrorValues       = new double[i_slidingWindowSize * 2];
        o_Epsilon           = new Epsilon();

        //Data Structures
        
        leftReservoir = new Reservoir(2000,i_blockSize);
        rightReservoir = new Reservoir(50,i_blockSize);
        
        arr_UniqueNumbers   = new ArrayList<Integer>();
        
    }

    public boolean setInput(double _dInputValue)
    {        
        ++i_instanceCounter;
        i_numInstances++;

        addToRightReservoir(_dInputValue);
        d_Total = d_Total + _dInputValue;        
      
        if( (i_instanceCounter % i_sampleSize) == 0) //Drift point check
        {
            //rightReservoir.markLastAddedBlock();
            
            //Assume that there is no warning or drift now
            b_IsDrift = false;
            
            //System.out.println("Instance count :"+i_instanceCounter);
           //System.out.println("Total :"+d_Total);

            //Detect a drift with warning significance level

            //System.out.println("Drift Type :"+iDriftType);
            //displayStatistics();
            
            updateWeight();
            driftAction(getDriftType());            

        }        
        return false;
    }
    
    public void driftAction(int DRIFTTYPE)
    {
        int iDriftType =  DRIFTTYPE;  
        
        if(iDriftType == DRIFT) 
        {
            b_IsDrift   = true;  //Setting system state to drift
            clearLeftReservoir();//Due to drift clear the data that belong to the old concept in Sk
            moveRightToLeft(); //All instances in Sj should be moved to Sk if no drift or if drift but not in warning
            i_numInstances = 0;
            m=0;
        }
        else if(iDriftType == INTERNAL_DRIFT)
        {
            b_IsDrift   = false;  
            clearLeftReservoir(); //Due to drift clear the data that belong to the old concept in Sk
            moveRightToLeft();
            i_numInstances = 0;
            m=0;
        }
        else //No drift is detected
        {           
            b_IsDrift   = false;
            moveRightToLeft(); //All instances in Sj should be moved to Sk if no drift or if drift but not in warning             
        }
    }

    /**
     * Adding new instance to Set Sj
     * <p>
     * @param  _dInputValue  A double instance
     * @return  void
     */
    private void addToRightReservoir(double _dInputValue)
    {    
        rightReservoir.addElement(_dInputValue);
    }

     /**
     * Set Statistical Reporting Type
     * <p>
     * @param  _iReportingType  The type of the reporting. The possible values can be DRIFT_ONLY, WARNING_ONLY, DRIFT_WARNING_ONLY and ALL
     * @return  void
     */
    public void setReportingType(int _iReportingType)
    {
        i_reportingType = _iReportingType;
    }

    /**
     * Moving the instances from Set Sj to Set Sk
     * <p>
     * @param   void
     * @return  void
     */
    private void moveRightToLeft()
    {              
        leftReservoir.copy(rightReservoir);
    }

    /**
     * Remove all elements from Sk after a drift is detected
     * <p>
     * After deletion the block counter is set to zero
     * @param   void
     * @return  void
     */
    private void clearLeftReservoir()
    {
        i_blockCounter= 0;
        d_Total = d_Total - leftReservoir.getTotal();
        leftReservoir.clear();
    }

    public void updateWeight()
    {
        if(getWidth() > i_blockSize)
        {
            d_setskMean = getLeftReservoirMean(); //Get the subsample mean from Set Sk
            d_setsjMean = getSetSjSampleMean(); //Get the subsample mean from Set Sj
            o_Epsilon   = getEpsilon();
          
            
            if(i_instanceCounter > i_sampleSize && leftReservoir.getSize() > 0)
            {
        	//
//        	System.out.println(d_setsjMean + "," + d_setskMean);
//        	System.out.println(o_Epsilon.d_driftEpsilon);
//       	System.out.println(Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1);
        	pWeight = (Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1) > pWeight ? Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1 : pWeight;
        	
//        	if(TTL > 0)
//        	{
//        	    if((Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1) > pWeight)
//        	    {
//        		pWeight = Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1;
//        		TTL = 100;
//        	    }
//        	    TTL--;
//        	}
//        	else
//        	{
//        	    pWeight = Math.abs(d_setsjMean - d_setskMean) - o_Epsilon.d_driftEpsilon + 1;
//        	    TTL = 100;
//        	}
//        	//
            }
        }
    }
    
    /**
     * Detects a drift
     * <p>
     * @param   _bIsWarning Drift detection in warning significance level or actual significance level
     * @return  boolean True - if drift is detected. False - otherwise
     */
    private int getDriftType()
    {
        if(getWidth() > i_blockSize)
        {
            d_setskMean = getLeftReservoirMean(); //Get the subsample mean from Set Sk
            d_setsjMean = getSetSjSampleMean(); //Get the subsample mean from Set Sj
            o_Epsilon   = getEpsilon();
          
            
            if(i_instanceCounter > i_sampleSize && leftReservoir.getSize() > 0)
            {
                    if(o_Epsilon.d_driftEpsilon <= Math.abs(d_setsjMean - d_setskMean )) //Drift is detetced
                    {
                        if(d_setsjMean > d_setskMean)
                        {                           
                            return DRIFT;
                        }
                        else
                        {
                            return INTERNAL_DRIFT;
                        }
                }
                else
                {
                    return HOMOGENEOUS;
                }
            }
            return HOMOGENEOUS;
        }
        else
        {
            return HOMOGENEOUS;
        }
    }

    /**
     * Returns the subsample mean from
     * <p>
     * @param  _iReportingType  The type of the reporting. The possible values can be DRIFT_ONLY, WARNING_ONLY, DRIFT_WARNING_ONLY and ALL
     * @return  void
     */
    private double getLeftReservoirMean()
    {
        return leftReservoir.getSampleMean();
    }
    
    private double getSetSjSampleMean()
    {
        return rightReservoir.getSampleMean();
    }
   
    private double getVariance()
    {
    double dMean = getPopulationMean();
    double d1minusMean = dMean - 1;
    double dtotalsize = getWidth();
    double x = getTotal()*d1minusMean*d1minusMean + (dtotalsize - getTotal())*dMean*dMean;
    double y = dtotalsize -1;
    return x/y;
    }
    
    private Epsilon getEpsilon()
    {
        int iNumberOfTests = leftReservoir.getNumOfTests() + rightReservoir.getNumOfTests() -1;
        iNumberOfTests = i_numInstances / i_blockSize;
        
        if(iNumberOfTests >= 1)
        {        
            if(i_inequality == BERNSTEIN)
            {
                d_variance = getVariance();                
                m++;
                //Drift epsilon
                double ddeltadash = getDriftEpsilon(iNumberOfTests);             
                double x = Math.log(2.0/ddeltadash);            
                double drootterm = Math.sqrt(x*x + 18*d_variance*m*x);
                double depsilon = (1.0/(3*m))*(x+drootterm);
                //double depsilon = (2.0/(3*i_sampleSize))*(x + drootterm);  
                o_Epsilon.d_driftEpsilon = depsilon;

            }
            
            else if(i_inequality == ADWINBOUND) 
            {
                //Drift epsilon
                double ddeltadash = getDriftEpsilon(iNumberOfTests);
                double m = i_sampleSize / 2.0;
                d_variance = getVariance();
                double x = Math.log(2.0/ddeltadash);
                o_Epsilon.d_driftEpsilon = Math.sqrt((2.0/m)*d_variance*x)+ (2.0/(3*m))*x;
                                
            }
            else if(i_inequality == HOEFFDING)
            {
                //Drift epsilon
                double ddeltadash = getDriftEpsilon(iNumberOfTests);
                double x = Math.log(4.0/ddeltadash);
                o_Epsilon.d_driftEpsilon = Math.sqrt((2.0/i_sampleSize)*x);
            }
        }
        return o_Epsilon;
    }
    
    private double getDriftEpsilon(int _inumTests)
    {
        double ddeltadash = d_sigLevel;
        double dSeriesTotal = 2.0*(1.0 - Math.pow(0.5,_inumTests));
        if(i_errorCorrection == SERIES)
              ddeltadash = d_sigLevel/dSeriesTotal;           
            //ddeltadash = d_sigLevel*d_ErrorValues[_inumTests - 1];
        else if(i_errorCorrection == BONFERRONI)
            ddeltadash = d_sigLevel/_inumTests;
        else if(i_errorCorrection == NOCOORECTION)
            ddeltadash = d_sigLevel;
        
        return ddeltadash;
    }
    
    private double getPopulationMean()
    {
        return getTotal()/getWidth();
    }
    private double getTotal()
    {
        return rightReservoir.getTotal()+leftReservoir.getTotal();
    }

    private void displayStatistics()
    {
         System.out.println("Statistics");
         System.out.println("********************************************");
//         if(b_IsWarning)
//         {
//             System.out.println("System State        :"+ "Warning");
//         }
//         else if(b_IsDrift)
//         {
//             System.out.println("System State        :"+ "Drift");
//         }
//
         System.out.println("Instance Counter    :"+ i_instanceCounter);
//         System.out.println("Block Counter       :"+ i_blockCounter);
//         System.out.println("Size of Sk          :"+ arr_SkSet.size());
//         System.out.println("Size of Sj          :"+ arr_SjSet.size());
//
         System.out.println("Left Reservoir Mean    :"+leftReservoir.getSampleMean());
         System.out.println("Right Reservoir Mean   :"+rightReservoir.getSampleMean());
//         if(i_inequality == HOEBERN || i_inequality == BERNSTEIN)
//         {
//             System.out.println("Window mean         :"+d_populationMean);
        System.out.println("Variance                :"+d_variance);
//         }
        System.out.println("Epsilon                 :"+o_Epsilon.d_driftEpsilon);
//         System.out.println();

    }

    public int getWidth()
    {
        return leftReservoir.getSize() + rightReservoir.getSize();
    }


    public double getEstimation()
    {
        int iWidth = getWidth();

        if(iWidth != 0)
            return getTotal()/getWidth();
        else
            return 0;
    }

    public void getDescription(StringBuilder sb, int indent) {
    }

    private static double calculateError(int _iTotalBlockLength)
    {
        int n =_iTotalBlockLength - 1;
        double dTotalError = 0.0;
        for(int j = 1; j <= n; j++)
        {
            double dTotal = 1.0;
            for(int k = n; k >= j; k--)
            {
                dTotal = dTotal * (k+1);
            }
            dTotalError = dTotalError + 1/dTotal;
        }
        return (dTotalError + 1);
    }
    
    private static void fillErrorValues()
    {
        for(int iIndex = 0; iIndex < d_ErrorValues.length; iIndex++)
        {
            d_ErrorValues[iIndex] = calculateError(iIndex + 1);
        }
        b_IsCalculated = true;
    }
    
    //Gama's method
    public boolean IsWarning()
    {
        int iWidth = getWidth();
        double dErrorProbability = d_Total/iWidth;
        double dStdev = Math.sqrt((dErrorProbability*(1 - dErrorProbability))/iWidth);
        double dStdevProbability = dErrorProbability + dStdev;
        if(d_minStdevProbability >= dStdevProbability)
        {
            d_minProbability        = dErrorProbability;
            d_minStdev              = dStdev;
            d_minStdevProbability   = dStdevProbability;
        }
        
        
        if((dErrorProbability + dStdev) > (d_minProbability + 3*d_minStdev ))
        {
            d_minProbability = Double.MAX_VALUE;
            d_minStdev = Double.MAX_VALUE;
            d_minStdevProbability = Double.MAX_VALUE;
            return true;
        }
        return false;
    }
    
}

class Epsilon
{
    double d_warningEpsilon = 0.0;
    double d_driftEpsilon = 0.0;

    public Epsilon() 
    {
        d_warningEpsilon = 0.0;
        d_driftEpsilon = 0.0;
    }   
    
    public void getDescription(StringBuilder sb, int indent) {
    }
    
}