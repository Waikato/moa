package core;

public class Options
{
    private final String[] InputMode = { "CSVReader", "HyperplaneGenerator",
	    "RBFGenerator" };

    public final String[] DriftBound = { "Hoeffding", "Bernstein", "Markov",
	    "Chebyshev", "McDiarmid", "Bennet" };

    public final String[] CutPointDetectionMode = { "ADWIN", "mode2", "mode3" };

    // inputstream options
    private String inputMode;
    private String filePath;
    private int numCentroids;
    private int modelRandomSeed;
    private int instanceRandomSeed;
    private int numClasses;
    private int numAttributes;
    private int numDriftCentroids;
    private int speedChange;
    private int numDriftAttributes;
    private int changeMagnitude;
    private int noisePercentage;
    private int sigmaPercentage;

    // driftdetection options
    private String driftBound;
    private double delta;

    // cutpointdetection options
    private String cutPointMode;

    public Options()
    {
	// initializing default values
	speedChange = 0;
	numDriftCentroids = 50;
	modelRandomSeed = 1;
	instanceRandomSeed = 1;
	numClasses = 2;
	numAttributes = 10;
	numCentroids = 50;
	numDriftAttributes = 2;
	changeMagnitude = 0;
	noisePercentage = 5;
	sigmaPercentage = 10;

	delta = 0.05;
    }

