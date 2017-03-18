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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import moa.evaluation.MeasureCollection;

/**
 * AbstractGraphCanvas is an abstract class offering scaling functionality and
 * the structure of the underlying Axes and Drawing classes.
 * 
 * The functionality of the scaling is as following: the size of the canvas is 
 * determined by two sizes: the size itself and the preferredSize, which is
 * used by the parental viewport to set its size. If the window is rescaled
 * (e.g. by dragging the window) the preferred size does not change, which
 * results in setting the size to the viewport size. If the y axis is zoomed,
 * the baseHeight determines the new size and preferred size.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see GraphCanvas, AbstractGraphAxes, AbstractGraphResult
 */
public abstract class AbstractGraphCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	protected MeasureCollection[] measures;

	protected int measureSelected;

	protected AbstractGraphAxes axesPanel;

	protected AbstractGraphPlot curvePanel;

	private static final int X_OFFSET_LEFT = 35;

	private static final int X_OFFSET_RIGHT = 5;

	private static final int Y_OFFSET_BOTTOM = 20;

	private static final int Y_OFFSET_TOP = 20;

	private double max_y_value;

	private int max_x_value;

	private double x_resolution;

	private double y_resolution;

	private double baseHeight;
	
	/**
	 * Initialises an AbstractGraphCanvas by constructing its
	 * AbstractGraphAxes, GraphMultiCurve as well as setting the initial sizes.
	 */
	public AbstractGraphCanvas(AbstractGraphAxes ax, AbstractGraphPlot g) {
		this.axesPanel = ax;
		this.curvePanel = g;

		this.curvePanel.setLocation(X_OFFSET_LEFT + 1, Y_OFFSET_TOP);

		add(this.axesPanel);
		this.axesPanel.add(this.curvePanel);

		this.measureSelected = 0;
		this.max_y_value = 1;
		this.max_x_value = 1;
		this.x_resolution = 1;
		this.y_resolution = 1;

		updateXResolution();
		updateYResolution();

		this.baseHeight = getHeight();

	}

	private void updateBaseHeight() {
		if (y_resolution > 1) {
			this.baseHeight = getHeight() / y_resolution;
		} else {
			this.baseHeight = 111;
		}
	}

	/**
	 * Scales the resolution on the x-axis by the given value and updates the
	 * canvas.
	 * 
	 * @param factor
	 *            value the x resolution will be scaled by
	 */
	public void scaleXResolution(double factor) {
		this.x_resolution *= factor;
		updateXResolution();
		updateCanvas(true);
	}

	/**
	 * Increases the resolution on the y-axis by the given value, with a minimum
	 * value of 1.0, and updates the canvas.
	 * 
	 * @param value
	 *            value the y resolution will be scaled by
	 */
	public void scaleYResolution(double value) {
		this.y_resolution = Math.max(1.0, this.y_resolution + value);
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
	 * and preferred sizes are updated and the canvas is repainted.
	 * 
	 * @param force
	 *            enforce repainting
	 */
	public void updateCanvas(boolean force) {
		if (updateMaxValues() || force) {
			setSize(getWidth(), (int) (this.baseHeight * y_resolution));
			setPreferredSize(new Dimension(
					(int) Math.max(getPreferredSize().getWidth(), (int) (max_x_value / x_resolution) + X_OFFSET_LEFT),
					getHeight()));
			this.repaint();
		}
	}

	private double maxValue() {
		double max = Double.MIN_VALUE;

		for (int i = 0; i < this.measures.length; i++) {
			if (this.measures[i].getMaxValue(this.measureSelected) > max) {
				max = this.measures[i].getMaxValue(this.measureSelected);
			}
		}
		return max;
	}

	private int maxNumValues() {
		int max = 0;

		for (int i = 0; i < this.measures.length; i++) {
			if (this.measures[i].getNumberOfValues(this.measureSelected) > max) {
				max = this.measures[i].getNumberOfValues(this.measureSelected);
			}
		}
		return max;
	}

	/**
	 * Computes the maximum values of the registered measure collections and
	 * updates the member values accordingly.
	 * 
	 * @return true, if the values have changed
	 */
	private boolean updateMaxValues() {

		int max_x_value_new;
		double max_y_value_new;

		if (this.measures == null) {
			// no values received yet -> reset axes
			max_x_value_new = 1;
			max_y_value_new = 1;
		} else {
			max_x_value_new = maxNumValues();
			max_y_value_new = maxValue();
		}

		// resizing needed?
		if (max_x_value_new != this.max_x_value || 
			max_y_value_new != this.max_y_value) {
			this.max_x_value = max_x_value_new;
			this.max_y_value = max_y_value_new;
			updateYValue();
			return true;
		}
		return false;
	}

	/**
	 * Updates the x resolution.
	 */
	private void updateXResolution() {
		axesPanel.setXResolution(x_resolution);
		curvePanel.setXResolution(x_resolution);
	}

	/**
	 * Updates the y resolution.
	 */
	private void updateYResolution() {
		axesPanel.setYResolution(y_resolution);
	}

	/**
	 * Updates the y values of the axes and curve panel.
	 */
	private void updateYValue() {
		axesPanel.setYMaxValue(max_y_value);
		curvePanel.setYMaxValue(max_y_value);
	}

	/**
	 * Updates the size of the axes, curve and event panel. Recomputes the event
	 * locations if necessary.
	 */
	private void updateChildren() {
		axesPanel.setSize(getWidth(), getHeight());
		curvePanel.setSize(getWidth() - X_OFFSET_LEFT - X_OFFSET_RIGHT, getHeight() - Y_OFFSET_BOTTOM - Y_OFFSET_TOP);
	}

	@Override
	protected void paintChildren(Graphics g) {
		updateBaseHeight();
		updateChildren();
		super.paintChildren(g);
	}

}
