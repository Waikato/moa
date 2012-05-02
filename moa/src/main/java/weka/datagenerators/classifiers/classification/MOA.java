/*
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

/*
 * MOA.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.datagenerators.classifiers.classification;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.MOAUtils;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.datagenerators.ClassificationGenerator;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.streams.InstanceStream;
import moa.streams.generators.LEDGenerator;

/**
 <!-- globalinfo-start -->
 * A wrapper around MOA instance streams.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -h
 *  Prints this help.</pre>
 * 
 * <pre> -o &lt;file&gt;
 *  The name of the output file, otherwise the generated data is
 *  printed to stdout.</pre>
 * 
 * <pre> -r &lt;name&gt;
 *  The name of the relation.</pre>
 * 
 * <pre> -d
 *  Whether to print debug informations.</pre>
 * 
 * <pre> -S
 *  The seed for random function (default 1)</pre>
 * 
 * <pre> -n &lt;num&gt;
 *  The number of examples to generate (default 100)</pre>
 * 
 * <pre> -B &lt;classname + options&gt;
 *  The MOA stream generator.
 *  (default: moa.streams.generators.LEDGenerator)</pre>
 * 
 <!-- options-end -->
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MOA
  extends ClassificationGenerator {
  
  /** for serialization. */
	private static final long serialVersionUID = 13533833825026962L;

	/** the actual data generator. */
  protected InstanceStream m_ActualGenerator = new LEDGenerator();

  /** for manipulating the generator through the GUI. */
  protected ClassOption m_Generator = new ClassOption("InstanceStream", 'B', "The MOA instance stream generator to use from within WEKA.", InstanceStream.class, m_ActualGenerator.getClass().getName());
  
  /**
   * Returns a string describing this data generator.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "A wrapper around MOA instance streams.";
  }

 /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = enumToVector(super.listOptions());

    result.add(new Option(
    		"\tThe MOA stream generator.\n"
    		+ "\t(default: " + MOAUtils.toCommandLine(new LEDGenerator()) + ")",
    		"B", 1, "-B <classname + options>"));

    return result.elements();
  }

  /**
   * Parses a list of options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -h
   *  Prints this help.</pre>
   * 
   * <pre> -o &lt;file&gt;
   *  The name of the output file, otherwise the generated data is
   *  printed to stdout.</pre>
   * 
   * <pre> -r &lt;name&gt;
   *  The name of the relation.</pre>
   * 
   * <pre> -d
   *  Whether to print debug informations.</pre>
   * 
   * <pre> -S
   *  The seed for random function (default 1)</pre>
   * 
   * <pre> -n &lt;num&gt;
   *  The number of examples to generate (default 100)</pre>
   * 
   * <pre> -B &lt;classname + options&gt;
   *  The MOA stream generator.
   *  (default: moa.streams.generators.LEDGenerator)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String        			tmpStr;
    ClassOption					option;

    tmpStr = Utils.getOption('B', options);
    option = (ClassOption) m_Generator.copy();
    if (tmpStr.length() == 0)
    	option.setCurrentObject(new LEDGenerator());
    else
    	option.setCurrentObject(MOAUtils.fromCommandLine(m_Generator, tmpStr));
    setGenerator(option);
    
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the datagenerator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]      	options;
    int           	i;
    
    result = new Vector<String>();

    result.add("-B");
    result.add(MOAUtils.toCommandLine(m_ActualGenerator));
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    return result.toArray(new String[result.size()]);
  }
  
  /**
   * Sets the MOA stream generator to use.
   * 
   * @param value the stream generator to use
   */
  public void setGenerator(ClassOption value) {
  	m_Generator       = value;
  	m_ActualGenerator = (InstanceStream) MOAUtils.fromOption(m_Generator);
  }
  
  /**
   * Returns the current MOA stream generator in use.
   * 
   * @return the stream generator in use
   */
  public ClassOption getGenerator() {
  	return m_Generator;
  }
  
  /**
   * Returns the tooltip displayed in the GUI.
   * 
   * @return the tooltip
   */
  public String generatorTipText() {
  	return "The MOA stream generator to use.";
  }

  /**
   * Return if single mode is set for the given data generator
   * mode depends on option setting and or generator type.
   * 
   * @return single mode flag, always true
   * @throws Exception if mode is not set yet
   */
  public boolean getSingleModeFlag() throws Exception {
    return true;
  }

  /**
   * Initializes the format for the dataset produced. 
   *
   * @return the format for the dataset 
   * @throws Exception if the generating of the format failed
   */
  public Instances defineDataFormat() throws Exception {
  	int		numExamples;
  	
  	m_ActualGenerator = (InstanceStream) MOAUtils.fromOption(m_Generator);
  	((AbstractOptionHandler) m_ActualGenerator).prepareForUse();
    m_DatasetFormat = new Instances(m_ActualGenerator.getHeader());

    // determine number of instances to generate
    numExamples = getNumExamples();
    if (m_ActualGenerator.estimatedRemainingInstances() != -1) {
    	if (m_ActualGenerator.estimatedRemainingInstances() < numExamples)
    		numExamples = (int) m_ActualGenerator.estimatedRemainingInstances();
    }
    setNumExamplesAct(numExamples);
    
    return m_DatasetFormat;
  }

  /**
   * Generates one example of the dataset. 
   *
   * @return the generated example, null if no further example available
   * @throws Exception if the format of the dataset is not yet defined
   * @throws Exception if the generator only works with generateExamples
   * which means in non single mode
   */
  public Instance generateExample() throws Exception {
  	if (m_ActualGenerator.hasMoreInstances())
  		return m_ActualGenerator.nextInstance();
  	else
  		return null;
  }

  /**
   * Generates all examples of the dataset. Re-initializes the random number
   * generator with the given seed, before generating instances.
   *
   * @return the generated dataset
   * @throws Exception if the format of the dataset is not yet defined
   * @throws Exception if the generator only works with generateExample,
   * which means in single mode
   * @see   #getSeed()
   */
  public Instances generateExamples() throws Exception {
    Instances       result;
    Instance				inst;
    int             i;

    result   = new Instances(m_DatasetFormat, 0);
    m_Random = new Random(getSeed());

    for (i = 0; i < getNumExamplesAct(); i++) {
    	inst = generateExample();
    	if (inst != null)
    		result.add(inst);
    	else
    		break;
    }
    
    return result;
  }

  /**
   * Generates a comment string that documentates the data generator.
   * By default this string is added at the beginning of the produced output
   * as ARFF file type, next after the options.
   * 
   * @return string contains info about the generated rules
   */
  public String generateStart () {
    return "";
  }

  /**
   * Generates a comment string that documentats the data generator.
   * By default this string is added at the end of theproduces output
   * as ARFF file type.
   * 
   * @return string contains info about the generated rules
   * @throws Exception if the generating of the documentaion fails
   */
  public String generateFinished() throws Exception {
    return "";
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for executing this class.
   *
   * @param args should contain arguments for the data producer: 
   */
  public static void main(String[] args) {
    runDataGenerator(new MOA(), args);
  }
}
