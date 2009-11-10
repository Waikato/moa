/*
 *    AbstractMOAObject.java
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
package moa;

import sizeof.agent.SizeOfAgent;
import moa.core.SerializeUtils;
import moa.core.SizeOf;

public abstract class AbstractMOAObject implements MOAObject {

	public MOAObject copy() {
		return copy(this);
	}

	public int measureByteSize() {
		return measureByteSize(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		getDescription(sb, 0);
		return sb.toString();
	}

	public static MOAObject copy(MOAObject obj) {
		try {
			return (MOAObject) SerializeUtils.copyObject(obj);
		} catch (Exception e) {
			throw new RuntimeException("Object copy failed.", e);
		}
	}

	public static int measureByteSize(MOAObject obj) {
		return (int) SizeOf.sizeOf(obj);
	}

}
