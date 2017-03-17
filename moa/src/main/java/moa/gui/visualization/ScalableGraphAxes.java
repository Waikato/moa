/*
 *    ScalableGraphAxes.java
 *    Original Work: Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Jansen (moa@cs.rwth-aachen.de)
 *    Modified Work: Copyright (C) 2017 Otto-von-Guericke-University, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de) 
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

/**
 * ScalableGraphAxes is a clone of GraphAxes, but enables support for properly
 * scaling the y axis.
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphAxes
 */
public class ScalableGraphAxes extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;
	private static final int X_OFFSET_LEFT = 35;
    private static final int X_OFFSET_RIGHT = 5;
    private static final int Y_OFFSET_BOTTOM = 20;
    private static final int Y_OFFSET_TOP = 20;

    private int height;
    private int width;

    private double x_resolution; //how many pixels per 1px
    private double y_resolution;
    private int processFrequency;

    private double max_value;

    /** Creates new form GraphAxes */
    public ScalableGraphAxes() {
    	this.max_value = 1;
    	
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
    }

    public void setXResolution(double resolution){
        x_resolution = resolution;
    }
    
    public void setYResolution(double resolution) {
    	y_resolution = resolution;
    }

    public void setProcessFrequency(int frequency){
        processFrequency = frequency;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //stream not started yet
        if(processFrequency == 0) return;

        height = getHeight()-Y_OFFSET_BOTTOM-Y_OFFSET_TOP;
        width = getWidth()-X_OFFSET_LEFT-X_OFFSET_RIGHT;

        //System.out.println(width);

        g.setColor(new Color(236,233,216));
        g.fillRect(0, 0, getWidth(), getHeight());

        //draw background
        g.setColor(Color.WHITE);
        g.fillRect(X_OFFSET_LEFT, Y_OFFSET_TOP, width, height);

        g.setFont(new Font("Tahoma", 0, 11));
        

        xAxis(g);
        yAxis(g);
    }

    private void xAxis(Graphics g){
        g.setColor(Color.BLACK);
        
        //x-achsis
        g.drawLine(X_OFFSET_LEFT, calcY(0), width+X_OFFSET_LEFT, calcY(0));

        //x achsis labels
        int w = 100;
        for (int i = 0; w*i < width-X_OFFSET_RIGHT; i++) {
            g.drawLine(w*i+X_OFFSET_LEFT, height+Y_OFFSET_TOP, w*i+X_OFFSET_LEFT, height+Y_OFFSET_TOP+5);

            String label = Integer.toString((int)(w*i*processFrequency*x_resolution));

            int str_length = g.getFontMetrics().stringWidth(label);
            g.drawString(label,w*i+X_OFFSET_LEFT-str_length/2,height+Y_OFFSET_TOP+18);
        }
    }


    private void yAxis(Graphics g){
        //y-achsis
        g.setColor(Color.BLACK);
        g.drawLine(X_OFFSET_LEFT, calcY(0), X_OFFSET_LEFT, Y_OFFSET_TOP);

        //center horizontal line
        g.setColor(new Color(220,220,220));
        g.drawLine(X_OFFSET_LEFT, height/2+Y_OFFSET_TOP, getWidth(), height/2+Y_OFFSET_TOP);
        
        g.setColor(Color.BLACK);

        //y-achsis markers + labels
        DecimalFormat d = new DecimalFormat("0.00");
        int digits_y = (int)(Math.log10(max_value))-1;
        double upper = Math.ceil(max_value/Math.pow(10,digits_y));
        if(digits_y < 0) upper*=Math.pow(10,digits_y);

        if(Double.isNaN(upper)) {
        	upper = 1.0;
        }
        double numLabels = Math.min(Math.pow(2,  y_resolution), 32); // technically, this
        // is numLabels-1, but as we're iterating 0<=i<=numLabels, we need the
        // extra label. Also don't draw more that 32 labels
        
        for (int i = 0; i <= numLabels; i++) {
        	double fraction = i/numLabels;
        	double value = fraction*upper;
        	g.drawString(d.format(value), 1, (int) ((1-fraction)*height) + Y_OFFSET_TOP+5);
        	g.drawLine(X_OFFSET_LEFT - 5, (int) ((1-fraction)*height) + Y_OFFSET_TOP, 
        			   X_OFFSET_LEFT, (int) ((1-fraction)*height) + Y_OFFSET_TOP);
        }

    }

    public void setYMaxValue(double max){
        max_value = max;
    }

    private int calcY(double value){
        return (int)(height-(value/max_value)*height)+Y_OFFSET_TOP;
    }

}
