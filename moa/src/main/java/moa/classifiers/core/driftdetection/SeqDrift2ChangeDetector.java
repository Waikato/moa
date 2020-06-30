/*
 *    SeqDrift2ChangeDetector.java
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
import java.util.List;
import moa.AbstractMOAObject;

/**
 * SeqDriftChangeDetector.java. This extends Abstract Change Detector 
 * as required by MOA.
 * 
 * Pears, R., Sakthithasan, S., {@literal &} Koh, Y. (2014). Detecting concept change in
 * dynamic data streams. Machine Learning, 97(3), 259-293. doi:10.1007/s10994-013-5433-9
 *
 * @author Sakthithasan Sripirakas sripirakas363 at yahoo dot com
 * @version $Revision: 7 $
 */
public class SeqDrift2ChangeDetector extends AbstractChangeDetector {

    protected SeqDrift2 seqdrift;

    public FloatOption deltaSeqDrift2Option = new FloatOption("deltaSeq2Drift", 'd',
            "Delta of SeqDrift2 change detection",0.01, 0.0, 1.0);
    
    public IntOption blockSeqDrift2Option = new IntOption("blockSeqDrift2Option",'b',"Block size of SeqDrift2 change detector", 200, 100, 10000);

    @Override
    public void input(double inputValue) {
        if (this.seqdrift == null) {
            resetLearning();
        }
        this.isChangeDetected = seqdrift.setInput(inputValue);
        this.isWarningZone = false;
        this.delay = 0.0;
        this.estimation = seqdrift.getEstimation();
    }

