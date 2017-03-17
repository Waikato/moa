/*
 *    IademNominalAttributeMultiwayTest.java
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

import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;

public class IademNominalAttributeMultiwayTest extends NominalAttributeMultiwayTest {
    int maxBranches;

    public IademNominalAttributeMultiwayTest(int attIndex, int maxBranches) {
        super(attIndex);
        this.maxBranches = maxBranches;
    }
    
    @Override
    public int maxBranches() {
        return maxBranches;
    }
}
