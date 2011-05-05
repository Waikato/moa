package moa.gui.visualization;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.cluster.Cluster;
import moa.clusterers.ClusterGenerator;
import moa.cluster.Clustering;
import moa.clusterers.AbstractClusterer;
import moa.evaluation.MeasureCollection;
import moa.gui.TextViewerPanel;
import moa.gui.clustertab.ClusteringVisualEvalPanel;
import moa.gui.clustertab.ClusteringVisualTab;
import moa.streams.clustering.ClusterEvent;
import weka.core.Instance;
import moa.gui.clustertab.ClusteringSetupTab;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.RandomRBFGeneratorEvents;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;

/**
 *
 * @author jansen
 */
public class RunVisualizer implements Runnable, ActionListener, ClusterEventListener{
    
    private int processFrequency;


    private int m_wait_frequency = 1000;
    private int redrawInterval = 100;
    
    public static final int initialPauseInterval = 500000;

    public static boolean debug_stop = false;

    private static boolean work;
    private final ClusteringStream m_stream0;
    private final ClusteringStream m_stream1;
    private boolean m_stream_duplicate;
    private StreamPanel m_streampanel0;
    private StreamPanel m_streampanel1;
    private int m_stream0_decayHorizon;
    private int m_stream1_decayHorizon;
    private double m_stream0_decay_rate; //lambda
    private double m_stream1_decay_rate; //lambda
    private double m_stream0_decay_threshold;
    private double m_stream1_decay_threshold;

    private AbstractClusterer m_clusterer0;
    private AbstractClusterer m_clusterer1;

    private MeasureCollection[] m_measures0 = null;
    private MeasureCollection[] m_measures1 = null;

    private ClusteringVisualEvalPanel m_evalPanel;
    private GraphCanvas m_graphcanvas;
    private ClusteringVisualTab m_visualPanel;


    private boolean stop = false;
    private static int m_timestamp;
    private static int m_lastPauseTimestamp;

    //not pretty to have all the clusterings, but otherwise we can't just redraw clusterings
    private Clustering gtClustering0 = null;
    private Clustering gtClustering1 = null;
    private Clustering macro0 = null;
    private Clustering macro1 = null;
    private Clustering micro0 = null;
    private Clustering micro1 = null;

    private ArrayList<ClusterEvent> clusterEvents;
    private boolean autoScreenshots = false;
    
    private final TextViewerPanel m_logPanel;
    

    public RunVisualizer(ClusteringVisualTab visualPanel, ClusteringSetupTab clusteringSetupTab){
        m_visualPanel = visualPanel;
        m_streampanel0 = visualPanel.getLeftStreamPanel();
        m_streampanel1 = visualPanel.getRightStreamPanel();
        m_graphcanvas = visualPanel.getGraphCanvas();
        m_evalPanel = visualPanel.getEvalPanel();
        m_logPanel = clusteringSetupTab.getLogPanel();

        m_stream0 = clusteringSetupTab.getStream0();
        m_stream0_decayHorizon = m_stream0.getDecayHorizon();
        m_stream0_decay_threshold = m_stream0.getDecayThreshold();
        m_stream0_decay_rate = (Math.log(1.0/m_stream0_decay_threshold)/Math.log(2)/m_stream0_decayHorizon);


        //if stream should be duplicated we use same stream for performance 
        //reasons and just copy the generated instances later on
        m_stream_duplicate = clusteringSetupTab.duplicateStream();
        if(!m_stream_duplicate){
            m_stream1 = clusteringSetupTab.getStream1();
            m_stream1_decayHorizon = m_stream1.getDecayHorizon();
            m_stream1_decay_threshold = m_stream1.getDecayThreshold();
            m_stream1_decay_rate = (Math.log(1.0/m_stream1_decay_threshold)/Math.log(2)/m_stream1_decayHorizon);
        }
        else{
            m_stream1 = null;
            m_stream1_decayHorizon = m_stream0_decayHorizon;
            m_stream1_decay_threshold = m_stream0_decay_threshold;
            m_stream1_decay_rate = m_stream0_decay_rate;
        }

        m_timestamp = 0;
        m_lastPauseTimestamp = 0;
        work = true;


        if(m_stream0 instanceof RandomRBFGeneratorEvents){
            ((RandomRBFGeneratorEvents)m_stream0).addClusterChangeListener(this);
            clusterEvents = new ArrayList<ClusterEvent>();
            m_graphcanvas.setClusterEventsList(clusterEvents);
        }
        m_stream0.prepareForUse();
        if(!m_stream_duplicate)
            m_stream1.prepareForUse();

        m_clusterer0 = clusteringSetupTab.getClusterer0();
        m_clusterer0.prepareForUse();
        //if duplicate algorithm is active the panel returns a copy of clusterer0
        m_clusterer1 = clusteringSetupTab.getClusterer1();
        m_clusterer1.prepareForUse();

        m_measures0 = clusteringSetupTab.getMeasures();
        m_measures1 = clusteringSetupTab.getMeasures();


        //TODO this needs to come from the setup panel, not the stream
        processFrequency = m_stream0.getEvaluationFrequency();

        //get those values from the generator
        int dims = m_stream0.numAttsOption.getValue();
        visualPanel.setDimensionComobBoxes(dims);
        visualPanel.setPauseInterval(initialPauseInterval);

        m_evalPanel.setMeasures(m_measures0, m_measures1, this);
        m_graphcanvas.setGraph(m_measures0[0], m_measures1[0],0,processFrequency);
    }


