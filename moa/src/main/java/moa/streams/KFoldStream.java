package moa.streams;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.MOAObject;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.tasks.TaskMonitor;

public class KFoldStream extends AbstractOptionHandler  implements ExampleStream {


	private static final long serialVersionUID = 1L;
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to filter.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public IntOption kOption = new IntOption(
            "k", 'k',
            "Simulating the k-th fold.", 0);
    
    public IntOption numFoldsOption = new IntOption(
            "numFolds", 'n',
            "The number of simulated folds.", 1);
	
    protected ExampleStream baseStream;
    protected int k;
    protected int numFolds;
    
    @Override
	public InstancesHeader getHeader() {
		// TODO Auto-generated method stub
		return baseStream.getHeader();
	}

	@Override
	public long estimatedRemainingInstances() {
		// TODO Auto-generated method stub
		return baseStream.estimatedRemainingInstances() / (long)numFolds;
	}

	@Override
	public boolean hasMoreInstances() {
		// TODO Auto-generated method stub
		return baseStream.hasMoreInstances();
	}

	@Override
	public Example<Instance> nextInstance() {
		// TODO Auto-generated method stub
		
		Example<Instance> result = baseStream.nextInstance();
		
		for(int i = 0; i < numFolds-1; ++i)
		{
			baseStream.nextInstance();
		}
		return result;
	}

	@Override
	public boolean isRestartable() {
		// TODO Auto-generated method stub
		return baseStream.isRestartable();
	}

	@Override
	public void restart() {
		// TODO Auto-generated method stub
		baseStream.restart();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		baseStream = (ExampleStream) getPreparedClassOption(streamOption);

		for(int i = 0; i < k-1; ++i)
		{
			baseStream.nextInstance();
		}
	}

}
