package moa.gui.visualization;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

public abstract class AbstractGraphPlot extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
    protected double max_value = 1;
    protected MeasureCollection[] measures;
    protected int measureSelected = 0;

    protected double x_resolution;

	public AbstractGraphPlot() {
		this.max_value = 1;
    	this.measureSelected = 0;
    	
    	setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
	}
	
    /**
     * Sets minimum and maximum y value.
     * @param min minimum y value
     * @param max maximum y value
     */
    protected void setYMaxValue(double max){
        this.max_value = max;
    }

    /**
     * Sets the resolution on the x-axis
     * @param x_resolution resolution on the x-axis
     */
    protected void setXResolution(double x_resolution) {
        this.x_resolution = x_resolution;
    }

}
