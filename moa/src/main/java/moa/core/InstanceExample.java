package moa.core;

import com.yahoo.labs.samoa.instances.Instance;

public class InstanceExample implements Example<Instance> {

	public Instance instance;

  	public InstanceExample (Instance inst)
   	{                             
		this.instance = inst;    
  	}  

	@Override
	public Instance getData() {
		return this.instance;
	}
	
	@Override
	public double weight() {
		return this.instance.weight();
	}

	@Override
	public void setWeight(double w) {
		this.instance.setWeight(w);
	}

	@Override
	public Example copy() {
		return new InstanceExample(instance.copy());
	}

} 
