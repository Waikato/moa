/*
 *    Buffer.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
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
package moa.gui.experimentertab;

import moa.tasks.MainTask;

/**
 * This class is the buffer where the threads get each task to execute
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class Buffer {

    MainTask tasks[];
    int cantTask = 0;

    /**
     * Buffer Constructor 
     * @param tasks
     */
    public Buffer(MainTask tasks[]) {
        this.tasks = tasks;
    }

    /**
     * Returns the next task to be executed.
     *
     * @return the next task to be executed.
     */
    synchronized MainTask getTask() {
        if (this.tasks.length != this.cantTask) {
            return this.tasks[this.cantTask++];
        }
        return null;
    }

    /**
     * Returns the number of executed tasks.
     *
     * @return the number of executed tasks.
     */
    synchronized int getCantTask() {
        return this.cantTask;
    }

    /**
     * Returns the number of tasks.
     *
     * @return the number of tasks.
     */
    synchronized int getSize() {
        return this.tasks.length;
    }

}