    public void run() {
        if(true)
            runVisual();
        else
            runDebug();
    }


    public void runVisual() {
        int processCounter = 0;
        int speedCounter = 0;
        LinkedList<DataPoint> pointBuffer0 = new LinkedList<DataPoint>();
        LinkedList<DataPoint> pointBuffer1 = new LinkedList<DataPoint>();

        while(work || processCounter!=0){
            if (m_stream0.hasMoreInstances() && ((m_stream1 == null && m_stream_duplicate) || m_stream1.hasMoreInstances())) {
                m_timestamp++;
                speedCounter++;
                processCounter++;
                if(m_timestamp%100 == 0){
                    m_visualPanel.setProcessedPointsCounter(m_timestamp);
                }

                Instance next0 = m_stream0.nextInstance();
                Instance next1;
                if(!m_stream_duplicate)
                    next1 = m_stream1.nextInstance();
                else
                    next1 = next0;

//                if(m_timestamp < 40000) continue;
                DataPoint point0 = new DataPoint(next0,m_timestamp);
                DataPoint point1 = new DataPoint(next1,m_timestamp);

                pointBuffer0.add(point0);
                pointBuffer1.add(point1);
                while(pointBuffer0.size() > m_stream0_decayHorizon)
                    pointBuffer0.removeFirst();
                while(pointBuffer1.size() > m_stream1_decayHorizon)
                    pointBuffer1.removeFirst();

                if(m_visualPanel.isEnabledDrawPoints()){
                    m_streampanel0.drawPoint(point0);
                    m_streampanel1.drawPoint(point1);
                    if(processCounter%redrawInterval==0){
                        m_streampanel0.applyDrawDecay(m_stream0_decayHorizon/(float)(redrawInterval));
                        m_streampanel1.applyDrawDecay(m_stream1_decayHorizon/(float)(redrawInterval));
                    }
                }

                //thats a bit of a hack, use boolean keepLabel() in Clusterer interface?
                Instance traininst0 = new DenseInstance(point0);
                if(m_clusterer0 instanceof ClusterGenerator)
                    traininst0.setDataset(point0.dataset());
                else
                    traininst0.deleteAttributeAt(point0.classIndex());

                Instance traininst1 = new DenseInstance(point1);
                if(m_clusterer1 instanceof ClusterGenerator)
                    traininst1.setDataset(point1.dataset());
                else
                    traininst1.deleteAttributeAt(point1.classIndex());

                m_clusterer0.trainOnInstanceImpl(traininst0);
                m_clusterer1.trainOnInstanceImpl(traininst1);

                if (processCounter >= processFrequency) {
                    processCounter = 0;
                    for(DataPoint p:pointBuffer0)
                        p.updateWeight(m_timestamp, m_stream0_decay_rate);
                    for(DataPoint p:pointBuffer1)
                        p.updateWeight(m_timestamp, m_stream1_decay_rate);

                    ArrayList<DataPoint> pointarray0 = new ArrayList<DataPoint>(pointBuffer0);
                    ArrayList<DataPoint> pointarray1 = new ArrayList<DataPoint>(pointBuffer1);
                    processClusterings(pointarray0,pointarray1);

//                    pointarray0.clear();
//                    pointarray1.clear();

                    int pauseInterval = m_visualPanel.getPauseInterval();
                    if(pauseInterval!=0 && m_lastPauseTimestamp+pauseInterval<=m_timestamp){
                        m_visualPanel.toggleVisualizer(true);
                    }
                        
                }
            } else {
                System.out.println("DONE");
                return;
            }
            if(speedCounter > m_wait_frequency && m_wait_frequency < 15){
                try {
                    synchronized (this) {
                        if(m_wait_frequency == 0)
                            wait(50);
                        else
                            wait(10);
                    }
                } catch (InterruptedException ex) {
                    
                }
                speedCounter = 0;
            }
        }
        if(!stop){
            m_streampanel0.drawPointPanels(pointBuffer0, m_timestamp, m_stream0_decay_rate, m_stream0_decay_threshold);
            m_streampanel1.drawPointPanels(pointBuffer1, m_timestamp, m_stream1_decay_rate, m_stream1_decay_threshold);
            work_pause();
        }
    }

