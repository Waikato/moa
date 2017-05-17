package testers;

import inputstream.CSVReader;

/*
 * Tester class for testing the output of CSVReader
 */

public class CSVReaderTester implements Tester
{
    private String filePath;
    private CSVReader reader;

    public CSVReaderTester()
    {
	filePath = "src\\testers\\example.csv";
	reader = new CSVReader(filePath);
    }

    @Override
    public void doTest()
    {
	while (reader.hasNextTransaction())
	{
	    System.out.println(reader.hasNextTransaction());
	    System.out.println(reader.getNextTransaction());
	}
    }

}
