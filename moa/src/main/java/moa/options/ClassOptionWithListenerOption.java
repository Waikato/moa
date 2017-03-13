package moa.options;

import javax.swing.event.ChangeListener;

public class ClassOptionWithListenerOption extends ClassOption {
	
	private static final long serialVersionUID = 1L;
	
	protected ChangeListener listener;
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType,
			String defaultCLIString, ChangeListener listener) 
	{
		super(name, cliChar, purpose, requiredType, defaultCLIString);
		this.listener = listener;
		
		System.out.println("ClassOptionWithListenerOption created");
	}
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType, 
			String defaultCLIString, String nullString,
			ChangeListener listener) {
        super(name, cliChar, purpose, requiredType, defaultCLIString, 
        		nullString);
        this.listener = listener;
    }
	
	public ChangeListener getListener() {
		return this.listener;
	}

}
