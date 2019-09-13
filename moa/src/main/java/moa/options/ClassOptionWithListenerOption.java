/*
 *    ClassOptionWithListenerOption.java
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
package moa.options;

import javax.swing.event.ChangeListener;

/**
 * ClassOption that can be given a ChangeListener. The listener is notified
 * whenever a new value is set for this option.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public class ClassOptionWithListenerOption extends ClassOption {
	
	private static final long serialVersionUID = 1L;
	
	protected ChangeListener listener;
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType,
			String defaultCLIString) 
	{
		super(name, cliChar, purpose, requiredType, defaultCLIString);
	}
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType,
			String defaultCLIString, ChangeListener listener) 
	{
		super(name, cliChar, purpose, requiredType, defaultCLIString);
		this.listener = listener;
	}
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType, 
			String defaultCLIString, String nullString) 
	{
        super(name, cliChar, purpose, requiredType, defaultCLIString, 
        		nullString);
    }
	
	public ClassOptionWithListenerOption(String name, char cliChar, 
			String purpose, Class<?> requiredType, 
			String defaultCLIString, String nullString,
			ChangeListener listener) 
	{
        super(name, cliChar, purpose, requiredType, defaultCLIString, 
        		nullString);
        this.listener = listener;
    }
	
	public void setChangeListener(ChangeListener listener) {
		this.listener = listener;
	}
	
	public ChangeListener getChangeListener() {
		return this.listener;
	}
	
	@Override
	public void setValueViaCLIString(String s) {
		super.setValueViaCLIString(s);
		
		// inform dependent options about change
		if (this.listener != null) {
			this.listener.stateChanged(null);
		}
	}

}
