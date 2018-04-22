/*
 *    RuleExpandedMessage.java
 *    Copyright (C) 2017 University of Porto, Portugal
 *    @author J. Duarte, J. Gama
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
package moa.classifiers.rules.featureranking.messages;


public class RuleExpandedMessage implements FeatureRankingMessage {
	private int attributeIndex;
	private boolean isSpecialization;

	public int getAttributeIndex() {
		return attributeIndex;
	}

	public RuleExpandedMessage(int attributeIndex) {
		this.attributeIndex = attributeIndex;
		setSpecialization(false);
	}
	
	public RuleExpandedMessage(int attributeIndex, boolean isSpecialization) {
		this.attributeIndex = attributeIndex;
		this.setSpecialization(isSpecialization);
	}

	public boolean isSpecialization() {
		return isSpecialization;
	}

	protected void setSpecialization(boolean isSpecialization) {
		this.isSpecialization = isSpecialization;
	}


}
