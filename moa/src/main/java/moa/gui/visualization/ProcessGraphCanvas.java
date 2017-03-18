/*
 *    BasicGraphCanvas.java
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

import moa.evaluation.MeasureCollection;

/**
 * ProcessGraphCanvas is the basic implementation of AbstractGraphCanvas
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphCanvas, GraphCanvas
 */
public class ProcessGraphCanvas extends AbstractGraphCanvas {

	private static final long serialVersionUID = 1L;

	private int[] processFrequencies;

	private int min_processFrequency;

	/**
	 * Initialises a GraphCanvasMulti by constructing its GraphAxes,
	 * GraphMultiCurve and EventPanel members as well as setting the initial
	 * sizes.
	 */
	public ProcessGraphCanvas() {
		super(new ScalableGraphAxes(), new GraphMultiCurve());
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
	 * @param mSelect
	 *            currently selected measure
	 * @param processFrequencies
	 *            information about the process frequencies of the curves
	 * @param min_processFrequency
	 *            minimun process frequency
	 */
	public void setGraph(MeasureCollection[] measures, int mSelect, int[] processFrequencies,
			int min_processFrequency) {
		this.measures = measures;
		this.measureSelected = mSelect;
		this.processFrequencies = processFrequencies;
		this.min_processFrequency = min_processFrequency;
		((ScalableGraphAxes) this.axesPanel).setProcessFrequency(min_processFrequency);
		((GraphMultiCurve) this.curvePanel).setProcessFrequency(min_processFrequency);
		((GraphMultiCurve) this.curvePanel).setGraph(measures, mSelect, processFrequencies);
		updateCanvas(false);
	}

}
