package moa.streams;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.LinkedList;

/**
 * Helper class for pipelines in CapyMOA.
 * Implements the stream interface and also acts as a queue by supporting `addToQueue`.
 *
 * @author Marco Heyden (marco.heyden@kit.edu)
 * @version $Revision: 1 $
 */
public class QueueStream extends AbstractOptionHandler implements ExampleStream {

    private LinkedList<InstanceExample> queue = new LinkedList<>();

    void addToQueue(Instance newInstance) {
        queue.add(new InstanceExample(newInstance));
    }


    @Override
    public long measureByteSize() {
        return 0;
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {}

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }

    @Override
    public InstancesHeader getHeader() {
        return null;
    }

    @Override
    public long estimatedRemainingInstances() {
        return queue.size();
    }

    @Override
    public boolean hasMoreInstances() {
        return queue.size() > 0;
    }

    @Override
    public InstanceExample nextInstance() {
        return queue.removeFirst();
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
        queue = new LinkedList<>();
    }
}
