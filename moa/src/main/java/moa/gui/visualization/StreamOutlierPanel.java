/*
 *    StreamPanel.java
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

package moa.gui.visualization;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import moa.cluster.SphereCluster;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import moa.clusterers.outliers.MyBaseOutlierDetector.Outlier;

public class StreamOutlierPanel extends JPanel implements ComponentListener {
    private OutlierPanel highlighted_outlier = null;
    private double zoom_factor = 0.2;
    private int zoom = 1;
    private int width_org;
    private int height_org;
    private int activeXDim = 0;
    private int activeYDim = 1;
    
    private JPanel layerOutliers;
    private LabelAlgorithmPanel layerAlgorithmTitle;
    
    private RunOutlierVisualizer m_visualizer = null;
    private MyBaseOutlierDetector m_outlierDetector = null;    
    
    //Buffered Image stuff
    private BufferedImage pointImg;
    private BufferedImage canvasImg; 
    private ImgPanel layerCanvas;
    private boolean bAntiAlias = false;
    private int EVENTSIZE = 10;

    class ImgPanel extends JPanel{
        public BufferedImage image = null;
        public void setImage(BufferedImage image){
            setSize(image.getWidth(), image.getWidth());
            this.image = image;
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            if(image!=null)
                g2.drawImage(image, null, 0, 0);
        }
    }
    
    class LabelAlgorithmPanel extends JPanel{
        public Color color;
        
        public LabelAlgorithmPanel() {
            setOpaque(true);
            setLayout(null);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setColor(color);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public StreamOutlierPanel(Color colorAlgorithmTitle) {
        initComponents();
        
        layerAlgorithmTitle = new LabelAlgorithmPanel();
        layerAlgorithmTitle.color = colorAlgorithmTitle;
        add(layerAlgorithmTitle);
        
        layerOutliers = getNewLayer();                
        add(layerOutliers);
        
        layerCanvas = new ImgPanel();
        add(layerCanvas);
        
        addComponentListener(this);        
    }

    private JPanel getNewLayer(){
        JPanel layer = new JPanel();
        layer.setOpaque(false);
        layer.setLayout(null);
        return layer;
    }
    
    public void drawOutliers(Vector<Outlier> outliers, Color color){
        drawOutliers(layerOutliers, outliers, color);
    }
    
    public void repaintOutliers() {
        layerOutliers.repaint();
    }
    
    public void setOutliersVisibility(boolean visibility){
        layerOutliers.setVisible(visibility);
        layerOutliers.repaint();
    }
    
    public void setPointsVisibility(boolean visibility){        
        layerCanvas.setVisible(visibility);       
        layerCanvas.repaint();
    }
    
    public void clearPoints() {
        Graphics2D imageGraphics = (Graphics2D) pointImg.createGraphics();
                
        imageGraphics.setColor(Color.WHITE);       
        imageGraphics.setPaint(Color.WHITE);
        imageGraphics.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
           
        ApplyToCanvas(pointImg);
        RedrawPointLayer();
    }
    
    public void clearEvents() {
        ApplyToCanvas(pointImg);
        //RedrawPointLayer();
    }
    
    public void ApplyToCanvas(BufferedImage img) {
        Graphics2D g = (Graphics2D) canvasImg.createGraphics();
        g.drawImage(img, 0, 0, this);
    }
    
    public void RedrawPointLayer() {
        //System.out.println("print?");
        layerCanvas.setImage(canvasImg);
        layerCanvas.repaint();
    }
    
    private void drawPoint(
            DataPoint point, 
            boolean bShowDecay,
            Color c, 
            boolean bFill,
            boolean bRedrawPointImg)
    {
        Graphics2D imageGraphics = (Graphics2D) pointImg.createGraphics();

        if (bAntiAlias) {
            imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int size = Math.min(getWidth(), getHeight());
        int x = (int) Math.round(point.value(getActiveXDim()) * size);
        int y = (int) Math.round(point.value(getActiveYDim()) * size);
        //System.out.println("drawPoint: size="+size+" x="+x+" y="+y);

        if (c == null) {
            // fixed color of points
            c = Color.GRAY;
            // get a color by class of point
            // Color c = PointPanel.getPointColorbyClass((int)point.classValue(), 10);        
        }
        
        if (bShowDecay) {
            int minValue = 40; // 20
            double w = point.weight();            
            int alpha = (int) (255 * w + minValue);
            if (alpha > 255) alpha = 255;
            //System.out.println("alpha="+alpha+"w="+w);
            c = new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
        }
        
        imageGraphics.setColor(c);
        int psize = PointPanel.POINTSIZE;
        int poffset = 2;
        imageGraphics.drawOval(x - poffset, y - poffset, psize, psize);
        if (bFill) imageGraphics.fillOval(x - poffset, y - poffset, psize, psize);
        
        if (bRedrawPointImg) {
            ApplyToCanvas(pointImg);
            RedrawPointLayer();
        }
    }

    public void drawPoint(DataPoint point, boolean bShowDecay, boolean bRedrawPointImg){
        drawPoint(point, bShowDecay, null, true, bRedrawPointImg);
    }
    
    /*public static BufferedImage duplicateImage(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException();
        }

        BufferedImage j = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        j.setData(image.getData());
        return j;
    }*/
    
    public void drawEvent(OutlierEvent outlierEvent, boolean bRedrawPointImg)
    {
        Graphics2D imageGraphics = (Graphics2D) canvasImg.createGraphics();

        if (bAntiAlias) {
            imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int size = Math.min(getWidth(), getHeight());
        int x = (int) Math.round(outlierEvent.point.value(getActiveXDim()) * size);
        int y = (int) Math.round(outlierEvent.point.value(getActiveYDim()) * size);
        //System.out.println("drawPoint: size="+size+" x="+x+" y="+y);

        Color c = outlierEvent.outlier ? Color.RED : Color.BLACK;
        
        imageGraphics.setColor(c);
        int psize = EVENTSIZE;
        int poffset = EVENTSIZE / 2;
        imageGraphics.drawOval(x - poffset, y - poffset, psize, psize);

        if (bRedrawPointImg) {
            RedrawPointLayer();
        }
    }

    public void applyDrawDecay(float factor, boolean bRedrawPointImg){
        //System.out.println("applyDrawDecay: factor="+factor);
                
        // 1)
        int v = Color.GRAY.getRed();
        //System.out.println("applyDrawDecay: v="+v);
        RescaleOp brightenOp = new RescaleOp(1f, (255-v)*factor, null);
        
        // 2)
        //RescaleOp brightenOp = new RescaleOp(1f + factor, 0, null);
        
        // 3)
        //RescaleOp brightenOp = new RescaleOp(1f, (255)*factor, null);
        
        pointImg = brightenOp.filter(pointImg, null);
        
        if (bRedrawPointImg) {
            ApplyToCanvas(pointImg);
            RedrawPointLayer();
        }
    }

    private void drawOutliers(JPanel layer, Vector<Outlier> outliers, Color color){        
        layer.removeAll();
        for (Outlier outlier : outliers) {  
            int length = outlier.inst.numValues() - 1; // -1
            double[] center = new double[length]; // last value is the class
            for (int i = 0; i < length; i++) {
                center[i] = outlier.inst.value(i);                
            }            
            SphereCluster cluster = new SphereCluster(center, 0);                
                
            OutlierPanel outlierpanel = new OutlierPanel(m_outlierDetector, outlier, cluster, color, this);
            
            layer.add(outlierpanel);
            outlierpanel.updateLocation();
        }

        layer.repaint();
    }

    public void screenshot(String filename, boolean svg, boolean png){
    	if(layerOutliers.getComponentCount() == 0)
            return;
    	
        BufferedImage image = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
        if(png){
            synchronized(getTreeLock()){
                Graphics g = image.getGraphics();
                paintAll(g);
                try {
                    ImageIO.write(image, "png", new File(filename+".png"));
                } catch (Exception e) { }
            }
        }
        if(svg){
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename+".svg")));
                int width = 500;
                out.write("<?xml version=\"1.0\"?>\n");
                out.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
                out.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\""+width+"\" height=\""+width+"\">\n");

                if(layerOutliers.isVisible()){
                    for(Component comp :layerOutliers.getComponents()){
                        if(comp instanceof ClusterPanel)
                            out.write(((ClusterPanel)comp).getSVGString(width));
                    }
                }
                
                out.write("</svg>");
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(StreamPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public OutlierPanel getHighlightedOutlierPanel(){
        return highlighted_outlier;
    }

    public void setHighlightedOutlierPanel(OutlierPanel outlierpanel){
        //if (highlighted_outlier == outlierpanel) 
        //    return;
        
        //System.out.println("setHighlightedOutlierPanel");
        
        // restore previous highlighted outlier
        if (highlighted_outlier != null)            
            highlighted_outlier.highlight(false);
        
        highlighted_outlier = outlierpanel;
        if (highlighted_outlier != null)  
            highlighted_outlier.highlight(true);
        
        repaint();
    }

    public void setZoom(int x, int y, int zoom_delta, JScrollPane scrollPane){
        
        if(zoom ==1){
            width_org = getWidth();
            height_org = getHeight();
        }
        zoom+=zoom_delta;
        
        if(zoom<1) zoom = 1;
        else{
            int size = (int)(Math.min(width_org, height_org)*zoom_factor*zoom);

            setSize(new Dimension(size*zoom, size*zoom));
            setPreferredSize(new Dimension(size*zoom, size*zoom));

            scrollPane.getViewport().setViewPosition(new Point((int)(x*zoom_factor*zoom+x),(int)( y*zoom_factor*zoom+y)));
        }
    }

    public int getActiveXDim() {
        return activeXDim;
    }

    public void setActiveXDim(int activeXDim) {
        this.activeXDim = activeXDim;
    }

    public int getActiveYDim() {
        return activeYDim;
    }

    public void setActiveYDim(int activeYDim) {
        this.activeYDim = activeYDim;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(255, 255, 255));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked       
        if (highlighted_outlier != null){
            highlighted_outlier.highlight(false);
            highlighted_outlier = null;
        }
    }//GEN-LAST:event_formMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    public void setVisualizer(RunOutlierVisualizer v) {
        m_visualizer = v;
    }
    
    public void setOutlierDetector(MyBaseOutlierDetector outlierDetector) {
        m_outlierDetector = outlierDetector;
    }
    
    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
        //System.out.println("componentResized");

        int heightAlgorithmTitle = 2;
        int size = Math.min(getWidth(), getHeight() - heightAlgorithmTitle);
        layerOutliers.setBounds(0, heightAlgorithmTitle, size, size);
        layerAlgorithmTitle.setBounds(0, 0, getWidth(), heightAlgorithmTitle);

        pointImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        canvasImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        layerCanvas.setBounds(0, heightAlgorithmTitle, size, size); 

        Graphics2D imageGraphics = (Graphics2D) pointImg.getGraphics();
        imageGraphics.setColor(Color.white);
        imageGraphics.fillRect(0, 0, getWidth(), getHeight());
        imageGraphics.dispose();    
               
        ApplyToCanvas(pointImg);
        RedrawPointLayer();
        
        if (m_visualizer != null) {
            m_visualizer.redrawOnResize();
        }
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }
}
