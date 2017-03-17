/*
 *    IademGreenwaldKhannaQuantileSummary.java
 *
 *    @author Isvani Frias-Blanco
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

package moa.classifiers.trees.iadem;

import moa.core.GreenwaldKhannaQuantileSummary;

public class IademGreenwaldKhannaQuantileSummary extends GreenwaldKhannaQuantileSummary {

    private static final long serialVersionUID = 1L;

    public IademGreenwaldKhannaQuantileSummary(int maxTuples) {
        super(maxTuples);
    }

    public int maxNumberOfObservation(int i) { 
        int index = i - 1;
        if (index >= 0 && index < this.summary.length && this.summary[index] != null) {
            return (int) (this.summary[index].g + this.summary[index].delta - 1);
        }
        return 0;
    }

    @Override
    public int findIndexOfTupleGreaterThan(double val) {
        return super.findIndexOfTupleGreaterThan(val);
    }
}
