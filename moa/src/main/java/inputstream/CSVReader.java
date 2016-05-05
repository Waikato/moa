package inputstream;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CSVReader implements InputStreamInterface
{

    private String filePath;
    private BufferedReader reader;

    public CSVReader(String filePath)
    {
	this.filePath = filePath;

	try
	{
	    this.reader = new BufferedReader(new FileReader(filePath));
	} catch (FileNotFoundException e)
	{
	    System.err.println("Error: Input file not found.");
	}
    }

    @Override
    public boolean hasNextTransaction()
    {
	try
	{
	    return this.reader.ready();
	} catch (IOException e)
	{
	    e.printStackTrace();
	}
	return false;
    }

    @Override
    public String getNextTransaction()
    {
	try
	{
	    return this.reader.readLine();
	} catch (IOException e)
	{
	    e.printStackTrace();
	}

	return null;
    }

    public String getFilePath()
    {
	return this.filePath;
    }

    public void setFilePath(String filePath)
    {
	this.filePath = filePath;
    }

}
