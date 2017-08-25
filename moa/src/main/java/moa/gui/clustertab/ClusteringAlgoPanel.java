/*
 *    ClusteringAlgoPanel.java
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

package moa.gui.clustertab;

import java.awt.BorderLayout;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.gui.GUIUtils;
import com.github.javacliparser.gui.OptionEditComponent;
import com.github.javacliparser.gui.OptionsConfigurationPanel;
import moa.options.ClassOption;
import com.github.javacliparser.Option;
import moa.streams.clustering.ClusteringStream;
import moa.streams.generators.RandomRBFGenerator;

public class ClusteringAlgoPanel extends javax.swing.JPanel implements ActionListener{

    protected List<OptionEditComponent> editComponents = new LinkedList<OptionEditComponent>();

    private ClassOption streamOption = new ClassOption("Stream", 's',
                "", ClusteringStream.class,
                "RandomRBFGeneratorEvents");

   private ClassOption algorithmOption0 = new ClassOption("Algorithm0", 'a',
               "Algorithm to use.", Clusterer.class, "ClusterGenerator");

   private ClassOption algorithmOption1 = new ClassOption("Algorithm1", 'c',
               "Comparison algorithm", Clusterer.class, "clustream.WithKmeans", "None");


    public ClusteringAlgoPanel() {
        initComponents();
    }

    public void renderAlgoPanel(){

        setLayout(new BorderLayout());

        ArrayList<Option> options = new ArrayList<Option>();
        options.add(streamOption);
        options.add(algorithmOption0);
        options.add(algorithmOption1);

        JPanel optionsPanel = new JPanel();
        GridBagLayout gbLayout = new GridBagLayout();
        optionsPanel.setLayout(gbLayout);

        //Create generic label constraints
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.fill = GridBagConstraints.NONE;
        gbcLabel.anchor = GridBagConstraints.EAST;
        gbcLabel.weightx = 0;
        gbcLabel.insets = new Insets(5, 5, 5, 5);

        //Create generic editor constraints
        GridBagConstraints gbcOption = new GridBagConstraints();
        gbcOption.gridx = 1;
        gbcOption.fill = GridBagConstraints.HORIZONTAL;
        gbcOption.anchor = GridBagConstraints.CENTER;
        gbcOption.weightx = 1;
        gbcOption.insets = new Insets(5, 5, 5, 0);

        //Stream Option
        JLabel labelStream = new JLabel("Stream");
        labelStream.setToolTipText("Stream to use.");
        optionsPanel.add(labelStream, gbcLabel);
        JComponent editorStream = getEditComponent(streamOption);
        labelStream.setLabelFor(editorStream);
        editComponents.add((OptionEditComponent) editorStream);
        optionsPanel.add(editorStream, gbcOption);

        //Algorithm0 Option
        JLabel labelAlgo0 = new JLabel("Algorithm1");
        labelAlgo0.setToolTipText("Algorithm to use.");
        optionsPanel.add(labelAlgo0, gbcLabel);
        JComponent editorAlgo0 = getEditComponent(algorithmOption0);
        labelAlgo0.setLabelFor(editorAlgo0);
        editComponents.add((OptionEditComponent) editorAlgo0);
        optionsPanel.add(editorAlgo0, gbcOption);

        //Algorithm1 Option
        JLabel labelAlgo1 = new JLabel("Algorithm2");
        labelAlgo1.setToolTipText("Comparison algorithm to use.");
        optionsPanel.add(labelAlgo1, gbcLabel);
        JComponent editorAlgo1 = getEditComponent(algorithmOption1);
        labelAlgo1.setLabelFor(editorAlgo1);
        editComponents.add((OptionEditComponent) editorAlgo1);
        optionsPanel.add(editorAlgo1, gbcOption);

        //use comparison Algorithm Option
        GridBagConstraints gbcClearButton = new GridBagConstraints();
        gbcClearButton.gridx = 2;
        gbcClearButton.gridy = 2;
        gbcClearButton.fill = GridBagConstraints.NONE;
        gbcClearButton.anchor = GridBagConstraints.CENTER;
        gbcClearButton.insets = new Insets(5, 0, 5, 5);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
        clearButton.setActionCommand("clear");
        optionsPanel.add(clearButton, gbcClearButton);


        add(optionsPanel);
    }

    public JComponent getEditComponent(Option option){
        return OptionsConfigurationPanel.getEditComponent(option);
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("clear")){
            algorithmOption1.setValueViaCLIString("None");
            editComponents.get(2).setEditState("None");
        }
    }

    public AbstractClusterer getClusterer0(){
        AbstractClusterer c = null;
        applyChanges();
        try {
            c = (AbstractClusterer) ClassOption.cliStringToObject(algorithmOption0.getValueAsCLIString(), Clusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(ClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }

    public AbstractClusterer getClusterer1(){
        AbstractClusterer c = null;
        applyChanges();
        if(!algorithmOption1.getValueAsCLIString().equals("None")){
            try {
                c = (AbstractClusterer) ClassOption.cliStringToObject(algorithmOption1.getValueAsCLIString(), Clusterer.class, null);
            } catch (Exception ex) {
                Logger.getLogger(ClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return c;
    }

    public ClusteringStream getStream(){
        ClusteringStream s = null;
        applyChanges();
        try {
            s = (ClusteringStream) ClassOption.cliStringToObject(streamOption.getValueAsCLIString(), ClusteringStream.class, null);
        } catch (Exception ex) {
            Logger.getLogger(ClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }

    public String getStreamValueAsCLIString(){
        applyChanges();
        return streamOption.getValueAsCLIString();
    }

    public String getAlgorithm0ValueAsCLIString(){
        applyChanges();
        return algorithmOption0.getValueAsCLIString();
    }

    public String getAlgorithm1ValueAsCLIString(){
        applyChanges();
        return algorithmOption1.getValueAsCLIString();
    }

    /* We need to fetch the right item from editComponents list, index needs to match GUI order */
    public void setStreamValueAsCLIString(String s){
        streamOption.setValueViaCLIString(s);
        editComponents.get(0).setEditState(streamOption.getValueAsCLIString());
    }

    public void setAlgorithm0ValueAsCLIString(String s){
        algorithmOption0.setValueViaCLIString(s);
        editComponents.get(1).setEditState(algorithmOption0.getValueAsCLIString());
    }

    public void setAlgorithm1ValueAsCLIString(String s){
        algorithmOption1.setValueViaCLIString(s);
        editComponents.get(2).setEditState(algorithmOption1.getValueAsCLIString());
    }

    public void applyChanges() {
            for (OptionEditComponent editor : this.editComponents) {
                    try {
                            editor.applyState();
                    } catch (Exception ex) {
                            GUIUtils.showExceptionDialog(this, "Problem with option "
                                            + editor.getEditedOption().getName(), ex);
                    }
            }
    }

    public void setPanelTitle(String title){
        setBorder(javax.swing.BorderFactory.createTitledBorder(null,title, javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11)));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cluster Algorithm Setup", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N
        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
