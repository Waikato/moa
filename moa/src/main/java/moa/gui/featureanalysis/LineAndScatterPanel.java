/*
 *    LineAndScatterPanel.java
 *    Copyright (C) 2020 University of Waikato, Hamilton, New Zealand
 *    @author Yongheng Ma (2560653665@qq.com)
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
 *
 */
package moa.gui.featureanalysis;

import com.yahoo.labs.samoa.instances.Instances;
import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

/**
 * This is a sub panel in VisualizeFeatures tab. It is used to show a feature as a line graph or a scatter diagram, or to show
 * an amplified line graph or scatter diagram, or to show all features' line graphs or scatter diagrams.
 */
public class LineAndScatterPanel extends JPanel {
    /**
     * This holds the current set of instances
     */
    protected Instances m_data;

    /**The attribute index starting from 0*/
    protected int m_attributeIndex;

    /** Attribute name */
    protected String m_attributeName;

    /** plot type drop list:
     *          "plot type: Line graph"
     *          "plot type: Scatter diagram"
     *          "No plot type"
     *
     *     m_selectedPlotTyeIndex means the selected plot index
     */
    protected int m_selectedPlotTyeIndex;

    /**The string of the the selected plot type such as "plot type: Line graph" */
    protected String m_selectedPlotTyeItem;

    /**The start instance index of x axis for line graph or scatter diagram */
    protected int m_intStartIndex;

    /**The end instance index of x axis for line graph or scatter diagram */
    protected int m_intEndIndex;

    /** Double list which store the feature data */
    protected List<double[]> featureVectorList;

    /** THe drawing tool provided by jmathplot.jar */
    protected Plot2DPanel plot = new Plot2DPanel();

    /**
     * The feature range start index. For example, if the feature range is from 1 to 9,
     * then the start index is 1 and the end index is 9.
     *
     * The default plot number in a popup screen is 9 at most.
     */
    protected int m_featureRangeStartIndex;

    /**
     * The feature range end index. For example, if the feature range is from 1 to 9,
     * then the start index is 1 and the end index is 9.
     *
     * The default plot number in a popup screen is 9 at most.
     */
    protected int m_featureRangeEndIndex;

    /** The string of feature range. For example,"feature range: 1 -- 9". */
    protected String m_featureRange;

    public int getSelectedPlotTyeIndex() {
        return m_selectedPlotTyeIndex;
    }

    public String getSelectedPlotItem() {
        return m_selectedPlotTyeItem;
    }

    public void setSelectedPlotItem(String SelectedPlotItem) {
        m_selectedPlotTyeItem=SelectedPlotItem;
    }

    public String getAttributeName() {
        return m_attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.m_attributeName = attributeName;
    }

    public int getIntStartIndex() {
        return m_intStartIndex;
    }

    public void setIntStartIndex(int intStartIndex) {
        this.m_intStartIndex = intStartIndex;
    }

    public int getIntEndIndex() {
        return m_intEndIndex;
    }

    public void setIntEndIndex(int intEndIndex) {
        this.m_intEndIndex = intEndIndex;
    }

    public int getFeatureRangeStartIndex() {
        return m_featureRangeStartIndex;
    }

    public void setFeatureRangeStartIndex(int featureRangeStartIndex) {
        this.m_featureRangeStartIndex = featureRangeStartIndex;
    }

    public int getFeatureRangeEndIndex() {
        return m_featureRangeEndIndex;
    }

    public void setFeatureRangeEndIndex(int featureRangeEndIndex) {
        this.m_featureRangeEndIndex = featureRangeEndIndex;
    }

