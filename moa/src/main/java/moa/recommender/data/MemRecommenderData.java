/*
 *    MemRecommenderData.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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

package moa.recommender.data;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class MemRecommenderData extends AbstractOptionHandler implements RecommenderData {
    
     moa.recommender.rc.data.impl.MemRecommenderData drm;
    
    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        drm = new moa.recommender.rc.data.impl.MemRecommenderData();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public moa.recommender.rc.data.RecommenderData getData() {
        return drm;
    }
    
}
