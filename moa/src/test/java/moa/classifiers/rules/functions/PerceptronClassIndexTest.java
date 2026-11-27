/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package moa.classifiers.rules.functions;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class PerceptronClassIndexTest {

    private final Attribute x = new Attribute("x");
    private final Attribute group = new Attribute("group", Arrays.asList("a", "b"));
    private final Attribute z = new Attribute("z");
    private final Attribute target = new Attribute("target");

    @Test
    public void predictionsAreIndependentOfClassPosition() {
        double[] expected = predictions(x, group, z, target);
        assertArrayEquals(expected, predictions(x, target, group, z), 0.0);
        assertArrayEquals(expected, predictions(target, x, group, z), 0.0);
    }

    @Test
    public void numericFeatureBeforeNominalFeatureKeepsItsValue() {
        double[] expected = predictions(x, z, group, target);
        assertArrayEquals(expected, predictions(x, target, z, group), 0.0);
    }

    private double[] predictions(Attribute... attributes) {
        Instances data = new Instances("regression", attributes, 0);
        data.setClassIndex(Arrays.asList(attributes).indexOf(target));

        Perceptron learner = new Perceptron();
        learner.prepareForUse();
        double[] predictions = new double[5];
        for (int i = 0; i < predictions.length; i++) {
            Instance row = new DenseInstance(1.0, new double[data.numAttributes()]);
            row.setDataset(data);
            row.setValue(x, i);
            row.setValue(group, i % 2);
            row.setValue(z, i * i);
            row.setClassValue(2 * i + 1);
            learner.trainOnInstance(row);
            predictions[i] = learner.getVotesForInstance(row)[0];
        }
        return predictions;
    }
}
