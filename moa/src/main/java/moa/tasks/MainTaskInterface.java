package moa.tasks;

public interface MainTaskInterface {

	public Object doTask();
	
	public String getCLICreationString(Class<?> c);
	
	public Object copy();
}
