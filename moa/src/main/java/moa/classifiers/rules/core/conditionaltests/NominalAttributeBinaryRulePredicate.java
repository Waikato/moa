/*
 *    NominalAttributeBinaryRulePredicate.java
 *    Copyright (C) 2013 University of Porto, Portugal
 *    @author E. Almeida, A. Carvalho, J. Gama
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
package moa.classifiers.rules.core.conditionaltests;

import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.classifiers.rules.core.Predicate;
import moa.core.StringUtils;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceInformation;

/**
 * Nominal binary conditional test for instances to use to split nodes in rules.
 *
 * @version $Revision: 7 $
 */
public class NominalAttributeBinaryRulePredicate extends NominalAttributeBinaryTest implements Predicate {

	public NominalAttributeBinaryRulePredicate(int attIndex, int attValue) {
		super(attIndex, attValue);
	}
	//used for negation
	protected boolean state=true;

	private static final long serialVersionUID = 1L;

	@Override
	public boolean evaluate(Instance inst) {
		if(state)
			return (branchForInstance(inst) == 0) ;
		else
			return (branchForInstance(inst) != 0);
	}

	@Override
	public void negateCondition() {
		state=!state;
		
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {   
		if(state)
			StringUtils.appendIndented(sb, indent+1, "In" + attIndex + " = " + attValue);
		else
			StringUtils.appendIndented(sb, indent+1, "In" + attIndex + " <> " + attValue);
	}
	
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		getDescription(sb,0);
		return sb.toString();
	}

	@Override
	public int getAttributeIndex() {
		return attIndex;
	}

	@Override
	public boolean isEqualOrLess() {
		return state;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent,
			InstanceInformation instInformation) {
		getDescription(sb,indent);
		
	}
	
	
}