    public void parseArgs(String[] args)
    {
	// System.out.println(iterateMode(args));

	boolean gotInputMode = false;
	boolean gotDriftBound = false;
	boolean gotCutPointDetectionMode = false;
	boolean gotFilePath = false;
	boolean gotNumCentroids = false;
	boolean gotModelRandomSeed = false;
	boolean gotInstanceRandomSeed = false;
	boolean gotNumClasses = false;
	boolean gotNumAttributes = false;
	boolean gotNumDriftCentroids = false;
	boolean gotSpeedChange = false;
	boolean gotNumDriftAttributes = false;
	boolean gotChangeMagnitude = false;
	boolean gotNoisePercentage = false;
	boolean gotSigmaPercentage = false;
	boolean gotDelta = false;

	int i = 0;
	while (i < args.length)
	{
	    String optionStr = args[i];
	    // System.out.println(optionStr);

	    /******************************************************************************************
	     * Input Stream Options
	     ******************************************************************************************/

	    if (optionStr == "-inputMode")
	    {
		if (gotInputMode)
		{
		    throw new Error(
			    "\nOptions Error: Specified input mode more than once");
		}
		if (i + 1 > args.length - 1
			|| !checkIfInputInMode(InputMode, args[i + 1]))
		{
		    throw new Error(
			    "\nOptions Error: Invalid input mode specified\n"
				    + "Valid input modes are: "
				    + iterateMode(InputMode));
		}
		inputMode = args[i + 1];
		i += 2;
		gotInputMode = true;
		continue;
	    }

	    if (optionStr == "-filePath")
	    {
		try
		{
		    if (gotFilePath)
		    {
			throw new Error(
				"\nOptions Error: Specified file path more than once");
		    }
		    if (i + 1 > args.length - 1)
		    {
			throw new Error(
				"\nOptions Error: No file path specified\n");
		    }
		    filePath = args[i + 1];
		    i += 2;
		    gotFilePath = true;
		    continue;
		} catch (NullPointerException e)
		{
		    System.err.println("Error: File path not found");
		    e.printStackTrace();
		}
	    }

	    if (optionStr == "-numCentroids")
	    {
		try
		{
		    if (gotNumCentroids)
		    {
			throw new Error(
				"\nOptions Error: Specified number of centroids more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of centroids specified\n");
		    }
		    numCentroids = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumCentroids = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of centroids specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-modelRandomSeed")
	    {
		try
		{
		    if (gotModelRandomSeed)
		    {
			throw new Error(
				"\nOptions Error: Specified model random seed more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid model random seed specified\n");
		    }
		    modelRandomSeed = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotModelRandomSeed = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Model random seed specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-instanceRandomSeed")
	    {
		try
		{
		    if (gotInstanceRandomSeed)
		    {
			throw new Error(
				"\nOptions Error: Specified instance random seed more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid instance random seed specified\n");
		    }
		    instanceRandomSeed = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotInstanceRandomSeed = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Instance random seed specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-numClasses")
	    {
		try
		{
		    if (gotNumClasses)
		    {
			throw new Error(
				"\nOptions Error: Specified number of classes more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of classes specified\n");
		    }
		    numClasses = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumClasses = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of classes specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-numAttributes")
	    {
		try
		{
		    if (gotNumAttributes)
		    {
			throw new Error(
				"\nOptions Error: Specified number of attributes more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of attributes specified\n");
		    }
		    numAttributes = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumAttributes = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of attributes specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-numAttributes")
	    {
		try
		{
		    if (gotNumAttributes)
		    {
			throw new Error(
				"\nOptions Error: Specified number of attributes more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of attributes specified\n");
		    }
		    numAttributes = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumAttributes = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of attributes specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-numDriftCentroids")
	    {
		try
		{
		    if (gotNumDriftCentroids)
		    {
			throw new Error(
				"\nOptions Error: Specified number of drift centroids more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of drift centroids specified\n");
		    }
		    numDriftCentroids = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumDriftCentroids = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of drift centroids specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-speedChange")
	    {
		try
		{
		    if (gotSpeedChange)
		    {
			throw new Error(
				"\nOptions Error: Specified speed change more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid speed change specified\n");
		    }
		    speedChange = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotSpeedChange = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Speed change specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-numDriftAttributes")
	    {
		try
		{
		    if (gotNumDriftAttributes)
		    {
			throw new Error(
				"\nOptions Error: Specified number of drift attributes more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid number of drift attributes specified\n");
		    }
		    numDriftAttributes = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNumDriftAttributes = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Number of drift attributes specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-changeMagnitude")
	    {
		try
		{
		    if (gotChangeMagnitude)
		    {
			throw new Error(
				"\nOptions Error: Specified change magnitude more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid change magnitude specified\n");
		    }
		    changeMagnitude = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotChangeMagnitude = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Change magnitude specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-noisePercentage")
	    {
		try
		{
		    if (gotNoisePercentage)
		    {
			throw new Error(
				"\nOptions Error: Specified noise percentage more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid noise percentage specified\n");
		    }
		    noisePercentage = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotNoisePercentage = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Noise percentage specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    if (optionStr == "-sigmaPercentage")
	    {
		try
		{
		    if (gotSigmaPercentage)
		    {
			throw new Error(
				"\nOptions Error: Specified sigma percentage more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Integer.parseInt(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid sigma percentage specified\n");
		    }
		    sigmaPercentage = Integer.parseInt(args[i + 1]);
		    i += 2;
		    gotSigmaPercentage = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Sigma percentage specified must be an integer");
		    e.printStackTrace();
		    break;
		}
	    }

	    /******************************************************************************************
	     * Drift Bound Options
	     ******************************************************************************************/

	    if (optionStr == "-driftBound")
	    {
		if (gotDriftBound)
		{
		    throw new Error(
			    "\nOptions Error: Specified drift bound more than once");
		}
		if (i + 1 > args.length - 1
			|| !checkIfInputInMode(DriftBound, args[i + 1]))
		{
		    throw new Error(
			    "\nOptions Error: Invalid drift bound specified\n"
				    + "Valid drift bounds are: "
				    + iterateMode(DriftBound));
		}
		driftBound = args[i + 1];
		i += 2;
		gotDriftBound = true;
		continue;
	    }

	    if (optionStr == "-delta")
	    {
		try
		{
		    if (gotDelta)
		    {
			throw new Error(
				"\nOptions Error: Specified delta more than once");
		    }
		    if (i + 1 > args.length - 1
			    || Double.parseDouble(args[i + 1]) < 0)
		    {
			throw new Error(
				"\nOptions Error: Invalid delta specified\n");
		    }
		    delta = Double.parseDouble(args[i + 1]);
		    i += 2;
		    gotDelta = true;
		    continue;
		} catch (NumberFormatException e)
		{
		    System.err
			    .println("Error: Delta specified must be a double");
		    e.printStackTrace();
		    break;
		}

	    }

	    /******************************************************************************************
	     * Cut Point Detectopn Options
	     ******************************************************************************************/
	}

    }

    public void printUsage()
    {
	System.out
		.println("***************************************************************");
	System.out
		.println("*  Kauri - Knowledge Acquisition Using Real-time Integration  *");
	System.out
		.println("***************************************************************");

	System.out.println("\n*  Input Modes:");
    }

    // Iterate through the mode array and return the content one by one in each
    // line
    public String iterateMode(String[] mode)
    {
	String str = "";

	for (String s : mode)
	{
	    str += "\n- " + s;
	}
	return str;
    }

    // Checks to see if the specified mode contains the input
    public boolean checkIfInputInMode(String[] mode, String input)
    {
	for (String s : mode)
	{
	    if (s.equals(input))
	    {
		return true;
	    }
	}
	return false;
    }

}
