/*
 *    MultilabelArffFileStream.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *    @author Jesse Read (jmr30@cs.waikato.ac.nz)
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
package moa.streams.generators.multilabel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import moa.streams.ArffFileStream;
import moa.core.InputStreamProgressMonitor;
import moa.core.InstancesHeader;
import moa.core.MultilabelInstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.tasks.TaskMonitor;
import moa.core.MultilabelInstance;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class MultilabelArffFileStream extends ArffFileStream {

	@Override
	public String getPurposeString() {
		return "A stream read from an ARFF file.";
	}
	
	private static final long serialVersionUID = 1L;

	public IntOption numLabelsOption = new IntOption( "numLabels", 'l',
			"The number of labels. e.g. n = 10 : the first 10 binary attributes are the labels; n = -10 the last 10 binary attributes are the labels.", -1, -1, Integer.MAX_VALUE);

	public MultilabelArffFileStream() {
	}

	public MultilabelArffFileStream(String arffFileName, int numLabels) {
		this.arffFileOption.setValue(arffFileName);
		this.numLabelsOption.setValue(numLabels);
		restart();
	}

	@Override
	public InstancesHeader getHeader() {
		return new MultilabelInstancesHeader(this.instances,numLabelsOption.getValue());
	}

}
