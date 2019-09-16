/*
 *    ExampleStream.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.streams;

import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.MOAObject;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;

/**
 * Interface representing a data stream of examples.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public interface ExampleStream<E extends Example> extends MOAObject, CapabilitiesHandler {

	/**
	 * Gets the header of this stream. This is useful to know attributes and
	 * classes. InstancesHeader is an extension of weka.Instances.
	 *
	 * @return the header of this stream
	 */
	InstancesHeader getHeader();

	/**
	 * Gets the estimated number of remaining instances in this stream
	 *
	 * @return the estimated number of instances to get from this stream
	 */
	long estimatedRemainingInstances();

	/**
	 * Gets whether this stream has more instances to output. This is useful when
	 * reading streams from files.
	 *
	 * @return true if this stream has more instances to output
	 */
	boolean hasMoreInstances();

	/**
	 * Gets the next example from this stream.
	 *
	 * @return the next example of this stream
	 */
	E nextInstance();

	/**
	 * Gets whether this stream can restart.
	 *
	 * @return true if this stream can restart
	 */
	boolean isRestartable();

	/**
	 * Restarts this stream. It must be similar to starting a new stream from
	 * scratch.
	 *
	 */
	void restart();

	@Override
	default ImmutableCapabilities defineImmutableCapabilities() {
		// We are restricting streams based on view mode
		return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}
