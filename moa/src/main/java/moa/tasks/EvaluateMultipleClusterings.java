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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.opencsv.*;

import moa.clusterers.AbstractClusterer;
import moa.core.ObjectRepository;
import moa.gui.visualization.RunVisualizer;
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

	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv reslts to.", "dumpClustering.csv", "csv", true);
	
	public FlagOption mergeResultsOption = new FlagOption("mergeResults", 'm', 
			"Produce a csv file that contains all of the individual results merged together.");
	
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
		
		String mergedFile = this.dumpFileOption.getValueAsCLIString();
		String tempFile = mergedFile.substring(0, mergedFile.length()-4).concat("*.csv");
		
		for(int i = 0 ; i < this.numStreamsOption.getValue() ; i++)
		{
			// Get Base task
            this.task = new EvaluateClustering();
            
            // Learner
            this.task.learnerOption.setValueViaCLIString(this.learnerOption.getValueAsCLIString());
            
            // Build stream
            fStream = (FileStream) getPreparedClassOption(this.streamOption);
            arffFile = fStream.arffFileOption.getValueAsCLIString();
            arffFile = arffFile.substring(0, arffFile.lastIndexOf('n')+1).concat(i+".arff");
            fStream.arffFileOption.setValueViaCLIString(arffFile);
            this.task.streamOption.setValueViaCLIString(fStream.getCLICreationString(fStream.getClass()));
            
            // Build Output File
            outputFile = this.dumpFileOption.getValueAsCLIString();
            if(outputFile.endsWith(".csv"))
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
            
            if(this.mergeResultsOption.isSet())
            {
            	System.out.println("Merge "+mergedFile+" and "+outputFile);
            	if (i == 0)
            		writeToMergeFile(mergedFile, outputFile);
            	else
            		mergeFiles(mergedFile, outputFile, tempFile);
            }
		}

		return result;
	}
	
	private void writeToMergeFile(String mergedFile, String outputFile)
	{
		CSVWriter writer = null;
		CSVParser parser = null;
		CSVReader reader = null;
		
		//System.out.println("WTMF: "+mergedFile+" <-- "+outputFile);
		
		try
		{
			// Prepare an output file			
			if (!mergedFile.endsWith(".csv"))
			{
				mergedFile += ".csv";
			}

			writer = new CSVWriter(new FileWriter(mergedFile),';', '\"', '\\', "\n");
			parser = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(false).build();
			reader = new CSVReaderBuilder(new FileReader(outputFile)).withCSVParser(parser).build();
			
			String[] nextLine = reader.readNext();
			while(nextLine != null)
			{
				writer.writeNext(nextLine);
				nextLine = reader.readNext();
			}
		}
		catch (IOException ex)
		{
			Logger.getLogger(RunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
		}
		finally
		{
			try
			{
				writer.close();
				reader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void mergeFiles(String mergedFile, String outputFile, String tempFile)
	{
		CSVWriter writer = null;
		CSVParser parser1 = null;
		CSVReader reader1 = null;
		CSVParser parser2 = null;
		CSVReader reader2 = null;

		//System.out.println("mergeFiles "+mergedFile+" * "+outputFile+" (temp: "+tempFile+")");
		
		try
		{
			// Prepare an output file			
			if (!mergedFile.endsWith(".csv"))
			{
				mergedFile += ".csv";
			}
			writer = new CSVWriter(new FileWriter(tempFile),';', '\"', '\\', "\n");
			parser1 = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(false).build();
			reader1 = new CSVReaderBuilder(new FileReader(mergedFile)).withCSVParser(parser1).build();
			parser2 = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(false).build();
			reader2 = new CSVReaderBuilder(new FileReader(outputFile)).withCSVParser(parser2).build();


			int state = 0;
			
			String[] nextLine1 = reader1.readNext();
			int mergedWidth = nextLine1.length;
			String[] nextLine2 = reader2.readNext();
			int outputWidth = nextLine2.length;
			String[] nextLine = mergeArrays(nextLine1, nextLine2);
			
			writer.writeNext(nextLine);

			// While both mergedFile and outputFile have lines remaining
			while(state == 0)
			{
				nextLine1 = reader1.readNext();
				if(nextLine1 == null)
				{
					nextLine1 = new String[mergedWidth];
					state += 1;
				}
				
				nextLine2 = reader2.readNext();
				if(nextLine2 == null)
				{
					nextLine2 = new String[outputWidth];
					state += 2;
				}
				
				if(state == 3)
				{
					break;
				}
				
				nextLine = mergeArrays(nextLine1, nextLine2);
				writer.writeNext(nextLine);
			}
			
			// While only outputFile has lines remaining
			while(state == 1)
			{
				nextLine2 = reader2.readNext();
				if(nextLine == null)
				{
					break;
				}
				else
				{
					nextLine1 = new String[mergedWidth];
					nextLine = mergeArrays(nextLine1, nextLine2);
					writer.writeNext(nextLine);
				}
			}
			
			// While only mergedFile has lines remaining
			while(state == 2)
			{
				nextLine1 = reader1.readNext();
				if(nextLine == null)
				{
					break;
				}
				else
				{
					nextLine2 = new String[outputWidth];
					nextLine = mergeArrays(nextLine1, nextLine2);
					writer.writeNext(nextLine);
				}
			}
		}
		catch (IOException ex)
		{
			Logger.getLogger(RunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
		}
		finally
		{
			try
			{
				writer.close();
				reader1.close();
				reader2.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		writeToMergeFile(mergedFile, tempFile);
	}

	private String[] mergeArrays(String[] nextLine1, String[] nextLine2)
	{
		int length1 = nextLine1.length;
		int length2 = nextLine2.length;
				
		if(nextLine1[nextLine1.length-1].equals(""))
			length1--;
		if(nextLine2[nextLine2.length-1].equals(""))
			length2--;
		
		String[] nextLine = new String[length1+length2];
		for(int i = 0 ; i < length1 ; i++)
		{
			nextLine[i] = nextLine1[i];
		}
		for(int i = 0 ; i < length2 ; i++)
		{
			nextLine[length1+i] = nextLine2[i];
		}
		
		return nextLine;
	}

}
