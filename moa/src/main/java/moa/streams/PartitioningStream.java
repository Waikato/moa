/*
 *    ALPrequentialEvaluationTask.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
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
package moa.streams;

import java.util.Random;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * This stream partitions the base stream into n distinct streams and outputs one of them
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
public class PartitioningStream extends AbstractOptionHandler  implements ExampleStream {


	private static final long serialVersionUID = 1L;
	
	public ClassOption streamOption = new ClassOption("baseStream", 's',
            "Stream which is used for partitioning.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption partitionIndexOption = new IntOption(
            "partitionIndex", 'i',
            "The index of the partition, which should be used as output.", 0);

    public IntOption numPartitionsOption = new IntOption(
            "numPartitions", 'n',
            "The number of total streams the base stream is split into.", 1);
    
    public IntOption randomSeedOption = new IntOption(
            "randomSeed", 'r',
            "The seed which is used (all other partitions should use the same one to guarentee).", 0);
	
    protected ExampleStream baseStream;
    protected int partitionIndex;
    protected int numPartitions;
    protected Random random;
    
    @Override
	public InstancesHeader getHeader() {
		return baseStream.getHeader();
	}

	@Override
	public long estimatedRemainingInstances() {
		long baseEstimatedRemainingInstances = baseStream.estimatedRemainingInstances();
		// if the base stream is endless it will return something smaller than 0
		if(baseEstimatedRemainingInstances < 0)
		{
			return baseEstimatedRemainingInstances;
		}
		
		long numRemaining = (baseEstimatedRemainingInstances * (numPartitions - 1)) / (numPartitions);
		if(numPartitions == 1)
		{
			numRemaining = baseEstimatedRemainingInstances;
		}
		return numRemaining;
	}

	@Override
	public boolean hasMoreInstances() {
		return baseStream.hasMoreInstances();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Example<Instance> nextInstance() {
		// take the first instance from the stream which is guaranteed to be valid
		Example<Instance> result = baseStream.nextInstance();
		
		discardNexInstancesNotFromPartition();
		
		return result;
	}

	@Override
	public boolean isRestartable() {
		return baseStream.isRestartable();
	}

	@Override
	public void restart() {
		// initialize the random seed with the corresponding option
		random = new Random(randomSeedOption.getValue());
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// get parameter from options
		baseStream = (ExampleStream) getPreparedClassOption(streamOption);
		partitionIndex = partitionIndexOption.getValue();
		numPartitions = numPartitionsOption.getValue();
		
		// initialize the random seed with the corresponding option
		random = new Random(randomSeedOption.getValue());
		// discard the first instances if they do not belong to the stream
		discardNexInstancesNotFromPartition();
	}
	
	/**
	 * get the partition which is excluded from seeing the next instance
	 * @return the index of the excluded partition
	 */
	protected int getNextPartitionToLeaveOut()
	{
		return random.nextInt(numPartitions);
	}
	
	/**
	 * check if this stream is excluded from seeing the next instance
	 * @return
	 */
	protected boolean isNextInstanceFromPartition()
	{
		return getNextPartitionToLeaveOut() != partitionIndex;
	}
	
	/**
	 * discarding all instances which are exluded until an instance which can be seen by this
	 * stream or the stream is empty
	 */
	protected void discardNexInstancesNotFromPartition()
	{
		while(!isNextInstanceFromPartition() && baseStream.hasMoreInstances() && numPartitions > 1)
		{
			baseStream.nextInstance();
		}
	}

}
