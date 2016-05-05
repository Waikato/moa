package driftmodelintegration;

import java.io.Serializable;

import cutpointdetection.OnePassDetector.EDD;
import driftmodelintegration.core.Data;

public class LocalStatisticPI
{
    HoeffdingTree ht;
//    ADWIN adwin;
    EDD edd;
    
    double pCount = 0;
    double pTotal = 0;
        
    public LocalStatisticPI(double delta)
    {
	ht = new HoeffdingTree();
//	adwin = new ADWIN(delta);
	edd = new EDD(delta, 100, 1, 0);
    }
    
    public void Learn(Data instance)
    {
	ht.learn(instance);
    }
    
    public String accept(Data instance)
    {
	double weight = 0.0;
	//Receive instance and return updated statistics
	Serializable classLabel = ht.getModel().predict(instance);
	
	if(classLabel.equals(instance.get(ht.getLabelAttribute())))
	{
	    pCount++;
	    pTotal++;
//	    adwin.setInput(1.0);
	    edd.setInput(1.0);
	}
	else
	{
	    pTotal++;
//	    adwin.setInput(0.0);
	    edd.setInput(0.0);
	}
	
//	weight = adwin.getWeight();
	weight = edd.getWeight();
	
	return weight + "," + pCount / pTotal;
    }
    
    
        
}
