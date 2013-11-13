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
 */

package weka.classifiers.meta;

import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MOAUtils;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.util.Enumeration;
import java.util.Vector;

import moa.classifiers.Classifier;
import moa.classifiers.trees.DecisionStump;
import moa.options.ClassOption;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;

/**
 <!-- globalinfo-start -->
 * Wrapper for MOA classifiers.<br/>
 * <br/>
 * Since MOA doesn't offer a mechanism to query a classifier for the types of attributes and classes it can handle, the capabilities of this wrapper are hard-coded: nominal and numeric attributes and only nominal class attributes are allowed.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -B &lt;classname + options&gt;
 *  The MOA classifier to use.
 *  (default: moa.classifiers.DecisionStump)</pre>
 *
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MOA
  extends weka.classifiers.AbstractClassifier
  implements UpdateableClassifier {

	/** for serialization. */
	private static final long serialVersionUID = 2605797948130310166L;

	/** the actual moa classifier to use for learning. */
	protected Classifier m_ActualClassifier = new DecisionStump();

	/** the moa classifier option (this object is used in the GenericObjectEditor). */
	protected ClassOption m_Classifier = new ClassOption(
			"classifier", 'B', "The MOA classifier to use from within WEKA.",
			Classifier.class, m_ActualClassifier.getClass().getName().replace("moa.classifiers.", ""),
			m_ActualClassifier.getClass().getName());

        
        protected WekaToSamoaInstanceConverter instanceConverter;
        
  /**
   * Returns a string describing the classifier.
   *
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
        "Wrapper for MOA classifiers.\n\n"
      + "Since MOA doesn't offer a mechanism to query a classifier for the "
      + "types of attributes and classes it can handle, the capabilities of "
      + "this wrapper are hard-coded: nominal and numeric attributes and "
      + "only nominal class attributes are allowed.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
        "\tThe MOA classifier to use.\n"
        + "\t(default: " + MOAUtils.toCommandLine(new DecisionStump()) + ")",
        "B", 1, "-B <classname + options>"));

    Enumeration en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   *
   * <pre> -B &lt;classname + options&gt;
   *  The MOA classifier to use.
   *  (default: moa.classifiers.trees.DecisionStump)</pre>
   *
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
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
    option = (ClassOption) m_Classifier.copy();
    if (tmpStr.length() == 0)
    	option.setCurrentObject(new DecisionStump());
    else
    	option.setCurrentObject(MOAUtils.fromCommandLine(m_Classifier, tmpStr));
    setClassifier(option);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]      	options;
    int           	i;

    result = new Vector<String>();

    result.add("-B");
    result.add(MOAUtils.toCommandLine(m_ActualClassifier));

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Sets the MOA classifier to use.
   *
   * @param value the classifier to use
   */
  public void setClassifier(ClassOption value) {
  	m_Classifier       = value;
  	m_ActualClassifier = (Classifier) MOAUtils.fromOption(m_Classifier);
  }

  /**
   * Returns the current MOA classifier in use.
   *
   * @return the classifier in use
   */
  public ClassOption getClassifier() {
  	return m_Classifier;
  }

  /**
   * Returns the tooltip displayed in the GUI.
   *
   * @return the tooltip
   */
  public String classifierTipText() {
  	return "The MOA classifier to use.";
  }

  /**
   * Returns the Capabilities of this classifier. Maximally permissive
   * capabilities are allowed by default. MOA doesn't specify what
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Generates a classifier.
   *
   * @param data set of instances serving as training data
   * @throws Exception if the classifier has not been
   * generated successfully
   */
  public void buildClassifier(Instances data) throws Exception {


        this.instanceConverter = new WekaToSamoaInstanceConverter();
  	getCapabilities().testWithFail(data);

  	data = new Instances(data);
  	data.deleteWithMissingClass();

  	m_ActualClassifier.resetLearning();
  	for (int i = 0; i < data.numInstances(); i++)
  		updateClassifier(data.instance(i));
        
  }

  /**
   * Updates a classifier using the given instance.
   *
   * @param instance the instance to included
   * @throws Exception if instance could not be incorporated
   * successfully
   */
  public void updateClassifier(Instance instance) throws Exception {
		m_ActualClassifier.trainOnInstance(instanceConverter.samoaInstance(instance));
  }

  /**
   * Predicts the class memberships for a given instance. If
   * an instance is unclassified, the returned array elements
   * must be all zero. If the class is numeric, the array
   * must consist of only one element, which contains the
   * predicted value.
   *
   * @param instance the instance to be classified
   * @return an array containing the estimated membership
   * probabilities of the test instance in each class
   * or the numeric prediction
   * @throws Exception if distribution could not be
   * computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
  	double[]	result;

  	result = m_ActualClassifier.getVotesForInstance(instanceConverter.samoaInstance(instance));
        // ensure that the array has as many elements as there are
        // class values!
        if (result.length < instance.numClasses()) {
          double[] newResult = new double[instance.numClasses()];
          System.arraycopy(result, 0, newResult, 0, result.length);
          result = newResult;
        }

  	try {
  		Utils.normalize(result);
  	}
  	catch (Exception e) {
  		result = new double[instance.numClasses()];
  	}

  	return result;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Returns a string representation of the model.
   *
   * @return the string representation
   */
  public String toString() {
  	StringBuilder		result;

  	result = new StringBuilder();
  	m_ActualClassifier.getDescription(result, 0);

  	return result.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String [] args) {
    runClassifier(new MOA(), args);
  }
}
