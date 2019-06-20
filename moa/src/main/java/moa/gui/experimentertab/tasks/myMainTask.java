/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.experimentertab.tasks;


import moa.core.ObjectRepository;
import moa.tasks.AbstractTask;
import moa.tasks.TaskMonitor;

/**
 *
 * @author Alberto
 */
public abstract class myMainTask extends AbstractTask {

    private static final long serialVersionUID = 1L;

    /** The number of instances between monitor updates. */
    protected static final int INSTANCES_BETWEEN_MONITOR_UPDATES = 10;

    /** File option to save the final result of the task to. */
//    public FileOption outputFileOption = new FileOption("taskResultFile", 'O',
//            "File to save the final result of the task to.", null, "moa", true);

    @Override
    protected Object doTaskImpl(TaskMonitor monitor, ObjectRepository repository) {
        Object result = doMainTask(monitor, repository);
//        if (monitor.taskShouldAbort()) {
//            return null;
//        }
//        File outputFile = this.outputFileOption.getFile();
//        if (outputFile != null) {
//            if (result instanceof Serializable) {
//                monitor.setCurrentActivity("Saving result of task "
//                        + getTaskName() + " to file " + outputFile + "...",
//                        -1.0);
//                try {
//                    SerializeUtils.writeToFile(outputFile,
//                            (Serializable) result);
//                } catch (IOException ioe) {
//                    throw new RuntimeException("Failed writing result of task "
//                            + getTaskName() + " to file " + outputFile, ioe);
//                }
//            } else {
//                throw new RuntimeException("Result of task " + getTaskName()
//                        + " is not serializable, so cannot be written to file "
//                        + outputFile);
//            }
//        }
        return result;
    }

    /**
     * This method performs this task.
     * <code>AbstractTask</code> implements <code>doTask</code>,
     * that uses <code>doTaskImpl</code>.
     * <code>myMainTask</code> implements <code>doTaskImpl</code> using
     * <code>doMainTask</code> so its extensions only need to implement
     * <code>doMainTask</code>.
     *
     * @param monitor the TaskMonitor to use
     * @param repository  the ObjectRepository to use
     * @return an object with the result of this task
     */
    protected abstract Object doMainTask(TaskMonitor monitor,
            ObjectRepository repository);
}
