/*
 *    EvaluatePrequential.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.gui.experimentertab.tasks;

import moa.tasks.*;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import moa.core.*;
import moa.evaluation.*;
import moa.options.ClassOption;

/**
 * Task for prequential cross-validation evaluation of a classifier on a stream by testing then training with each
 * example in sequence and doing cross-validation at the same time.
 *
 * <p>Albert Bifet, Gianmarco De Francisci Morales, Jesse Read, Geoff Holmes, Bernhard Pfahringer: Efficient Online
 * Evaluation of Big Data Stream Classifiers. KDD 2015: 59-68</p>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluatePrequentialCV extends ExperimenterTask {

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by doing prequential evaluation (testing then training with each" +
                " example in sequence) and doing cross-validation.";
    }

    private static final long serialVersionUID = 1L;

       public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "WindowClassificationPerformanceEvaluator");

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000000, -1, Integer.MAX_VALUE);

    public IntOption timeLimitOption = new IntOption("timeLimit", 't',
            "Maximum number of seconds to test/train for (-1 = no limit).", -1,
            -1, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
            'f',
            "How many instances between samples of the learning performance.",
            100000, 0, Integer.MAX_VALUE);

    public IntOption memCheckFrequencyOption = new IntOption(
            "memCheckFrequency", 'q',
            "How many instances between memory bound checks.", 100000, 0,
            Integer.MAX_VALUE);

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 'w',
            "The number of distributed models.", 10, 1, Integer.MAX_VALUE);

    public MultiChoiceOption validationMethodologyOption = new MultiChoiceOption(
            "validationMethodology", 'a', "Validation methodology to use.", new String[]{
            "Cross-Validation", "Bootstrap-Validation", "Split-Validation"},
            new String[]{"k-fold distributed Cross Validation",
                    "k-fold distributed Bootstrap Validation",
                    "k-fold distributed Split Validation"
            }, 0);

    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random behaviour of the task.", 1);


    @Override
    public Class<?> getTaskResultType() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    protected Object doTaskImpl(TaskMonitor monitor, ObjectRepository repository) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
   
}
