/*
 *    RW_kNN.java
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
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.Random;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.core.Measurement;
/**
 * Reservoir Window k-Nearest Neighbors (RW_kNN)
 *
 * Parameters:
 * -k number of neighbours
 * -limitW maximum number of instances inside the window
 * -limitR maximum number of instances inside the reservoir
 * 
 * @author Maroua Bahri (maroua.bahri@inria.fr)
 * Paper:
 * "Incremental k-Nearest Neighbors Using Reservoir Sampling for Data Streams"
 *  Maroua Bahri and Albert Bifet
 * https://link.springer.com/chapter/10.1007/978-3-030-88942-5_10
 * BibTex:
 * "@inproceedings{bahri2021incremental,
 * title={Incremental k-nearest neighbors using reservoir sampling for data streams},
 * author={Bahri, Maroua and Bifet, Albert},
 * booktitle={Discovery Science: 24th International Conference},
 * pages={122--137},
 * year={2021},
 * organization={Springer}
 * }"
 */


public class RW_kNN extends AbstractClassifier implements MultiClassClassifier {
    private static final long serialVersionUID = 1L;

    public IntOption kOption = new IntOption( "k", 'k', "The number of neighbors", 5, 1, Integer.MAX_VALUE);

    public IntOption limitOptionWindow = new IntOption("limitW", 'w', "The maximum number of instances to store in the window",  500, 1, Integer.MAX_VALUE);

    public IntOption limitOptionReservoir = new IntOption("limitR", 'r', "The maximum number of instances to store in the reservoir", 500, 1, Integer.MAX_VALUE);

    public MultiChoiceOption nearestNeighbourSearchOption = new MultiChoiceOption("nearestNeighbourSearch", 'n', "Nearest Neighbour Search to use", new String[]{"LinearNN", "KDTree"}, new String[]{"Brute force search algorithm for nearest neighbour search. ", "KDTree search algorithm for nearest neighbour search"}, 0);

    int C = 0;
    protected Instances window;
    protected Instances reservoir;


    @Override
    public void setModelContext(InstancesHeader context) {
        try {
            this.window = new Instances(context,0);
            this.reservoir = new Instances(context,0);
            this.window.setClassIndex(context.classIndex());
            this.reservoir.setClassIndex(context.classIndex());
        } catch(Exception e) {
            System.err.println("Error: no Model Context available.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void resetLearningImpl() {
        this.window = null;
        this.reservoir = null;
    }

    public void trainOnInstanceImpl(Instance inst) {
        Random r = new Random();
        if (inst.classValue() > (double)this.C)
            this.C = (int)inst.classValue();

        if (this.window == null) {
            this.window = new Instances(inst.dataset());
            this.reservoir = new Instances(inst.dataset());
        }
        if (this.limitOptionReservoir.getValue() <= this.reservoir.numInstances()) {
            int replaceIndex = r.nextInt(this.limitOptionReservoir.getValue() - 1);
            this.reservoir.set(replaceIndex, inst);
        } else
            this.reservoir.add(inst);

        if (this.limitOptionWindow.getValue() <= this.window.numInstances())
            this.window.delete(0);

        this.window.add(inst);
    }

    public double[] getVotesForInstance(Instance inst) {
        double[] v = new double[this.C + 1];
        try {
            NearestNeighbourSearch search;
            NearestNeighbourSearch searchR;
            if (this.nearestNeighbourSearchOption.getChosenIndex() == 0) {
                search = new LinearNNSearch(this.window);
                searchR = new LinearNNSearch(this.reservoir);
            } else {
                search = new KDTree();
                searchR = new KDTree();
               search.setInstances(this.window);
                searchR.setInstances(this.reservoir);
            }

            if (this.window.numInstances() > 0) {
                // kNN inside the window
                Instances neighbours_Wind = search.kNearestNeighbours(inst, Math.min(this.kOption.getValue(), this.window.numInstances()));
                // kNN inside the reservoir
                Instances neighbours_Reser = searchR.kNearestNeighbours(inst, Math.min(this.kOption.getValue(), this.reservoir.numInstances()));
                for(int i = 0; i < neighbours_Reser.numInstances(); i++) {
                    v[(int)neighbours_Wind.instance(i).classValue()]++;
                    v[(int)neighbours_Reser.instance(i).classValue()]++;
                }
            }

        } catch(Exception e) {
            //System.err.println("Error: kNN search failed.");
            //e.printStackTrace();
            //System.exit(1);
            return new double[inst.numClasses()];
        }
        return v;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    public boolean isRandomizable() {
        return false;
    }
}