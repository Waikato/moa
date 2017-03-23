/*
 *    AbstractGraphCanvas.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
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

import java.awt.Graphics;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/**
 * AbstractGraphCanvas is an abstract class offering scaling functionality and
 * the structure of the underlying Axes and Plot classes.
 * 
 * The functionality of the scaling is as following: the size of the canvas is
 * determined by two sizes: the size itself and the preferredSize, which is used
 * by the parental viewport to set its size. If the window is rescaled (e.g. by
 * dragging the window) the preferred size does not change, which results in
 * setting the size to the viewport size. If the y axis is zoomed, the
 * baseHeight determines the new size and preferred size.
 * 
 * This class is partially based on GraphCanvas.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas, AbstractGraphAxes, AbstractGraphPlot
 */
public abstract class AbstractGraphCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	protected MeasureCollection[] measures;

	protected int measureSelected;

	protected AbstractGraphAxes axesPanel;

	protected AbstractGraphPlot plotPanel;

	protected static final int X_OFFSET_LEFT = 35;

	protected static final int X_OFFSET_RIGHT = 5;

	protected static final int Y_OFFSET_BOTTOM = 20;

	protected static final int Y_OFFSET_TOP = 20;

	protected double max_y_value;

	protected double max_x_value;

	protected double x_resolution;

	protected double y_resolution;

	protected double baseHeight;
	protected double baseWidth;

	/**
	 * Initialises an AbstractGraphCanvas by constructing its AbstractGraphAxes,
	 * AbstractGraphPlot as well as setting initial sizes.
	 */
	public AbstractGraphCanvas(AbstractGraphAxes ax, AbstractGraphPlot g) {
		this.axesPanel = ax;
		this.plotPanel = g;

		this.plotPanel.setLocation(X_OFFSET_LEFT + 1, Y_OFFSET_TOP);

		add(this.axesPanel);
		this.axesPanel.add(this.plotPanel);

		this.measureSelected = 0;
		this.max_y_value = 1;
		this.max_x_value = 1;
		this.x_resolution = 1;
		this.y_resolution = 1;

		updateXResolution();
		updateYResolution();

		this.baseHeight = getHeight();
	}

	/**
	 * Updates the base height, which is used to determine the canvas size. It
	 * is defined as the current height divided by the y_resolution. To prevent
	 * unnecessary scrolling after reducing the y_resolution, the baseHeight is
	 * reset to its initial value of 111 on y_resolution = 1.
	 */
	private void updateBaseHeight() {
		if (y_resolution > 1) {
			this.baseHeight = getHeight() / y_resolution;
		} else {
			this.baseHeight = 111;
		}
	}
	
	private void updateBaseWidth() {
		if (x_resolution > 1) {
			this.baseWidth = getWidth() / x_resolution;
		} else {
			this.baseWidth = 500;
		}
	}

	/**
	 * Scales the resolution on the x-axis by the given factor and updates the
	 * canvas.
	 * 
	 * @param factor
	 *            factor the x_resolution will be scaled by
	 */
	public void scaleXResolution(double factor) {
		this.x_resolution *= factor;
		updateXResolution();
		updateCanvas(true);
	}

	/**
	 * Scales the resolution on the y-axis by the given factor and updates the
	 * canvas.
	 * 
	 * @param factor
	 *            factor the y_resolution will be scaled by
	 */
	public void scaleYResolution(double factor) {
		this.y_resolution *= factor;
		updateYResolution();
		updateCanvas(true);
	}

	/**
	 * Returns the currently selected measure index.
	 * 
	 * @return currently selected measure index
	 */
	public int getMeasureSelected() {
		return this.measureSelected;
	}

	/**
	 * Updates the canvas: if values have changed or it is forced, the canvas
	 * and preferred sizes are updated and the canvas is repainted. The size is
	 * defined as the current width * (baseHeight*y_resolution). The preferred
	 * size, which controls the parental viewport, is set as the maximum of the
	 * current preferred width and the latest point of the Plot, and the current
	 * height.
	 * 
	 * @param force
	 *            enforce repainting
	 */
	public void updateCanvas(boolean force) {
		if (updateMaxValues() || force) {
			setSize();
			setPreferredSize();
//			setSize(getWidth(), (int) (this.baseHeight * y_resolution));
//			setPreferredSize(new Dimension(
//					(int) Math.max(getPreferredSize().getWidth(), (int) (max_x_value / x_resolution) + X_OFFSET_LEFT),
//					getHeight()));
			this.repaint();
		}
	}
	
	public abstract void setSize();
	public abstract void setPreferredSize();

	/**
	 * Computes the maximum value of the underlying measures at the currently
	 * selected measure.
	 * 
	 * @return max value of measures at measureSelected
	 */
	private double maxValue() {
		double max = Double.MIN_VALUE;

		for (int i = 0; i < this.measures.length; i++) {
			if (this.measures[i].getMaxValue(this.measureSelected) > max) {
				max = this.measures[i].getMaxValue(this.measureSelected);
			}
		}
		return max;
	}
	
	public abstract double getMaxXValue();

	/**
	 * Computes the maximum values of the registered measure collections and
	 * updates the member values accordingly.
	 * 
	 * @return true, if the values have changed
	 */
	private boolean updateMaxValues() {

		double max_x_value_new;
		double max_y_value_new;

		if (this.measures == null) {
			// no values received yet -> reset axes
			max_x_value_new = 1;
			max_y_value_new = 1;
		} else {
			max_x_value_new = getMaxXValue();
			max_y_value_new = maxValue();
		}

		// resizing needed?
		if (max_x_value_new != this.max_x_value || max_y_value_new != this.max_y_value) {
			this.max_x_value = max_x_value_new;
			this.max_y_value = max_y_value_new;
			updateMaxYValue();
			updateMaxXValue();
			updateYUpperValue();
			updateXUpperValue();
			return true;
		}
		return false;
	}

	/**
	 * Updates the x resolution of the axes and plot panel.
	 */
	private void updateXResolution() {
		axesPanel.setXResolution(x_resolution);
		plotPanel.setXResolution(x_resolution);
	}

	/**
	 * Updates the y resolution of the axes and plot panel.
	 */
	private void updateYResolution() {
		axesPanel.setYResolution(y_resolution);
	}

	/**
	 * Updates the max y value of the axes and plot panel.
	 */
	private void updateMaxYValue() {
		axesPanel.setYMaxValue(max_y_value);
		plotPanel.setYMaxValue(max_y_value);
	}
	
	/**
	 * Updates the max x value of the axes and plot panel.
	 */
	private void updateMaxXValue() {
		axesPanel.setXMaxValue(max_x_value);
		plotPanel.setXMaxValue(max_x_value);
	}
	
	private void updateYUpperValue() {
		int digits_y = (int)(Math.log10(max_y_value))-1;
        double upper = Math.ceil(max_y_value/Math.pow(10,digits_y));
        if(digits_y < 0) upper*=Math.pow(10,digits_y);

        if(Double.isNaN(upper)) {
        	upper = 1.0;
        }
        
        this.axesPanel.setYUpperValue(upper);
        this.plotPanel.setYUpperValue(upper);
	}
	
	private void updateXUpperValue() {
		int digits_x = (int)(Math.log10(max_x_value))-1;
        double upper = Math.ceil(max_x_value/Math.pow(10,digits_x));
        if(digits_x < 0) upper*=Math.pow(10,digits_x);

        if(Double.isNaN(upper)) {
        	upper = 1.0;
        }
        
        this.axesPanel.setXUpperValue(upper);
        this.plotPanel.setXUpperValue(upper);
	}

	/**
	 * Updates the size of the axes, curve and event panel. Recomputes the event
	 * locations if necessary.
	 */
	private void updateChildren() {
		axesPanel.setSize(getWidth(), getHeight());
		plotPanel.setSize(
				getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT, 
				getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP
		);
	}

	@Override
	protected void paintChildren(Graphics g) {
		updateBaseHeight();
		updateBaseWidth();
		updateChildren();
		super.paintChildren(g);
	}

}
