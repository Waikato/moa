package core;

import java.awt.geom.Arc2D.Double;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ARFFLoader
{
    String relation;
    ArrayList<String> attributes;
    
    BufferedReader reader;
    
    boolean headingProcessed = false;
        
    public ARFFLoader(String fileName)
    {
	try
	{
	    reader = new BufferedReader(new FileReader(fileName));
	    attributes = new ArrayList<String>();
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
    }
    
    public void processHeading()
    {
	try
	{
	    String line = "";
	
	    while ((line = reader.readLine()) != null)
	    {
		if(line.toLowerCase().equals("@data"))
		{
		    headingProcessed = true;
		    break; 
		}
		else if(line.toLowerCase().equals("@relations"))
		{
		    relation = line.substring(line.indexOf(" ")).trim();
		}
		else if(line.toLowerCase().equals("@attribute"))
		{
		    attributes.add(line.substring(line.indexOf(" ")).trim());
		}
	    }
	
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
    
    // REQUIRES CHANGING - ASSUMES FEATURES ARE ALL NUMERIC
    public Map<String, Class<?>> getFeatureType()
    {
	HashMap<String, Class<?>> map = new HashMap<String, Class<?>>();
	
	for(int i = 0; i < attributes.size() - 1; i++)
	{
	    String[] split = attributes.get(i).split(" ");
	    
	    map.put(split[0], Double.class);
	}
	
	String[] split = attributes.get(attributes.size()-1).split(" ");	    
	map.put(split[0], String.class);
	
	return map;
    }

    public boolean hasNext()
    {
	try
	{
	    return this.reader.ready();
	} 
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	return false;
    }


    public String getNextLine()
    {
	try
	{
	    return this.reader.readLine();
	} 
	catch (IOException e)
	{
	    e.printStackTrace();
	}

	return null;
    }
    
    public void close()
    {
	try
	{
	    reader.close();
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
}
