package moa.core;

public interface Example<T extends Object> {

	T getData();

	double weight();

	void setWeight(double weight);

	Example copy();
}
