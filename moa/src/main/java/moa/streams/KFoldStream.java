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

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

/**
 * This stream simulates a k-fold crossvalidation scenario where the folds' instances are interlaced
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
public class KFoldStream extends AbstractOptionHandler  implements ExampleStream {


	private static final long serialVersionUID = 1L;
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to filter.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption foldIndexOption = new IntOption(
            "foldIndex", 'i',
            "Simulating the foldIndex-th fold.", 0);
    
    public IntOption numFoldsOption = new IntOption(
            "numFolds", 'n',
            "The number of simulated folds.", 1);
	
    protected ExampleStream baseStream;
    protected int foldIndex;
    protected int numFolds;
    protected int foldCounter;
    
    @Override
	public InstancesHeader getHeader() {
		return baseStream.getHeader();
	}

	@Override
	public long estimatedRemainingInstances() {
		return (baseStream.estimatedRemainingInstances() * (long)(numFolds+1))/(long)numFolds;
	}

	@Override
	public boolean hasMoreInstances() {
		return baseStream.hasMoreInstances();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Example<Instance> nextInstance() {
		// take the first instance from the stream which is guarenteed to be valid
		Example<Instance> result = baseStream.nextInstance();
		
		// discard the next instance if the corresponding fold is the one to leave out
		foldCounter = (foldCounter + 1)%numFolds;
		if(foldCounter == foldIndex)
		{
			baseStream.nextInstance();
			foldCounter = (foldCounter + 1)%numFolds;
		}
		
		return result;
	}

	@Override
	public boolean isRestartable() {
		return baseStream.isRestartable();
	}

	@Override
	public void restart() {
		baseStream.restart();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// get parameter from options
		baseStream = (ExampleStream) getPreparedClassOption(streamOption);
		foldIndex = foldIndexOption.getValue();
		numFolds = numFoldsOption.getValue();
		
		// initialize the fold counter to keep track at which fold the next instance belongs
		foldCounter = 0;
		// discard the first instance if the fold index is zero
		if(foldCounter == foldIndex)
		{
			baseStream.nextInstance();
			foldCounter = (foldCounter + 1)%numFolds;
		}
	}

}
