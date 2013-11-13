/*
 *    RunVisualizer.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

/*
 * todo:
 *  hide clusters when drawing
 *  pause fix
 *  show always clusters...
 */

package moa.gui.visualization;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import moa.clusterers.outliers.MyBaseOutlierDetector.Outlier;
import moa.clusterers.outliers.MyBaseOutlierDetector.OutlierNotifier;
import moa.clusterers.outliers.MyBaseOutlierDetector.PrintMsg;
import moa.core.FastVector;
import moa.evaluation.MeasureCollection;
import moa.evaluation.OutlierPerformance;
import moa.gui.TextViewerPanel;
import moa.gui.outliertab.OutlierSetupTab;
import moa.gui.outliertab.OutlierVisualEvalPanel;
import moa.gui.outliertab.OutlierVisualTab;
import moa.streams.clustering.ClusterEvent;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class RunOutlierVisualizer implements Runnable, ActionListener, ClusterEventListener{	
    /** the pause interval, being read from the gui at startup */
    public static final int initialPauseInterval = 1000;
    
    /** factor to control the speed */
    private int m_wait_frequency = 100;
    
    private int m_drawOutliersInterval = 100;
    
    /** after how many instances do we repaint the streampanel?
     *  the GUI becomes very slow with small values 
     * */
    private int m_redrawInterval = 100;
    
    private int m_eventsInterval = 20;
    private int m_eventsDecay = 100;
    
    // interval for measurement of processing time per object
    private int m_MeasureInterval = 1000;
    
    /* flags to control the run behavior */
    private static boolean bWork;
    private boolean bStop = false;
    
    /* total amount of processed instances */
    private static int timestamp;
            
    /* amount of instances to process in one step*/
    private int m_pauseInterval;
    
    private boolean m_bWaitWinFull;
    
    /* the stream that delivers the instances */
    private final ClusteringStream m_stream0;
    
    /* amount of relevant instances; older instances will be dropped;
       creates the 'sliding window' over the stream; 
       is strongly connected to the decay rate and decay threshold*/
    private int m_stream0_decayHorizon;

    /* the decay threshold defines the minimum weight of an instance to be relevant */
    private double m_stream0_decay_threshold;
    
    /* the decay rate of the stream, often reffered to as lambda;
       is being calculated from the horizion and the threshold 
       as these are more intuitive to define */
    private double m_stream0_decay_rate;   
    
    private static final int ALGORITHM_1 = 0;
    private static final int ALGORITHM_2 = 1;
    private static final int MAX_ALGORITHMS = 2;
    
    boolean bUseAlgorithm2 = false;
    
    /* the outlier detector */
    public MyBaseOutlierDetector m_outlier[] = new MyBaseOutlierDetector[MAX_ALGORITHMS];
    
    /* the measure collections contain all the measures */
    private MeasureCollection[] m_measures[] = new MeasureCollection[MAX_ALGORITHMS][];

    /* left and right stream panel that datapoints and clusterings will be drawn to */
    private StreamOutlierPanel m_streampanel[] = new StreamOutlierPanel[MAX_ALGORITHMS];
    
    private TreeSet<OutlierEvent> eventBuffer[] = new TreeSet[MAX_ALGORITHMS];
    
    /* panel that shows the evaluation results */
    private OutlierVisualEvalPanel m_evalPanel;
    
    /* panel to hold the graph */
    private GraphCanvas m_graphcanvas;
    
    /* reference to the visual panel */
    private OutlierVisualTab m_visualPanel;
    
    /* points of window */
    private LinkedList<DataPoint> pointBuffer0;
    
    //private boolean bRedrawPointImg = true;
    
    int nProcessed;
    Long m_timePreObjSum[] = new Long[MAX_ALGORITHMS];
    int m_timePreObjInterval = 100;
    
    /* reference to the log panel */
    private final TextViewerPanel m_logPanel;
    
    private class LogPanelPrintMsg implements PrintMsg {        
        @Override
        public void println(String s) {
            m_logPanel.addText(s);
        }
        
        @Override
        public void print(String s) {
            m_logPanel.addText(s);
        }    
        
        @Override
        public void printf(String fmt, Object... args) {
            m_logPanel.addText(String.format(fmt, args));
        }  
    }
    
    private class MyOutlierNotifier extends OutlierNotifier {
        int idxAlgorithm;

        public MyOutlierNotifier(int idxAlgorithm) {
            this.idxAlgorithm = idxAlgorithm;
        }
        
        @Override
        public void OnOutlier(Outlier outlier) {
            DataPoint point = new DataPoint(outlier.inst, (int)outlier.id);
            OutlierEvent oe = new OutlierEvent(point, true, new Long(timestamp));
            if (!eventBuffer[idxAlgorithm].add(oe)) {
                // there is a previous such event, override it
                eventBuffer[idxAlgorithm].remove(oe);
                eventBuffer[idxAlgorithm].add(oe);
            }
            // System.out.println("OnOutlier outlier.id " + outlier.id + " timestamp " + timestamp);
        }
        
        @Override
        public void OnInlier(Outlier outlier) {
            DataPoint point = new DataPoint(outlier.inst, (int)outlier.id);
            OutlierEvent oe = new OutlierEvent(point, false, new Long(timestamp));
            if (!eventBuffer[idxAlgorithm].add(oe)) {
                // there is a previous such event, override it
                eventBuffer[idxAlgorithm].remove(oe);
                eventBuffer[idxAlgorithm].add(oe);
            }
            // System.out.println("OnInlier outlier.id " + outlier.id + " timestamp " + timestamp);
        }
    }
    
    public RunOutlierVisualizer(OutlierVisualTab visualPanel, OutlierSetupTab outlierSetupTab){
        m_outlier[ALGORITHM_1] = outlierSetupTab.getOutlierer0();        
        m_outlier[ALGORITHM_1].prepareForUse();        
        // show algorithm output to log-panel
        m_outlier[ALGORITHM_1].SetUserInfo(false, false, new LogPanelPrintMsg(), 2000);
        m_outlier[ALGORITHM_1].outlierNotifier = new MyOutlierNotifier(ALGORITHM_1);
        
        m_outlier[ALGORITHM_2] = outlierSetupTab.getOutlierer1();
        bUseAlgorithm2 = (m_outlier[ALGORITHM_2] != null);
        if (bUseAlgorithm2) {
            m_outlier[ALGORITHM_2].prepareForUse();        
            // show algorithm output to log-panel
            m_outlier[ALGORITHM_2].SetUserInfo(false, false, new LogPanelPrintMsg(), 2000);
            m_outlier[ALGORITHM_2].outlierNotifier = new MyOutlierNotifier(ALGORITHM_2);
        }
        
        m_visualPanel = visualPanel;
        m_streampanel[ALGORITHM_1] = visualPanel.getLeftStreamPanel();
        m_streampanel[ALGORITHM_1].setVisualizer(this);
        m_streampanel[ALGORITHM_1].setOutlierDetector(m_outlier[ALGORITHM_1]);
        if (bUseAlgorithm2) {
            m_streampanel[ALGORITHM_2] = visualPanel.getRightStreamPanel();
            m_streampanel[ALGORITHM_2].setVisualizer(this);
            m_streampanel[ALGORITHM_2].setOutlierDetector(m_outlier[ALGORITHM_2]);
        }
        m_logPanel = outlierSetupTab.getLogPanel();
        m_graphcanvas = visualPanel.getGraphCanvas();
        m_evalPanel = visualPanel.getEvalPanel();

        m_stream0 = outlierSetupTab.getStream0();
        m_stream0_decayHorizon = m_stream0.getDecayHorizon();
        m_stream0_decay_threshold = m_stream0.getDecayThreshold();
        m_stream0_decay_rate = (Math.log(1.0/m_stream0_decay_threshold)/Math.log(2)/m_stream0_decayHorizon);

        eventBuffer[ALGORITHM_1] = new TreeSet<OutlierEvent>();
        eventBuffer[ALGORITHM_2] = new TreeSet<OutlierEvent>();
        
        timestamp = 0;
        bWork = true;

        if (m_stream0 instanceof RandomRBFGeneratorEvents) {
            // ((RandomRBFGeneratorEvents)m_stream0).addClusterChangeListener(this);
        }
        m_stream0.prepareForUse();
        
        // clear log
        m_logPanel.setText("");
        
        // init measures
        m_measures[ALGORITHM_1] = outlierSetupTab.getMeasures();
        m_measures[ALGORITHM_2] = outlierSetupTab.getMeasures();
        // show as default process time per object
        m_graphcanvas.setGraph(m_measures[ALGORITHM_1][0], m_measures[ALGORITHM_2][0], 0, 100);
        
        updateSettings();
        
        // get those values from the generator
        if (m_stream0 instanceof moa.streams.clustering.FileStream) {
            // manually set last value to be the class
            m_stream0.numAttsOption.setValue(m_stream0.numAttsOption.getValue() - 1);
        }
        int dims = m_stream0.numAttsOption.getValue();
        visualPanel.setDimensionComobBoxes(dims);
    }
    
    private void updateSettings() {
        m_pauseInterval = m_visualPanel.getPauseInterval();
        m_wait_frequency = m_visualPanel.GetSpeed();
        setPointsVisibility(m_visualPanel.getPointVisibility());
        setOutliersVisibility(m_visualPanel.getOutliersVisibility());
        m_bWaitWinFull = m_visualPanel.getWaitWinFull();
    }

    @Override
    public void run() {        
        nProcessed = 0;
        m_timePreObjSum[ALGORITHM_1] = 0L;
        m_timePreObjSum[ALGORITHM_2] = 0L;
        pointBuffer0 = new LinkedList<DataPoint>();
        
        while (!bStop) {            
            if (!bWork || CanPause()) {
                updateSettings();
                
                drawOutliers();                
                showOutliers(true);                
                m_streampanel[ALGORITHM_1].repaint();
                if (bUseAlgorithm2)
                    m_streampanel[ALGORITHM_2].repaint();                
                //redraw();
                
                work_pause();
                if (bStop) break;

                updateSettings();
                showOutliers(false);
                m_streampanel[ALGORITHM_1].setHighlightedOutlierPanel(null);
                if (bUseAlgorithm2)
                    m_streampanel[ALGORITHM_2].setHighlightedOutlierPanel(null);
            }            
            
            synchronized(this) {
                processData();
            }
        }
    }
    
    private void MeasuredProcessStreamObj(int idxAlgorithm, Instance newInst) {        
        m_outlier[idxAlgorithm].processNewInstanceImpl(newInst);
        
        if (nProcessed % m_timePreObjInterval == 0) {           
            UpdateTimePerObj(idxAlgorithm, m_outlier[idxAlgorithm].getTimePerObj());
        }
    }
    
    private void processData() {            
        if (m_stream0.hasMoreInstances()) {
            timestamp++;
            nProcessed++;

            if (timestamp % 100 == 0) 
                m_visualPanel.setProcessedPointsCounter(timestamp);

            Instance nextStreamObj0 = m_stream0.nextInstance().getData();
            DataPoint point0 = new DataPoint(nextStreamObj0,timestamp);

            pointBuffer0.add(point0);
            while (pointBuffer0.size() > m_stream0_decayHorizon) {
                pointBuffer0.removeFirst();
            }
            
            MeasuredProcessStreamObj(ALGORITHM_1, nextStreamObj0);
            if (bUseAlgorithm2)
                MeasuredProcessStreamObj(ALGORITHM_2, nextStreamObj0);
            
            // draw new point
            m_streampanel[ALGORITHM_1].drawPoint(point0, false, false);
            if (bUseAlgorithm2)
                m_streampanel[ALGORITHM_2].drawPoint(point0, false, false);
            
            // apply decay at points    
            if (nProcessed % m_redrawInterval == 0) {                
                float f;
                if (m_stream0_decayHorizon <= m_redrawInterval) 
                    f = 1;
                else
                    f = ((float)m_redrawInterval) / ((float)m_stream0_decayHorizon);
                m_streampanel[ALGORITHM_1].applyDrawDecay(f, false);
                if (bUseAlgorithm2)
                    m_streampanel[ALGORITHM_2].applyDrawDecay(f, false);
            }
            
            // draw events
            //if (nProcessed % m_eventsInterval == 0) {
            drawEvents();
            
            // redraw point layer
            m_streampanel[ALGORITHM_1].RedrawPointLayer();
            if (bUseAlgorithm2)
                m_streampanel[ALGORITHM_2].RedrawPointLayer();
            
            /*if (nProcessed % m_drawOutliersInterval == 0)         
                drawOutliers();*/

            if (CanPause()) {
                // automatically pause each m_pauseInterval objects
                
                //updatePointsWeight();
                showOutliers(true);
                drawOutliers();

                m_visualPanel.toggleVisualizer(true);
            }
        } else {
            System.out.println("DONE");
            pause();
            return;
        }

        simulateVisualSpeed(); // simulate visualization speed 
    }
    
    private void UpdateTimePerObj(int idxAlgorithm, double t) {
        double ms = t / (1000.0 * 1000.0);
        OutlierPerformance op = (OutlierPerformance) m_measures[idxAlgorithm][0];
        op.addTimePerObject(ms);
        
        //m_evalPanel.update();
        m_graphcanvas.updateCanvas();
        
        m_logPanel.addText("Algorithm " + idxAlgorithm + ", process time per object (ms): " + String.format("%.3f", ms));
    }   

    private void ShowStatistics() {
        synchronized(this) {
            m_logPanel.addText(" ");
            m_logPanel.addText("Algorithm1 " + m_outlier[ALGORITHM_1].getStatistics());
            if (bUseAlgorithm2)
                m_logPanel.addText("Algorithm2 " + m_outlier[ALGORITHM_2].getStatistics());
        }
    }
    
    private boolean CanPause() {
        if (m_pauseInterval != 0) {
            return (nProcessed % m_pauseInterval == 0);
        }
        return false;
    }
    
    private void simulateVisualSpeed() {        
        int iMaxWaitFreq = 100;
        
        int iSleepMS = iMaxWaitFreq - m_wait_frequency;
        if (iSleepMS < 0) iSleepMS = 0;
        if (iSleepMS > iMaxWaitFreq) iSleepMS = iMaxWaitFreq;
        //System.out.println("iSleepMS="+iSleepMS + ", m_wait_frequency="+m_wait_frequency);
        
        if (iSleepMS > 0)
            Sleep(iSleepMS); 
    }
    
    private void updatePointsWeight() {
        for(DataPoint p : pointBuffer0)
            p.updateWeight(timestamp, m_stream0_decay_rate);
    }
    
    public void setWaitWinFull(boolean b) {
        m_bWaitWinFull = b;
    }
    
    private boolean CanShowOutliers() {
        return (!m_bWaitWinFull || (nProcessed >= m_stream0_decayHorizon));
    }

    private void drawOutliers(){
        drawOutliers(ALGORITHM_1);
        if (bUseAlgorithm2)
            drawOutliers(ALGORITHM_2);
    }
    
    private void drawOutliers(int idxAlgorithm){
        Vector<MyBaseOutlierDetector.Outlier> outliers = m_outlier[idxAlgorithm].getOutliersResult();        
        if ((outliers != null) && (outliers.size() > 0) && CanShowOutliers())
            m_streampanel[idxAlgorithm].drawOutliers(outliers, Color.RED);
    }
    
    private void deleteExpiredEvents(int idxAlgorithm) {
        // delete expired events older than m_eventsDecay
        boolean b = true;
        while (b) {
            b = false;
            if (eventBuffer[idxAlgorithm].size() > 0) {
                OutlierEvent ev = eventBuffer[idxAlgorithm].first();
                if (timestamp > ev.timestamp + m_eventsDecay) {
                    eventBuffer[idxAlgorithm].remove(ev);
                    b = true;
                }
            }
        }
    }
    
    private void drawEvents() {
        drawEvents(ALGORITHM_1);
        if (bUseAlgorithm2)
            drawEvents(ALGORITHM_2);
    }
    
    private void drawEvents(int idxAlgorithm) {        
        m_streampanel[idxAlgorithm].clearEvents();
        
        // System.out.println("Alg " + idxAlgorithm + " events " + eventBuffer[idxAlgorithm].size());
        
        deleteExpiredEvents(idxAlgorithm);
        
        if (CanShowOutliers()) {
            for (OutlierEvent ev : eventBuffer[idxAlgorithm]) {
                m_streampanel[idxAlgorithm].drawEvent(ev, false);
            }        
        }
        
        //m_streampanel[idxAlgorithm].RedrawPointLayer();        
    }
    
    private void drawPoints() {
        drawPoints(ALGORITHM_1);
        if (bUseAlgorithm2)
            drawPoints(ALGORITHM_2);
    }
    
    private void drawPoints(int idxAlgorithm) {
        m_streampanel[idxAlgorithm].clearPoints();  
        
        for (DataPoint point0 : pointBuffer0) {
            point0.updateWeight(timestamp, m_stream0_decay_rate);
            m_streampanel[idxAlgorithm].drawPoint(point0, true, false);
        }        
        
        m_streampanel[idxAlgorithm].RedrawPointLayer();        
    }
    
    private void showOutliers(boolean bShow) {
        showOutliers(ALGORITHM_1, bShow);
        if (bUseAlgorithm2)
            showOutliers(ALGORITHM_2, bShow);
    }
    
    private void showOutliers(int idxAlgorithm, boolean bShow) {
        if (!bWork && m_visualPanel.isEnabledDrawOutliers() && CanShowOutliers()) {
            m_streampanel[idxAlgorithm].setOutliersVisibility(bShow);
        }
        else {
            m_streampanel[idxAlgorithm].setOutliersVisibility(false);
        }
    }

    private void _redraw() {        
        m_streampanel[ALGORITHM_1].clearPoints();    
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].clearPoints();  
        
        drawPoints();
        drawEvents();
        
        showOutliers(true);
        // drawClusterings();

        m_streampanel[ALGORITHM_1].repaint();
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].repaint();
    }

    public void redraw() {
        synchronized(this) {
            _redraw();
        }
    }
    
    private void _redrawOnResize() {                
        m_streampanel[ALGORITHM_1].clearPoints();  
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].clearPoints();  
        
        drawPoints();   
        drawEvents();
        showOutliers(true);        
        drawOutliers();

        m_streampanel[ALGORITHM_1].repaint();
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].repaint();
    }
    
    public void redrawOnResize() {
        synchronized(this) {
            _redrawOnResize();
        }
    }

    public static int getCurrentTimestamp(){
        return timestamp;
    }

    private void work_pause(){
        while(!bWork && !bStop){
            Sleep(200);
       }
    }
    
    private void Sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) { }
    }

    public static void pause(){
        bWork = false;
    }

    public static void resume(){
        bWork = true;
    }
    
    public void stop(){
        bWork = false;
        bStop = true;
        ShowStatistics();
    }

    public void setSpeed(int speed) {
        m_wait_frequency = speed;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    public void setPointsVisibility(boolean selected) {
        m_streampanel[ALGORITHM_1].setPointsVisibility(selected);
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].setPointsVisibility(selected);
    }
    
    public void setOutliersVisibility(boolean selected) {
        m_streampanel[ALGORITHM_1].setOutliersVisibility(selected);
        if (bUseAlgorithm2)
            m_streampanel[ALGORITHM_2].setOutliersVisibility(selected);
    }

    @Override
    public void changeCluster(ClusterEvent e) {
        System.out.println(e.getType()+": "+e.getMessage());
    }

    public void exportCSV(String filepath) {
        PrintWriter out = null;
        try {
            if(!filepath.endsWith(".csv"))
                filepath+=".csv";
            out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));            
            out.write("\n");            
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(RunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    public void weka() {
    	try{
    		Class.forName("weka.gui.Logger");
    	}
    	catch (Exception e){
    		m_logPanel.addText("Please add weka.jar to the classpath to use the Weka explorer.");
    		return;
    	}    	    	
    	
        Clustering wekaClustering;
        wekaClustering = null;

        if(wekaClustering == null || wekaClustering.size()==0){
            m_logPanel.addText("Empty Clustering");
            return;
        }

        int dims = wekaClustering.get(0).getCenter().length;
        FastVector attributes = new FastVector();
        for(int i = 0; i < dims; i++)
                attributes.addElement( new Attribute("att" + i) );

        Instances instances = new Instances("trainset",attributes,0);

        for(int c = 0; c < wekaClustering.size(); c++){
            Cluster cluster = wekaClustering.get(c);
            Instance inst = new DenseInstance(cluster.getWeight(), cluster.getCenter());
            inst.setDataset(instances);
            instances.add(inst);
        }

        WekaExplorer explorer = new WekaExplorer(instances);
    }


}

