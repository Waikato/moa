package moa.streams.filters;

import moa.streams.InstanceStream;
import moa.streams.MultiTargetInstanceStream;

public interface MultiLabelStreamFilter extends MultiTargetInstanceStream {

    /**
     * Sets the input stream to the filter
     *
     * @param stream the input stream to the filter
     */
    public void setInputStream(InstanceStream stream);
}
