package driftmodelintegration.core;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import sizeof.agent.SizeOfAgent;

/**
 * <p>
 * This class is the default implementation of the Data item. The complete
 * implementation is based upon Java's core LinkedHashMap implementation.
 * </p>
 * <p>
 * Objects of this class should not be created directory, but rather by using a
 * {@link stream.data.DataFactory}.
 * </p>
 * 
 * @author Christian Bockermann &lt;christian.bockermann@udo.edu&gt;
 * 
 */
public class DataImpl extends LinkedHashMap<String, Serializable> implements Data {
    /** The unique class ID */
    private static final long serialVersionUID = -7751681008628413236L;

    public DataImpl() {
    }

    /**
     * @param data
     */
    public DataImpl(Map<String, Serializable> data) {
        super(data);
    }

    /**
     * @return 
     * @see stream.Measurable#getByteSize()
     */
    public double getByteSize() {
        double size = 0.0d;

        for (String key : keySet()) {
            size += key.length() + 1; // provide the rough size of one byte for
                                      // each character + a single terminating
                                      // 0-byte

            // add the size of each value of this map
            Serializable value = get(key);
            size += SizeOfAgent.fullSizeOf(value);
        }

        return size;
    }
}