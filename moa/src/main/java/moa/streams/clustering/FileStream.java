/**
 * [FileStream.java]
 * 
 * @author Timm Jansen
 * @editor Yunsu Kim
 * 
 * Last Edited: 2013/06/27
 * Data Management and Data Exploration Group, RWTH Aachen University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.streams.clustering;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import moa.core.InputStreamProgressMonitor;
import moa.core.InstanceExample;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.ObjectRepository;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.Option;
import moa.tasks.TaskMonitor;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

public class FileStream extends ClusteringStream{

	@Override
	public String getPurposeString() {
		return "A stream read from an ARFF file. HINT: Visualization only works correctly with numerical 0-1 normalized attributes!";
	}

	private static final long serialVersionUID = 1L;


    String defaultfile = "KDDCup99.arff";

	public FileOption arffFileOption = new FileOption("arffFile", 'f',
			"ARFF file to load.", defaultfile, "arff", false);

	public IntOption classIndexOption = new IntOption(
			"classIndex",
			'c',
			"Class index of data. 0 for none or -1 for last attribute in file.",
			-1, -1, Integer.MAX_VALUE);

    public FlagOption normalizeOption = 
    		new FlagOption("normalize", 'n', 
    				"Numerical data will be normalized to 0-1 " +
    				"for the visualization to work. The complete arff file needs to be read upfront.");

    public ListOption removeAttributesOption = new ListOption("removeAttributes", 'r',
            "Attributes to remove. Enter comma seperated list, " +
            "starting with 1 for first attribute.", 
            new IntOption("removeAttribute", ' ', "Attribute to remove.",-1),
            new Option[0], ',');	
	
    public FlagOption keepNonNumericalAttrOption = 
    		new FlagOption("keepNonNumericalAttr", 'K',
    		"Non-numerical attributes are being filtered by default " +
    		"(except the class attribute). " +
    		"Check to keep all attributes. This option is being " +
    		"overwritten by the manual attribute removal filter.");
	

    
  
	protected Instances instances;

	protected Reader fileReader;

	protected boolean hitEndOfFile;

    protected InstanceExample lastInstanceRead;

	protected int numInstancesRead;

	protected InputStreamProgressMonitor fileProgressMonitor;
	
	private Integer[] removeAttributes = null;
    
	private Instances filteredDataset = null;
    
	private ArrayList<Double[]> valuesMinMaxDiff = null;

	
	public FileStream(){
		//remove numAttritube Option from ClusteringStream as that is being set internally for Filestream
		numAttsOption = null;
	}
	
	@Override
	public void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		restart();
	}

	public InstancesHeader getHeader() {
		return new InstancesHeader(this.filteredDataset);
	}

	public long estimatedRemainingInstances() {
		double progressFraction = this.fileProgressMonitor
				.getProgressFraction();
		if ((progressFraction > 0.0) && (this.numInstancesRead > 0)) {
			return (long) ((this.numInstancesRead / progressFraction) - this.numInstancesRead);
		}
		return -1;
	}

	public boolean hasMoreInstances() {
		return !this.hitEndOfFile;
	}

    public InstanceExample nextInstance() {
        InstanceExample prevInstance = this.lastInstanceRead;
		this.hitEndOfFile = !readNextInstanceFromFile();
		return prevInstance;
	}

	public boolean isRestartable() {
		return true;
	}

	public void restart() {
		try {
			if (fileReader != null) {
				fileReader.close();
			}
			InputStream fileStream = new FileInputStream(arffFileOption.getFile());
			fileProgressMonitor = new InputStreamProgressMonitor(fileStream);
			fileReader = new BufferedReader(new InputStreamReader(fileProgressMonitor));
            instances = new Instances(fileReader, 1, this.classIndexOption.getValue());
			if (classIndexOption.getValue() < 0) {
				instances.setClassIndex(instances.numAttributes() - 1);
			} else if (classIndexOption.getValue() > 0) {
				instances.setClassIndex(classIndexOption.getValue() - 1);
			}


			//use hashset to delete duplicates and attributes numbers that aren't valid
			HashSet<Integer> attributes =  new HashSet<Integer>(); 
			Option[] rawAttributeList = removeAttributesOption.getList();
			for (int i = 0; i < rawAttributeList.length; i++) {
				int attribute = ((IntOption)rawAttributeList[i]).getValue();
				if(1 <= attribute && attribute <= instances.numAttributes())
					attributes.add(attribute-1);
				else
					System.out.println("Found invalid attribute removal description: " +
							"Attribute option "+attribute
							+" will be ignored. Filestream only has "
							+instances.numAttributes()+" attributes.");
			}
			
			//remove all non numeric attributes except the class attribute
			if(!keepNonNumericalAttrOption.isSet()){
				for (int i = 0; i < instances.numAttributes(); i++) {
					if(!instances.attribute(i).isNumeric() && i != instances.classIndex()){
						attributes.add(i);
					}
				}
			}
			
			//read min/max values in case we need to normalize
			if(normalizeOption.isSet())
				valuesMinMaxDiff = readMinMaxDiffValues(attributes);
			
			//convert hashset to array and sort array so we can delete attributes in a sequence
			removeAttributes = attributes.toArray(new Integer[0]);
			Arrays.sort(removeAttributes);
			
			//set updated number of attributes (class attribute included)
			numAttsOption = new IntOption("numAtts", 'a',"", instances.numAttributes() - removeAttributes.length);
			
			if(removeAttributes.length > 0){
				System.out.println("Removing the following attributes:");
				for (int i = 0; i < removeAttributes.length; i++) {
					System.out.println((removeAttributes[i]+1)+" "
							+instances.attribute(removeAttributes[i]).name());
				}
			}
            
			//create filtered dataset
			filteredDataset = new Instances(instances);
			for (int i = removeAttributes.length-1; i >= 0 ; i--) {
				filteredDataset.deleteAttributeAt(removeAttributes[i]);
				if(true){
					
				}
			}

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
				Instance rawInstance = this.instances.instance(0);
				
				//remove dataset from instance so we can delete attributes
				for (int i = removeAttributes.length-1; i >= 0 ; i--) {
					rawInstance.deleteAttributeAt(removeAttributes[i]);	
				}
				//set adjusted dataset for instance
				rawInstance.setDataset(filteredDataset);

				if (normalizeOption.isSet() && valuesMinMaxDiff != null) {
					for (int i = 0; i < rawInstance.numAttributes() ; i++) {
						if (valuesMinMaxDiff.get(i)[2] != 1 &&		// Already normalized
							valuesMinMaxDiff.get(i)[2] != 0 &&		// Max. value is 0 (unable to be normalized)
							i != rawInstance.classIndex()) {		// Class label is not subject to be normalized
							double v = rawInstance.value(i);
							v = (v - valuesMinMaxDiff.get(i)[0]) / valuesMinMaxDiff.get(i)[2];
							rawInstance.setValue(i, v);
						}
					}
				}
				
                this.lastInstanceRead = new InstanceExample(rawInstance);
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
			throw new RuntimeException(
					"ArffFileStream failed to read instance from stream.", ioe);
		}
	}
	
	/**
	 * @param ignoredAttributes Attributes that will be ignored
	 * @return A list with min/max and diff=max-min values per attribute of the arff file 
	 */
	protected ArrayList<Double[]> readMinMaxDiffValues(HashSet<Integer> ignoredAttributes) {
		ArrayList<Double[]> valuesMinMaxDiff = null;
		
		if(ignoredAttributes == null)
			ignoredAttributes = new HashSet<Integer>();
		
		try {
			InputStream fileStream = new FileInputStream(arffFileOption.getFile());
			InputStreamProgressMonitor fileProgressMonitor = new InputStreamProgressMonitor(fileStream);
			Reader fileReader = new BufferedReader(new InputStreamReader(fileProgressMonitor));
            Instances instances = new Instances(fileReader, 1, this.classIndexOption.getValue());

			valuesMinMaxDiff = new ArrayList<Double[]>();
			for (int i = 0; i < instances.numAttributes()-ignoredAttributes.size(); i++) {
				Double[] values =  {Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,0.0};
				valuesMinMaxDiff.add(values);
			}
			
			System.out.print("Reading arff file for normalization...");
			int counter = 0;
			while (instances.readInstance(fileReader)) {
				Instance instance = instances.instance(0);
				int a = 0;
				for (int i = 0; i < instances.numAttributes(); i++) {
					if(!ignoredAttributes.contains(i)){
						double value = instance.value(i);
						if(value < valuesMinMaxDiff.get(a)[0])
							valuesMinMaxDiff.get(a)[0] = value;
						if(value > valuesMinMaxDiff.get(a)[1])
							valuesMinMaxDiff.get(a)[1] = value;
						a++;
					}
				}
				instances.delete();

				//show some progress
				counter++;
				if(counter >= 10000){
					counter = 0;
					System.out.print(".");
				}
			}
			if (fileReader != null) {
				fileReader.close();
				fileReader = null;
			}
			System.out.println("done!");

			for (int i = 0; i < valuesMinMaxDiff.size(); i++) {
				valuesMinMaxDiff.get(i)[2]=valuesMinMaxDiff.get(i)[1]-valuesMinMaxDiff.get(i)[0];
			}

			return valuesMinMaxDiff;
		} catch (IOException ioe) {
			throw new RuntimeException(
					"ArffFileStream failed to read instance from stream.", ioe);
		}
	}	
	

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
