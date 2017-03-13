package moa.gui;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.ClassOptionEditComponent;

import moa.options.ClassOptionWithListener;

public class ClassOptionWithListenerEditComponent extends ClassOptionEditComponent {
	
	private static final long serialVersionUID = 1L;

	public ClassOptionWithListenerEditComponent(Option opt) {
		super(opt);
		
		System.out.println("ClassOptionWithListenerEditComponent created");
		
		this.addChangeListener(((ClassOptionWithListener) opt).getListener());
	}

}
