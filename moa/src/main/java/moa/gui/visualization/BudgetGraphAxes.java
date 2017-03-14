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
    
    private double max_value;
	
	/**
	 * TODO javadoc
	 */
    public BudgetGraphAxes() {
    	
    	this.max_value = 1;
    	
    	GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        this.height = getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP;
        this.width = getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT;

        g.setColor(new Color(236,233,216));
        g.fillRect(0, 0, getWidth(), getHeight());

        // draw background
        g.setColor(Color.WHITE);
        g.fillRect(X_OFFSET_LEFT, Y_OFFSET_TOP, width, height);

        g.setFont(new Font("Tahoma", 0, 11));
        

        this.xAxis(g);
        this.yAxis(g);
    }
    
    /**
     * TODO javadoc
     * @param g
     */
    private void xAxis(Graphics g){
        g.setColor(Color.BLACK);
        
        //x-axis
        g.drawLine(X_OFFSET_LEFT, Y_OFFSET_TOP + this.height, this.width+X_OFFSET_LEFT, Y_OFFSET_TOP + this.height);

        //x axis labels
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
        	DecimalFormat d = new DecimalFormat("0.00");
        	for (double i = 0.0; i <= 1.0; i += 0.2) {
        		int x = (int) (this.width * i) + X_OFFSET_LEFT;
        		g.drawLine(x, this.height + Y_OFFSET_TOP, x, this.height+Y_OFFSET_TOP + 5);
        		String label = d.format(i);
        		int str_length = g.getFontMetrics().stringWidth(label);
                g.drawString(label, x - str_length/2, this.height + Y_OFFSET_TOP + 18);
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
        g.drawLine(X_OFFSET_LEFT, (int) this.height + Y_OFFSET_TOP, X_OFFSET_LEFT, Y_OFFSET_TOP);

        //center horizontal line
        g.setColor(new Color(220,220,220));
        g.drawLine(X_OFFSET_LEFT, this.height/2+Y_OFFSET_TOP, getWidth(), this.height/2+Y_OFFSET_TOP);

        //3 y-achsis markers + labels
        g.setColor(Color.BLACK);
        DecimalFormat d = new DecimalFormat("0.00");
        int digits_y = (int)(Math.log10(this.max_value))-1;
        double upper = Math.ceil(this.max_value/Math.pow(10,digits_y));
        if(digits_y < 0) upper*=Math.pow(10,digits_y);

        if(Double.isNaN(upper)) upper =1.0;

        g.drawString(d.format(0.0), 3, this.height+Y_OFFSET_TOP+5);
        g.drawString(d.format(upper/2), 3, this.height/2+Y_OFFSET_TOP + 5);
        g.drawString(d.format(upper), 3, Y_OFFSET_TOP + 5);
        g.drawLine(X_OFFSET_LEFT-5, this.height+Y_OFFSET_TOP, X_OFFSET_LEFT, this.height+Y_OFFSET_TOP);
        g.drawLine(X_OFFSET_LEFT-5, this.height/2+Y_OFFSET_TOP, X_OFFSET_LEFT, this.height/2+Y_OFFSET_TOP);
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
}
