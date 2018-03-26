/*
 *    ALMainTask.java
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
package moa.tasks.meta;

import java.util.List;

/**
 * This class provides a superclass for Active Learning tasks, which 
 * enables convenient searching for those tasks for example when showing 
 * a list of available Active Learning tasks.
 * Furthermore, it specifies the type of threads being used as
 * {@link ALTaskThread}.
 * 
 * @author Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
 * @version $Revision: 1 $
 */
public abstract class ALMainTask extends MetaMainTask {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public abstract List<ALTaskThread> getSubtaskThreads();
}
