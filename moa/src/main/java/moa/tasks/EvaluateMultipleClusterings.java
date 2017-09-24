/*
 *    EvaluateMultipleClusterings.java
 *    Copyright (C) 2017 Richard Hugh Moulton
 *    @author Richard Hugh Moulton
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

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;

import moa.MOAObject;
import moa.clusterers.AbstractClusterer;
import moa.core.ObjectRepository;
import moa.evaluation.LearningCurve;
import moa.gui.BatchCmd;
import moa.options.ClassOption;
import moa.streams.clustering.ClusteringStream;
import moa.streams.clustering.FileStream;

/**
 * Task for evaluating a clusterer on multiple (related) streams.
 *
 * @author Richard Hugh Moulton
 */
public class EvaluateMultipleClusterings extends AuxiliarMainTask {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4846907962483608940L;

	@Override
	public String getPurposeString()
	{
		return "Evaluates a clusterer on multiple (related) streams.";
	}

	String defaultfile = "covtypeNorm.arff";
	
	public ClassOption learnerOption = new ClassOption("learner", 'l',
			"Clusterer to train.", AbstractClusterer.class, "clustream.Clustream");

	public ClassOption streamOption = new ClassOption("stream", 's',
            "Base stream to learn from (must use FileStream).",  ClusteringStream.class,
            "FileStream");

	public IntOption numStreamsOption = new IntOption("numStreams", 'n',
			"The number of streams to iterate through (must be named according to WriteMultipleStreamsToARFF format.",
			100, 2, Integer.MAX_VALUE);

	public FlagOption generalEvalOption = new FlagOption("General", 'g',
			"GPrecision, GRecall, Redundancy, numCluster, numClasses");
   
    public FlagOption f1Option = new FlagOption("F1", 'f', "F1-P, F1-R, Purity.");
    
    public FlagOption entropyOption = new FlagOption("Entropy", 'e',
			"GT cross entropy, FC cross entropy, Homogeneity, Completeness, V-Measure, VarInformation.");
    
    public FlagOption cmmOption = new FlagOption("CMM", 'c',
			"CMM, CMM Basic, CMM Missed, CMM Misplaced, CMM Noise, CA Seperability, CA Noise, CA Model.");

    public FlagOption ssqOption = new FlagOption("SSQ", 'q', "SSQ.");
    
    public FlagOption separationOption = new FlagOption("Separation", 'p', "BSS, BSS-GT, BSS-Ratio.");
    
    public FlagOption silhouetteOption = new FlagOption("Silhouette", 'h', "SilhCoeff.");
    
    public FlagOption statisticalOption = new FlagOption("Statistical", 't', "van Dongen, Rand statistic.");

	
	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv reslts to.", "dumpClustering.csv", "csv", true);

	protected EvaluateClustering task;
	
	@Override
	public Class<?> getTaskResultType()
	{
		return Object.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository)
	{
		Object result = null;
		String arffFile, outputFile;
		FileStream fStream;
		
		for(int i = 0 ; i < this.numStreamsOption.getValue() ; i++)
		{
			// Get Base task
            this.task = new EvaluateClustering();
            
            // Learner
            this.task.learnerOption.setValueViaCLIString(this.learnerOption.getValueAsCLIString());
            
            // Build stream
            fStream = (FileStream) getPreparedClassOption(this.streamOption);
            arffFile = fStream.arffFileOption.getValueAsCLIString();
            arffFile = arffFile.substring(0, arffFile.length()-6).concat(i+".arff");
            fStream.arffFileOption.setValueViaCLIString(arffFile);
            this.task.streamOption.setValueViaCLIString(fStream.getCLICreationString(fStream.getClass()));
            
            // Build Output File
            outputFile = this.dumpFileOption.getValueAsCLIString();
            if(outputFile.substring(outputFile.length()-4, outputFile.length()).equals(".csv"))
			{
            	outputFile = outputFile.substring(0, outputFile.length()-4);
			}
            outputFile = outputFile.concat("n"+i+".csv");
            this.task.dumpFileOption.setValueViaCLIString(outputFile);
            
            // Instance Limit
            this.task.instanceLimitOption.setValue(-1);
            
            // Measure Collection
            // Create an array to summarize the selected measures
        	boolean[] measureCollection = new boolean[8];
        	measureCollection[0] = this.generalEvalOption.isSet();
        	measureCollection[1] = this.f1Option.isSet();
        	measureCollection[2] = this.entropyOption.isSet();
        	measureCollection[3] = this.cmmOption.isSet();
        	measureCollection[4] = this.ssqOption.isSet();
        	measureCollection[5] = this.separationOption.isSet();
        	measureCollection[6] = this.silhouetteOption.isSet();
        	measureCollection[7] = this.statisticalOption.isSet();
            
            this.task.setMeasures(measureCollection);
            
            System.out.println("Evaluation #"+(i+1)+" of "+this.numStreamsOption.getValue()+
            		": "+this.task.getCLICreationString(this.task.getClass()));
            
            //Run task
            result = this.task.doTask(monitor, repository);
		}

		return result;
	}

}