    /**
     * Parse String to number.
     * For example, featureRange is "feature range: 1 -- 9", then is parsed into 0 and 8.
     */
    public void setFeatureRange(String featureRange) {
        this.m_featureRange = featureRange;

        String rangeStart="",rangeEnd="";

        /**
         * The string is separated by one single space character so it is split by
         * one single space character.
         * featureRange example: "feature range: 1 -- 9", or "feature range: 5".
         */
        String[] split=featureRange.split("[ ]");
        if(split.length==5){
            rangeStart=split[2].trim();
            rangeEnd=split[4].trim();
        }else if(split.length==3){
            rangeStart=split[2].trim();
            rangeEnd=split[2].trim();
        }
        /**
         * The number shown to user in droplist is natural number,but the index of data in array is from 0,
         * so the following number should minus one.
         */
        this.setFeatureRangeStartIndex(Integer.parseInt(rangeStart)-1);
        this.setFeatureRangeEndIndex(Integer.parseInt(rangeEnd)-1);
    }


    /** Set dataset which is the data source of line graph or scatter diagram. */
    public void setInstances(Instances inst) {
        m_data = inst;

        if (m_data != null) {
            int numInst = m_data.size();
            int numAttributes = m_data.numAttributes();
            featureVectorList=new ArrayList<double[]>();

            double[] featureVector = new double[numInst];

            Future[] futures=new Future[numAttributes];

            ExecutorService service = Executors.newFixedThreadPool(numAttributes);
            for(int i = 0; i < numAttributes; i++ ){
                //Future<double[]> model = service.submit(new ExtractFeatureVectorCallable(Integer.valueOf(i)));
                futures[i] = service.submit(new ExtractFeatureVectorCallable(Integer.valueOf(i)));

                /**
                 * In particular, "model.get()" is a synchronized operation, which can not be run with the
                 * above service.submit(), otherwise the multiple threads can not exert their effect.
                 */
                //featureVector = model.get();
            }
            for(int j=0; j<numAttributes;j++){
                try {
                    featureVector= (double[]) futures[j].get();
                    featureVectorList.add(featureVector);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            service.shutdown();
            while (!service.isTerminated()) {
            }
        }
    }

    /**
     * Extract feature values from dataset using multiple threads.
     */
    class ExtractFeatureVectorCallable implements Callable<double[]> {
        private int m_attributeIndex;//start from 0
        public ExtractFeatureVectorCallable(Integer attribIndex) {
            this.m_attributeIndex = attribIndex.intValue();
        }
        @Override
        public double[] call() throws Exception {
            int numInst=m_data.numInstances();
            double[] featureVector=new double[numInst];

            for (int i = 0; i < numInst; i++) {
                featureVector[i] = m_data.get(i).value(m_attributeIndex);
            }
            return featureVector;
        }
    }

    public void setAttributeIndex(int attributeIndex) {
        m_attributeIndex = attributeIndex;
    }

    public int getAttributeIndex() {
        return m_attributeIndex;
    }


    public LineAndScatterPanel() {
        plot.setSize(super.getWidth(), super.getHeight());
        /**
         * for line graph or scatter diagram, the first attribute is the default attribute to show
         */
        this.setAttributeIndex(0);
    }

    /** User set plot related parameters in GUI such as plot type, selected attribute */
    public void setSelectedPlotInfo(int selectedPlotTyeIndex, String selectedPlotItem, int attributeIndex, String attributeName) {
        this.m_selectedPlotTyeIndex = selectedPlotTyeIndex;
        this.setAttributeIndex(attributeIndex);
        this.setAttributeName(attributeName);
        this.setSelectedPlotItem(selectedPlotItem);

        /**
         * remove the last plot in which maybe there is empty graph or there is a graph,
         * so that the next plot is new and updated by new data
         */
        this.remove(plot);//this is a class which extends JPanel
        plot = new Plot2DPanel();
        this.repaint();
    }

    /**
     * This override method is used to paint embedded line graph or scatter diagram in VisualizeFeature Tab.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setLayout(new BorderLayout());
        String attributeName=this.getAttributeName();

        if (m_data != null) {
            double[] y_axis=featureVectorList.get(getAttributeIndex());

            m_intStartIndex=getIntStartIndex();
            m_intEndIndex=getIntEndIndex();

            /**
             * when Plot2DPanel plots, it's plot range is [startIndex,endIndex)
             * For example,range [1,201) means that Plot2DPanel will plot the first point but not plot the 201th point.
             */
            double[] y_axis_segment=Arrays.copyOfRange(y_axis,m_intStartIndex-1,m_intEndIndex);

            double[] x_axis_segment=this.setXAxisCoordinate();

            if (getSelectedPlotItem().equalsIgnoreCase("plot type: Line graph")) {
                plot.addLegend("SOUTH");
                plot.addLinePlot(attributeName, Color.BLUE,x_axis_segment,y_axis_segment);
                plot.removePlotToolBar();
                plot.setVisible(true);
                this.add(plot, BorderLayout.CENTER);
                super.revalidate();//This code refresh the whole window
            } else if (getSelectedPlotItem().equalsIgnoreCase("plot type: Scatter diagram")) {
                plot=new Plot2DPanel();
                plot.addLegend("SOUTH");
                plot.addScatterPlot(attributeName, Color.BLUE, x_axis_segment,y_axis_segment);
                plot.removePlotToolBar();
                plot.setVisible(true);

                this.add(plot, BorderLayout.CENTER);
                super.revalidate();//This code refresh the VisualizeFeature Tab
            }else if(getSelectedPlotItem().equalsIgnoreCase("No plot type")){
                super.revalidate();
            }
        }
    }

