package moa.gui.colorGenerator;

import java.awt.Color;
import java.util.Random;

/**
 * This class generates colors in the HSV space.
 * The space where the colors are sampled from
 * can be configured by specifying the range of 
 * hue, saturation and brightness.
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class HSVColorGenerator implements ColorGenerator{

	// range of the hue
	private float hueMin;
	private float hueMax;
	// range of the saturation
	private float saturationMin;
	private float saturationMax;
	// range of the brightness
	private float brightnessMin;
	private float brightnessMax;
	
	/**
	 * constructor which sets the range to:
	 * 		hue - [0.0, 1.0) 
	 * 		saturation - [1.0, 1.0] 
	 * 		brightness - [1.0, 1.0] 
	 */
	public HSVColorGenerator()
	{
		this(1.0f,1.0f,1.0f,1.0f);
	}
	
	/**
	 * constructor which sets the range of the hue to [0,1) and sets the 
	 * ranges for saturation and brightness to the parameter
	 */
	public HSVColorGenerator(float saturationMin, float saturationMax, float brightnessMin, float brightnessMax)
	{
		this(0.0f, 1.0f, saturationMin,saturationMax,brightnessMin,brightnessMax);
	}
	
	/**
	 * constructor which sets the ranges for saturation and brightness to the parameter
	 */
	public HSVColorGenerator(float hueMin, float hueMax, float saturationMin, float saturationMax, float brightnessMin, float brightnessMax)
	{
		// set the ranges according to the given parameter
		this.hueMin = hueMin;
		this.hueMax = hueMax;
		this.saturationMin = saturationMin;
		this.saturationMax = saturationMax;
		this.brightnessMin = brightnessMin;
		this.brightnessMax = brightnessMax;
	}

	/**
	 * Generate numColors unique colors which should be easily distinguishable.
	 * 
	 * @param numColors the number of colors to generate
	 * @return an array of unique colors
	 */
	@Override
	public Color[] generateColors(int numColors) {
		Color[] colors = new Color[numColors];
		// fix the seed to always get the same colors for the same numColors parameter and ranges for hue, saturation and brightness
		Random rand = new Random(0);
		for(int i = 0; i < numColors; ++i)
		{
			float hueRatio = i/(float)numColors;
			float saturationRatio = rand.nextFloat();
			float brightnessRatio = rand.nextFloat();
			float hue = lerp(hueMin, hueMax, hueRatio);
			float saturation = lerp(saturationMin, saturationMax, saturationRatio);
			float brightness = lerp(brightnessMin, brightnessMax, brightnessRatio);
			colors[i] = Color.getHSBColor(hue, saturation, brightness);
		}
		
		return colors;
	}
	
	/**
	 * linear interpoplate between a minimal and maximal value given a ratio
	 * 
	 * @param min the minimal value which can be reached by setting the factor to 0.0
	 * @param max the maximal value which can be reached by setting the factor to 1.0
	 * @param ratio a float between 0.0 and 1.0 which describes the ratio between min and max to incorporate
	 * @return return the interpolated value
	 */
	private float lerp(float min, float max, float ratio)
	{
		return min + (max-min) * ratio;
	}
}
