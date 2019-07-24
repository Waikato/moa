package moa.streams.filters;

import com.yahoo.labs.samoa.instances.InstancesHeader;

public class RemoveNumericalAttributeFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Removes discrete attribute examples in a stream.";
    }

    private static final long serialVersionUID = 1L;

    protected InstancesHeader streamHeader;

    @Override
    protected void restartImpl() {

    }

    @Override
    public InstancesHeader getHeader() {
        return null;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
