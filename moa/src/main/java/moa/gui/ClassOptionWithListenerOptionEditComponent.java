/*
 *    ClassOptionWithListenerOptionEditComponent.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.gui;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.ClassOptionEditComponent;

import moa.options.ClassOptionWithListenerOption;

/**
 * EditComponent for the {@link ClassOptionWithListenerOption}. When its state
 * is changed, the changes are directly applied to the corresponding Option so
 * that potential listeners can be notified and updates can be processed.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ClassOptionWithListenerOptionEditComponent extends ClassOptionEditComponent {
	
	private static final long serialVersionUID = 1L;

	public ClassOptionWithListenerOptionEditComponent(Option opt) {
		super(opt);
		
		this.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!ClassOptionWithListenerOptionEditComponent.this.textField
						.getText().isEmpty()) 
				{
					// apply state to set the selected value in the ClassOption
					// so that it can be picked up by dependent options
					ClassOptionWithListenerOptionEditComponent.this.applyState();
				}
			}
			
		});
	}

}
