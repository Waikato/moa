/*
 *    SeqDrift1ChangeDetector.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Sakthithasan Sripirakas sripirakas363 at yahoo dot com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package moa.classifiers.core.driftdetection;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import java.util.ArrayList;
import moa.AbstractMOAObject;
import moa.classifiers.core.driftdetection.SeqDrift2ChangeDetector.Repository;

/**
 * SeqDrift1ChangeDetector.java. This extends Abstract Change Detector 
 * as required by MOA.
 * 
 * Sakthithasan, S., Pears, R., & Koh, Y. (2013). One Pass Concept Change
 * Detection for Data Streams. In J. Pei, V. Tseng, L. Cao, H. Motoda, & G. Xu
 * (Eds.), Advances in Knowledge Discovery and Data Mining (Vol. 7819, pp.
 * 461-472): Springer Berlin Heidelberg.
 *
 * @author Sakthithasan Sripirakas sripirakas363 at yahoo dot com
 * @version $Revision: 7 $
 */
public class SeqDrift1ChangeDetector extends AbstractChangeDetector {

    protected SeqDrift1 seqDrift1;
    //protected ADWIN adwin;

    public FloatOption deltaOption = new FloatOption("deltaSeqDrift1", 'd',
            "Delta of SeqDrift1 change detection",0.01, 0.0, 1.0);
    
    public FloatOption deltaWarningOption = new FloatOption("deltaWarningOption", 
            'w', "Delta of SeqDrift1 change detector to declare warning state",0.1, 0.0, 1.0);
    
    public IntOption blockSeqDriftOption = new IntOption("blockSeqDrift1Option",'b',"Block size of SeqDrift1 change detector", 200, 100, 10000);

    @Override
    public void input(double inputValue) {
        if (this.seqDrift1 == null) {
            resetLearning();
        }
        this.isChangeDetected = seqDrift1.setInput(inputValue);
        this.isWarningZone = false;
        this.delay = 0.0;
        this.estimation = seqDrift1.getEstimation();
    }

