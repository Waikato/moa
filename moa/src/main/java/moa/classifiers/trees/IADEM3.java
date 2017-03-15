/*
 *    IADEM3.java
 *
 *    @author Isvani Frias-Blanco (ifriasb at hotmail dot com)
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
package moa.classifiers.trees;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.trees.iadem3.IADEM3Tree;
import moa.classifiers.trees.iademutils.IademNumericAttributeObserver;
import moa.core.Measurement;

/**
 *
 *
 * @author Isvani Frías-Blanco (ifriasb at hotmail dot com)
 */
public class IADEM3 extends IADEM2 {

    private static final long serialVersionUID = 1L;

    public IntOption maxNestingLevelOption = new IntOption("maxNestingLevel", 'p',
            "Maximum level of nesting for alternative subtrees (-1 => unbounded).",
            1, -1, Integer.MAX_VALUE);

    public IntOption maxSubtreesPerNodeOption = new IntOption("maxSubtreesPerNode", 'w',
            "Maximum number of alternative subtrees per split node (-1 => unbounded).",
            1, -1, Integer.MAX_VALUE);

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);
        IademNumericAttributeObserver obsContinuos = newNumericClassObserver();
        this.tree = new IADEM3Tree(ih,
                this.attributeDiferentiation.getValue(),
                this.splitMeasure,
                this.leafPredictionOption.getChosenIndex(),
                this.naiveBayesLimit,
                this.percentInCommon,
                obsContinuos,
                (int) obsContinuos.getMaxOfValues(),
                (AbstractChangeDetector) ((AbstractChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy(),
                true,
                this.splitTestsOption.getChosenIndex() == 1,
                this.splitTestsOption.getChosenIndex() == 0,
                this.gracePeriodOption.getValue(),
                0,
                this.maxNestingLevelOption.getValue(),
                this.maxSubtreesPerNodeOption.getValue());
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[] {
            new Measurement("tree size (nodes)", this.tree.getNumberOfNodes()),
            new Measurement("tree size (leaves)", this.tree.getNumberOfLeaves()),
            new Measurement("interchanged trees", ((IADEM3Tree) this.tree).getChangedTrees())
        };
    }
}
