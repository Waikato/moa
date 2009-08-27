/*
 *    WindowClassificationPerformanceEvaluator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.evaluation;

import moa.AbstractMOAObject;
import moa.core.Measurement;
import moa.options.IntOption;
import weka.core.Utils;

public class WindowClassificationPerformanceEvaluator extends AbstractMOAObject
		implements ClassificationPerformanceEvaluator {

	private static final long serialVersionUID = 1L;

	protected double TotalweightObserved = 0;
	
	protected int width;

	protected Estimator weightObserved;

	protected Estimator weightCorrect;
	
	public class Estimator {
		protected double[] window;
		protected int posWindow;
		protected int lenWindow;
		protected int SizeWindow;
		protected double sum;
		
		public Estimator(int sizeWindow) {
			window = new double[sizeWindow];
			SizeWindow = sizeWindow;
			posWindow = 0;
		}
		public void add(double value) {
			sum -= window[posWindow];
			sum += value;
			window[posWindow] = value;
			posWindow++;
			if (posWindow == SizeWindow) 
				posWindow = 0;
		}
		public double total() {
			return sum;
		}
	}
	
	public void setWindowWidth(int w) {
		this.width = w;
		reset();
	}
	
	public void reset() {
		this.weightCorrect = new Estimator(this.width);
		this.weightObserved = new Estimator(this.width);
		this.TotalweightObserved  = 0;
	}

	public void addClassificationAttempt(int trueClass, double[] classVotes,
			double weight) {
		if (TotalweightObserved == 0) reset();
		if (weight > 0.0) {
			this.TotalweightObserved += weight;
			this.weightObserved.add(weight);
			if (Utils.maxIndex(classVotes) == trueClass) {
				this.weightCorrect.add(weight);
			} else {
				this.weightCorrect.add(0);
			}
			
		}
	}

	public Measurement[] getPerformanceMeasurements() {
		return new Measurement[] {
				new Measurement("classified instances",
						this.TotalweightObserved),
				new Measurement("classifications correct (percent)",
						getFractionCorrectlyClassified() * 100.0) };
	}

	public double getTotalWeightObserved() {
		return this.weightObserved.total();
	}

	public double getFractionCorrectlyClassified() {
		return this.weightObserved.total() > 0.0 ? (double) this.weightCorrect.total()
				/ this.weightObserved.total() : 0.0;
	}

	public double getFractionIncorrectlyClassified() {
		return 1.0 - getFractionCorrectlyClassified();
	}

	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
				sb, indent);
	}

}
