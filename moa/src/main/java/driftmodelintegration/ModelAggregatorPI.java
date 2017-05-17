package driftmodelintegration;

import inputstream.CSVReader;

import java.util.ArrayList;

import driftmodelintegration.core.Data;
import driftmodelintegration.core.DataImpl;


public class ModelAggregatorPI
{

    private int i_numOfStreams = 4;
    private double threshold;
    private int attSplit = 6;
    private int initInstances = 1000;
    
    private String[] instances;
    private String[][] attributeLabels;
    private CSVReader[] readers;
    private LocalStatisticPI[] locals;
    
    private Double[][] localData;
    
    //@param args(numberOfStreams, detectionThreshold, directoryOfStreams)
    public ModelAggregatorPI(String[] args, double delta)
    {
	i_numOfStreams = Integer.parseInt(args[0]);
	this.threshold = Double.parseDouble(args[1]);
	
	instances = new String[i_numOfStreams];
	readers = new CSVReader[i_numOfStreams];
	locals = new LocalStatisticPI[i_numOfStreams];
	localData = new Double[i_numOfStreams][2];
	
	for(int i = 0; i < i_numOfStreams; i++)
	{
	    readers[i] = new CSVReader(args[i+2]);
	    locals[i] = new LocalStatisticPI(delta);
	}
	
	init();
    }
    
    public void init()
    {
	attributeLabels = new String[i_numOfStreams][];
	int count = 1;
	for(int k = 0; k < i_numOfStreams ; k++)
	{
	    String[] temp = new String[attSplit+1];
	    
	    for(int i = 0; i < attSplit; i++)
	    {
		temp[i] = "attribute" + count;
		count++;
	    }
	    
	    attributeLabels[k] = temp;
	    attributeLabels[k][attSplit] = "class";
	    
	    
	    locals[k].ht.setLabelAttribute("class");
	}

	ArrayList<Data> list1 = new ArrayList<Data>(initInstances);
	ArrayList<Data> list2 = new ArrayList<Data>(initInstances);
	ArrayList<Data> list3 = new ArrayList<Data>(initInstances);
	ArrayList<Data> list4 = new ArrayList<Data>(initInstances);
	
	for(int i = 0; i < initInstances; i++)
	{
	    list1.add(convertToData(readers[0].getNextTransaction(), 0));
	    list2.add(convertToData(readers[1].getNextTransaction(), 1));
	    list3.add(convertToData(readers[2].getNextTransaction(), 2));
	    list4.add(convertToData(readers[3].getNextTransaction(), 3));
	}
	
	locals[0].ht.initialize(list1);
	locals[1].ht.initialize(list2);
	locals[2].ht.initialize(list3);
	locals[3].ht.initialize(list4);
    }
    
    // 
    public void read()
    {
	for(int i = 0; i < i_numOfStreams; i++)
	{
	    instances[i] = readers[i].getNextTransaction();
	}
    }
    
    public void Learn()
    {
	read();
	for(int i = 0; i < i_numOfStreams; i++)
	{
	    locals[i].Learn(convertToData(instances[i], i));
	}
    }
    
    public void sendToLocal()
    {
	for(int i = 0; i< i_numOfStreams; i++)
	{
	    String[] lData = locals[i].accept(convertToData(instances[i], i)).split(",");
	    localData[i][0] = Double.parseDouble(lData[0]);
	    localData[i][1] = Double.parseDouble(lData[1]);
	}
    }
    
    public boolean checkDrift()
    {
	read();
	sendToLocal();
	
	boolean isDrift = calculateWeight() > threshold;
	
	if(isDrift)
	{
	    resetWeight();
//	    System.out.println("------------------TRUE------------------");
	    return true;
	}
	else
	{
//	    System.out.println("------------------FALSE------------------");
	    return false;
	}

    }
    
    public void resetWeight()
    {
	for(int i = 0; i < i_numOfStreams; i++)
	{
//	    locals[i].adwin.setWeight(0.0);
	    locals[i].edd.setWeight(0.0);
	}
    }
    
    public double calculateWeight()
    {
	double x = 0.0;
	double h = 0.0;
	
	String s = "";
	
	for(int i = 0; i < i_numOfStreams; i++)
	{
	    s += localData[i][0] + "," + localData[i][1] + ",";
	    
	    h += Math.sqrt(localData[i][0] * localData[i][1]);
	    
	    x += Math.sqrt(localData[i][0] * localData[i][1]) / (4 * Math.log10(localData[i][0] + 1));
	}
	
//	System.out.println(s);
	
	return h / x;
    }
      
    public Data convertToData(String instance, int streamID)
    {
	DataImpl data = new DataImpl();
	String[] inst = instance.split(",");
	
	
	for(int i = 0; i < inst.length; i++)
	{
	    data.put(attributeLabels[streamID][i], inst[i]);
	}
	
	return data;
    }
    
    
    
}


