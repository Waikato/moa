/*
 *    ConceptDriftStream.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet
 
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

import java.util.Random;

import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.Option;
import moa.options.OptionHandler;
import moa.streams.filters.StreamFilter;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

// Generator that adds concept drift to examples in a stream
//
// Example:
//
// ConceptDriftStream -s (generators.AgrawalGenerator -f 7) 
//    -d (generators.AgrawalGenerator -f 2) -w 1000000 -p 900000 
//
// s : Stream 
// d : Concept drift Stream
// p : Central position of concept drift change
// w : Width of concept drift change

public class ConceptDriftStream extends AbstractOptionHandler implements
		InstanceStream {

	@Override
	public String getPurposeString() {
		return "Adds Concept Drift to examples in a stream.";
	}
	
	private static final long serialVersionUID = 1L;

	public ClassOption streamOption = new ClassOption("stream", 's',
			"Stream to add concept drift.", InstanceStream.class,
			"generators.RandomTreeGenerator");

	public ClassOption driftstreamOption = new ClassOption("driftstream", 'd',
			"Concept drift Stream.", InstanceStream.class,
			"generators.RandomTreeGenerator");

	public FloatOption alphaOption = new FloatOption("alpha",
			'a', "Angle alpha of change grade.", 0.0, 0.0, 90.0);

	public IntOption positionOption = new IntOption("position",
			'p', "Central position of concept drift change.", 0);

	public IntOption widthOption = new IntOption("width",
			'w', "Width of concept drift change.", 1000);

	public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
			"Seed for random noise.", 1);	

	protected InstanceStream inputStream;

	protected InstanceStream driftStream;

	protected Random random;
	
	protected int numberInstanceStream;

	@Override
	public void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
		this.inputStream = (InstanceStream) getPreparedClassOption(this.streamOption);
		this.driftStream = (InstanceStream) getPreparedClassOption(this.driftstreamOption);
		this.random = new Random(this.randomSeedOption.getValue());
		numberInstanceStream=0;
		if (this.alphaOption.getValue() != 0.0) {
			this.widthOption.setValue((int) (1/Math.tan(this.alphaOption.getValue()*Math.PI/180)));
		}
	}

	public long estimatedRemainingInstances() {
		return this.inputStream.estimatedRemainingInstances()+this.driftStream.estimatedRemainingInstances();
	}

	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	public boolean hasMoreInstances() {
		return ( this.inputStream.hasMoreInstances() || this.driftStream.hasMoreInstances() ) ;
	}

	public boolean isRestartable() {
		return ( this.inputStream.isRestartable() && this.driftStream.isRestartable() );
	}

	public Instance nextInstance() {
		numberInstanceStream++;
		double x= -4.0 * (double)(numberInstanceStream - this.positionOption.getValue())/ (double) this.widthOption.getValue();
		double probabilityDrift= 1.0 / ( 1.0 + Math.exp ( x ));
		if (this.random.nextDouble() > probabilityDrift)
			return this.inputStream.nextInstance();
		else
			return this.driftStream.nextInstance();
		
	}

	public void restart() {
		this.inputStream.restart();
		this.driftStream.restart();
		numberInstanceStream=0;
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
