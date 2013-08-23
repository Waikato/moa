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

public class ColorArray {
	
	public static ColorObject[] mVisibleColors = {
			new ColorObject("blue", new Color(0x0000ff)),
			new ColorObject("blueviolet", new Color(0x8a2be2)),
			new ColorObject("brown", new Color(0xa52a2a)),
			new ColorObject("burlywood", new Color(0xdeb887)),
			new ColorObject("cadetblue", new Color(0x5f9ea0)),
			//new ColorObject("chartreuse", new Color(0x7fff00)),
			new ColorObject("chocolate", new Color(0xd2691e)),
			new ColorObject("coral", new Color(0xff7f50)),
			new ColorObject("cornflowerblue", new Color(0x6495ed)),
			new ColorObject("crimson", new Color(0xdc143c)),
			new ColorObject("cyan", new Color(0x00ffff)),
			new ColorObject("darkblue", new Color(0x00008b)),
			new ColorObject("darkcyan", new Color(0x008b8b)),
			new ColorObject("darkgoldenrod", new Color(0xb8860b)),
			new ColorObject("darkgreen", new Color(0x006400)),
			new ColorObject("darkkhaki", new Color(0xbdb76b)),
			new ColorObject("darkmagenta", new Color(0x8b008b)),
			new ColorObject("darkolivegreen", new Color(0x556b2f)),
			new ColorObject("darkorange", new Color(0xff8c00)),
			// new ColorObject("darkorchid", new Color(0x9932cc)),
			new ColorObject("darkred", new Color(0x8b0000)),
			new ColorObject("darksalmon", new Color(0xe9967a)),
			new ColorObject("darkseagreen", new Color(0x8fbc8f)),
			new ColorObject("darkslateblue", new Color(0x483d8b)),
			new ColorObject("darkslategray", new Color(0x2f4f4f)),
			// new ColorObject("darkturquoise", new Color(0x00ced1)),
			new ColorObject("darkviolet", new Color(0x9400d3)),
			new ColorObject("deeppink", new Color(0xff1493)),
			new ColorObject("deepskyblue", new Color(0x00bfff)),
			// new ColorObject("dodgerblue", new Color(0x1e90ff)),
			new ColorObject("firebrick", new Color(0xb22222)),
			new ColorObject("forestgreen", new Color(0x228b22)),
			new ColorObject("fuchsia", new Color(0xff00ff)),
			new ColorObject("gold", new Color(0xffd700)),
			new ColorObject("goldenrod", new Color(0xdaa520)),
			//new ColorObject("green", new Color(0x008000)),
			new ColorObject("greenyellow", new Color(0xadff2f)),
			new ColorObject("hotpink", new Color(0xff69b4)),
			new ColorObject("indianred", new Color(0xcd5c5c)),
			new ColorObject("indigo", new Color(0x4b0082)),
			//new ColorObject("lawngreen", new Color(0x7cfc00)),
			// new ColorObject("lime", new Color(0x00ff00)),
			// new ColorObject("limegreen", new Color(0x32cd32)),
			new ColorObject("magenta", new Color(0xff00ff)),
			new ColorObject("maroon", new Color(0x800000)),
			new ColorObject("olive", new Color(0x808000)),
			new ColorObject("orange", new Color(0xffa500)),
			new ColorObject("orangered", new Color(0xff4500)),
			new ColorObject("pink", new Color(0xffc0cb)),
			new ColorObject("powderblue", new Color(0xb0e0e6)),
			new ColorObject("purple", new Color(0x800080)),
			new ColorObject("red", new Color(0xff0000)),
			new ColorObject("royalblue", new Color(0x4169e1)),
			new ColorObject("saddlebrown", new Color(0x8b4513)),
			new ColorObject("salmon", new Color(0xfa8072)),
			new ColorObject("seagreen", new Color(0x2e8b57)),
			new ColorObject("skyblue", new Color(0x87ceeb)),
			new ColorObject("slateblue", new Color(0x6a5acd)),
			new ColorObject("tomato", new Color(0xff6347)),
			new ColorObject("violet", new Color(0xee82ee)) };

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
