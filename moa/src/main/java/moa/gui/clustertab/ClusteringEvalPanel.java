/*
 *    ClusteringEvalPanel.java
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

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import moa.core.AutoClassDiscovery;
import moa.core.AutoExpandVector;
import moa.evaluation.ClassificationMeasureCollection;
import moa.evaluation.MeasureCollection;

public class ClusteringEvalPanel extends javax.swing.JPanel {

    Class<?>[] measure_classes = null;

    ArrayList<JLabel> labels = null;

    ArrayList<JCheckBox> checkboxes = null;

    /** Creates new form ClusteringEvalPanel */
    public ClusteringEvalPanel() {
        initComponents();
        measure_classes = findMeasureClasses();
        labels = new ArrayList<JLabel>();
        checkboxes = new ArrayList<JCheckBox>();
        addComponents();
    }

    private void addComponents() {
        GridBagConstraints gb = new GridBagConstraints();
        gb.insets = new java.awt.Insets(4, 7, 4, 7);
        int counter = 0;
        for (int i = 0; i < measure_classes.length; i++) {
            try {
                MeasureCollection m = (MeasureCollection) measure_classes[i].newInstance();
                for (int j = 0; j < m.getNumMeasures(); j++) {
                    String t = m.getName(j);
                    JLabel l = new JLabel(m.getName(j));
                    l.setPreferredSize(new java.awt.Dimension(100, 14));
                    //labels[i].setToolTipText("");
                    gb.gridx = 0;
                    gb.gridy = counter;
                    labels.add(l);
                    contentPanel.add(l, gb);

                    JCheckBox cb = new JCheckBox();

                    if (m.isEnabled(j)) {
                        cb.setSelected(true);
                    } else {
                        cb.setSelected(false);
                    }

                    gb.gridx = 1;
                    checkboxes.add(cb);
                    contentPanel.add(cb, gb);
                    counter++;
                }
            } catch (Exception ex) {
                Logger.getLogger("Couldn't create Instance for " + measure_classes[i].getName());
                ex.printStackTrace();
            }

        }
        JLabel dummy = new JLabel();
        gb.gridx = 0;
        gb.gridy++;
        gb.gridwidth = 3;
        gb.weightx = 1;
        gb.weighty = 1;
        add(dummy, gb);

    }

    private Class<?>[] findMeasureClasses() {
        AutoExpandVector<Class<?>> finalClasses = new AutoExpandVector<Class<?>>();
        Class<?>[] classesFound = AutoClassDiscovery.findClassesOfType("moa.evaluation",
                MeasureCollection.class);
        for (Class<?> foundClass : classesFound) {
            //Not add ClassificationMeasureCollection
            boolean noClassificationMeasures = true;
            for (Class cl: foundClass.getInterfaces()) {
                if (cl.toString().contains("moa.evaluation.ClassificationMeasureCollection")){
                    noClassificationMeasures = false;
                }
            }
            if (noClassificationMeasures ) { 
                finalClasses.add(foundClass);
            }
        }
        return finalClasses.toArray(new Class<?>[finalClasses.size()]);
    }

    public MeasureCollection[] getSelectedMeasures() {
        ArrayList<MeasureCollection> measuresSelect = new ArrayList<MeasureCollection>();

        int counter = 0;
        for (int i = 0; i < measure_classes.length; i++) {
            try {
                MeasureCollection m = (MeasureCollection) measure_classes[i].newInstance();
                boolean addMeasure = false;
                for (int j = 0; j < m.getNumMeasures(); j++) {
                    boolean selected = checkboxes.get(counter).isSelected();
                    m.setEnabled(j, selected);
                    if (selected) {
                        addMeasure = true;
                    }
                    counter++;
                }
                if (addMeasure) {
                    measuresSelect.add(m);
                }



            } catch (Exception ex) {
                Logger.getLogger("Couldn't create Instance for " + measure_classes[i].getName());
                ex.printStackTrace();
            }
        }


        MeasureCollection[] measures = new MeasureCollection[measuresSelect.size()];
        for (int i = 0; i < measures.length; i++) {
            measures[i] = measuresSelect.get(i);
        }
        return measures;
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

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Evaluation Measures", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);       
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight() > 650) {
            scrollPane.setPreferredSize(new java.awt.Dimension(100, 225));
        } else {
            scrollPane.setPreferredSize(new java.awt.Dimension(100, 115));
        }        
 
        contentPanel.setLayout(new java.awt.GridBagLayout());
        scrollPane.setViewportView(contentPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
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