    @Override
    public void resetLearning() {
        seqDrift1 = new SeqDrift1((double) this.deltaOption.getValue(),((int) this.blockSeqDriftOption.getValue()), ((double) this.deltaWarningOption.getValue()));
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
    

/**
 * SeqDrift1 uses sliding window to build a sequential change detection model
 * that uses statistically sound guarantees defined using Bernstein Bound on
 * false positive and false negative rates. This is a block based approach and
 * checks for changes in the data values only at block boundaries as opposed to
 * the methods on per instance basis. SeqDrift1 maintains a sliding window and
 * repository. Repository gathers the new instances and sliding window stores
 * only the data values that are statistically not different, in other words
 * from the same distribution. If the data values in the repository are
 * consistent with the values in sliding window the data values of the
 * repository are copied to the sliding window applying reservoir algorithm. The
 * hypothesis is that the mean values of the sliding window and right repository
 * are not statistically different. In addition, SeqDrift1 declares a warning
 * state depending on warning significance level and increases sample size to
 * get a statistically more rigorous mean value
 *
 *
 *
 * Sakthithasan, S., Pears, R., & Koh, Y. (2013). One Pass Concept Change
 * Detection for Data Streams. In J. Pei, V. Tseng, L. Cao, H. Motoda, & G. Xu
 * (Eds.), Advances in Knowledge Discovery and Data Mining (Vol. 7819, pp.
 * 461-472): Springer Berlin Heidelberg.
 *
 * @author Sakthithasan Sripirakas sripirakas363 at yahoo dot com
 */
public class SeqDrift1 extends AbstractMOAObject {

    private Repository leftRepository = null;
    private Repository rightRepository = null;
    private ArrayList<Integer> uniqueRandomNumbers = null;

    //parameters
    private double significanceLevel = 0.01;
    private int blockSize = 200;
    private int sampleSize = 200;
    private int slidingWindowBlockCount; 
    private double warningSignificanceLevel = 0.1;

    //variables
    private int instanceCount = 0;
    private double leftRepositoryMean = 0.0;
    private double rightRepositoryMean = 0.0;
    private int blockCount = 0;
    private double variance = 0.0;
    private boolean isWarning = false;
    private double total = 0.0;

    private Epsilon epsilon = null;

    public final static int DRIFT = 0;
    public final static int WARNING = 1;
    public final static int HOMOGENEOUS = 2;
    public final static int INTERNAL_DRIFT = 3;

    public SeqDrift1(double _significanceLevel, int _blockSize, double _significanceWarningLevel) {
        //Parameters
        significanceLevel = _significanceLevel;
        blockSize = _blockSize;
        sampleSize = _blockSize;
        slidingWindowBlockCount = (int) (1 / _significanceLevel); //Sliding window size is automaticall set as 1/delta
        warningSignificanceLevel = _significanceWarningLevel;

        //Variables
        instanceCount = 0;
        blockCount = 0;
        variance = 0.0;
        isWarning = false;
        total = 0.0;
        epsilon = new Epsilon();

        //Data Structures    
        SeqDrift2ChangeDetector sd = new SeqDrift2ChangeDetector();
        leftRepository = sd.new Repository(blockSize);
        rightRepository = sd.new Repository(blockSize);

        uniqueRandomNumbers = new ArrayList<Integer>();
    }

    public boolean setInput(double _inputValue) {
        ++instanceCount;

        addToRightRepository(_inputValue);
        total = total + _inputValue;

        if ((instanceCount % sampleSize) == 0) //Drift point check
        {
            rightRepository.markLastAddedBlock();

            if (isWarning) {
                removeExcessRightRepositoryValues();
            }
            //Assume that there is no warning or drift now
            isWarning = false;

            //Detect a drift with warning significance level
            int iDriftType = getDriftType();

            if (iDriftType == DRIFT) 
            {
                isWarning = false; //Warning state is set to false as it is an actual drift
                clearLeftRepository(); 
                moveValuesFromRightToLeft();
                sampleSize = blockSize;
                return true;
            } else if (iDriftType == WARNING) {
                isWarning = true;
                sampleSize = sampleSize * 2;
                return false;//In warning state no instance is moved from sliding window to repository. Thus returning now               
            } /*
             else if(iDriftType == INTERNAL_DRIFT)
             {
             isWarning = false;
             isDrift   = false;  
             sampleSize = blockSize;
             clearrepository(); //Due to drift clear the data that belong to the old concept in repository
             movefromsliding windowTorepository(); 
             return false;
             }
             */
            else 
            {
                isWarning = false;
                moveValuesFromRightToLeft(); //All instances in sliding window should be moved to repository if no drift or if drift but not in warning             
                return false;
            }
        }
        return false;
    }

    /**
     * Adding new instance to  sliding window
     * <p>
     * @param _inputValue A double instance
     * @return void
     */
    private void addToRightRepository(double _inputValue) {
        if ((rightRepository.getSize() < sampleSize) || isWarning) //By default sliding window should have maximum of blockSize instances except in warning state
        {
            rightRepository.add(new Double(_inputValue));
        } else {
            System.out.println("request to add to sliding window sliding window size :" + rightRepository.getSize() + " Warning :" + isWarning);
        }
    }

    /**
     * Removes excess instances in sliding window when the number of blocks is more than
     * the sliding window size threshold
     * <p>
     * @param void
     * @return void
     */
    private void removeExcessRightRepositoryValues() {
        int maxRightRepositorySize = slidingWindowBlockCount * blockSize;
        while (rightRepository.getSize() > maxRightRepositorySize) {
            total = total - rightRepository.getFirstBlockTotal();
            rightRepository.removeFirstBlock();
        }
    }

    /**
     * Moving the instances from  sliding window to  repository
     * <p>
     * @param void
     * @return void
     */
    private void moveValuesFromRightToLeft() {
        for (int iIndex = 0; iIndex < rightRepository.getSize(); iIndex++) // Copy all instances from sliding window to repository
        {
            if (((iIndex) % sampleSize) == 0) {
                leftRepository.add(rightRepository.get(iIndex), true);
            } else {
                leftRepository.add(rightRepository.get(iIndex));
            }
        }
        blockCount = blockCount + rightRepository.getSize() / blockSize; //Determine the block counter value after adding the instances

        if (slidingWindowBlockCount > 0) //Sliding is enabled
        {
            while (blockCount > slidingWindowBlockCount) //Remove old instances from  repository
            {
                total = total - leftRepository.getFirstBlockTotal();
                leftRepository.removeFirstBlock();
                --blockCount;
            }
        }
        if (!isWarning) //If in warning do not remove the instances from  sliding window
        {
            rightRepository.removeAll();
        } else {
            System.out.println("ERROR: requested to move instances from  sliding window to  repository");
            System.exit(2);
        }
    }

    /**
     * Remove all elements from repository after a drift is detected
     * <p>
     * After deletion the block counter is set to zero
     *
     * @param void
     * @return void
     */
    private void clearLeftRepository() {
        blockCount = 0;
        total = total - leftRepository.getTotal();
        leftRepository.removeAll();
    }

    /**
     * Detects a drift
     * <p>
     * @param _bIsWarning Drift detection in warning significance level or
     * actual significance level
     * @return boolean True - if drift is detected. False - otherwise
     */
    private int getDriftType() {
        if (getWidth() > blockSize) {
            leftRepositoryMean = getLeftRepositorySampleMean(); //Get the subsample mean from  repository
            rightRepositoryMean = getRightRepositorySampleMean(); //Get the subsample mean from  sliding window
            epsilon = getEpsilon();

            double absValue = Math.abs(rightRepositoryMean - leftRepositoryMean);
            if (instanceCount > sampleSize && leftRepository.getSize() > 0) {
                if (epsilon.d_warningEpsilon <= absValue) //Warning Drift is detetced
                {
                    if (epsilon.d_driftEpsilon <= absValue) //Drift is detetced
                    {
                        //if(rightRepositoryMean > leftRepositoryMean)
                        //{
                        return DRIFT;
                        //}
                        /*
                         else
                         {
                         return INTERNAL_DRIFT;
                         }
                         */
                    } else {
                        return WARNING;
                    }
                } else {
                    return HOMOGENEOUS;
                }
            }
            return HOMOGENEOUS;
        } else {
            return HOMOGENEOUS;
        }
    }
    private double getLeftRepositorySampleMean() {
        double leftTotal = 0.0;
        if (leftRepository.getSize() > 0) {
            if (leftRepository.getSize() <= sampleSize) {
                return getLeftResitoryMean();
            } else {
                int iPossibleSampleSize = getPossibleSampleSize();

                for (int iCount = 0; iCount < iPossibleSampleSize; iCount++) {
                    int iNextRandomNumber = getNextRandomNumber(leftRepository.getSize() - 1);
                    if (isUniqueRandomNumber(iNextRandomNumber)) {
                        leftTotal = leftTotal + leftRepository.get(iNextRandomNumber);
                    } else {
                        iCount--;
                    }
                }
                uniqueRandomNumbers.clear();
            }
        }
        return leftTotal / sampleSize;
    }

    private int getPossibleSampleSize() {
        int iNumberSampleElements = 0;
        int leftResitorySize = leftRepository.getSize();
        int rightRepositorySize = rightRepository.getSize();

        if (sampleSize <= leftResitorySize && sampleSize <= rightRepositorySize) {
            iNumberSampleElements = sampleSize;
        } else {
            if (leftResitorySize > rightRepositorySize) {
                sampleSize = leftResitorySize;
            } else {
                sampleSize = rightRepositorySize;
            }
        }
        return iNumberSampleElements;
    }

    private boolean isUniqueRandomNumber(int _iTrialNum) {
        for (int iIndex = 0; iIndex < uniqueRandomNumbers.size(); iIndex++) {
            if (uniqueRandomNumbers.get(iIndex) == _iTrialNum) {
                return false;
            }
        }
        uniqueRandomNumbers.add(_iTrialNum);
        return true;
    }

    private double getLeftResitoryMean() {
        double dTotal = 0.0;
        dTotal = dTotal + leftRepository.getTotal();
        return dTotal / sampleSize;
    }

    private double getRightRepositoryMean() {
        double dTotal = 0.0;
        dTotal = dTotal + rightRepository.getTotal();
        return dTotal / sampleSize;
    }

    private double getRightRepositorySampleMean() {
        double dTotal = 0.0;
        if (rightRepository.getSize() > 0) {
            if (rightRepository.getSize() <= sampleSize) {
                return getRightRepositoryMean();
            } else {
                int iPossibleSampleSize = getPossibleSampleSize();
                for (int iCount = 0; iCount < iPossibleSampleSize; iCount++) {
                    int iNextRandomNumber = getNextRandomNumber(rightRepository.getSize() - 1);
                    if (isUniqueRandomNumber(iNextRandomNumber)) {
                        dTotal = dTotal + rightRepository.get(iNextRandomNumber);
                    } else {
                        iCount--;
                    }
                }
                uniqueRandomNumbers.clear();
            }
        }
        return dTotal / sampleSize;
    }

    private int getNextRandomNumber(int _iSize) {
        double dRandomNumber = _iSize * Math.random();
        long lRoundedNumber = Math.round(dRandomNumber);
        return (int) lRoundedNumber;
    }

    private double getVariance() {
        double dMean = getPopulationMean();
        double d1minusMean = dMean - 1;
        double dtotalsize = getWidth();
        double x = total * d1minusMean * d1minusMean + (dtotalsize - total) * dMean * dMean;
        double y = dtotalsize - 1;
        return x / y;
    }

    private Epsilon getEpsilon() {
        int iNumberOfTests = leftRepository.getNumOfTests() + rightRepository.getNumOfTests();

        if (iNumberOfTests > 1) {
            variance = getVariance();

            //Drift epsilon
            double ddeltadash = getDriftEpsilon(iNumberOfTests);
            double x = Math.log(4.0 / ddeltadash);
            double squareRootValue = Math.sqrt(x * x + 18 * variance * sampleSize * x);
            double depsilon = (2.0 / (3 * sampleSize)) * (x + squareRootValue);
            epsilon.d_driftEpsilon = depsilon;

            //warning epsilon                             
            ddeltadash = getWarningEpsilon(iNumberOfTests);
            x = Math.log(4.0 / ddeltadash);
            squareRootValue = Math.sqrt(x * x + 18 * variance * sampleSize * x);
            depsilon = (2.0 / (3 * sampleSize)) * (x + squareRootValue);
            epsilon.d_warningEpsilon = depsilon;
        }
        return epsilon;
    }

    private double getDriftEpsilon(int _inumTests) {
        double errorValue = 2.0 * (1.0 - Math.pow(0.5, _inumTests));

        double ddeltadash = significanceLevel;
        ddeltadash = significanceLevel / errorValue;

        return ddeltadash;
    }

    private double getWarningEpsilon(int _inumTests) {
        double dTotalError = 2.0 * (1.0 - Math.pow(0.5, _inumTests));
        double ddeltadash = warningSignificanceLevel / dTotalError;

        return ddeltadash;
    }

    private double getPopulationMean() {
        return getTotal() / getWidth();
    }

    private double getTotal() {
        return total;
    }

    public int getWidth() {
        return leftRepository.getSize() + rightRepository.getSize();
    }

    public double getEstimation() {
        int iWidth = getWidth();

        if (iWidth != 0) {
            return getTotal() / getWidth();
        } else {
            return 0;
        }
    }

    public void getDescription(StringBuilder sb, int indent) {
    }

}

class Epsilon {

    double d_warningEpsilon = 0.0;
    double d_driftEpsilon = 0.0;

    public Epsilon() {
        d_warningEpsilon = 0.0;
        d_driftEpsilon = 0.0;
    }

    public void getDescription(StringBuilder sb, int indent) {
    }
  }
}