    private void processClusterings(ArrayList<DataPoint> points0, ArrayList<DataPoint> points1){
        gtClustering0 = new Clustering(points0);
//        gtClustering0 = ((RandomRBFGeneratorEvents)m_stream0).getClustering();
        //gtClustering0 = new Clustering(points0, 0.5, 5);
        //gtClustering0 = ((RandomRBFGeneratorEvents)m_stream0).getClustering();
        gtClustering1 = new Clustering(points1);
        //gtClustering1 = new Clustering(points1, 0.5, 5);
        //gtClustering0 = ((RandomRBFGeneratorEvents)m_stream0).getClustering();

        Clustering evalClustering0 = null;
        Clustering evalClustering1 = null;

        if(gtClustering0!= null && m_clusterer0 instanceof ClusterGenerator){
            ((ClusterGenerator)m_clusterer0).setSourceClustering(gtClustering0);
        }
        if(gtClustering1!= null && m_clusterer1 instanceof ClusterGenerator){
            ((ClusterGenerator)m_clusterer1).setSourceClustering(gtClustering1);
        }

        macro0 = m_clusterer0.getClusteringResult();
        evalClustering0 = macro0;

        if(m_clusterer0.implementsMicroClusterer()){
            micro0 = m_clusterer0.getMicroClusteringResult();
            if(macro0 == null && micro0 != null){
                    macro0 = moa.clusterers.KMeans.gaussianMeans(gtClustering0, micro0);
            }
            if(m_clusterer0.evaluateMicroClusteringOption.isSet())
                evalClustering0 = micro0;
            else
                evalClustering0 = macro0;
        }

        if(m_clusterer1!=null){
            macro1 = m_clusterer1.getClusteringResult();
            evalClustering1 = macro1;
            if(m_clusterer1.implementsMicroClusterer()){
                micro1 = m_clusterer1.getMicroClusteringResult();
                if(macro1 == null && micro1 != null){
                        macro1 = moa.clusterers.KMeans.gaussianMeans(gtClustering1, micro1);
                }
                if(m_clusterer1.evaluateMicroClusteringOption.isSet())
                    evalClustering1 = micro1;
                else
                    evalClustering1 = macro1;
            }
        }
        //clustering0 = ((RandomRBFGeneratorEvents)m_stream0).getClustering();

        if(evalClustering0!=null && evalClustering0.size() > 0){
            evaluateClustering(evalClustering0, gtClustering0, points0, true);
        }
        if(evalClustering1!=null && evalClustering1.size() > 0){
            evaluateClustering(evalClustering1, gtClustering1,points1, false);
        }

         drawClusterings();
         if(autoScreenshots){
            m_streampanel0.screenshot("Moa\\0_"+m_timestamp,false, true);
            m_streampanel1.screenshot("Moa\\1_"+m_timestamp, false, true);
         }
    }

