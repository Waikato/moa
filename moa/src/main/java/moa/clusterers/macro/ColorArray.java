/**
 * [ColorArray.java] for Subspace MOA
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
package moa.clusterers.macro;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ColorArray {

	private static final String COMMA_DELIMITER   = ",";
	public static ColorObject[] mVisibleColors;

	static {
		try {
			mVisibleColors = getColors();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ColorObject[] getColors() throws IOException {

		ArrayList<ColorObject> colorObjects = new ArrayList<>();
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = contextClassLoader.getResourceAsStream("colors.csv");
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		for (String line; (line = reader.readLine()) != null;) {
			String[] values = line.split(COMMA_DELIMITER);
			colorObjects.add(new ColorObject(values[0], Color.decode(values[1])));
		}

		return colorObjects.toArray(new ColorObject[0]);
	}

	public static Color getColor(int i) {
		Color res;
		try {
			res = mVisibleColors[i].getColor();
		} catch (ArrayIndexOutOfBoundsException e) {
			return Color.BLACK;
		}
		return res;
	}

	public static String getName(int i) {
		String res;
		try {
			res = mVisibleColors[i].getName();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
		return res;
	}

	public static double getNumColors() {
		return mVisibleColors.length;
	}
}
