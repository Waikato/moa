/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    EuclideanDistance.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 <!-- globalinfo-start -->
 * Implementing Euclidean distance (or similarity) function.<br/>
 * <br/>
 * One object defines not one distance but the data model in which the distances between objects of that data model can be computed.<br/>
 * <br/>
 * Attention: For efficiency reasons the use of consistency checks (like are the data models of the two instances exactly the same), is low.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Wikipedia. Euclidean distance. URL http://en.wikipedia.org/wiki/Euclidean_distance.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;misc{missing_id,
 *    author = {Wikipedia},
 *    title = {Euclidean distance},
 *    URL = {http://en.wikipedia.org/wiki/Euclidean_distance}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns off the normalization of attribute 
 *  values in distance calculation.</pre>
 * 
 * <pre> -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of columns to used in the calculation of the 
 *  distance. 'first' and 'last' are valid indices.
 *  (default: first-last)</pre>
 * 
 * <pre> -V
 *  Invert matching sense of column indices.</pre>
 * 
 <!-- options-end --> 
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class EuclideanDistance
  extends NormalizableDistance
  implements Cloneable{

  /** for serialization. */
  private static final long serialVersionUID = 1068606253458807903L;

  /**
   * Constructs an Euclidean Distance object, Instances must be still set.
   */
  public EuclideanDistance() {
    super();
  }

  /**
   * Constructs an Euclidean Distance object and automatically initializes the
   * ranges.
   * 
   * @param data 	the instances the distance function should work on
   */
  public EuclideanDistance(Instances data) {
    super(data);
  }

  /**
   * Returns a string describing this object.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Implementing Euclidean distance (or similarity) function.\n\n"
      + "One object defines not one distance but the data model in which "
      + "the distances between objects of that data model can be computed.\n\n"
      + "Attention: For efficiency reasons the use of consistency checks "
      + "(like are the data models of the two instances exactly the same), "
      + "is low.\n\n";
  }

  
  /**
   * Calculates the distance between two instances.
   * 
   * @param first 	the first instance
   * @param second 	the second instance
   * @return 		the distance between the two given instances
   */
  public double distance(Instance first, Instance second) {
    return Math.sqrt(distance(first, second, Double.POSITIVE_INFINITY));
  }
  
 
  /**
   * Updates the current distance calculated so far with the new difference
   * between two attributes. The difference between the attributes was 
   * calculated with the difference(int,double,double) method.
   * 
   * @param currDist	the current distance calculated so far
   * @param diff	the difference between two new attributes
   * @return		the update distance
   * @see		#difference(int, double, double)
   */
  protected double updateDistance(double currDist, double diff) {
    double	result;
    
    result  = currDist;
    result += diff * diff;
    
    return result;
  }
  
  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * is necessary to do so to get the correct distances if
   * distance(distance(Instance first, Instance second, double cutOffValue) is
   * used. This is because that function actually returns the squared distance
   * to avoid inaccuracies arising from floating point comparison.
   * 
   * @param distances	the distances to post-process
   */
  public void postProcessDistances(double distances[]) {
    for(int i = 0; i < distances.length; i++) {
      distances[i] = Math.sqrt(distances[i]);
    }
  }
  
  /**
   * Returns the squared difference of two values of an attribute.
   * 
   * @param index	the attribute index
   * @param val1	the first value
   * @param val2	the second value
   * @return		the squared difference
   */
  public double sqDifference(int index, double val1, double val2) {
    double val = difference(index, val1, val2);
    return val*val;
  }
  
  /**
   * Returns value in the middle of the two parameter values.
   * 
   * @param ranges 	the ranges to this dimension
   * @return 		the middle value
   */
  public double getMiddle(double[] ranges) {

    double middle = ranges[R_MIN] + ranges[R_WIDTH] * 0.5;
    return middle;
  }
  
  /**
   * Returns the index of the closest point to the current instance.
   * Index is index in Instances object that is the second parameter.
   *
   * @param instance 	the instance to assign a cluster to
   * @param allPoints 	all points
   * @param pointList 	the list of points
   * @return 		the index of the closest point
   * @throws Exception	if something goes wrong
   */
  public int closestPoint(Instance instance, Instances allPoints,
      			  int[] pointList) throws Exception {
    double minDist = Integer.MAX_VALUE;
    int bestPoint = 0;
    for (int i = 0; i < pointList.length; i++) {
      double dist = distance(instance, allPoints.instance(pointList[i]), Double.POSITIVE_INFINITY);
      if (dist < minDist) {
        minDist = dist;
        bestPoint = i;
      }
    }
    return pointList[bestPoint];
  }
  
  /**
   * Returns true if the value of the given dimension is smaller or equal the
   * value to be compared with.
   * 
   * @param instance 	the instance where the value should be taken of
   * @param dim 	the dimension of the value
   * @param value 	the value to compare with
   * @return 		true if value of instance is smaller or equal value
   */
  public boolean valueIsSmallerEqual(Instance instance, int dim,
      				     double value) {  //This stays
    return instance.value(dim) <= value;
  }


  public double attributeSqDistance(Instance first, Instance second, int attributeIndex)
  {
    validate();
    double d = -1;
	
    if(Double.isNaN(first.valueSparse(attributeIndex)) && Double.isNaN(second.valueSparse(attributeIndex))) {
      return 1; // assume attributes are different if both are missing
    }else
    if(Double.isNaN(first.valueSparse(attributeIndex))) {
      d = difference(attributeIndex, 0, second.valueSparse(attributeIndex));
      //System.out.println("d 1 missing " + d);
    } else
    if(Double.isNaN(second.valueSparse(attributeIndex))) {
      d = difference(attributeIndex, first.valueSparse(attributeIndex), 0);
      //System.out.println("d 2 missing " + d);
    }
	
    d = difference(attributeIndex,first.valueSparse(attributeIndex),second.valueSparse(attributeIndex));

    // if for some reason the difference returned was a NaN then return 1
    if (Double.isNaN(d))
      return 1;
    return d*d;
  }

  /**
   * calculates the normalised squared euclidean distance between two instances for a given attribute
   *
   * @param v1 value of attribute for first instance
   * @param v2 value of attribute for second instance
   * @param attributeIndex index of attribute
     * @return
     */
  public double attributeSqDistance(double v1, double v2, int attributeIndex)
  {
    validate();
    double d = -1;
    if(Double.isNaN(v1) && Double.isNaN(v2)) {
      return 1; // assume attributes are different if both are missing
    }else
    if(Double.isNaN(v1)) {
      d = difference(attributeIndex, 0, v2);
      //System.out.println("d 1 missing " + d);
    } else
    if(Double.isNaN(v2)) {
      d = difference(attributeIndex, v1, 0);
      //System.out.println("d 2 missing " + d);
    }
    d = difference(attributeIndex,v1,v2);

    // if for some reason the difference returned was a NaN then return 1
    if (Double.isNaN(d))
      return 1;
    return d*d;
  }

  /**
   * sets all attributes other than the indices in the array as inactive
   * @param activeAttributes indices of active attributes
   */
  public void setAttributeIndices(int[] activeAttributes)
  {
    m_ActiveIndices = new boolean[m_Data.numAttributes()];
    for (int i = 0; i < m_ActiveIndices.length; i++)
      m_ActiveIndices[i] = false;
    for (int i = 0; i < activeAttributes.length; i++)
    {
      m_ActiveIndices[activeAttributes[i]] = true; //m_AttributeIndices.isInRange(i);
    }
  }

  public double normalise(double v, int i)
  {
    validate();
    return norm(v,i);
  }
    
}
