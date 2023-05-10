package moa.gui.colorGenerator;

import java.awt.Color;

/**
 * This interface specifies the generateColors method for classes
 * which generate colors according different strategies such that
 * those colors can be distinguished easily.
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public interface ColorGenerator {
	/**
	 * Generate numColors unique colors which should be easily distinguishable.
	 * 
	 * @param numColors the number of colors to generate
	 * @return an array of unique colors
	 */
	public Color[] generateColors(int numColors);
}
