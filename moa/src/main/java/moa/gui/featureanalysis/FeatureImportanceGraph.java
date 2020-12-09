/*
 *    FeatureImportanceGraph.java
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

import org.math.plot.Plot2DPanel;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * This is a sub panel in FeatureImportance tab. It is used to show scores of feature importance of a data stream as a line graph.
 */
public class FeatureImportanceGraph extends JPanel {

    /** Store feature importance scores. */
    protected double[][] m_featureImportance;

    /**The selected attribute indices.*/
    protected int[] m_selectedAttributeIndices;

    /** THe drawing tool provided by jmathplot.jar */
    protected Plot2DPanel plot = new Plot2DPanel();

    /** Attribute names of dataset except the class attribute. */
    protected String[] m_attributeNames;

    public void setAttributeNames(String[] attribNames) {
        this.m_attributeNames = attribNames;
    }

    public void setFeatureImportance(double[][] featureImportance) {
        this.m_featureImportance = featureImportance;

        /** Crucial code to ensure the line graphs are updated when user change their choose. */
        this.remove(plot);
        plot = new Plot2DPanel();

        /**
         * show the line graph of the first feature importance as the default line graph
         */
        this.repaint();
    }

    public void setSelectedAttributeIndices(int[] selectedAttributeIndices) {
        this.m_selectedAttributeIndices = selectedAttributeIndices;

        /**
         * The following two line codes are crucial for updating line graphs
         * in the following method "public void paintComponent(Graphics g)"
         */
        this.remove(plot);
        plot = new Plot2DPanel();

        this.repaint();
    }

    public FeatureImportanceGraph(){
        plot.setSize(super.getWidth(), super.getHeight());
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setLayout(new BorderLayout());

        plot.addLegend("SOUTH");
        plot.setAxisLabel(0, "Win index");//X axis
        plot.setAxisLabel(1, "Feature importance");//Y axis
        plot.setFont(new Font("Courier", Font.BOLD, 12));
        plot.setVisible(true);

        if(m_featureImportance!=null){

            int selectedAttributeIndex;
            int rows=m_featureImportance.length;
            double[] featureImportance=new double[rows];

            if(m_selectedAttributeIndices==null ||
                    (Arrays.toString(m_selectedAttributeIndices).equalsIgnoreCase("[]"))
            ){ //
                /** User does not choose any attribute yet,show the first attribute importance as default. */
                selectedAttributeIndex=0;

                for(int i=0;i<rows; i++){
                    featureImportance[i]=m_featureImportance[i][selectedAttributeIndex];
                }
                /** Set the same colors for all lines*/
                //plot.addLinePlot(m_attributeNames[selectedAttributeIndex],Color.BLUE,featureImportance);

                /** Randomly set colors for different lines.*/
                plot.addLinePlot(m_attributeNames[selectedAttributeIndex],featureImportance);
                this.add(plot, BorderLayout.CENTER);
                super.revalidate();
            }else{
                int numSelectedAttributes=m_selectedAttributeIndices.length;
                for(int j=0; j<numSelectedAttributes; j++ ){
                    selectedAttributeIndex=m_selectedAttributeIndices[j];
                    for(int i=0;i<rows; i++){
                        featureImportance[i]=m_featureImportance[i][selectedAttributeIndex];
                    }

                    /** Set the same colors for all lines*/
                    //plot.addLinePlot(m_attributeNames[selectedAttributeIndex],Color.BLUE,featureImportance);

                    /** Randomly set colors for different lines.*/
                    plot.addLinePlot(m_attributeNames[selectedAttributeIndex],featureImportance);
                    this.add(plot, BorderLayout.CENTER);
                    super.revalidate();
                }
            }
        }
    }
}
