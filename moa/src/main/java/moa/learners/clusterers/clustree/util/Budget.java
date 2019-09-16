/*
 *    Budget.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Sanchez Villaamil (moa@cs.rwth-aachen.de)
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

package moa.learners.clusterers.clustree.util;

/**
 * This is an interface for classes that are to be given along with every data
 * point inserted in the tree. The tree has to inform the implementation of this
 * <code>Budget</code> interface of every operation it does, and ask at the
 * places where it can stop prematurely if it should. This models the arrival of
 * a new data point in the stream, before there was enough time to insert the
 * current one.
 */
public interface Budget {

	/**
	 * A function for the tree to ask if there is budget(time) left.
	 * 
	 * @return A <code>boolean</code> that is <code>true</code> if the tree should
	 *         go on, <code>false</code> otherwise.
	 */
	boolean hasMoreTime();

	/**
	 * Inform the <code>Budget</code> class that an integer addition has been
	 * performed by the tree.
	 */
	void integerAddition();

	/**
	 * Inform the <code>Budget</code> that a certain number of integer additions
	 * have been done.
	 * 
	 * @param number the number of additions done.
	 */
	void integerAddition(int number);

	/**
	 * Inform the <code>Budget</code> class that a double addition has been
	 * performed by the tree.
	 */
	void doubleAddition();

	/**
	 * Inform the <code>Budget</code> that a certain number of double additions have
	 * been performed.
	 * 
	 * @param number the number of additions done.
	 */
	void doubleAddition(int number);

	/**
	 * Inform the <code>Budget</code> class that a integer multiplicaton has been
	 * performed by the tree.
	 */
	void integerMultiplication();

	/**
	 * Inform the <code>Budget</code> that a certain number of integer
	 * multiplications have been performed.
	 * 
	 * @param number the number of multiplication done.
	 */
	void integerMultiplication(int number);

	/**
	 * Inform the <code>Budget</code> class that a double multiplicaton has been
	 * performed by the tree.
	 */
	void doubleMultiplication();

	/**
	 * Inform the <code>Budget</code> that a certain number of double
	 * multiplications have been performed.
	 * 
	 * @param number the number of multiplications done.
	 */
	void doubleMultiplication(int number);

	/**
	 * Inform the <code>Budget</code> class that a integer division has been
	 * performed by the tree.
	 */
	void integerDivision();

	/**
	 * Inform the <code>Budget</code> that a certain number of integer divisions
	 * have been performed.
	 * 
	 * @param number the number of division done.
	 */
	void integerDivision(int number);

	/**
	 * Inform the <code>Budget</code> class that a double division has been
	 * performed by the tree.
	 */
	void doubleDivision();

	/**
	 * Inform the <code>Budget</code> that a certain number of double divisions have
	 * been performed.
	 * 
	 * @param number the number of divisions done.
	 */
	void doubleDivision(int number);
}
