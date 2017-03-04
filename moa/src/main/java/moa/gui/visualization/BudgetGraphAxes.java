/*
 *    BudgetGraphAxes.java
 *    Copyright (C) Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
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
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

/*
 * TODO allow scaling on x-axis for many budgets
 */

public class BudgetGraphAxes extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private static final int X_OFFSET_LEFT = 35;
    private static final int X_OFFSET_RIGHT = 5;
    private static final int Y_OFFSET_BOTTOM = 20;
    private static final int Y_OFFSET_TOP = 20;
    
    private int height;
    private int width;

    private ArrayList<Double> budgets;
    
    private double max_value = 1;
	
	/**
	 * TODO javadoc
	 */
    public BudgetGraphAxes() {
    	
    	this.budgets = new ArrayList<Double>();
    	this.budgets.add(0.0);
    	this.budgets.add(0.5);
    	this.budgets.add(1.0);
    	
    	GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }

    /**
     * Adds a budget value to the list of registered budgets
     * @param budget
     */
    public void addBudget(double budget){
    	this.budgets.add(budget);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // no budget task performed yet
        if(this.budgets.isEmpty()) return;

        height = getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP;
        width = getWidth() - X_OFFSET_LEFT - X_OFFSET_LEFT;

        g.setColor(new Color(236,233,216));
        g.fillRect(0, 0, getWidth(), getHeight());

        // draw background
        g.setColor(Color.WHITE);
        g.fillRect(X_OFFSET_LEFT, Y_OFFSET_TOP, width, height);

        g.setFont(new Font("Tahoma", 0, 11));
        

        xAxis(g);
        yAxis(g);
    }
    
    /**
     * TODO javadoc
     * @param g
     */
    private void xAxis(Graphics g){
        g.setColor(Color.BLACK);
        
        //x-achsis
        g.drawLine(X_OFFSET_LEFT, calcY(0), width+X_OFFSET_LEFT, calcY(0));

        //x achsis labels
//        int numBudgets = this.budgets.size();
//        if (numBudgets <= 5) {
//        	// TODO make better- take max/min
//        	// up to 5 labels are drawn individually
//        	for (double budget : this.budgets) {
//        		int x = (int) Math.ceil((this.width - X_OFFSET_RIGHT - X_OFFSET_LEFT) * budget)+ X_OFFSET_LEFT;
//        		g.drawLine(x, height+Y_OFFSET_TOP, x, height+Y_OFFSET_TOP + 5);
//        		String label = String.valueOf(budget);
//        		int str_length = g.getFontMetrics().stringWidth(label);
//                g.drawString(label,x-str_length/2,height+Y_OFFSET_TOP+18);
//        	}
//        } else {
        	// show only fraction of budgets
        	for (double i = 0.0; i <= 1.0; i+= 0.2) {
        		int x = (int) Math.ceil((this.width - X_OFFSET_RIGHT - X_OFFSET_LEFT) * i) + X_OFFSET_LEFT;
        		g.drawLine(x, height+Y_OFFSET_TOP, x, height+Y_OFFSET_TOP + 5);
        		String label = String.format("%.2f", i);
        		int str_length = g.getFontMetrics().stringWidth(label);
                g.drawString(label,x-str_length/2,height+Y_OFFSET_TOP+18);
        	}
//        }
    }

	/**
	 * TODO javadoc
	 * @param g
	 */
    private void yAxis(Graphics g){
        //y-achsis
        g.setColor(Color.BLACK);
        g.drawLine(X_OFFSET_LEFT, calcY(0), X_OFFSET_LEFT, Y_OFFSET_TOP);

        //center horizontal line
        g.setColor(new Color(220,220,220));
        g.drawLine(X_OFFSET_LEFT, height/2+Y_OFFSET_TOP, getWidth(), height/2+Y_OFFSET_TOP);

        //3 y-achsis markers + labels
        g.setColor(Color.BLACK);
        DecimalFormat d = new DecimalFormat("0.00");
        int digits_y = (int)(Math.log10(max_value))-1;
        double upper = Math.ceil(max_value/Math.pow(10,digits_y));
        if(digits_y < 0) upper*=Math.pow(10,digits_y);

        if(Double.isNaN(upper)) upper =1.0;

        g.drawString(d.format(0.0), 3, height+Y_OFFSET_TOP+5);
        g.drawString(d.format(upper/2), 3, height/2+Y_OFFSET_TOP + 5);
        g.drawString(d.format(upper), 3, Y_OFFSET_TOP + 5);
        g.drawLine(X_OFFSET_LEFT-5, height+Y_OFFSET_TOP, X_OFFSET_LEFT,height+Y_OFFSET_TOP);
        g.drawLine(X_OFFSET_LEFT-5, height/2+Y_OFFSET_TOP, X_OFFSET_LEFT,height/2+Y_OFFSET_TOP);
        g.drawLine(X_OFFSET_LEFT-5, Y_OFFSET_TOP, X_OFFSET_LEFT,Y_OFFSET_TOP);

    }
    
    /**
     * TODO javadoc
     * @param min
     * @param max
     */
    public void setYMaxValue(double max){
        this.max_value = max;
    }

    /**
     * TODO javadoc
     * @param value
     * @return
     */
    private int calcY(double value){
        return (int)(this.height - (value / this.max_value) * this.height) + Y_OFFSET_TOP;
    }

}
