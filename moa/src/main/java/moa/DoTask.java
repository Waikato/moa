/*
 *    DoTask.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa;

import weka.core.Version;
import moa.core.Globals;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.core.TimingUtils;
import moa.core.WekaUtils;
import moa.options.ClassOption;
import moa.tasks.AbstractTask;
import moa.tasks.FailedTaskReport;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;
import moa.tasks.meta.MetaMainTask;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;

/**
 * Class for running a MOA task from the command line.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class DoTask {

    /** Array of characters to use to animate the progress of tasks running. */
    public static final char[] progressAnimSequence = new char[]{'-', '\\',
        '|', '/'};

    /** Maximum length of the status string that shows the progress of tasks running.  */
    public static final int MAX_STATUS_STRING_LENGTH = 79;

    /**
     * Checks if the Java version is recent enough to run MOA.
     *
     * @return true if the Java version is recent.
     */
    public static boolean isJavaVersionOK() {
        boolean isJavaVersionOK = true;
        String versionStr = System.getProperty("java.version");
        String[] parts;
        double version;
        if (versionStr.contains(".")) {
          parts = versionStr.split("\\.");
	}
	else {
          parts = new String[]{versionStr};
	}
	if (parts.length == 1) {
          try {
            version = Double.parseDouble(parts[0]);
	  }
	  catch (Exception e) {
            System.err.println("Unparsable Java version: " + versionStr);
            return false;
	  }
	}
	else {
          try {
            version = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]) / 10;
	  }
	  catch (Exception e) {
            System.err.println("Unparsable Java version: " + versionStr);
            return false;
	  }
	}
        if (version < 1.8) {
            isJavaVersionOK = false;
            System.err.println();
            System.err.println(Globals.getWorkbenchInfoString());
            System.err.println();
            System.err.print("Java 8 or higher is required to run MOA. ");
            System.err.println("Java version " + versionStr + " found");
        }
        return isJavaVersionOK;
    }

    /**
     * Checks if the Weka version is recent enough to run MOA.
     * For example, if the Weka version is not recent, there may be problems
     * due to the fact that <code>Instance</code> was a class before 3.7.1 and
     * now is an interface.
     *
     * @return true if the Weka version is recent.
     */
    public static boolean isWekaVersionOK() {
        Version version = new Version();
        if (version.isOlder("3.9.2")) {
            System.err.println();
            System.err.println(Globals.getWorkbenchInfoString());
            System.err.println();
            System.err.print("Weka 3.9.2 or higher is required to run MOA. ");
            System.err.println("Weka version " + Version.VERSION + " found");
            return false;
        } else {
            return true;
        }
    }
   

    /**
     * Main method for running tasks from the command line.
     *
     * @param args the options
     */
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println();
                System.err.println(Globals.getWorkbenchInfoString());
                System.err.println();
                System.err.println("No task specified.");
            } else {
                if (isJavaVersionOK() == false || WekaUtils.isWekaVersionOK() == false) {
                    return;
                }
                // create standard options
                FlagOption suppressStatusOutputOption = new FlagOption(
                        "suppressStatusOutput", 'S',
                        "Suppress the task status output that is normally send to stderr.");
                FlagOption suppressResultOutputOption = new FlagOption(
                        "suppressResultOutput", 'R',
                        "Suppress the task result output that is normally send to stdout.");
                IntOption statusUpdateFrequencyOption = new IntOption(
                        "statusUpdateFrequency",
                        'F',
                        "How many milliseconds to wait between status updates.",
                        1000, 0, Integer.MAX_VALUE);
                Option[] extraOptions = new Option[]{
                    suppressStatusOutputOption, suppressResultOutputOption,
                    statusUpdateFrequencyOption};
                // build a single string by concatenating cli options
                StringBuilder cliString = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    cliString.append(" ").append(args[i]);
                }
                // parse options
                AbstractTask task;
                try {
                	task = (AbstractTask) ClassOption.cliStringToObject(
                			cliString.toString(), MainTask.class, extraOptions);
                } catch(Exception e) {
                	// regular task could not be found, maybe it is a meta task
            		task = (AbstractTask) ClassOption.cliStringToObject(
            				cliString.toString(), MetaMainTask.class, extraOptions);
                }
                task.prepareForUse();
                
                Object result = null;
                if (suppressStatusOutputOption.isSet()) {
                    result = task.doTask();
                } else {
                    System.err.println();
                    System.err.println(Globals.getWorkbenchInfoString());
                    System.err.println();
                    boolean preciseTiming = TimingUtils.enablePreciseTiming();
                    // start the task thread
                    TaskThread taskThread = new TaskThread(task);
                    taskThread.start();
                    int progressAnimIndex = 0;
                    // inform user of progress
                    while (!taskThread.isComplete()) {
                        StringBuilder progressLine = new StringBuilder();
                        progressLine.append(progressAnimSequence[progressAnimIndex]);
                        progressLine.append(' ');
                        progressLine.append(StringUtils.secondsToDHMSString(taskThread.getCPUSecondsElapsed()));
                        progressLine.append(" [");
                        progressLine.append(taskThread.getCurrentStatusString());
                        progressLine.append("] ");
                        double fracComplete = taskThread.getCurrentActivityFracComplete();
                        if (fracComplete >= 0.0) {
                            progressLine.append(StringUtils.doubleToString(
                                    fracComplete * 100.0, 2, 2));
                            progressLine.append("% ");
                        }
                        progressLine.append(taskThread.getCurrentActivityString());
                        while (progressLine.length() < MAX_STATUS_STRING_LENGTH) {
                            progressLine.append(" ");
                        }
                        if (progressLine.length() > MAX_STATUS_STRING_LENGTH) {
                            progressLine.setLength(MAX_STATUS_STRING_LENGTH);
                            progressLine.setCharAt(
                                    MAX_STATUS_STRING_LENGTH - 1, '~');
                        }
                        System.err.print(progressLine.toString());
                        System.err.print('\r');
                        if (++progressAnimIndex >= progressAnimSequence.length) {
                            progressAnimIndex = 0;
                        }
                        try {
                            Thread.sleep(statusUpdateFrequencyOption.getValue());
                        } catch (InterruptedException ignored) {
                            // wake up
                        }
                    }
                    StringBuilder cleanupString = new StringBuilder();
                    for (int i = 0; i < MAX_STATUS_STRING_LENGTH; i++) {
                        cleanupString.append(' ');
                    }
                    System.err.println(cleanupString);
                    result = taskThread.getFinalResult();
                    if (!(result instanceof FailedTaskReport)) {
                        System.err.print("Task completed in "
                                + StringUtils.secondsToDHMSString(taskThread.getCPUSecondsElapsed()));
                        if (preciseTiming) {
                            System.err.print(" (CPU time)");
                        }
                        System.err.println();
                        System.err.println();
                    }
                }
                if (result instanceof FailedTaskReport) {
                    System.err.println("Task failed. Reason: ");
                    ((FailedTaskReport) result).getFailureReason().printStackTrace();
                } else {
                    if (!suppressResultOutputOption.isSet()) {
                        if (result instanceof Measurement[]) {
                            StringBuilder sb = new StringBuilder();
                            Measurement.getMeasurementsDescription(
                                    (Measurement[]) result, sb, 0);
                            System.out.println(sb.toString());
                        } else {
                            System.out.println(result);
                        }
                        System.out.flush();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
