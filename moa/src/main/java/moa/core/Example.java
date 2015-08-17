package moa.core;

public interface Example< T extends Object> {

	public T getData();

	public double weight();
	
	public void setWeight(double weight);

	public Example copy();
} 
