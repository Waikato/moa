/*
 *    Timestamp.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Wels (moa@cs.rwth-aachen.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
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
