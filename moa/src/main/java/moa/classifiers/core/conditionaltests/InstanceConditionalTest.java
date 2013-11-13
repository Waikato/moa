/*
 *    InstanceConditionalTest.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.classifiers.core.conditionaltests;

import moa.AbstractMOAObject;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Abstract conditional test for instances to use to split nodes in Hoeffding trees.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public abstract class InstanceConditionalTest extends AbstractMOAObject {

    /**
     *  Returns the number of the branch for an instance, -1 if unknown.
     *
     * @param inst the instance to be used
     * @return the number of the branch for an instance, -1 if unknown.
     */
    public abstract int branchForInstance(Instance inst);

    /**
     * Gets whether the number of the branch for an instance is known.
     *
     * @param inst
     * @return true if the number of the branch for an instance is known
     */
    public boolean resultKnownForInstance(Instance inst) {
        return branchForInstance(inst) >= 0;
    }

    /**
     * Gets the number of maximum branches, -1 if unknown.
     *
     * @return the number of maximum branches, -1 if unknown..
     */
    public abstract int maxBranches();

    /**
     * Gets the text that describes the condition of a branch. It is used to describe the branch.
     *
     * @param branch the number of the branch to describe
     * @param context the context or header of the data stream
     * @return the text that describes the condition of the branch
     */
    public abstract String describeConditionForBranch(int branch,
            InstancesHeader context);

    /**
     * Returns an array with the attributes that the test depends on.
     *
     * @return  an array with the attributes that the test depends on
     */
    public abstract int[] getAttsTestDependsOn();
}
