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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * Optional semicolon-separated event logger for warnings, drifts, and Concept
 * History lifecycle events. Disabled unless an explicit output path is given.
 */
public class DriftEventLogger implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String[] HEADERS = {
        "instance_number", "event_type", "affected_position",
        "voting_weight",
        "warning_setting", "drift_setting", "affected_classifier_created_on", "error_percentage",
        "amount_of_classifiers", "amount_of_active_warnings", "classifiers_on_warning", "concept_history_snapshot",
        "recurring_drift_to_history_id", "recurring_drift_to_classifier_created_on",
        "gng_Seen_examples", "gng_prototypes", "topology_error"
    };

    private transient PrintWriter writer;

    public DriftEventLogger(String filePath, boolean disabled) {
        if (disabled || filePath == null || filePath.trim().isEmpty()) {
            this.writer = null;
            return;
        }
        try {
            this.writer = new PrintWriter(filePath);
            log(HEADERS);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            this.writer = null;
        }
    }

    public boolean isEnabled() {
        return this.writer != null;
    }

    public void log(String[] fields) {
        if (this.writer != null && fields != null) {
            this.writer.println(String.join(";", fields));
            this.writer.flush();
        }
    }
}
