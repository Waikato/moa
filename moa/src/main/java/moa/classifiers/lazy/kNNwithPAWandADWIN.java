/*
 *    kNNAdaptive.java
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
 *    
 */
package moa.classifiers.lazy;

import java.util.ArrayList;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * k Nearest Neighbor ADAPTIVE with ADWIN+PAW.<p>
 *
 * Valid options are:
 * <p>
 *
 * -k number of neighbours <br>
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version 03.2012
 */
public class kNNwithPAWandADWIN extends kNN implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    private ADWIN adwin;

    protected int marker = 0;

    protected ArrayList<Integer> timeStamp;

    @Override
    public String getPurposeString() {
        return "kNNwithPAWandADWIN: kNN with Probabilistic Approximate Window and ADWIN";
    }

    protected double prob;

    @Override
    public void resetLearningImpl() {
        this.window = null;
        this.adwin = new ADWIN();
        this.prob = Math.pow(2.0, -1.0 / this.limitOption.getValue());
        this.time = 0;
    }

    protected int time;

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (inst.classValue() > C) {
            C = (int) inst.classValue();
        }
        // ADWIN
        if (this.window == null) {
            this.window = new Instances(inst.dataset());
        }

        if (this.timeStamp == null) {
            this.timeStamp = new ArrayList<Integer>(10);
        }
        for (int i = 0; i < this.window.size(); i++) {
            if (this.classifierRandom.nextDouble() > this.prob) {
                this.window.delete(i);
                this.timeStamp.remove(i);
            }
        }
        this.window.add(inst);
        this.timeStamp.add(this.time);
        this.time++;
        boolean correctlyClassifies = this.correctlyClassifies(inst);
        if (this.adwin.setInput(correctlyClassifies ? 0 : 1)) {
            //Change
            int size = (int) this.adwin.getWidth();
            for (int i = 0; i < this.window.size(); i++) {
                if (this.timeStamp.get(i) < this.time - size) {
                    this.window.delete(i);
                    this.timeStamp.remove(i);
                }
            }
        }

    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

}
