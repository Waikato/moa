/**
 * [ColorObject.java] for Subspace MOA
 *
 * @author Stephen Wels
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
package moa.learners.clusterers.macro;

import java.awt.Color;

public class ColorObject {

	private Color mColor;
	private String mName;

	public ColorObject(String name, Color c) {
		mColor = c;
		mName = name;
	}

	public Color getColor() {
		return mColor;
	}

	public String getName() {
		return mName;
	}
}
