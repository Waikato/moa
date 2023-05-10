/*
 *    ProcessGraphCanvas.java
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

import java.awt.Color;
import java.awt.Dimension;

import moa.evaluation.MeasureCollection;

/**
 * ProcessGraphCanvas is an implementation of AbstractGraphCanvas, showing one
 * or multiple curves over a process.
 *
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphCanvas
 */
public class ProcessGraphCanvas extends AbstractGraphCanvas {

	private static final long serialVersionUID = 1L;

	private int[] processFrequencies;

	private int min_processFrequency;

	/**
	 * Initialises a ProcessGraphCanvas by calling the super constructor with a
	 * ProcessGraphAxes as instance of AbstractGraphAxes and GraphMultiCurve as
	 * instance of AbstractGraphPlot. Also sets the min_processfrequency to
	 * initial value.
	 */
	public ProcessGraphCanvas() {
		super(new ProcessGraphAxes(), new GraphMultiCurve());
		// TODO implement
		this.min_processFrequency = 10000;
	}

	/**
	 * Returns the minimum process frequency.
	 *
	 * @return minimum process frequency
	 */
	public int getMinProcessFrequency() {
		return this.min_processFrequency;
	}

	/**
	 * Returns the list of registered process frequencies.
	 * 
	 * @return list of registered process frequencies
	 */
	public int[] getProcessFrequencies() {
		return this.processFrequencies;
	}

	/**
	 * Sets the graph containing multiple curves.
	 *
	 * @param measures
	 *            information about the curves
	 * @param measureStds
	 * 			  standard deviation values for the measures
	 * @param processFrequencies
	 *            information about the process frequencies of the curves
	 * @param min_processFrequency
	 *            minimum process frequency
	 * @param colors
	 *            color encodings for the curves
	 */
	public void setGraph(MeasureCollection[] measures, MeasureCollection[] measureStds, int[] processFrequencies, int min_processFrequency,
			Color[] colors) {
		this.measures = measures;
		this.processFrequencies = processFrequencies;
		this.min_processFrequency = min_processFrequency;
		((ProcessGraphAxes) this.axesPanel).setProcessFrequency(min_processFrequency);
		((GraphMultiCurve) this.plotPanel).setProcessFrequency(min_processFrequency);
		((GraphMultiCurve) this.plotPanel).setGraph(measures, measureStds, processFrequencies, colors);
		updateCanvas(false);
	}

	@Override
	public double getMinXValue() {
	    return 0;
	}

	@Override
	public double getMaxXValue() {
		int max = 0;

		for (int i = 0; i < this.measures.length; i++) {
			if (this.measures[i].getNumberOfValues(this.measureSelected) > max) {
				max = this.measures[i].getNumberOfValues(this.measureSelected);
			}
		}
		return max;
	}

	@Override
	public void setSize() {
		setSize(getWidth(), (int) (baseHeight * y_resolution));
	}

	@Override
	public void setPreferredSize() {
		setPreferredSize(new Dimension(
				(int) Math.max(getPreferredSize().getWidth(), (int) (max_x_value * x_resolution) + X_OFFSET_LEFT),
				getHeight()));
	}

}
