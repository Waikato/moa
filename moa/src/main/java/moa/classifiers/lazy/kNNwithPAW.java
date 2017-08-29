/*
 *    kNNwithPAW.java
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

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.core.driftdetection.ADWIN;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * k Nearest Neighbor ADAPTIVE with PAW.<p>
 *
 * Valid options are:
 * <p>
 *
 * -k number of neighbours <br>
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version 03.2012
 */
public class kNNwithPAW extends kNN implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    protected int marker = 0;

    @Override
    public String getPurposeString() {
        return "kNN+PAW: kNN with Probabilistic Approximate Window";
    }

    protected double prob;

    @Override
    public void resetLearningImpl() {
        this.window = null;
        this.prob = Math.pow(2.0, -1.0 / this.limitOption.getValue());
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (inst.classValue() > C) {
            C = (int) inst.classValue();
        }
        if (this.window == null) {
            this.window = new Instances(inst.dataset());
        }

        for (int i = 0; i < this.window.size(); i++) {
            if (this.classifierRandom.nextDouble() > this.prob) {
                this.window.delete(i);
            }
        }
        this.window.add(inst);

    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

}