    @Override
    public void resetLearning() {
        seqdrift = new SeqDrift2((double) this.deltaSeqDrift2Option.getValue(),((int) this.blockSeqDrift2Option.getValue()));
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
 * SeqDrift2 uses reservoir sampling to build a sequential change detection
* model that uses statistically sound guarantees defined using Bernstein Bound 
* on false positive and false negative rates. This is a block based approach and checks 
* for changes in the data values only at block boundaries as opposed to the 
* methods on per instance basis. SeqDrift maintains a reservoir and a repository.
* Repository gathers the new instances and reservoir stores only the data values 
* that are statistically not different, in other words from the same distribution. 
* If the data values in the repository are consistent with the values in reservoir, 
* the data values of the repository are copied to the reservoir applying 
* reservoir algorithm.The hypothesis is that the mean values of the reservoir and right repository 
* are not statistically different
 * 
 * 
 * 
 * Pears, R., Sakthithasan, S., {@literal &} Koh, Y. (2014). Detecting concept change in
 * dynamic data streams. Machine Learning, 97(3), 259-293. doi:10.1007/s10994-013-5433-9
 * 
 * @author Sakthithasan Sripirakas sripirakas363 at yahoo dot com
 */
public class SeqDrift2 extends AbstractMOAObject {

    private final Reservoir rightRepository;
    private final Reservoir leftReservoir;

    //parameters
    private final int blockSize;

    //parameters that are optimized
    private final double significanceLevel;
    private int leftReservoirSize;
    private final int rightRepositorySize;
    private final double k;

    //variables
    private int instanceCount = 0;
    private double leftReservoirMean = 0.0;
    private double rightRepositoryMean = 0.0;
    private double variance = 0.0;
    private double total = 0.0;
    private double epsilon = 0.0;

    private final static int DRIFT = 0;
    private final static int NODRIFT = 2;
    private final static int INTERNAL_DRIFT = 3;

    /**
     * SeqDrift change detector requires two parameters: significance level and 
     * block size. Significance level controls the false positive rate and block 
     * size sets the interval of two consecutive hypothesis tests
     * Block Size is a positive integer and significance level is a double value
     * between 0 and 1
     * @param _significanceLevel
     * @param _blockSize
     */
    public SeqDrift2(double _significanceLevel, int _blockSize) {
        significanceLevel = _significanceLevel;
        blockSize = _blockSize;
        leftReservoirSize = _blockSize;
        rightRepositorySize = _blockSize;
        k = 0.5;

        instanceCount = 0;
        variance = 0;
        total = 0.0;
        epsilon = 0.0;

        //Data Structures        
        leftReservoir = new Reservoir(leftReservoirSize, blockSize);
        rightRepository = new Reservoir(rightRepositorySize, blockSize);
    }
    /**
     * This method can be used to directly interface with SeqDrift change
     * detector. This method requires a numerical value as an input. The return 
     * value indicates whether there is a change detected or not. 
     * @param _inputValue numerical value
     */
    public boolean setInput(double _inputValue) {
        ++instanceCount;
        //i_numInstances++;

        addToRightReservoir(_inputValue);
        total = total + _inputValue;

        if ((instanceCount % blockSize) == 0) //checking for drift at block boundary
        {
            int iDriftType = getDriftType();

            if (iDriftType == DRIFT) {
                clearLeftReservoir();
                moveFromRepositoryToReservoir();
                return true;
            } 
            /*
            else if (iDriftType == INTERNAL_DRIFT) { //Ignoring the changes where 
            mean values decrease
                clearLeftReservoir();
                moveFromRepositoryToReservoir();
                return false;
            }
            */
            else //No drift is detected
            {
                moveFromRepositoryToReservoir();
                return false;
            }
        }
        return false;
    }

    /**
     *
     * <p>
     * This method adds new value to right reservoir
     *
     * @param _inputValue A double data value
     * @return void
     */
    private void addToRightReservoir(double _inputValue) {
        rightRepository.addElement(_inputValue);
    }

    /**
     *
     * <p>
     * This method copies the data values of the repository to the reservoir
     * applying reservoir algorithm
     *
     * @return void
     */
    private void moveFromRepositoryToReservoir() {
        leftReservoir.copy(rightRepository);
    }

    /**
     *
     * <p>
     * This method removes all elements from the reservoir after a drift is
     * detected.
     *
     * @return void
     */
    private void clearLeftReservoir() {
        total = total - leftReservoir.getTotal();
        leftReservoir.clear();
    }

    /**
     *
     * <p>
     * This method returns the type of drift detected The possible values are:
     * DRIFT, INTERNAL_DRIFT and NODRIFT
     *
     * @param
     * @return boolean True - if drift is detected. False - otherwise
     */
    private int getDriftType() {
        if (getWidth() > blockSize) {
            leftReservoirMean = getLeftReservoirMean();
            rightRepositoryMean = getRightRepositoryMean();
            optimizeEpsilon();

            if ((instanceCount > blockSize) && (leftReservoir.getSize() > 0)) {
                if (epsilon <= Math.abs(rightRepositoryMean - leftReservoirMean)) {
                    //if (rightRepositoryMean > leftReservoirMean) {
                        return DRIFT;
                    //} 
                    /*else {
                        return INTERNAL_DRIFT;
                    }
                    */
                } else {
                    return NODRIFT;
                }
            }
            return NODRIFT;
        } else {
            return NODRIFT;
        }
    }

    private double getLeftReservoirMean() {
        return leftReservoir.getSampleMean();
    }

    private double getRightRepositoryMean() {
        return rightRepository.getSampleMean();
    }

    private double getVariance() {
        double mean = getMean();
        double meanminum1 = mean - 1;
        double size = getWidth();
        double x = getTotal() * meanminum1 * meanminum1 + (size - getTotal()) * mean * mean;
        double y = size - 1;
        return x / y;
    }

    private void optimizeEpsilon() {
        int tests = leftReservoir.getSize() / blockSize;

        if (tests >= 1) {
            variance = getVariance();
            if (variance == 0) {
                variance = 0.0001; // to avoid divide by zero exception
            }

            //Drift epsilon
            double ddeltadash = getDriftEpsilon(tests);
            double x = Math.log(4.0 / ddeltadash);
            double ktemp = this.k;

            double previousStepEpsilon;
            double currentStepEpsilon;
            double squareRootValue = 0.0;

            boolean IsNotOptimized = true;
            while (IsNotOptimized) {
                squareRootValue = Math.sqrt(x * x + 18 * rightRepositorySize * x * variance);
                previousStepEpsilon = (1.0 / (3 * rightRepositorySize * (1 - ktemp))) * (x + squareRootValue);
                ktemp = 3 * ktemp / 4;
                currentStepEpsilon = (1.0 / (3 * rightRepositorySize * (1 - ktemp))) * (x + squareRootValue);

                if (((previousStepEpsilon - currentStepEpsilon) / previousStepEpsilon) < 0.0001) {
                    IsNotOptimized = false;
                }
            }
            ktemp = 4 * ktemp / 3;
            ktemp = adjustForDataRate(ktemp);
            leftReservoirSize = (int) (rightRepositorySize * (1 - ktemp) / ktemp);
            leftReservoir.setMaxSize(leftReservoirSize);
            squareRootValue = Math.sqrt(x * x + 18 * rightRepositorySize * x * variance);
            currentStepEpsilon = (1.0 / (3 * rightRepositorySize * (1 - ktemp))) * (x + squareRootValue);
            epsilon = currentStepEpsilon;

        }
    }

    private double getDriftEpsilon(int _inumTests) {
        double dSeriesTotal = 2.0 * (1.0 - Math.pow(0.5, _inumTests));
        double ddeltadash = significanceLevel / dSeriesTotal;
        return ddeltadash;
    }

    private double getMean() {
        return getTotal() / getWidth();
    }

    private double getTotal() {
        return rightRepository.getTotal() + leftReservoir.getTotal();
    }

    private double adjustForDataRate(double _dKr) {
        double meanIncrease = (rightRepository.getSampleMean() - leftReservoir.getSampleMean());
        double dk = _dKr;
        if (meanIncrease > 0) {
            dk = dk + ((-1) * (meanIncrease * meanIncrease * meanIncrease * meanIncrease) + 1) * _dKr;
        } else if (meanIncrease <= 0) {
            dk = _dKr;
        }
        return dk;
    }

    private int getWidth() {
        return leftReservoir.getSize() + rightRepository.getSize();
    }

    /**
     * Gets the prediction of next values.
     * @return Predicted value of next data value
     */
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

public class Reservoir {

    private int size;
    private double total;
    private final int blocksize;
    private final Repository dataContainer;
    private int instanceCount;
    private int MAX_SIZE;

    public Reservoir(int _iSize, int _iBlockSize) {
        MAX_SIZE = _iSize;
        total = 0;
        blocksize = _iBlockSize;
        instanceCount = 0;
        dataContainer = new Repository(blocksize);
    }

    public double getSampleMean() {
        return total / size;
    }

    public void addElement(double _dValue) {
        try {
            if (size < MAX_SIZE) {
                dataContainer.add(new Double(_dValue));
                total = total + _dValue;
                size++;
            } else {
                int irIndex = (int) (Math.random() * instanceCount);
                if (irIndex < MAX_SIZE) {
                    total = total - dataContainer.get(irIndex);
                    dataContainer.addAt(irIndex, _dValue);
                    total = total + _dValue;
                }
            }
            instanceCount++;
        } catch (Exception e) {
            System.out.println("2 Exception" + e);
        }

    }

    public double get(int _iIndex) {
        return dataContainer.get(_iIndex);
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        dataContainer.removeAll();
        total = 0;
        size = 0;
    }

    public double getTotal() {
        return total;
    }

    public void copy(Reservoir _oSource) {
        for (int iIndex = 0; iIndex < _oSource.getSize(); iIndex++) {
            addElement(_oSource.get(iIndex));
        }
        _oSource.clear();
    }

    public void setMaxSize(int _iMaxSize) {
        MAX_SIZE = _iMaxSize;
    }

}

public class Repository {

    private final int blockSize;
    private final List<Block> blocks;
    private int indexOfLastBlock;
    private int instanceCount;
    private double total;
    
    public Repository(int _iBlockSize) {
        blockSize = _iBlockSize;
        indexOfLastBlock = -1;
        instanceCount = 0;
        total = 0;
        blocks = new ArrayList<Block>();
    }

    public void add(double _dValue) {
        if ((instanceCount % blockSize) == 0) {
            blocks.add(new Block(blockSize));
            indexOfLastBlock++;
        }
        blocks.get(indexOfLastBlock).add(_dValue);
        instanceCount++;
        total = total + _dValue;
    }
    
    public void add(double _dValue, boolean _isTested)
	{
		if((instanceCount % blockSize) == 0)
		{
			blocks.add(new Block(blockSize, _isTested));
			indexOfLastBlock++;
		}
		blocks.get(indexOfLastBlock).add(_dValue);
		instanceCount++;
		total= total + _dValue;
	}

    public double get(int _iIndex) {
        return blocks.get(_iIndex / blockSize).data[(_iIndex % blockSize)];
    }

    public void addAt(int _iIndex, double _dValue) {
        blocks.get(_iIndex / blockSize).addAt(_iIndex % blockSize, _dValue);
    }

    public int getSize() {
        return instanceCount;
    }

    public double getTotal() {
        double dTotal = 0.0;
        for (int iIndex = 0; iIndex < blocks.size(); iIndex++) {
            dTotal = dTotal + blocks.get(iIndex).total;
        }
        return dTotal;
    }
    
    public double getFirstBlockTotal()
	{
		return blocks.get(0).total;

	}
    
    public void markLastAddedBlock()
        {
            if(blocks.size() > 0)
            {
                blocks.get(blocks.size() - 1).setTested(true);
            }
        }
    
    public void removeFirstBlock()
	{
		total = total - blocks.get(0).total;
		blocks.remove(0);
		instanceCount = instanceCount - blockSize;
		indexOfLastBlock--;
	}

    public void removeAll() {
        blocks.clear();
        indexOfLastBlock = -1;
        instanceCount = 0;
        total = 0;
    }
    
    public int getNumOfTests()
        {
            int iNumTests = 0;
            for(int iIndex = 0; iIndex < blocks.size(); iIndex++)
            {
                if(blocks.get(iIndex).IsTested())
                    iNumTests++;
            }
            return iNumTests;
        }

}

public class Block {

    public double[] data;
    public double total;
    private int indexOfLastValue;
    
    private boolean b_IsTested;

    Block(int _iLength) {
        data = new double[_iLength];
        total = 0.0;
        indexOfLastValue = 0;
        for (int iIndex = 0; iIndex < data.length; iIndex++) {
            data[iIndex] = -1;
        }
    }
    
    
    Block(int _iLength, boolean _isTested)
	{
		data = new double[_iLength];
                total     = 0.0;
                indexOfLastValue = 0;
                b_IsTested = _isTested;
                for(int iIndex=0;iIndex < data.length;iIndex++)
                {
                    data[iIndex] = -1;
                }
	}

    public void add(double _dValue) {
        if (indexOfLastValue < data.length) {
            data[indexOfLastValue] = _dValue;
            total = total + _dValue;
            indexOfLastValue++;
        } else {
            System.out.println("ERROR in adding to Block. Last Index :" + indexOfLastValue + " Total :" + total + " Array Length :" + data.length);
            System.exit(2);
        }
    }

    public void addAt(int _iIndex, double _dNewValue) {
        total = total - data[_iIndex] + _dNewValue;
        data[_iIndex] = _dNewValue;
    }
    
    public void setTested(boolean _isTested)
        {
            b_IsTested = _isTested;
        }
        public boolean IsTested()
        {
            return b_IsTested;
        }
    }
}
