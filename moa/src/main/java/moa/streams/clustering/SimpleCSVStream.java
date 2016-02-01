/*
 *    SimpleCSVStream.java
 *    Copyright (C) 2015 TU Dortmund University, Germany
 *    @author Jan Stallmann (jan.stallmann@tu-dortmund.de)
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
package moa.streams.clustering;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.InputStreamProgressMonitor;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

/**
 * Provides a simple input stream for csv files. Adds if necessary a class
 * attribute with an identical default value.
 *
 */
public class SimpleCSVStream extends ClusteringStream {

	private static final long serialVersionUID = 1L;

	String defaultfile = "/Users/kokomo40/Dropbox/BT Kim/Datasets/KDDCUP99/KDDCup99.arff";

	public FileOption csvFileOption = new FileOption("csvFile", 'f',
			"CSV file to load.", defaultfile, "csv", false);

	public StringOption splitCharOption = new StringOption("splitChar", 's',
			"Input CSV split character", ",");

	public FlagOption classIndexOption = new FlagOption("classIndex", 'c',
			"Last attribute is class index.");

	protected Instances dataset;

	protected BufferedReader fileReader;

	protected boolean hitEndOfFile;

	protected InstanceExample lastInstanceRead;

	protected int numInstancesRead;

	protected int numTokens;

	protected int numAttributes;

	protected InputStreamProgressMonitor fileProgressMonitor;

	/**
	 * Creates a simple ClusteringStream for csv files. Adds if necessary a
	 * class attribute with an identical default value.
	 * 
	 */
	public SimpleCSVStream() {
		this.numAttsOption = null;
		this.dataset = null;
		this.fileReader = null;
		this.hitEndOfFile = false;
		this.lastInstanceRead = null;
		this.numInstancesRead = 0;
		this.numTokens = 0;
		this.numAttributes = 0;
		this.fileProgressMonitor = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.options.AbstractOptionHandler#getPurposeString()
	 */
	@Override
	public String getPurposeString() {
		return "A stream read from an CSV file.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * moa.options.AbstractOptionHandler#prepareForUseImpl(moa.tasks.TaskMonitor
	 * , moa.core.ObjectRepository)
	 */
	@Override
	public void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		restart();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#getHeader()
	 */
	@Override
	public InstancesHeader getHeader() {
		return new InstancesHeader(this.dataset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#estimatedRemainingInstances()
	 */
	@Override
	public long estimatedRemainingInstances() {
		double progressFraction = this.fileProgressMonitor.getProgressFraction();
		if ((progressFraction > 0.0) && (this.numInstancesRead > 0)) {
			return (long) ((this.numInstancesRead / progressFraction) - this.numInstancesRead);
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#hasMoreInstances()
	 */
	@Override
	public boolean hasMoreInstances() {
		return !this.hitEndOfFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#nextInstance()
	 */
	@Override
	public InstanceExample nextInstance() {
		InstanceExample prevInstance = this.lastInstanceRead;
		if (prevInstance != null) {
			this.numInstancesRead++;
		}
		try {
			String line;
			do {
				line = this.fileReader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
			} while (line.isEmpty() || line.charAt(0) == '%'
					|| line.charAt(0) == '@');

			if (line != null) {
				StringTokenizer token = new StringTokenizer(line,
						splitCharOption.getValue());

				double[] value = new double[this.numAttributes];
				int i;
				for (i = 0; i < this.numTokens && token.hasMoreTokens(); i++) {
					value[i] = Double.valueOf(token.nextToken());
				}
				if (i < this.numTokens || token.hasMoreTokens()) {
					throw new RuntimeException(
							"Next Instance has an wrong cardinality!");
				}
				this.lastInstanceRead = new InstanceExample(new DenseInstance(1, value));
				this.lastInstanceRead.getData().setDataset(this.dataset);
				this.hitEndOfFile = false;
			} else {
				this.lastInstanceRead = null;
				this.hitEndOfFile = true;
			}
		} catch (IOException ioe) {
			throw new RuntimeException("Read next Instance failed.", ioe);
		}
		return prevInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#isRestartable()
	 */
	@Override
	public boolean isRestartable() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.streams.InstanceStream#restart()
	 */
	@Override
	public void restart() {
		try {
			if (fileReader != null) {
				fileReader.close();
			}
			InputStream fileStream = new FileInputStream(
					this.csvFileOption.getFile());
			this.fileProgressMonitor = new InputStreamProgressMonitor(
					fileStream);
			this.fileReader = new BufferedReader(new InputStreamReader(
					fileProgressMonitor));

			String line;
			do {
				line = this.fileReader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
			} while (line.isEmpty() || line.charAt(0) == '%'
					|| line.charAt(0) == '@');

			if (line != null) {
				StringTokenizer token = new StringTokenizer(line,
						splitCharOption.getValue());
				this.numTokens = token.countTokens();
				this.numAttributes = this.numTokens
						- (classIndexOption.isSet() ? 1 : 0) + 1;

				ArrayList<Attribute> attributes = new ArrayList<Attribute>(
						this.numAttributes);
				for (int i = 1; i < this.numAttributes; i++) {
					attributes.add(new Attribute("Dim " + i));
				}
				ArrayList<String> classLabels = new ArrayList<String>();
				classLabels.add("0");
		        attributes.add(new Attribute("class", classLabels));
				this.dataset = new Instances(csvFileOption.getFile().getName(),
						attributes, 0);
				this.dataset.setClassIndex(this.numAttributes - 1);
				numAttsOption = new IntOption("numAtts", 'a', "",
						this.numAttributes);

				double[] value = new double[this.numAttributes];
				for (int i = 0; i < this.numTokens && token.hasMoreTokens(); i++) {
					value[i] = Double.valueOf(token.nextToken());
				}
				this.lastInstanceRead = new InstanceExample(new DenseInstance(1, value));
				this.lastInstanceRead.getData().setDataset(this.dataset);
				this.numInstancesRead = 0;
				this.hitEndOfFile = false;
			} else {
				this.lastInstanceRead = null;
				this.numInstancesRead = 0;
				this.hitEndOfFile = true;
			}
		} catch (IOException ioe) {
			throw new RuntimeException("SimpleCSVStream restart failed.", ioe);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see moa.MOAObject#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		sb.append("SimpleCSVStream: ");
		sb.append(csvFileOption.getFile().getName());
		sb.append("\t NumInstancesRead: ");
		sb.append(this.numInstancesRead);
		sb.append("\t HitEndOfFile: ");
		sb.append(this.hitEndOfFile);
	}

}