    private void evaluateClustering(Clustering found_clustering, Clustering trueClustering, ArrayList<DataPoint> points, boolean algorithm0){
        //System.out.println("Start Eval on "+found_clustering.size()+" cluster");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < m_measures0.length; i++) {
            if(algorithm0){
                try {
                    double msec = m_measures0[i].evaluateClusteringPerformance(found_clustering, trueClustering, points);
                    sb.append(m_measures0[i].getClass().getSimpleName()+" took "+msec+"ms (Mean:"+m_measures0[i].getMeanRunningTime()+")");
                    sb.append("\n");

                } catch (Exception ex) { ex.printStackTrace(); }
            }
            else{
                try {
                    double msec = m_measures1[i].evaluateClusteringPerformance(found_clustering, trueClustering, points);
                    sb.append(m_measures1[i].getClass().getSimpleName()+" took "+msec+"ms (Mean:"+m_measures1[i].getMeanRunningTime()+")");
                    sb.append("\n");
                }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        m_logPanel.setText(sb.toString());
        m_evalPanel.update();
        m_graphcanvas.updateCanvas();
    }

    public void drawClusterings(){
        if(macro0!= null && macro0.size() > 0)
                m_streampanel0.drawMacroClustering(macro0, Color.BLUE);
        if(micro0!= null && micro0.size() > 0)
                m_streampanel0.drawMicroClustering(micro0, Color.GREEN);
        if(gtClustering0!= null && gtClustering0.size() > 0)
            m_streampanel0.drawGTClustering(gtClustering0, Color.BLACK);
//    if(m_visualPanel.isEnabledDrawClustering() && macro0!= null && macro0.size() > 0)
//            m_streampanel0.drawMacroClustering(macro0, Color.BLUE);
//    if(m_visualPanel.isEnabledDrawMicroclustering() && micro0!= null && micro0.size() > 0)
//            m_streampanel0.drawMicroClustering(micro0, Color.GREEN);
//    if(m_visualPanel.isEnabledDrawGroundTruth() && gtClustering0!= null && gtClustering0.size() > 0)
//        m_streampanel0.drawGTClustering(gtClustering0, Color.BLACK);
//        if(m_visualPanel.isEnabledDrawClustering()){
//            //drawGenClustering(m_streampanel0, ((RandomRBFGeneratorEvents)m_stream0).getGeneratingClusters() , Color.BLUE);
//
//        }
        if(macro1!= null && macro1.size() > 0)
                m_streampanel1.drawMacroClustering(macro1, Color.BLUE);
        if(micro1!= null && micro1.size() > 0)
                m_streampanel1.drawMicroClustering(micro1, Color.GREEN);
        if(gtClustering1!= null && gtClustering1.size() > 0)
            m_streampanel1.drawGTClustering(gtClustering1, Color.BLACK);
//        if(m_visualPanel.isEnabledDrawClustering() && macro1!= null && macro1.size() > 0)
//                m_streampanel1.drawMacroClustering(macro1, Color.BLUE);
//        if(m_visualPanel.isEnabledDrawMicroclustering() && micro1!= null && micro1.size() > 0)
//                m_streampanel1.drawMicroClustering(micro1, Color.GREEN);
//        if(m_visualPanel.isEnabledDrawGroundTruth() && gtClustering1!= null && gtClustering1.size() > 0)
//            m_streampanel1.drawGTClustering(gtClustering1, Color.BLACK);
     }



//    private void drawGenClustering(StreamPanel streampanel, Clustering clustering, Color color) {
//
//        ArrayList directions = ((RandomRBFGeneratorEvents)m_stream0).getDirections();
//
//        for (int c = 0; c < clustering.size(); c++) {
//            SphereCluster cluster = (SphereCluster)clustering.get(c);
//
//            ClusterPanel clusterpanel = new ClusterPanel(cluster, m_timestamp, color, decay_rate, streampanel);
//
//            if(c%2 == 1)
//                clusterpanel.setDirection((double[])directions.get(c/2));
//
//            clusterPanelBufferCounter++;
//            if(clusterPanelBufferCounter>=m_clusterPanelBuffer.length){
//                clusterPanelBufferCounter=0;
//            }
//            if(m_clusterPanelBuffer[clusterPanelBufferCounter]!=null){
//               streampanel.remove(m_clusterPanelBuffer[clusterPanelBufferCounter]);
//            }
//            m_clusterPanelBuffer[clusterPanelBufferCounter] = clusterpanel;
//
//            streampanel.add(clusterpanel);
//            clusterpanel.updateLocation();
//        }
//    }


//    //TODO: this is really aweful, we need to define layers within streampanel
//    //and enable/disable them on demand
    public void redraw(){
//        for (Component comp : m_streampanel0.getComponents()){
//            if(comp instanceof ClusterPanel)
//                m_streampanel0.remove(comp);
//        }
//        for (Component comp : m_streampanel1.getComponents()){
//            if(comp instanceof ClusterPanel)
//                m_streampanel1.remove(comp);
//        }

//        drawClusterings();
        m_streampanel0.repaint();
        m_streampanel1.repaint();
    }


    //do we really want to fade clusters on the panel?
//    public static int getAlphaValueForCluster(int point_timestamp){
//        int alpha = 255-(int)((m_timestamp-point_timestamp)*254.0/(DECAY_HORIZON));
//        if(alpha<0) alpha = 0;
//        return alpha;
//    }

    public static int getCurrentTimestamp(){
        return m_timestamp;
    }

    private void work_pause(){
        while(!work && !stop){
            try {
                synchronized (this) {
                    wait(1000);
                }
            } catch (InterruptedException ex) {
                
            }
       }
       run();
    }

    public static void pause(){
        work = false;
        m_lastPauseTimestamp = m_timestamp;
    }

    public static void resume(){
        work = true;
    }

    public void stop(){
        work = false;
        stop = true;
    }

    public void setSpeed(int speed) {
        m_wait_frequency = speed;
    }

    public void actionPerformed(ActionEvent e) {
        //reacte on graph selection
        int selected = Integer.parseInt(e.getActionCommand());
//        if(selected >= 0 && selected < m_measures0.length ){
            int counter = selected;
            int m_select = 0;
            int m_select_offset = 0;
            boolean found = false;
            for (int i = 0; i < m_measures0.length; i++) {
                for (int j = 0; j < m_measures0[i].getNumMeasures(); j++) {
                    counter--;
                    if(counter<0){
                        m_select = i;
                        m_select_offset = j;
                        found = true;
                        break;
                    }
                }
                if(found) break;
            }
            m_graphcanvas.setGraph(m_measures0[m_select], m_measures1[m_select],m_select_offset,processFrequency);
//        }
    }

    public void setPointLayerVisibility(boolean selected) {
        m_streampanel0.setPointVisibility(selected);
        m_streampanel1.setPointVisibility(selected);
    }
    public void setMicroLayerVisibility(boolean selected) {
        m_streampanel0.setMicroLayerVisibility(selected);
        m_streampanel1.setMicroLayerVisibility(selected);
    }
    public void setMacroVisibility(boolean selected) {
        m_streampanel0.setMacroLayerVisibility(selected);
        m_streampanel1.setMacroLayerVisibility(selected);
    }
    public void setGroundTruthVisibility(boolean selected) {
        m_streampanel0.setGroundTruthLayerVisibility(selected);
        m_streampanel1.setGroundTruthLayerVisibility(selected);
    }

    public void changeCluster(ClusterEvent e) {
        if(clusterEvents!=null) clusterEvents.add(e);
        System.out.println(e.getType()+": "+e.getMessage());
    }



    public void exportCSV(String filepath) {

//        new BoxAndWhisker("Boxplot", m_measures0);
//        BoxAndWhisker.generateScreenshot(m_measures0, "C:\\","Boxplot");

        PrintWriter out = null;
        try {

            if(!filepath.endsWith(".csv"))
                filepath+=".csv";
            out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
            String del = ";";

            Iterator<ClusterEvent> eventIt = null;
            ClusterEvent event = null;
            if(clusterEvents.size() > 0){
                eventIt = clusterEvents.iterator();
                event = eventIt.next();
            }
                 
            //raw data
            MeasureCollection measurecol[][] = new MeasureCollection[2][];
            measurecol[0] = m_measures0;
            measurecol[1] = m_measures1;
            int numValues = 0;
            //header
            out.write("Nr"+del);
            out.write("Event"+del);
            for (int m = 0; m < 1; m++) {
                for (int i = 0; i < measurecol[m].length; i++) {
                    for (int j = 0; j < measurecol[m][i].getNumMeasures(); j++) {
                        if(measurecol[m][i].isEnabled(j)){
                            out.write(m+"-"+measurecol[m][i].getName(j)+del);
                            numValues = measurecol[m][i].getNumberOfValues(j);
                        }
                    }
                }
            }
            out.write("\n");


            //rows
            for (int v = 0; v < numValues; v++){
                //Nr
                out.write(v+del);

                //events
                if(event!=null && event.getTimestamp()<=m_stream0_decayHorizon*v){
                    out.write(event.getType()+del);
                    if(eventIt!= null && eventIt.hasNext()){
                        event=eventIt.next();
                    }
                    else
                        event = null;
                }
                else
                    out.write(del);

                //values
                for (int m = 0; m < 1; m++) {
                    for (int i = 0; i < measurecol[m].length; i++) {
                        for (int j = 0; j < measurecol[m][i].getNumMeasures(); j++) {
                            if(measurecol[m][i].isEnabled(j)){
                                    out.write(measurecol[m][i].getValue(j, v)+del);
                            }
                        }
                    }
                }
                out.write("\n");
            }
//            //Boxplot data
//            out.write("\n");
//            out.write("\n");
//            out.write("Statistics\n");
//            out.write("Name"+del+"Min"+del+"1.Quartile"+del+"Media"+del+"2.Quartile"+del+"Max"+del+"Mean"+del+"\n");
//            for (int m = 0; m < 2; m++) {
//                MeasureCollection measurecol[];
//                if(m==0)
//                    measurecol = m_measures0;
//                else
//                    measurecol = m_measures1;
//
//                for (int i = 0; i < measurecol.length; i++) {
//                    for (int j = 0; j < measurecol[i].getNumMeasures(); j++) {
//                        if(measurecol[i].isEnabled(j)){
//                            out.write(m+"-"+measurecol[i].getName(j)+del);
//                            out.write(measurecol[i].getMinValue(i)+del);
//                            out.write(measurecol[i].getLowerQuartile(i)+del);
//                            out.write(measurecol[i].getMedian(i)+del);
//                            out.write(measurecol[i].getUpperQuartile(i)+del);
//                            out.write(measurecol[i].getMaxValue(i)+del);
//                            out.write(measurecol[i].getMean(i)+del);
//                            out.write("\n");
//                        }
//                    }
//                }
//            }

            out.close();

        } catch (IOException ex) {
            Logger.getLogger(RunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    public void runDebug() {

        int processCounter = 0;
        int speedCounter = 0;
        LinkedList<DataPoint> pointBuffer0 = new LinkedList<DataPoint>();
        LinkedList<DataPoint> pointBuffer1 = new LinkedList<DataPoint>();

        while(work || processCounter!=0){
            if (m_stream0.hasMoreInstances() && ((m_stream1 == null && m_stream_duplicate) || m_stream1.hasMoreInstances())) {
                m_timestamp++;
                speedCounter++;
                processCounter++;
                if(m_timestamp%100 == 0){
                    m_visualPanel.setProcessedPointsCounter(m_timestamp);
                }

                Instance next0 = m_stream0.nextInstance();
                Instance next1;
                if(!m_stream_duplicate)
                    next1 = m_stream1.nextInstance();
                else
                    next1 = next0;

                //if(m_timestamp < 5000) continue;
                DataPoint point0 = new DataPoint(next0,m_timestamp);
                DataPoint point1 = new DataPoint(next1,m_timestamp);

                pointBuffer0.add(point0);
                pointBuffer1.add(point1);
                while(pointBuffer0.size() > m_stream0_decayHorizon)
                    pointBuffer0.removeFirst();
                while(pointBuffer1.size() > m_stream1_decayHorizon)
                    pointBuffer1.removeFirst();

                PointPanel pointPanel0 = new PointPanel(point0,m_stream0_decay_rate,m_stream0_decay_threshold);
                PointPanel pointPanel1 = new PointPanel(point1,m_stream1_decay_rate,m_stream1_decay_threshold);



                if(m_visualPanel.isEnabledDrawPoints()){
                    m_streampanel0.add(pointPanel0);
                    m_streampanel1.add(pointPanel1);
                    pointPanel0.updateLocation();
                    pointPanel1.updateLocation();
                }
                if(processCounter%10==0){
                    for (Component comp : m_streampanel0.getComponents()){
                        if(comp instanceof ClusterPanel)
                            m_streampanel0.remove(comp);
                    }
                    for (Component comp : m_streampanel1.getComponents()){
                        if(comp instanceof ClusterPanel)
                            m_streampanel1.remove(comp);
                    }
                    //drawClustering(m_streampanel0, ((RandomRBFGeneratorEvents)m_stream0).getGeneratingClusters(), Color.BLUE);
//                    drawClustering(m_streampanel0, ((RandomRBFGeneratorEvents)m_stream0).getClustering(), Color.RED);
//                    drawClustering(m_streampanel1, new Clustering(pointBuffer1), Color.BLACK);
//                    drawGenClustering(m_streampanel0, ((RandomRBFGeneratorEvents)m_stream0).getGeneratingClusters() , Color.BLUE);
                    
                    m_streampanel0.repaint();
                    m_streampanel1.repaint();
                    processCounter = 0;
                }

                int pauseInterval = m_visualPanel.getPauseInterval();
                if(pauseInterval!=0 &&
                   m_lastPauseTimestamp+pauseInterval<=m_timestamp)
                    m_visualPanel.toggleVisualizer(true);


//                //thats a bit of a hack, use boolean keepLabel() in Clusterer interface?
//                Instance traininst0 = new Instance(point0);
//                if(m_clusterer0 instanceof ClusterGenerator)
//                    traininst0.setDataset(point0.dataset());
//                else
//                    traininst0.deleteAttributeAt(point0.classIndex());
//
//                Instance traininst1 = new Instance(point1);
//                if(m_clusterer1 instanceof ClusterGenerator)
//                    traininst1.setDataset(point0.dataset());
//                else
//                    traininst1.deleteAttributeAt(point0.classIndex());
//
//                m_clusterer0.trainOnInstanceImpl(traininst0);
//                m_clusterer1.trainOnInstanceImpl(traininst1);

            } else {
                System.out.println("DONE");
                return;
            }
            if(speedCounter > m_wait_frequency && m_wait_frequency < 15){
                try {
                    synchronized (this) {
                        if(m_wait_frequency == 0)
                            wait(50);
                        else
                            wait(10);
                    }
                } catch (InterruptedException ex) {

                }
                speedCounter = 0;
            }
        }
        if(!stop){
            work_pause();
        }
    }

    public void weka() {
        Clustering wekaClustering;
        if(m_clusterer0.implementsMicroClusterer() && m_clusterer0.evaluateMicroClusteringOption.isSet())
            wekaClustering = micro0;
        else
            wekaClustering = macro0;

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

