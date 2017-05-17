package testers;

import core.Options;

public class OptionsTester implements Tester
{
    private Options options;

    public OptionsTester()
    {
	options = new Options();
    }

    @Override
    public void doTest()
    {
	options.printUsage();

	String[][] args = {
		{ "-inputMode", "CSVReader", "-driftBound", "Hoeffding",
			"-numCentroids", "5" },
		{ "-inputMode", "RBFGenerator", "-numAttributes", "5",
			"-delta", "10.1" },
		{ "-inputMode", "HyperplaneGenerator", "-driftBound", "Markov",
			"-numAttributes", "10" } };

	int i = 0;
	while (i < args.length)
	{
	    options.parseArgs(args[i]);
	    i++;
	}
    }
}
