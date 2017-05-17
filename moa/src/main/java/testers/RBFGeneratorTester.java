package testers;

import inputstream.RBFGenerator;

public class RBFGeneratorTester implements Tester
{

    public RBFGeneratorTester()
    {

    }

    @Override
    public void doTest()
    {
	RBFGenerator gen = new RBFGenerator(1, 2, 5, 5, 5, 0.1);

	int i = 20;
	while (i-- > 0)
	{
	    System.out.println(gen.getNextTransaction());
	}
    }

}
