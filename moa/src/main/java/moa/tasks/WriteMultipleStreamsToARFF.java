/*
 *    WriteMultipleStreamsToARFF.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.StringOption;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.InstanceStream;
import moa.streams.generators.RandomRBFGenerator;

/**
 * Task to output multiple streams to a ARFF files using different random seeds
 *
 * @author Richard Hugh Moulton
 */
public class WriteMultipleStreamsToARFF extends AuxiliarMainTask
{

	private static final long serialVersionUID = 4687495597494400174L;

	// 121 prime and semi-prime numbers to use as the seeds for random number generators
	private static final int[] primesAndBiprimes = {0,1,2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,
			 61,67,71,73,79,83,89,97,101,103,107,109,113,127,
			 131,137,139,149,151,157,163,167,173,179,181,191,
			 193,197,199,211,223,227,229,233,239,241,251,257,
			 263,269,271,4,6,9,10,14,15,21,22,25,26,33,34,35,
			 38,39,46,49,51,55,57,58,62,65,69,74,77,82,85,86,
			 87,91,93,94,95,106,111,115,118,119,121,122,123,129,
			 133,134,141,142,143,145,146,155,158,159,161,166,
			 169,177,178,183,185,187};
	
	@Override
	public String getPurposeString()
	{
		return "Outputs multiple streams to their respective ARFF files.";
	}

	public ClassOption streamOption = new ClassOption("stream", 's',
			"Stream to write.", InstanceStream.class,
			"generators.RandomTreeGenerator");

	public FileOption arffFileOption = new FileOption("arffFile", 'a',
			"Destination ARFF file.", null, "arff", true);

	public IntOption maxInstancesOption = new IntOption("maxInstances", 'm',
			"Maximum number of instances to write to file.", 10000000, 0,
			Integer.MAX_VALUE);

	public FlagOption suppressHeaderOption = new FlagOption("suppressHeader",
			'h', "Suppress header from output.");

	public IntOption numStreamsOption = new IntOption("numStreams", 'n',
			"Number of streams to generate and write to file.", 100, 0, Integer.MAX_VALUE);
	
	public FlagOption randomFlagOne = new FlagOption("randomFlagOne", 'f',
			"Check in order to provide the first random option with different seeds.");
	
	public StringOption randomOneOption = new StringOption("randomOptionOne", '1',
            "Random option to provide with different seed values.", "m");
	
	public FlagOption randomFlagTwo = new FlagOption("randomFlagTwo", 'F',
			"Check in order to provide the second random option with different seeds.");
	
	public StringOption randomTwoOption = new StringOption("randomOptionTwo", '2',
            "Random option to provide with different seed values.", "i");

	protected InstanceStream stream;
	
	
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository)
	{
		// Store the original values of these two options, which may be altered during the remainder of this task.
		String streamCLI = this.streamOption.getValueAsCLIString();
		String fileCLI = this.arffFileOption.getValueAsCLIString();
		stream = (InstanceStream) getPreparedClassOption(this.streamOption);
		
		// For each stream to be created...
		for(int i = 0 ; i < this.numStreamsOption.getValue() ; i++)
		{
			// ...provide different seeds for each of the selected random processes...
			String s = streamCLI;
			if(this.randomFlagOne.isSet())
			{
				s = s.concat(" -"+randomOneOption.getValue()+" "+WriteMultipleStreamsToARFF.primesAndBiprimes[i]);
			}
			if(this.randomFlagTwo.isSet())
			{
				s = s.concat(" -"+randomTwoOption.getValue()+" "+
			WriteMultipleStreamsToARFF.primesAndBiprimes[i/WriteMultipleStreamsToARFF.primesAndBiprimes.length]);
			}
			this.streamOption.setValueViaCLIString(s);
			
			// ...update the file name of the ARFF file to output...
			s = fileCLI;
			if(s.substring(s.length()-5, s.length()).equals(".arff"))
			{
				s = s.substring(0, s.length()-5);
			}
			s = s.concat("n"+i+".arff");
			this.arffFileOption.setValueViaCLIString(s);
			File destFile = this.arffFileOption.getFile();
			
			// ...and attempt to write the file.
			if (destFile != null)
			{
				try
				{
					Writer w = new BufferedWriter(new FileWriter(destFile));
					System.out.println("Writing stream to ARFF: "+this.streamOption.getValueAsCLIString()+" to "+this.arffFileOption.getValueAsCLIString());
					monitor.setCurrentActivityDescription("Writing stream to ARFF: "+this.streamOption.getValueAsCLIString()+" to "+this.arffFileOption.getValueAsCLIString());
					if (!this.suppressHeaderOption.isSet())
					{
						w.write(stream.getHeader().toString());
						w.write("\n");
					}
					int numWritten = 0;
					while ((numWritten < this.maxInstancesOption.getValue())
							&& stream.hasMoreInstances())
					{
						w.write(stream.nextInstance().getData().toString());
						w.write("\n");
						numWritten++;
					}
					w.close();
				}
				catch (Exception ex)
				{
					throw new RuntimeException("Failed writing to file " + destFile, ex);
				}

			}
		}
		return "Streams written to ARFF files";
	}

	@Override
	public Class<?> getTaskResultType()
	{
		return String.class;
	}
}
