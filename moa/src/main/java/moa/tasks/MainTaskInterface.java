package moa.tasks;

public interface MainTaskInterface {

	Object doTask();

	String getCLICreationString(Class<?> c);

	Object copy();
}
