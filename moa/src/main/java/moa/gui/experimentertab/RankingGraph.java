/*
 *    RankingGraph.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
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
package moa.gui.experimentertab;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import moa.gui.experimentertab.statisticaltests.PValuePerTwoAlgorithm;
import moa.gui.experimentertab.statisticaltests.RankPerAlgorithm;
import nz.ac.waikato.cms.gui.core.BaseFileChooser;
import org.jfree.ui.FontChooserPanel;
import org.jfree.ui.StrokeChooserPanel;
import org.jfree.ui.StrokeSample;
import weka.gui.ExtensionFileFilter;

/**
 * Shows the comparison of several online learning algorithms on multiple
 * datasets by performing appropriate statistical tests.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class RankingGraph extends JFrame {
    
    JPanel zoomPanel;
    JPanel graphPanel;
    JPanel graphPanelDisplay = new JPanel();
    JPanel controlPanelDisplay = new JPanel();
    JPanel controlPanel;
    JPanel imgOptionsPanel;
    JPanel exportPanel;
    JButton btnSave, btnLineStroke, btnFont, btnLineDifStronke, btnDir;
    //JComboBox imgType;
    //JTextField JtextFieldimgName;
    JSlider xSlider, ySlider;
    int height, width;
    int x0, y0;
    int xScale, yScale;
    double pvalue;
    
    Font textFont = new Font("Arial", Font.PLAIN, 14);
    ArrayList<RankPerAlgorithm> algRank;
    ArrayList<PValuePerTwoAlgorithm> PValues;
    final static BasicStroke currentStroke = new BasicStroke(1.0f);
    final static float dash1[] = {1.5f};
    final static BasicStroke dashed = new BasicStroke(1.5f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, dash1, 0.0f);
    private StrokeSample[] availableStrokeSamples;
    private StrokeSample stroke = new StrokeSample(currentStroke);
    private StrokeSample difStroke = new StrokeSample(new BasicStroke(3.0f));

    public BufferedImage image;
    public Graphics2D gb;
    public String imgPath;

    /**
     * Class constructor.
     *
     * @param algRank
     * @param PValues
     * @param imgPath
     * @param pvalue
     */
    public RankingGraph(ArrayList<RankPerAlgorithm> algRank, ArrayList<PValuePerTwoAlgorithm> PValues, String imgPath, double pvalue) {
        super("Ranking Viewer");
        this.algRank = algRank;
        this.PValues = PValues;
        this.imgPath = imgPath;
        this.pvalue = pvalue;
        setSize(800, 640);
        initComponents();
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
    }
    
    private void initComponents() {
        Container container = getContentPane();
        width = getSize().width - 10;
        height = 70 * getSize().height / 100;
        graphPanel = new Graph();
        zoomPanel = new SliderPanel();
        controlPanel = new JPanel();
        imgOptionsPanel = new JPanel();
        exportPanel = new JPanel();
        btnSave = new JButton("Save");
        btnLineStroke = new JButton("Line 1 Stroke");
        btnFont = new JButton("Text Font");
        btnLineDifStronke = new JButton("Line 2 Stroke");
        btnDir = new JButton("Save as");
      
        graphPanelDisplay.setLayout(new BorderLayout());
        graphPanelDisplay.add(graphPanel, BorderLayout.CENTER);
        TitledBorder titleborder;
        titleborder = BorderFactory.createTitledBorder(" Zoom");
        zoomPanel.setBorder(titleborder);
        //titleborder = BorderFactory.createTitledBorder("Options");
        //controlPanel.setBorder(titleborder);
        
        titleborder = BorderFactory.createTitledBorder("Properties");
        imgOptionsPanel.setBorder(titleborder);
        
        titleborder = BorderFactory.createTitledBorder("Export");
        exportPanel.setBorder(titleborder);
        
        graphPanelDisplay.setPreferredSize(new Dimension(width, height));
        controlPanel.setPreferredSize(new Dimension(60 * width / 100,
                20 * getSize().height / 100));
        zoomPanel.setPreferredSize(new Dimension(30 * width / 100,
                20 * getSize().height / 100));
        
        EventControl evt = new EventControl();
        btnFont.addActionListener(evt);
        btnLineDifStronke.addActionListener(evt);
        btnLineStroke.addActionListener(evt);
        btnSave.addActionListener(evt);
        btnDir.addActionListener(evt);
        imgOptionsPanel.add(btnFont);
        imgOptionsPanel.add(btnLineStroke);
        imgOptionsPanel.add(btnLineDifStronke);
        
        exportPanel.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        //panel.add(new JLabel("Export as "));
       // panel.add(imgType);
       // panel.add(btnSave);
        
        JPanel panelName = new JPanel();
        //panelName.add(new JLabel("File name "));
       // panelName.add(JtextFieldimgName);
        panelName.add(btnDir);
        exportPanel.add(panelName, BorderLayout.CENTER);
        exportPanel.add(panel, BorderLayout.SOUTH);
        
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(imgOptionsPanel, BorderLayout.CENTER);
        controlPanel.add(exportPanel, BorderLayout.EAST);
        controlPanelDisplay.setLayout(new BorderLayout(1, 1));
        controlPanelDisplay.add(controlPanel, BorderLayout.CENTER);
        controlPanelDisplay.add(zoomPanel, BorderLayout.EAST);
        container.setLayout(new BorderLayout(1, 1));
        container.add("Center", graphPanelDisplay);
        container.add("South", controlPanelDisplay);
        xScale = 20;
        yScale = 20;
        x0 = width / 2;
        y0 = height / 5;
        this.availableStrokeSamples = new StrokeSample[4];
        this.availableStrokeSamples[0] = new StrokeSample(new BasicStroke(1.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dash1, 0.0f));
        this.availableStrokeSamples[1] = new StrokeSample(
                new BasicStroke(1.0f));
        this.availableStrokeSamples[2] = new StrokeSample(
                new BasicStroke(2.0f));
        this.availableStrokeSamples[3] = new StrokeSample(
                new BasicStroke(3.0f));
        
        addWindowStateListener((WindowEvent arg0) -> {
            width = getSize().width - 10;
            height = 70 * getSize().height / 100;
            x0 = width / 2;
            y0 = height / 5;
            xScale = 20;
            yScale = 20;
            xSlider.setValue(50);
            ySlider.setValue(20);
        });
        
    }
    
    private void strokeSelection(StrokeSample str) {
        StrokeChooserPanel panel = new StrokeChooserPanel(
                str, availableStrokeSamples);
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Stroke Selection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            str.setStroke(panel.getSelectedStroke());
            
        }
        
    }

    /**
     * Allow to select the text font.
     */
    public void fontSelection() {
        
        FontChooserPanel panel = new FontChooserPanel(textFont);
        int result
                = JOptionPane.showConfirmDialog(
                        this, panel, "Font Selection",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
                );
        
        if (result == JOptionPane.OK_OPTION) {
            textFont = panel.getSelectedFont();
            
        }
    }
    
    private class EventControl implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent evt) {
            Object source = evt.getSource();
            
            if (source == btnSave) {
                
            } else if (source == btnFont) {
                fontSelection();
            } else if (source == btnLineStroke) {
                strokeSelection(stroke);
            } else if (source == btnLineDifStronke) {
                strokeSelection(difStroke);
            } else if (source == btnDir) {
//                String path = "";
//                JFileChooser propDir = new JFileChooser();
//                int selection = propDir.showSaveDialog(JtextFieldimgName);
//                if (selection == JFileChooser.APPROVE_OPTION) {
//                    path = propDir.getSelectedFile().getAbsolutePath();
//                }
//                if (!path.equals("")) {
//                    JtextFieldimgName.setText(path);
//                    
//                }
              
                BaseFileChooser fileChooser = new BaseFileChooser();
                ExtensionFileFilter filterPNG = new ExtensionFileFilter(".png","PNG Image Files");
                fileChooser.addChoosableFileFilter(filterPNG);

                ExtensionFileFilter filterJPG = new ExtensionFileFilter(".jpg","JPG Image Files");
                fileChooser.addChoosableFileFilter(filterJPG);

                ExtensionFileFilter filterEPS = new ExtensionFileFilter(".eps","EPS Image Files");
                fileChooser.addChoosableFileFilter(filterEPS);
                
	
                fileChooser. setAcceptAllFileFilterUsed(false); 
                fileChooser.setCurrentDirectory(new File(imgPath));
                int option = fileChooser.showSaveDialog(null);
                if (option == JFileChooser.APPROVE_OPTION) { 
                 String fileDesc = fileChooser.getFileFilter().getDescription();
           
                if (fileDesc.startsWith("PNG")) {
                    
                    if (fileChooser.getSelectedFile().getName().toUpperCase().endsWith("PNG")==true) {
                        try {
                                ImageIO.write(image, "png",fileChooser.getSelectedFile());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    } else {
                       try {
                                ImageIO.write(image, "png", new File(fileChooser.getSelectedFile().getAbsolutePath()+".png"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                } else if (fileDesc.startsWith("JPG")) {
                    if (fileChooser.getSelectedFile().getName().toUpperCase().endsWith("JPG")==true) {
                       try {
                                ImageIO.write(image, "jpg", fileChooser.getSelectedFile());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    } else {
                        try {
                                ImageIO.write(image, "jpg", new File(fileChooser.getSelectedFile().getAbsolutePath()+".jpg"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            
        }//else
            }
            graphPanel.repaint();
            
        }
    }
    
    class Graph extends JPanel implements MouseListener,
            MouseMotionListener, MouseWheelListener {
        
        int offsetX, offsetY;
        boolean dragging;
        
        Graph() {
            setBackground(Color.white);
            offsetX = x0;
            offsetY = y0;
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        
        @Override
        public void mousePressed(MouseEvent evt) {
            
            if (dragging) {
                return;
            }
            int x = evt.getX();
            int y = evt.getY();
            offsetX = x - x0;
            offsetY = y - y0;
            dragging = true;
        }
        
        @Override
        public void mouseReleased(MouseEvent evt) {
            dragging = false;
            repaint();
        }
        
        @Override
        public void mouseDragged(MouseEvent evt) {
            if (dragging == false) {
                return;
            }
            
            int x = evt.getX();
            int y = evt.getY();
            x0 = x - offsetX;
            y0 = y - offsetY;
            
            repaint();
        }
        
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double value = 0.05f * e.getPreciseWheelRotation();
            int xleft = x0 - width / 3;
            int xrigth = x0 + width / 3;
          
            if(value > 0 && (xrigth + xScale) - (xleft - xScale) <= 50){
                return;
            }
            else{
               xScale -= value * 100; 
            }
            repaint();
        }
        
        @Override
        public void mouseMoved(MouseEvent evt) {
        }
        
        @Override
        public void mouseClicked(MouseEvent evt) {
        }
        
        @Override
        public void mouseEntered(MouseEvent evt) {
        }
        
        @Override
        public void mouseExited(MouseEvent evt) {
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graficar(g, x0, y0);
            
        }
        
        void Graficar(Graphics ap, int xg, int yg) {

            Graphics2D g = (Graphics2D) ap;
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
            gb = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(textFont);
            gb.setFont(textFont);
            // X axis
            g.setPaint(Color.BLACK);
            
            gb.setBackground(Color.WHITE);
            gb.clearRect(0, 0, image.getWidth(), image.getHeight());
            gb.setColor(Color.BLACK);
            
            int xleft = xg - width / 3;
            int xrigth = xg + width / 3;
            g.setStroke(stroke.getStroke());
            gb.setStroke(stroke.getStroke());
            //Values
            int xin = xleft - xScale;
            int xfin = xrigth + xScale;
            int rank1 = (int)Math.floor(algRank.get(0).rank);
            //ticks number
            int ticks = (int) (Math.ceil(algRank.get(algRank.size() - 1).rank));
            int xdiv = (xfin - xin) / ticks; //number of divisions in the axis
            int xinr = rank1*xdiv + xin;
            //axis
              g.draw(new Line2D.Double(xinr, yg, xrigth + xScale, yg));
              gb.draw(new Line2D.Double(xinr, yg, xrigth + xScale, yg));

            int j = rank1;
            //put the ticks
            g.setStroke(currentStroke);
            gb.setStroke(currentStroke);
            for (int i = /*xin*/xinr; i <= xfin; i += xdiv) {
                g.draw(new Line2D.Double(i, yg - 4, i, yg));
                g.drawString("" + j, i, yg - 6);
                gb.draw(new Line2D.Double(i, yg - 4, i, yg));
                gb.drawString("" + j, i, yg - 6);
                j++;
            }
            int ydiv = (height / 2) / 4 - (height / 10);
            
            int ya = ydiv + yScale;
            FontMetrics fm = g.getFontMetrics();
            g.setStroke(stroke.getStroke());
            gb.setStroke(stroke.getStroke());
            boolean visited[][] = new boolean[algRank.size()][algRank.size()];
            for (int i = 0; i < algRank.size(); i++) {
                for (int k = 0; k < algRank.size(); k++) {
                    visited[i][k] = false;
                }
            }
           
            //Draw algorithms Lines
            for (int i = 0; i < algRank.size(); i++) {
                g.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, (double) (xdiv * algRank.get(i).rank + xin), yg));
                gb.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, (double) (xdiv * algRank.get(i).rank + xin), yg));
                int k;
                for (k = i + 1; k <= algRank.size() - 1; k++) {
                    int index = PValuePerTwoAlgorithm.getIndex(PValues,
                            algRank.get(i).algName, algRank.get(k).algName);
                    boolean v = PValues.get(index).isSignicativeBetterThan(pvalue);
                    if (v == true) {
                        visited[i][k] = true;
                    } else {
                        break;
                    }
                }
                g.setStroke(difStroke.getStroke());
                gb.setStroke(difStroke.getStroke());
                if (i == 0 && k - 1 != 0) {
                    g.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), (yg + ya + yg) / 2, (double) (xdiv * algRank.get(k - 1).rank + xin), (yg + ya + yg) / 2));
                    gb.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), (yg + ya + yg) / 2, (double) (xdiv * algRank.get(k - 1).rank + xin), (yg + ya + yg) / 2));
                } else if (i != 0) {
                    //If the last visited by the current algorithm is not in the visited of the previous one then draw a line
                    if (visited[i - 1][k - 1] == false && (i != k - 1)) {
                        g.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), (yg + ya + yg) / 2, (double) (xdiv * algRank.get(k - 1).rank + xin), (yg + ya + yg) / 2));
                        gb.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), (yg + ya + yg) / 2, (double) (xdiv * algRank.get(k - 1).rank + xin), (yg + ya + yg) / 2));
                    }
                }
                g.setStroke(stroke.getStroke());
                gb.setStroke(stroke.getStroke());
                if (i < algRank.size() / 2) {
                    int lenght = fm.stringWidth(algRank.get(i).algName);
                    g.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, xinr-3, yg + ya));
                    g.drawString(algRank.get(i).algName, xinr-3 - lenght - 10, yg + ya);
                    gb.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, xinr-3, yg + ya));
                    gb.drawString(algRank.get(i).algName, xinr-3 - lenght - 10, yg + ya);
                } else {                    
                    g.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, xfin+3, yg + ya));
                    g.drawString(algRank.get(i).algName, xfin+3 + 10, yg + ya);
                    gb.draw(new Line2D.Double((double) (xdiv * algRank.get(i).rank + xin), yg + ya, xfin+3, yg + ya));
                    gb.drawString(algRank.get(i).algName, xfin+3 + 10, yg + ya);
                }
                ya += 20;
                
            }
            
        }
        
    }

    /**
     * Allows you to increase or decrease the scale of the graph.
     */
    public class SliderPanel extends JPanel {

        /**
         * Constructor.
         */
        public SliderPanel() {
            setLayout(new GridLayout(1, 2));
            
            xSlider = new JSlider(JSlider.VERTICAL, -400, 400, 50);
            xSlider.addChangeListener((ChangeEvent e) -> {
                xScale = (int) xSlider.getValue();
                graphPanel.repaint();
            });
            
            add(xSlider);
            
            ySlider = new JSlider(JSlider.VERTICAL, 1, 400, 20);
            ySlider.addChangeListener((ChangeEvent e) -> {
                yScale = (int) ySlider.getValue();
                graphPanel.repaint();
            });
            add(ySlider);
            
            xSlider.setMinorTickSpacing(20);
            xSlider.setPaintTicks(true);
            xSlider.setPaintLabels(true);
            ySlider.setMinorTickSpacing(20);
            ySlider.setPaintTicks(true);
            ySlider.setPaintLabels(true);
            
        }
        
    }
    
} // class
