/*
 *    RunTasks.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
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
package moa.tasks;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.StringOption;

import moa.MOAObject;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.tasks.conceptdrift.ConceptDriftMainTask;
import moa.tasks.conceptdrift.EvaluateConceptDrift;

/**
 * Task for running several experiments modifying values of parameters.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class RunStreamTasks extends ConceptDriftMainTask {

    @Override
    public String getPurposeString() {
        return "Runs several experiments modifying values of parameters.";
    }

    private static final long serialVersionUID = 1L;

    public ClassOption taskOption = new ClassOption("task", 't',
            "Task to do.", Task.class, "EvaluatePrequential -l active.ALUncertainty -i 1000000 -d temp.txt");

    public StringOption streamParameterOption = new StringOption("streamParameter", 'p',
            "Stream parameter to vary.", "b");

    public FloatOption firstValueOption = new FloatOption("firstValue",
            'f', "First value", 0.0);

    public FloatOption lastValueOption = new FloatOption("lastValue",
            'l', "Last value", 1.0);

    public FloatOption incrementValueOption = new FloatOption("incrementValue",
            'i', "Increment value", 0.1);

    @Override
    public Class<?> getTaskResultType() {
        return this.task.getTaskResultType();
    }

    protected Task task;

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Object result = null;
        Task taskBase = (Task) getPreparedClassOption(this.taskOption); 
        //for each possible value of the parameter
        for (int valueParameter = (int) this.firstValueOption.getValue();
                valueParameter <= this.lastValueOption.getValue();
                valueParameter += (int) this.incrementValueOption.getValue()) {
            //Add parameter
            this.task = (Task) ((MOAObject) taskBase).copy();
            if (this.task instanceof EvaluateConceptDrift) {
                String stream = ((EvaluateConceptDrift) this.task).streamOption.getValueAsCLIString();
                ((EvaluateConceptDrift) this.task).streamOption.setValueViaCLIString(stream + " -" + streamParameterOption.getValue() + " " + valueParameter);
            }
            //Run task
            result = this.task.doTask(monitor, repository);
        }
        return result;
    }
}
