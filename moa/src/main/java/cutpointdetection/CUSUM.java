/*
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Manuel Baena (mbaena@lcc.uma.es)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cutpointdetection;


/**
 * Drift detection method based in Cusum
 *
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version $Revision: 7 $
 */
public class CUSUM implements CutPointDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    private int m_n;

    private double sum;

    private double x_mean;

    private double alpha;

    private double delta;


    private double lambda;

    public double getLambda(){
	return lambda;
    }

    private boolean isChangeDetected;
    private double estimation;
    private boolean isWarningZone;
    private int delay;
    private int minNumInstancesOption = 30;
    private int time;

    public CUSUM() {
	resetLearning();
    }
    
    public CUSUM(double lambda) {
	resetLearning();
	setLambda(lambda);
    }

    public void resetLearning() {
	m_n = 1;
	x_mean = 0.0;
	sum = 0.0;
	delta = 0.005;
//	lambda = 50;
    }

    public boolean setInput(double x) {
	// It monitors the error rate
	if (this.isChangeDetected == true) {
	    resetLearning();
	}

	x_mean = x_mean + (x - x_mean) / (double) m_n;
	sum = Math.max(0, sum + x - x_mean - this.delta);
	// System.out.println(sum + " " + x_mean + " " + m_n);
	m_n++;

	// System.out.print(prediction + " " + m_n + " " + (m_p+m_s) + " ");
	this.estimation = x_mean;
	this.isChangeDetected = false;
	this.isWarningZone = false;
	this.delay = 0;

	if (m_n < (this.minNumInstancesOption)) {
	    return false;
	}

	if (sum > this.lambda) {
	    this.isChangeDetected = true;
	    return this.isChangeDetected;
	} 

	return false;
    }

    public void setDelta(double delta){
	this.delta = delta;
    }

    public void setLambda(double lambda){
	this.lambda = lambda;
    }


    //Timer

    public void timer() {
	time++;
    }

    public void resetTime(){
	time = 0;

    }

    public int getTime(){
	return time;
    }



}