    /**
     * This method is used to paint line graph or scatter diagram in popup window from VisualizeFeature Tab.
     */
    public void paintAmplifiedPlot() {
        /**
         * The plot object here must be different from the plot object in method paintComponent(Graphics g),
         * otherwise, the diagram here will overlapped on the diagram plotted in method paintComponent(Graphics g).
         */
        Plot2DPanel amplifyPlot=new Plot2DPanel();

        String attrName=this.getAttributeName();
        if (m_data != null) {
            double[] y_axis=featureVectorList.get(getAttributeIndex());

            m_intStartIndex=getIntStartIndex();
            m_intEndIndex=getIntEndIndex();
            double[] y_axis_segment=Arrays.copyOfRange(y_axis,m_intStartIndex-1,m_intEndIndex);


            double[] x_axis_segment = setXAxisCoordinate();

            JFrame jf=new JFrame("Amplification of Line graph or scatter diagram");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            Dimension d=this.getScreenSize();
            jf.setSize(d);

            jf.setLayout(new BorderLayout());
            jf.add(amplifyPlot, BorderLayout.CENTER);
            amplifyPlot.addLegend("SOUTH");
            amplifyPlot.setAxisLabel(0, "Instance index");//X axis
            amplifyPlot.setAxisLabel(1, "Instance attribute value");//Y axis
            amplifyPlot.setFont(new Font("Courier", Font.BOLD, 12));

            amplifyPlot.setVisible(true);
            jf.setVisible(true);

            if (getSelectedPlotItem().equalsIgnoreCase("plot type: Line graph")) {
                amplifyPlot.addLinePlot(attrName, Color.BLUE,x_axis_segment,y_axis_segment);
            } else if (getSelectedPlotItem().equalsIgnoreCase("plot type: Scatter diagram")) {
                /**
                 * add a title to differentiate feature name and different legend
                 */
                BaseLabel title = new BaseLabel("Feature: "+attrName, Color.BLACK, 0.5, 1.1);
                title.setFont(new Font("Courier", Font.BOLD, 20));
                amplifyPlot.addPlotable(title);

                int attIndex=getAttributeIndex();
                int numValues=m_data.attribute(attIndex).numValues();
                Object[] attributeValues=new Object[numValues];
                if(m_data.attribute(attIndex).isNominal()){
                    attributeValues=m_data.attribute(attIndex).getAttributeValues().toArray();
                }

                if(attrName.endsWith("(nominal)")){
                    //multiple legend
                    /**
                     * 1.filter different y axis values, count for each different value
                     * 2.according to y axis values, get new sub pair x_axis array and y axis array
                     * 3.plot graph
                     *
                     * For example,the original x axis array and y axis array are the following
                     * x_axis: [50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0]
                     * y_axis: [0.0,   1.0,  0.0, 1.0,   2.0,  1.0,  0.0,  1.0,  2.0,  0.0,  2.0]
                     *
                     * After step 1,get y axis values: 0.0,1.0,2.0. The count of 0.0 is 4,the count of 1.0 is 4,
                     * and the count of 2.0 is 3
                     *
                     * After step 2,get the following three sub pair arrays
                     * sub array 1: the y axis value is 0.0
                     * x_axis: [50.0, 52.0, 56.0, 59.0]
                     * y_axis: [0.0,   0.0,  0.0,  0.0]
                     *
                     * sub array 1:the y axis value is 1.0
                     * x_axis: [51.0, 53.0, 55.0, 57.0]
                     * y_axis: [1.0,  1.0,   1.0,  1.0]
                     *
                     * sub array 1:the y axis value is 2.0
                     * x_axis: [54.0, 58.0, 60.0]
                     * y_axis: [2.0,  2.0,  2.0]
                     *
                     */
                    int len =y_axis_segment.length;
                    Map hm=new HashMap();
                    for(int h=0;h<len;h++){
                        if(hm.containsKey(y_axis_segment[h])){
                            int count=(int)hm.get(y_axis_segment[h]);
                            hm.put(y_axis_segment[h],count+1);
                        }else{
                            hm.put(y_axis_segment[h],1);
                        }
                    }

                    for(Object key: hm.keySet()){
                        //System.out.println("y value: "+(double)key + ", y value count: "+ hm.get(key));

                        double y_value=(double)key;
                        int value_count=(int)hm.get(key);
                        double[] y_axis_sub=new double[value_count];

                        double[] x_axis_sub=new double[value_count];
                        List<Double> al=new ArrayList();

                        /**
                         * Plot2DPanel.addScatterPlot(String name, double[] X, double[] Y) method has a bug.
                         * When it plot two points, it wrongly swap a pair of x axis and y axis. But when it
                         * plots one point or more than two point, it behaves well.
                         *
                         * Bug example: the original data is as the following two points
                         *              x_axis: [6.0, 58.0]
                         *              y_axis: [96.0, 96.0]
                         * but actually, it plots the following two wrong points
                         *              x_axis: [6.0, 96.0]
                         *              y_axis: [58.0, 96.0]
                         *
                         * A compromise solution: a little bit strange, but it works.
                         *              1. swap x_axis[1] for y_axis[0] in advance;
                         *                  before swap(right data):
                         *                          x_axis: [6.0, 58.0]
                         *                          y_axis: [96.0, 96.0]
                         *                  after swap(wrong data):
                         *                         x_axis: [6.0, 96.0]
                         *                         y_axis: [58.0, 96.0]
                         *
                         *              2. take advantage of the bug, it swap x_axis[1] for y_axis[0] the 2nd time, and get the right data
                         *                         x_axis: [6.0, 58.0]
                         *                         y_axis: [96.0, 96.0]
                         */

                        if(value_count==2){
                            y_axis_sub[1]=y_value;//y_axis_sub[1]
                            int count=0;
                            for(int s=0;s<len;s++){
                                if(y_axis_segment[s]==y_value){
                                    count++;
                                    if(count==1){
                                        al.add(x_axis_segment[s]);//the source of y_axis_sub[0]
                                    }else if(count==2){
                                        y_axis_sub[0]= x_axis_segment[s];//y_axis_sub[0]
                                        al.add(Double.valueOf(y_value));//the source of y_axis_sub[1]
                                    }
                                    continue;
                                }
                            }
                        }else {
                            if (value_count > 2) {
                                for (int f = 0; f < value_count; f++) {
                                    y_axis_sub[f] = y_value;
                                }
                            }else if(value_count==1){
                                y_axis_sub[0]=y_value;
                            }

                            /** Common code for "value_count==1" and "value_count > 2" */
                            for(int s=0;s<len;s++){
                                if(y_axis_segment[s]==y_value){
                                    al.add(x_axis_segment[s]);
                                    continue;
                                }
                            }
                        }

                        for(int t=0;t<al.size();t++){
                            x_axis_sub[t]=al.get(t).doubleValue();
                        }

                        String attributeValue=(String)attributeValues[(int)y_value];
                        amplifyPlot.addScatterPlot(attributeValue, x_axis_sub,y_axis_sub);
                    }
                }else{
                    amplifyPlot.addScatterPlot(attrName, Color.BLUE, x_axis_segment,y_axis_segment);
                }
            }
        }
    }

