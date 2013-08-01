/*
 *    Timestamp.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Wels (moa@cs.rwth-aachen.de)
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

package moa.clusterers.denstream;

import moa.AbstractMOAObject;
public class Timestamp extends AbstractMOAObject{

    private long timestamp;

    public Timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp() {
        timestamp = 0;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void increase() {
        timestamp++;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void getDescription(StringBuilder sb, int i) {
    }
}
