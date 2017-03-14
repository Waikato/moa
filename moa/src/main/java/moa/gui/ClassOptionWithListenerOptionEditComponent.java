package moa.gui;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.ClassOptionEditComponent;

import moa.options.ClassOptionWithListenerOption;

public class ClassOptionWithListenerOptionEditComponent extends ClassOptionEditComponent {
	
	private static final long serialVersionUID = 1L;

	public ClassOptionWithListenerOptionEditComponent(Option opt) {
		super(opt);
		
		this.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!ClassOptionWithListenerOptionEditComponent.this.textField.getText().isEmpty()) {
					ClassOptionWithListenerOptionEditComponent.this.applyState();
					
					((ClassOptionWithListenerOption) opt).getListener().stateChanged(e);
				}
			}
			
		});
	}

}
