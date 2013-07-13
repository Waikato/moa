/*
 *    MemRecommenderData.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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
