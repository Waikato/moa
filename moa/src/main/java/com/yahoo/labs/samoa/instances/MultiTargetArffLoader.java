/*
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */
package com.yahoo.labs.samoa.instances;

import java.io.Reader;

public class MultiTargetArffLoader extends ArffLoader {

    public MultiTargetArffLoader(Reader reader) {
        super(reader);
    }

    public MultiTargetArffLoader(Reader reader, Range range) {
        super(reader, range);
    }

    @Override
    protected Instance newSparseInstance(double d, double[] res) {
        return new SparseInstance(d, res); // TODO
    }

    @Override
    protected Instance newDenseInstance(int numAttributes) {
        // numAttributes is this.instanceInformation.numAttributes()
        this.range.setUpper(numAttributes);
        return new DenseInstance(numAttributes);
    }

}
