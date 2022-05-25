package moa.streams;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.InputStreamProgressMonitor;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.clustering.ClusterEvent;
import moa.streams.generators.cd.ConceptDriftGenerator;
import moa.tasks.TaskMonitor;

/**
 * Stream reader of ARFF files.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ArffFileStream extends AbstractOptionHandler
		implements InstanceStream, ConceptDriftGenerator, CapabilitiesHandler {

	@Override
	public String getPurposeString() {
		return "A stream read from an ARFF file.";
	}

	private static final long serialVersionUID = 1L;

	public FileOption arffFileOption = new FileOption("arffFile", 'f', "ARFF file to load.", null, "arff", false);

	public StringOption outputIndexesOption = new StringOption("outputIndexes", 'c',
			"Indices of output (class) attributes. Can be provided in a comma or semicolon separated list of single values or ranges.",
			"-1");

	public StringOption inputIndexesOption = new StringOption("inputIndexes", 'i',
			"Indices of input (class) attributes. Can be provided in a comma or semicolon separated list of single values or ranges. Leave blank for all non-output attributes.",
			"");

	protected InstancesHeader instances;

	protected Reader fileReader;

	protected boolean hitEndOfFile;

	protected InstanceExample lastInstanceRead;

	protected int numInstancesRead;

	protected InputStreamProgressMonitor fileProgressMonitor;

	public ArffFileStream() {
	}

	public ArffFileStream(String arffFileName, int classIndex) {
		this.arffFileOption.setValue(arffFileName);
		this.outputIndexesOption.setValue(Integer.toString(classIndex));
		this.inputIndexesOption.setValue("");
		restart();
	}

	public ArffFileStream(String arffFileName, String outputIndexes) {
		this.arffFileOption.setValue(arffFileName);
		this.outputIndexesOption.setValue(outputIndexes);
		restart();
	}

	public ArffFileStream(String arffFileName, String outputIndexes, String inputIndexes) {
		this.arffFileOption.setValue(arffFileName);
		this.outputIndexesOption.setValue(outputIndexes);
		this.inputIndexesOption.setValue(inputIndexes);
		restart();
	}

	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		restart();
	}

	@Override
	public InstancesHeader getHeader() {
		return new InstancesHeader(this.instances);
	}

	@Override
	public long estimatedRemainingInstances() {
		double progressFraction = this.fileProgressMonitor.getProgressFraction();
		if ((progressFraction > 0.0) && (this.numInstancesRead > 0)) {
			return (long) ((this.numInstancesRead / progressFraction) - this.numInstancesRead);
		}
		return -1;
	}

	@Override
	public boolean hasMoreInstances() {
		return !this.hitEndOfFile;
	}

	@Override
	public InstanceExample nextInstance() {
		InstanceExample prevInstance = this.lastInstanceRead;
		this.hitEndOfFile = !readNextInstanceFromFile();
		return prevInstance;
	}

	@Override
	public boolean isRestartable() {
		return true;
	}

	@Override
	public void restart() {
		try {
			if (this.fileReader != null) {
				this.fileReader.close();
			}
			InputStream fileStream = new FileInputStream(this.arffFileOption.getFile());
			this.fileProgressMonitor = new InputStreamProgressMonitor(fileStream);
			this.fileReader = new BufferedReader(new InputStreamReader(this.fileProgressMonitor));
			this.instances = new InstancesHeader(new Instances(this.fileReader, this.outputIndexesOption.getValue(),
					this.inputIndexesOption.getValue()));
			this.numInstancesRead = 0;
			this.lastInstanceRead = null;
			this.hitEndOfFile = !readNextInstanceFromFile();
		} catch (IOException ioe) {
			throw new RuntimeException("ArffFileStream restart failed.", ioe);
		}
	}

	protected boolean readNextInstanceFromFile() {
		try {
			if (this.instances.readInstance(this.fileReader)) {
				this.lastInstanceRead = new InstanceExample(this.instances.instance(0));
				this.instances.delete(); // keep instances clean
				this.numInstancesRead++;
				return true;
			}
			if (this.fileReader != null) {
				this.fileReader.close();
				this.fileReader = null;
			}
			return false;
		} catch (IOException ioe) {
			throw new RuntimeException("ArffFileStream failed to read instance from stream.", ioe);
		}
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
	}

	protected ArrayList<ClusterEvent> clusterEvents;

	@Override
	public ArrayList<ClusterEvent> getEventsList() {
		// This is used only in the CD Tab
		return this.clusterEvents;
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == ArffFileStream.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}