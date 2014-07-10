package moa.classifiers.rules.core.anomalydetection;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;

import moa.options.OptionHandler;


/**
 *  Anomaly Detector interface to implement methods that detects change.
 *
 * @author João Duarte (joaomaiaduarte at gmail dot com)
 * @version $Revision: 1$
 */
public interface AnomalyDetector extends OptionHandler {


    /**
     * Adding an instance to the anomaly detector<br><br>
     *
     * @return true if anomaly is detected and false otherwise
     */
    public boolean updateAndCheckAnomalyDetection(MultiLabelInstance instance);

    
    @Override
    public AnomalyDetector copy();
}