package moa.streams;

import java.lang.invoke.SwitchPoint;
import java.util.List;
import java.util.Random;

import weka.core.pmml.FieldMetaInfo.Interval;
import weka.experiment.Compute;
import java_cup.internal_error;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.core.driftdetection.SEEDChangeDetector.SEED;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.generators.HyperplaneGenerator;
import moa.tasks.TaskMonitor;

public class MultipleConceptDriftStreamGenerator extends AbstractOptionHandler implements
InstanceStream{

	private static final long serialVersionUID = 1L;
	
    @Override
    public String getPurposeString() {
        return "Adds multiple Concept Drift to examples in a stream. (Use generators.HyperplaneGenerator as the base generator)";
    }
	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}
    
    public IntOption streamLengthOption = new IntOption("streamlen", 'l', 
    		"Length of the stream", 1000000);
    
    public IntOption numDriftsOption = new IntOption("numdrifts", 'd', 
    		"Number of Drifts in the Stream. Must be greater than 1", 1, 1, Integer.MAX_VALUE);
    
    public IntOption widthOption = new IntOption("width",
            'w', "Width of concept drift change.", 1000);
    
    public FloatOption standardDev = new FloatOption("standardDeviation", 'S', "Standard deviation for the gaussian random number used to decide the location of each drift point.", 0.0, 0.0, 2.0);
    
    public IntOption drifIntOptiontRandomSeedOption = new IntOption("driftRandomSeed", 'r',
            "Seed for generating drift streams", 1);

    
    // Options for Hyperplane Generator. In current version, the Hyperplane cannot rotate.
    public IntOption numClassesOption = new IntOption("numClasses", 'c',
            "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);
    
    public IntOption numAttsOption = new IntOption("numAtts", 'p',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);
    
    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 5, 0, 100);
    public IntOption hyperplaneRandomSeedOption = new IntOption("hyperplaneRandomSeed", 'h',
            "Seed for generating hyperplane", 1);

    protected int numberInstance;
    
    protected HyperplaneGenerator stream1;
   
    protected HyperplaneGenerator stream2;
    
    protected int switchPoint; 
    
    protected int driftPosition;
    
    protected int previousSwitchPoint;
    
    protected Random driftRandom;
    
    protected Random hyperplaneRandom;
    
	@Override
	public InstancesHeader getHeader() {
		return this.stream1.getHeader();
	}
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
		driftRandom = new Random(drifIntOptiontRandomSeedOption.getValue());
		hyperplaneRandom = new Random(hyperplaneRandomSeedOption.getValue());
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		previousSwitchPoint = 0;
		driftPosition = computeNextDriftPosition(switchPoint, previousSwitchPoint);
		
		stream1 = getNewGenerator(hyperplaneRandom.nextInt());
		stream2 = getNewGenerator(hyperplaneRandom.nextInt());
		

		
	}
	private HyperplaneGenerator getNewGenerator(int seed){
		
		HyperplaneGenerator stream = new HyperplaneGenerator();
		stream.instanceRandomSeedOption.setValue(seed);
		stream.numClassesOption = numClassesOption;
		stream.numAttsOption = numAttsOption;
		stream.noisePercentageOption = noisePercentageOption;
		stream.prepareForUse();
		
		return stream;
	}

	@Override
	public long estimatedRemainingInstances() {
		return streamLengthOption.getValue() - numberInstance;
	}

	@Override
	public boolean hasMoreInstances() {
		return numberInstance < streamLengthOption.getValue();
	}

	@Override
	public Example nextInstance() {
		
		numberInstance++;
		// perform the switch
		if(numberInstance >= switchPoint){
			stream1 = stream2;
			stream2 = getNewGenerator(hyperplaneRandom.nextInt());
			previousSwitchPoint = switchPoint;
			switchPoint += streamLengthOption.getValue() / numDriftsOption.getValue();
			driftPosition = computeNextDriftPosition(switchPoint, previousSwitchPoint);
		}
		
		
		double x = -4.0 * (double) (numberInstance - driftPosition) / (double) this.widthOption.getValue();
		double probabilityDrift = 1.0 / (1.0 + Math.exp(x));
		
        if (this.driftRandom.nextDouble() > probabilityDrift) {
            return this.stream1.nextInstance();
        } else {
            return this.stream2.nextInstance();
        }
	}
	
	public int computeNextDriftPosition(int switchPoint, int previousSwitchPoint){
		
		double gaussianRandom = driftRandom.nextGaussian()*standardDev.getValue();
		// limit the range of gaussianRandom within [-0.9, 0.9]. 
		if (gaussianRandom > 0.9)
		{
			gaussianRandom = 0.9;
		}else if (gaussianRandom < -0.9)
		{
			gaussianRandom = -0.9;
		}
		
		
		int blockCentrePoint = (switchPoint - previousSwitchPoint) / 2;
		int driftPositionMean = previousSwitchPoint + blockCentrePoint;
		int newDriftpos = driftPositionMean + (int)(blockCentrePoint * gaussianRandom); 
		
		return newDriftpos;
	}

	@Override
	public boolean isRestartable() {
		return this.stream1.isRestartable() && this.stream2.isRestartable();
	}

	@Override
	public void restart() {
		this.stream1.restart();
		this.stream2.restart();
		
		numberInstance = 0;
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		previousSwitchPoint = 0;
		
		
	}
}
