package moa.gui.visualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

public abstract class AbstractGraphAxes extends javax.swing.JPanel{
	
	private static final long serialVersionUID = 1L;
	protected static final int X_OFFSET_LEFT = 35;
    protected static final int X_OFFSET_RIGHT = 5;
    protected static final int Y_OFFSET_BOTTOM = 20;
    protected static final int Y_OFFSET_TOP = 20;
    
    protected int height;
    protected int width;

    protected double x_resolution;
    protected double y_resolution;
    
    protected double max_value;

	public AbstractGraphAxes() {
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
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        height = getHeight()-Y_OFFSET_BOTTOM-Y_OFFSET_TOP;
        width = getWidth()-X_OFFSET_LEFT-X_OFFSET_RIGHT;

        g.setColor(new Color(236,233,216));
        g.fillRect(0, 0, getWidth(), getHeight());

        //draw background
        g.setColor(Color.WHITE);
        g.fillRect(X_OFFSET_LEFT, Y_OFFSET_TOP, width, height);

        g.setFont(new Font("Tahoma", 0, 11));
        

        xAxis(g);
        yAxis(g);
    }

    protected void xAxis(Graphics g) {
    	g.setColor(Color.BLACK);
        
        //x-achsis
        g.drawLine(X_OFFSET_LEFT, calcY(0), width+X_OFFSET_LEFT, calcY(0));
        
        drawXLabels(g);
    }
    
    protected abstract void drawXLabels(Graphics g);

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
