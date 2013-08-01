/**
 * [ColorObject.java] for Subspace MOA
 * 
 * @author Stephen Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macro;
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
