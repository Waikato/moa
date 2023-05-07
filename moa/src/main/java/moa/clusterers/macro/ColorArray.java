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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ColorArray {

	private static final ColorObject[] ORIGINAL_COLOURS = {
	  new ColorObject("blue", 0x0000ff),
	  new ColorObject("blueviolet", 0x8a2be2),
	  new ColorObject("brown", 0xa52a2a),
	  new ColorObject("burlywood", 0xdeb887),
	  new ColorObject("cadetblue", 0x5f9ea0),
	  //new ColorObject("chartreuse", 0x7fff00),
	  new ColorObject("chocolate", 0xd2691e),
	  new ColorObject("coral", 0xff7f50),
	  new ColorObject("cornflowerblue", 0x6495ed),
	  new ColorObject("crimson", 0xdc143c),
	  new ColorObject("cyan", 0x00ffff),
	  new ColorObject("darkblue", 0x00008b),
	  new ColorObject("darkcyan", 0x008b8b),
	  new ColorObject("darkgoldenrod", 0xb8860b),
	  new ColorObject("darkgreen", 0x006400),
	  new ColorObject("darkkhaki", 0xbdb76b),
	  new ColorObject("darkmagenta", 0x8b008b),
	  new ColorObject("darkolivegreen", 0x556b2f),
	  new ColorObject("darkorange", 0xff8c00),
	  // new ColorObject("darkorchid"0 x9932cc),
	  new ColorObject("darkred", 0x8b0000),
	  new ColorObject("darksalmon", 0xe9967a),
	  new ColorObject("darkseagreen", 0x8fbc8f),
	  new ColorObject("darkslateblue", 0x483d8b),
	  new ColorObject("darkslategray", 0x2f4f4f),
	  // new ColorObject("darkturquoise"0 x00ced1),
	  new ColorObject("darkviolet", 0x9400d3),
	  new ColorObject("deeppink", 0xff1493),
	  new ColorObject("deepskyblue", 0x00bfff),
	  // new ColorObject("dodgerblue"0 x1e90ff),
	  new ColorObject("firebrick", 0xb22222),
	  new ColorObject("forestgreen", 0x228b22),
	  new ColorObject("fuchsia", 0xff00ff),
	  new ColorObject("gold", 0xffd700),
	  new ColorObject("goldenrod", 0xdaa520),
	  //new ColorObject("green"0 x008000),
	  new ColorObject("greenyellow", 0xadff2f),
	  new ColorObject("hotpink", 0xff69b4),
	  new ColorObject("indianred", 0xcd5c5c),
	  new ColorObject("indigo", 0x4b0082),
	  //new ColorObject("lawngreen"0 x7cfc00),
	  // new ColorObject("lime"0 x00ff00),
	  // new ColorObject("limegreen"0 x32cd32),
	  new ColorObject("magenta", 0xff00ff),
	  new ColorObject("maroon", 0x800000),
	  new ColorObject("olive", 0x808000),
	  new ColorObject("orange", 0xffa500),
	  new ColorObject("orangered", 0xff4500),
	  new ColorObject("pink", 0xffc0cb),
	  new ColorObject("powderblue", 0xb0e0e6),
	  new ColorObject("purple", 0x800080),
	  new ColorObject("red", 0xff0000),
	  new ColorObject("royalblue", 0x4169e1),
	  new ColorObject("saddlebrown", 0x8b4513),
	  new ColorObject("salmon", 0xfa8072),
	  new ColorObject("seagreen", 0x2e8b57),
	  new ColorObject("skyblue", 0x87ceeb),
	  new ColorObject("slateblue", 0x6a5acd),
	  new ColorObject("tomato", 0xff6347),
	  new ColorObject("violet", 0xee82ee) };

	private static final String COMMA_DELIMITER   = ",";
	public static ColorObject[] mVisibleColors;

	static {
		try {
		  	ArrayList<ColorObject> colours = new ArrayList<>();
		  	for (ColorObject obj : ORIGINAL_COLOURS)
		  	  colours.add(obj);
		  	colours.addAll(getColors());
			mVisibleColors = colours.toArray(new ColorObject[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<ColorObject> getColors() throws IOException {

		ArrayList<ColorObject> colorObjects = new ArrayList<>();
		ClassLoader classLoader = ColorArray.class.getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream("colors.csv");
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		for (String line; (line = reader.readLine()) != null;) {
			String[] values = line.split(COMMA_DELIMITER);
			colorObjects.add(ColorObject.decode(values[0], values[1]));
		}

		return colorObjects;
	}

	public static int getColor(int i) {
		try {
			return mVisibleColors[i].getColor();
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0; // Black
		}
	}

	public static String getName(int i) {
		try {
			return mVisibleColors[i].getName();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
	}

	public static double getNumColors() {
		return mVisibleColors.length;
	}
}
