package cutpointdetection;

import java.util.Random;

public class ChanceDetector
{
    private double chance;
    private Random RNG;

    public ChanceDetector(int seed)
    {
	chance = 0.05;
	RNG = new Random(seed);
    }

    public ChanceDetector(double chance, int seed)
    {
	this.chance = chance;
	RNG = new Random(seed);
    }

    public boolean setInput(double input)
    {
	if (RNG.nextDouble() < chance)
	{
	    return true;
	} else
	{
	    return false;
	}
    }

    public void setChance(double chance)
    {
	this.chance = chance;
    }

    public double getChance()
    {
	return this.chance;
    }
}
