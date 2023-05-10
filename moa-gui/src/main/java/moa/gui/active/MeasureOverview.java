/*
 *    MeasureOverview.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
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
package moa.gui.active;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import moa.evaluation.MeasureCollection;

/**
 * MeasureOverview provides a graphical overview of the current and mean
 * measure values during the runtime of a task.
 * 
 * This class is partially based on moa.gui.clustertab.ClusteringVisualEvalPanel.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see moa.gui.clustertab.ClusteringVisualEvalPanel
 */
public class MeasureOverview extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private MeasureCollection[] measures;
    private int measureCollectionSelected;
    
    private String variedParamName;
    private double[] variedParamValues;
    
    private JPanel contentPanel;
    private JScrollPane scrollPane;
    
    private ButtonGroup radioGroup;
    private JRadioButton[] radioButtons;
    private JLabel[] currentValues;
    private JLabel[] meanValues;
    
    private JLabel labelMeasure;
    private JLabel labelCurrent;
    private JLabel labelMean;
    
    private JComboBox<String> paramBox;

    /**
     * Creates a new MeasureOverview.
     * @param measures MeasureCollection array
     * @param variedParamName name of the varied parameter
     * @param variedParamValues values of the varied parameter
     */
    public MeasureOverview(MeasureCollection[] measures, String variedParamName, double[] variedParamValues) {
        // set basic parameters
        setBorder(BorderFactory.createTitledBorder("Values"));
        setPreferredSize(new Dimension(250, 115));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        // init scroll pane
        scrollPane = new JScrollPane();
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(270, 180));
        
        // init content of the scroll pane
        contentPanel = new JPanel();
        contentPanel.setPreferredSize(new Dimension(100, 105));
        contentPanel.setLayout(new GridBagLayout());
        scrollPane.setViewportView(contentPanel);
        
        // init param combo box
        paramBox = new JComboBox<String>();
        // prevent height rescaling
        paramBox.setMaximumSize(new Dimension(paramBox.getMaximumSize().width, paramBox.getPreferredSize().height));
        paramBox.setEnabled(false);
        paramBox.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>) e.getSource();
                measureCollectionSelected = cb.getSelectedIndex();
                // if the cb got disabled, set idx to 0
                if (measureCollectionSelected == -1) {
                    measureCollectionSelected = 0;
                }
                update();
            }
        });

        add(scrollPane);
        add(paramBox);
        
        // add labels
        labelMeasure = new JLabel("Measure");
        labelCurrent = new JLabel("Current");
        labelMean    = new JLabel("Mean");

        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0;
        gb.gridy = 0;
        contentPanel.add(labelMeasure, gb);

        gb.gridx = 1;
        contentPanel.add(labelCurrent, gb);
        
        gb.gridx = 2;
        contentPanel.add(labelMean, gb);
        
        this.measures = measures;
        this.measureCollectionSelected = 0;
        this.variedParamName = variedParamName;
        this.variedParamValues = variedParamValues;
        
        if (measures == null || measures.length == 0) {
            // no measures to display
            return;
        }
        int numMeasures = measures[0].getNumMeasures(); 
        
        // init radio buttons
        this.radioGroup = new ButtonGroup();
        this.radioButtons = new JRadioButton[numMeasures];
        gb = new GridBagConstraints();
        gb.gridx = 0;
        gb.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < numMeasures; i++) {
            JRadioButton rb = new JRadioButton(measures[0].getName(i));
            rb.setBorder(BorderFactory.createLineBorder(Color.RED));
            rb.setActionCommand(Integer.toString(i));
            this.radioButtons[i] = rb;

            gb.gridy = 1 + i;
            this.contentPanel.add(rb, gb);
            this.radioGroup.add(rb);
        }
        this.radioButtons[0].setSelected(true);
        
        // init values
        this.currentValues = new JLabel[numMeasures];
        this.meanValues = new JLabel[numMeasures];
        
        gb = new GridBagConstraints();
        gb.weightx = 1.0;
        
        for (int i = 0; i < numMeasures; i++) {
            gb.gridy = 1 + i;
            // current values
            this.currentValues[i] = new JLabel("-");
            this.currentValues[i].setHorizontalAlignment(SwingConstants.CENTER);
            gb.gridx = 1;
            this.contentPanel.add(this.currentValues[i], gb);
            
            // mean values
            this.meanValues[i] = new JLabel("-");
            this.meanValues[i].setHorizontalAlignment(SwingConstants.CENTER);
            gb.gridx = 2;
            this.contentPanel.add(this.meanValues[i], gb);
        }

        // I have no idea where these numbers come from. its taken from 
        // ClusteringVisualEvalPanel
        contentPanel.setPreferredSize(new Dimension(250, this.currentValues.length*(14+8)+20));
        
        // update parameter box
        updateParamBox();

    }
    
    /**
     * Sets the ActionListener for the radio buttons.
     * @param listener ActionListener assigned to the radio buttons
     */
    public void setActionListener(ActionListener listener) {
        for (int i = 0; i < this.radioButtons.length; i++) {
            this.radioButtons[i].addActionListener(listener);
        }
    }
    
    /**
     * Updates the measure overview by assigning new measure collections and
     * varied parameter properties. If no measures are currently to display,
     * reset the display to hyphens. Otherwise display the last measured and
     * mean values.
     * Updates the parameter combo box if needed.
     * @param measures new MeasureCollection[]
     * @param variedParamName new varied parameter name
     * @param variedParamValues new varied parameter values
     */
    public void update(MeasureCollection[] measures, String variedParamName, double[] variedParamValues) {
        this.measures = measures;
        this.variedParamName = variedParamName;
        this.variedParamValues = variedParamValues;
        update();
        updateParamBox();
    }
    
    /**
     * Updates the measure overview. If no measures are currently to display,
     * reset the display to hyphens. Otherwise display the last measured and
     * mean values.
     */
    public void update() {
        if (this.measures == null || this.measures.length == 0) {
            // no measures to show -> empty entries
            for (int i = 0; i < this.currentValues.length; i++) {
                this.currentValues[i].setText("-");
                this.meanValues[i].setText("-");
            }
            return;
        }
        
        DecimalFormat d = new DecimalFormat("0.00");
        
        MeasureCollection mc;
        if (this.measures.length > this.measureCollectionSelected) {
        	mc = this.measures[this.measureCollectionSelected];
        }
        else {
        	mc = this.measures[0];
        }
        
        for (int i = 0; i < this.currentValues.length; i++) {
            // set current value
            if(Double.isNaN(mc.getLastValue(i))) {
                this.currentValues[i].setText("-");
            } else {
                this.currentValues[i].setText(d.format(mc.getLastValue(i)));
            }
            
            // set mean value
            if(Double.isNaN(mc.getMean(i))) {
                this.meanValues[i].setText("-");
            } else {
                this.meanValues[i].setText(d.format(mc.getMean(i)));
            }
        } 
    }
    
    /**
     * Updates the parameter combo box. If there is no varied parameter, empty
     * and disable the box. Otherwise display the available parameters.
     */
    private void updateParamBox() {
        if (this.variedParamValues == null || this.variedParamValues.length == 0) {
            // no varied parameter -> set to empty box
            this.paramBox.removeAllItems();
            this.paramBox.setEnabled(false);
        } else if (this.paramBox.getItemCount() != this.variedParamValues.length) {
            // varied parameter changed -> set the paramBox new
            this.paramBox.removeAllItems();
            for (int i = 0; i < variedParamValues.length; i++) {
                this.paramBox.addItem(String.format("%s: %s", this.variedParamName, this.variedParamValues[i]));
            }
            this.paramBox.setEnabled(true);
        }
    }

}
