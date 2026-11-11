/*
 *    PruningEngine.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Vinicius H.A Souza (alves.vinicius@ufpr.br)
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
 */



package moa.classifiers.dynse.pruning;

import com.yahoo.labs.samoa.instances.Instances;
import moa.MOAObject;
import moa.classifiers.Classifier;

import java.util.List;

/* Defines an interface for Pruning engines, a module for the dynse classifier */
public interface PruningEngine extends MOAObject {

    void prune(List<Classifier> pool, Instances accuracyEstimationWindow,
               Classifier newClassifier, int maxPoolSize);

    void reset();
}