    /**
     * 1. For nominal feature, just display scatter diagrams in which the data range is from m_intStartIndex to m_intEndIndex
     * 2. Fro numeric feature, show line graph or scatter diagram depending on user's plot type choice,
     *    in which the data range is from m_intStartIndex to m_intEndIndex
     */
    public void visualizeAll(){
        if (m_data != null) {
            m_intStartIndex=getIntStartIndex();
            m_intEndIndex=getIntEndIndex();

            /**
             * String:
             *         plot type: Line graph
             *         plot type: Scatter diagram
             *         No plot type
             */
            m_selectedPlotTyeItem=getSelectedPlotItem();

            JFrame jf=new JFrame("Visualize all features as line graphs or scatter diagrams");
            jf.setSize(830,700);
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            jf.setVisible(true);
            Plot2DPanel plot2d;

            JPanel contentPane=new JPanel();
            contentPane.setVisible(true);
            contentPane.setLayout(new GridLayout(4,3));
            JScrollPane scroller = new JScrollPane(contentPane);
            jf.add(scroller);

            m_featureRangeStartIndex=getFeatureRangeStartIndex();
            m_featureRangeEndIndex=getFeatureRangeEndIndex();

            for(int p=m_featureRangeStartIndex;p<=m_featureRangeEndIndex;p++){

                double[] y_axis_visualizeAll=featureVectorList.get(p);
                double[] y_axis_visualizeAll_segment=Arrays.copyOfRange(y_axis_visualizeAll,m_intStartIndex-1,m_intEndIndex);

                double[] x_axis_visualizeAll_segment = setXAxisCoordinate();

                boolean isNominal=m_data.attribute(p).isNominal();

                plot2d=new Plot2DPanel();

                JPanel jp=new JPanel();
                jp.setLayout(new BorderLayout());
                jp.setVisible(true);
                String name=m_data.attribute(p).name();
                jp.setBorder(BorderFactory.createTitledBorder(name));
                jp.setPreferredSize(new Dimension(200,200));

                if(isNominal){
                    plot2d.addScatterPlot(name,Color.BLUE,x_axis_visualizeAll_segment,y_axis_visualizeAll_segment);
                }else{
                    if (getSelectedPlotItem().equalsIgnoreCase("plot type: Line graph")) {
                        plot2d.addLinePlot(name,Color.BLUE,x_axis_visualizeAll_segment,y_axis_visualizeAll_segment);
                    }else if (getSelectedPlotItem().equalsIgnoreCase("plot type: Scatter diagram")) {
                        plot2d.addScatterPlot(name,Color.BLUE,x_axis_visualizeAll_segment,y_axis_visualizeAll_segment);
                    }

                }

                plot2d.setVisible(true);
                plot2d.removePlotToolBar();
                jp.add(plot2d,BorderLayout.CENTER);
                contentPane.add(jp);
            }
        }
    }

    /** set x axis Coordinate */
    private double[] setXAxisCoordinate(){
        int length=m_intEndIndex-m_intStartIndex+1;
        double[] x_axis_visualizeAll_segment= new double[length];
        for(int k=0;k<length;k++){
            x_axis_visualizeAll_segment[k]=m_intStartIndex-1+k;
        }
        return x_axis_visualizeAll_segment;
    }

    /** Get the screen size so that the amplified graph size is the same as the screen size. */
    public  Dimension getScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = screenSize.width;
        int h = screenSize.height;

        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
                this.getGraphicsConfiguration());

        w = w - (screenInsets.left + screenInsets.right);
        h = h - (screenInsets.top + screenInsets.bottom);

        return new Dimension(w, h);
    }
}
