package moa.tasks.ipynb;

import java.util.Map;

/**
 * Implement a code cell
 */
public class CodeCellBuilder extends NotebookCellBuilder {
    int executionCount;

    CodeCellBuilder() {
        executionCount = 0;
    }

    @Override
    protected String cellType() {
        return "code";
    }

    @Override
    protected Map<String, String> fields() {
        Map<String, String> fields = super.fields();

        fields.put("execution_count", Integer.toString(executionCount));
        fields.put("outputs", "[]");

        return fields;
    }
}
