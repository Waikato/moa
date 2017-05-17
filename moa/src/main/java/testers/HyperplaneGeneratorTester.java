package testers;

import inputstream.HyperplaneGenerator;

public class HyperplaneGeneratorTester implements Tester
{
    private int instanceRandomSeed;
    private int numAttributes;

    public HyperplaneGeneratorTester()
    {
	this.instanceRandomSeed = 3;
	this.numAttributes = 5;
    }

    @Override
    public void doTest()
    {
	HyperplaneGenerator gen = new HyperplaneGenerator(instanceRandomSeed,
		numAttributes, 3, 0.05, 0.05);

	int i = 10;
	while (i-- > 0)
	{
	    System.out.println(gen.getNextTransaction());
	}
    }

}
