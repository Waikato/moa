package moa.gui;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.ClassOptionEditComponent;

import moa.options.ClassOptionWithListenerOption;

public class ClassOptionWithListenerOptionEditComponent extends ClassOptionEditComponent {
	
	private static final long serialVersionUID = 1L;

	public ClassOptionWithListenerOptionEditComponent(Option opt) {
		super(opt);
		
		System.out.println("ClassOptionWithListenerOptionEditComponent created");
		
		this.addChangeListener(((ClassOptionWithListenerOption) opt).getListener());
	}

}
