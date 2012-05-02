/*
 *    InstanceStream.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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

import moa.MOAObject;
import moa.core.InstancesHeader;
import weka.core.Instance;

/**
 * Interface representing a data stream of instances. 
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $ 
 */
public interface InstanceStream extends MOAObject {

    /**
     * Gets the header of this stream.
     * This is useful to know attributes and classes.
     * InstancesHeader is an extension of weka.Instances.
     *
     * @return the header of this stream
     */
    public InstancesHeader getHeader();

    /**
     * Gets the estimated number of remaining instances in this stream
     *
     * @return the estimated number of instances to get from this stream
     */
    public long estimatedRemainingInstances();

    /**
     * Gets whether this stream has more instances to output.
     * This is useful when reading streams from files.
     *
     * @return true if this stream has more instances to output
     */
    public boolean hasMoreInstances();

    /**
     * Gets the next instance from this stream.
     *
     * @return the next instance of this stream
     */
    public Instance nextInstance();

    /**
     * Gets whether this stream can restart.
     *
     * @return true if this stream can restart
     */
    public boolean isRestartable();

    /**
     * Restarts this stream. It must be similar to
     * starting a new stream from scratch.
     *
     */
    public void restart();
}
