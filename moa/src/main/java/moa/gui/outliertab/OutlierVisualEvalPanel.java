/*
 *    OutlierVisualEvalPanel.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *
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
package moa.gui.outliertab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import moa.evaluation.MeasureCollection;

public class OutlierVisualEvalPanel extends javax.swing.JPanel{
    private ArrayList<JLabel> names;
    private ArrayList<JLabel> values;
    private ArrayList<JRadioButton> radiobuttons;
    private MeasureCollection[] measures0;
    private ButtonGroup radioGroup;

    private JLabel labelDummy;
    private JLabel labelMeasure;
    private JLabel labelCurrent0;
    private JLabel labelMean0;

    /** Creates new form OutlierEvalPanel */
    public OutlierVisualEvalPanel() {
        initComponents();
        radioGroup = new ButtonGroup();
    }


    public void setMeasures(MeasureCollection[] measures0, ActionListener listener){
        this.measures0 = measures0;

        names = new ArrayList<JLabel>();
        values = new ArrayList<JLabel>();
        radiobuttons = new ArrayList<JRadioButton>();

        for (int i = 0; i < measures0.length; i++) {
            for (int j = 0; j < measures0[i].getNumMeasures(); j++) {
                if(measures0[i].isEnabled(j)){
                    names.add(new JLabel(measures0[i].getName(j)));

                }
            }
        }
        
        setLayout(new java.awt.GridBagLayout());
        GridBagConstraints gb;

        //Radiobuttons
        radioGroup = new ButtonGroup();
        gb = new GridBagConstraints();
        gb.gridx=0;
        for (int i = 0; i < names.size(); i++) {
            JRadioButton rb = new JRadioButton();
            rb.setActionCommand(Integer.toString(i));
            rb.addActionListener(listener);
            radiobuttons.add(rb);

            gb.gridy = i+1;
            contentPanel.add(rb, gb);
            radioGroup.add(rb);
        }
        radiobuttons.get(0).setSelected(true);

        //name labels
        gb = new GridBagConstraints();
        gb.gridx=1;
        for (int i = 0; i < names.size(); i++) {
            names.get(i).setPreferredSize(new Dimension(40, 14));
            gb.anchor = GridBagConstraints.WEST;
            gb.insets = new java.awt.Insets(4, 7, 4, 7);
            gb.gridy = i+1;
            contentPanel.add(names.get(i), gb);
        }

        

        int counter = 0;
        for (int i = 0; i < measures0.length; i++) {
            for (int j = 0; j < measures0[i].getNumMeasures(); j++) {
                if(!measures0[i].isEnabled(j)) continue;
                for (int k = 0; k < 4; k++) {
                    String tooltip ="";
                    Color color = Color.black;
                    switch(k){
                        case 0:
                            tooltip = "current value";
                            color = Color.red;
                        break;
                        case 1:
                            tooltip = "current value";
                            color = Color.blue;
                        break;
                        case 2:
                            tooltip = "mean";
                            color = Color.black;
                        break;
                        case 3:
                            tooltip = "mean";
                            color = Color.black;
                        break;
                    }
                    JLabel l = new JLabel("-");
                    l.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                    l.setPreferredSize(new java.awt.Dimension(50, 14));
                    l.setToolTipText(measures0[i].getName(j)+" "+tooltip);
                    l.setForeground(color);
                    values.add(l);
                    gb = new GridBagConstraints();
                    gb.gridy = 1 + counter;
                    gb.gridx = 2 + k;
                    gb.weightx = 1.0;
                    contentPanel.add(l, gb);
                }
                counter++;
            }
        }

        //dummy label to align the labels at the top
        gb = new GridBagConstraints();
        gb.gridx = 0;
        gb.gridy = names.size()+2;
        gb.gridwidth = 6;
        gb.weighty = 1.0;
        JLabel fill = new JLabel();
        contentPanel.add(fill, gb);

        addLabels();

        contentPanel.setPreferredSize(new Dimension(250, names.size()*(14+8)+20));
    }

    private void addLabels(){
        labelMeasure = new javax.swing.JLabel("Measure");
        labelCurrent0 = new JLabel("Current");
        labelMean0 = new javax.swing.JLabel("Mean");
        labelDummy = new javax.swing.JLabel();
        GridBagConstraints gb = new GridBagConstraints();
        gb.gridy = 0;

        gb.gridx = 0;
        contentPanel.add(labelDummy, gb);

        gb.gridx = 1;
        contentPanel.add(labelMeasure, gb);

        gb = new GridBagConstraints();
        gb.gridy = 0;
        gb.gridx = 2;
        gb.gridwidth = 2;
        contentPanel.add(labelCurrent0, gb);


        gb = new GridBagConstraints();
        gb.gridy = 0;
        gb.gridx=4;
        gb.gridwidth = 2;
        contentPanel.add(labelMean0, gb);

    }

    public void update(){
        DecimalFormat d = new DecimalFormat("0.00");

        if(measures0!=null){
            int counter = 0;
            for (MeasureCollection m : measures0) {
                for (int i = 0; i < m.getNumMeasures(); i++) {
                    if(!m.isEnabled(i)) continue;
                    if(Double.isNaN(m.getLastValue(i)))
                        values.get(counter*4).setText("-");
                    else
                        values.get(counter*4).setText(d.format(m.getLastValue(i)));

                    if(Double.isNaN(m.getMean(i)))
                        values.get(counter*4+2).setText("-");
                    else
                        values.get(counter*4+2).setText(d.format(m.getMean(i)));
                    counter++;
                }
            }
        }
    }

    @Override
    //this is soooooo bad, but freaking gridbag somehow doesnt kick in...???
    protected void paintComponent(Graphics g) {
        scrollPane.setPreferredSize(new Dimension(getWidth()-20, getHeight()-30));
        super.paintComponent(g);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrollPane = new javax.swing.JScrollPane();
        contentPanel = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Values"));
        setPreferredSize(new java.awt.Dimension(250, 115));
        setLayout(new java.awt.GridBagLayout());

        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new java.awt.Dimension(270, 180));

        contentPanel.setPreferredSize(new java.awt.Dimension(100, 105));
        contentPanel.setLayout(new java.awt.GridBagLayout());
        scrollPane.setViewportView(contentPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollPane, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentPanel;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

}
