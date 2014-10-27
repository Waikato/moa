package moa.integration;

import junit.framework.TestCase;
import moa.core.Globals;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.tasks.FailedTaskReport;
import moa.tasks.Task;
import moa.tasks.TaskThread;

import org.junit.Test;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;

/* test that all clusterers run, i.e., do not die on simpe input **/
public class SimpleClusterTest extends TestCase {

	final static String [] Clusterers = new String[]{"ClusterGenerator", "CobWeb", "KMeans", 
		"clustream.Clustream", "clustree.ClusTree", "denstream.WithDBSCAN -i 1000", "streamkm.StreamKM"};
	
	@Test
	public void testClusterGenerator(){testClusterer(Clusterers[0]);}
//	@Test
//	public void testCobWeb(){testClusterer(Clusterers[1]);}
	@Test
	public void testClustream(){testClusterer(Clusterers[3]);}
	@Test
	public void testClusTree(){testClusterer(Clusterers[4]);}
	@Test
	public void testDenStream(){testClusterer(Clusterers[5]);}
	@Test
	public void testStreamKM(){testClusterer(Clusterers[6]);}
	
	void testClusterer(String clusterer) {
		System.out.println("Processing: " + clusterer);
		try {
			doTask(new String[]{"EvaluateClustering -l " + clusterer});
		} catch (Exception e) {
			assertTrue("Failed on clusterer " + clusterer + ": " + e.getMessage(), false);
		}
	}
	
	// code copied from moa.DoTask.main, to allow exceptions to be thrown in case of failure
	public void doTask(String [] args) throws Exception {
        if (args.length < 1) {
            System.out.println();
            System.out.println(Globals.getWorkbenchInfoString());
            System.out.println();
            System.out.println("No task specified.");
        } else {
            if (moa.DoTask.isJavaVersionOK() == false ) {//|| moa.DoTask.isWekaVersionOK() == false) {
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
            Task task = (Task) ClassOption.cliStringToObject(cliString.toString(), Task.class, extraOptions);
            
            StringBuilder sb = new StringBuilder();
            task.getDescription(sb, 4);
            System.out.println(sb.toString());
            
            
            Object result = null;
            if (suppressStatusOutputOption.isSet()) {
                result = task.doTask();
            } else {
                System.out.println();
                System.out.println(Globals.getWorkbenchInfoString());
                System.out.println();
                boolean preciseTiming = TimingUtils.enablePreciseTiming();
                // start the task thread
                TaskThread taskThread = new TaskThread(task);
                taskThread.start();
                int progressAnimIndex = 0;
                // inform user of progress
                while (!taskThread.isComplete()) {
                    StringBuilder progressLine = new StringBuilder();
                    progressLine.append(moa.DoTask.progressAnimSequence[progressAnimIndex]);
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
                    while (progressLine.length() < moa.DoTask.MAX_STATUS_STRING_LENGTH) {
                        progressLine.append(" ");
                    }
                    if (progressLine.length() > moa.DoTask.MAX_STATUS_STRING_LENGTH) {
                        progressLine.setLength(moa.DoTask.MAX_STATUS_STRING_LENGTH);
                        progressLine.setCharAt(
                        		moa.DoTask.MAX_STATUS_STRING_LENGTH - 1, '~');
                    }
                    System.out.print(progressLine.toString());
                    System.out.print('\r');
                    if (++progressAnimIndex >= moa.DoTask.progressAnimSequence.length) {
                        progressAnimIndex = 0;
                    }
                    try {
                        Thread.sleep(statusUpdateFrequencyOption.getValue());
                    } catch (InterruptedException ignored) {
                        // wake up
                    }
                }
                StringBuilder cleanupString = new StringBuilder();
                for (int i = 0; i < moa.DoTask.MAX_STATUS_STRING_LENGTH; i++) {
                    cleanupString.append(' ');
                }
                System.out.println(cleanupString);
                result = taskThread.getFinalResult();
                if (!(result instanceof FailedTaskReport)) {
                    System.out.print("Task completed in "
                            + StringUtils.secondsToDHMSString(taskThread.getCPUSecondsElapsed()));
                    if (preciseTiming) {
                        System.out.print(" (CPU time)");
                    }
                    System.out.println();
                    System.out.println();
                }
            }
            
            if (result instanceof FailedTaskReport) {
                System.out.println("Task failed. Reason: ");
                ((FailedTaskReport) result).getFailureReason().printStackTrace();
                throw new Exception(((FailedTaskReport) result).getFailureReason());
            } else {
                if (!suppressResultOutputOption.isSet()) {
                    if (result instanceof Measurement[]) {
                        sb = new StringBuilder();
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

	}
	
}
