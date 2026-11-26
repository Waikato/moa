/*
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
 */

package moa.classifiers.meta;

import com.yahoo.labs.samoa.instances.Instances;
import java.io.Serializable;

/**
 * Strategy interface for computing similarity distances between a query window
 * and stored concept topology prototypes in {@link GroCH}.
 */
public interface ConceptDistance extends Serializable {

    /**
     * Computes the distance between two sets of instances/prototypes.
     */
    double distance(Instances query, Instances target);

    /**
     * Optional pre-extraction hook executed once per query window before
     * iterating over all candidate groups in Concept History.
     */
    default double[][] prepareQuery(Instances query) {
        return null;
    }

    /**
     * Fast-path distance evaluation using pre-extracted primitive matrices
     * when supported by the concrete strategy.
     */
    default double distance(Instances query, double[][] queryMatrix, Instances target, double[][] targetWeights) {
        return distance(query, target);
    }
}
