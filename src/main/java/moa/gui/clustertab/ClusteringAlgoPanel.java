package moa.gui.clustertab;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moa.clusterers.AbstractClusterer;
import moa.clusterers.Clusterer;
import moa.gui.GUIUtils;
import moa.gui.OptionEditComponent;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.Option;
import moa.options.Options;
import moa.streams.InstanceStream;
import moa.streams.clustering.ClusteringStream;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ClusteringAlgoPanel.java
 *
 * Created on 20.03.2010, 10:20:18
 */

/**
 *
 * @author admin
 */
public class ClusteringAlgoPanel extends javax.swing.JPanel {

    protected List<OptionEditComponent> editComponents = new LinkedList<OptionEditComponent>();

    private ClassOption streamOption = new ClassOption("Stream", 's',
                "Stream to learn from.", ClusteringStream.class,
                "RandomRBFGeneratorEvents");

   private ClassOption algorithmOption = new ClassOption("Algorithm", 'a',
               "Algorithm to use.", Clusterer.class,
                "ClusterGenerator");

   private FlagOption sameStreamOption = new FlagOption("duplicateStream", 'S', "Same as above");
   private FlagOption sameAlgoOption = new FlagOption("duplicateAlgorithm", 'A', "Same as above");

   private boolean secondAlgorithm = false;

    /** Creates new form ClusteringAlgoPanel */
    public ClusteringAlgoPanel() {
        //initComponents();

    }

    public void renderAlgoPanel(boolean secondAlgorithm){
        this.secondAlgorithm = secondAlgorithm;
        
        setLayout(new BorderLayout());

        ArrayList<Option> options = new ArrayList<Option>();
        options.add(streamOption);
        if(secondAlgorithm)
            options.add(sameStreamOption);
        options.add(algorithmOption);
        if(secondAlgorithm)
            options.add(sameAlgoOption);
        
        JPanel optionsPanel = new JPanel();
        GridBagLayout gbLayout = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        optionsPanel.setLayout(gbLayout);
        for (int i = 0; i < options.size(); i++) {
            JLabel label = new JLabel(options.get(i).getName());
            label.setToolTipText(options.get(i).getPurpose());
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            optionsPanel.add(label, gbc);
            JComponent editor = options.get(i).getEditComponent();
            label.setLabelFor(editor);
            editComponents.add((OptionEditComponent) editor);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            optionsPanel.add(editor, gbc);
       }

        if(secondAlgorithm)
            setPanelTitle("Cluster Comparison-Algorithm Setup");
        add(optionsPanel);
    }

    public AbstractClusterer getClusterer(){
        AbstractClusterer c = null;
        applyChanges();
        try {
            c = (AbstractClusterer) ClassOption.cliStringToObject(algorithmOption.getValueAsCLIString(), Clusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(ClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }

    public boolean duplicateClusterer(){
        applyChanges();
        return sameAlgoOption.isSet();
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

    public boolean duplicateStream(){
        applyChanges();
        return sameStreamOption.isSet();
    }

    public String getStreamValueAsCLIString(){
        applyChanges();
        return streamOption.getValueAsCLIString();
    }

    public String getAlgorithmValueAsCLIString(){
        applyChanges();
        return algorithmOption.getValueAsCLIString();
    }

    public void setStreamValueAsCLIString(String s){
        streamOption.setValueViaCLIString(s);
        int index = 0;
        editComponents.get(index).setEditState(streamOption.getValueAsCLIString());
    }

    public void setAlgorithmValueAsCLIString(String s){
        algorithmOption.setValueViaCLIString(s);
        int index = secondAlgorithm?2:1;
        editComponents.get(index).setEditState(algorithmOption.getValueAsCLIString());
    }

    public void setDuplicateStream(boolean state){
        if(secondAlgorithm){
            sameStreamOption.setValue(state);
            editComponents.get(1).setEditState(sameStreamOption.getValueAsCLIString());
        }
    }

    public void setDuplicateClusterer(boolean state){
        if(secondAlgorithm){
            sameAlgoOption.setValue(state);
            editComponents.get(3).setEditState(sameAlgoOption.getValueAsCLIString());
        }
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

        optionsFrame = new javax.swing.JFrame();

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cluster Algorithm Setup", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N
        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFrame optionsFrame;
    // End of variables declaration//GEN-END:variables

}
