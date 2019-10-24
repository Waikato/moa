package moa.tasks.JSON;

/**
 * Implement a code cell
 */
public class CodeCell extends NotebookCell {
    private int execution_count;
    private StringBuilder outputs;

    public CodeCell() {
        cell_type = new StringBuilder("code");
        outputs = new StringBuilder();
        execution_count = 0;
    }

    /**
     * Appends a string to cell in a new separate line
     *
     * @param st   the string to be appended
     */
    public NotebookCell addNewLine(String st) {
        StringBuilder workingString = new StringBuilder();
        switch (workingMode) {
            case CELL_TYPE:
                addNewLineToStringBuilder(st, cell_type);
                break;
            case METADATA:
                addNewLineToStringBuilder(st, metadata);
                break;
            case SOURCE:
                addNewLineToStringBuilder(st, source);
                break;
            case OUTPUTS:
                addNewLineToStringBuilder(st, outputs);
                break;
            case EXECUTION_COUNT:
                execution_count = Integer.valueOf(st);
                break;
        }

        return this;
    }

    /**
     * Adds a string to cell in a current line at the last position before the return (\n) character
     *
     * @param st   the string to be added
     */
    public NotebookCell add(String st) {

        switch (workingMode) {
            case CELL_TYPE:
                addToStringBuilder(st,cell_type);
                break;
            case METADATA:
                addToStringBuilder(st,metadata);
                break;
            case SOURCE:
                addToStringBuilder(st,source);
                break;
            case OUTPUTS:
                addToStringBuilder(st,outputs);
                break;
            case EXECUTION_COUNT:
                execution_count = Integer.valueOf(st);
                break;
        }
        return this;
    }

    /**
     * Creates a cell with the right format
     */
    public NotebookCell createCell() {
        cell.append("\"" + "cell_type" + "\"" + ": " + "\"")
                .append(cell_type)
                .append("\",\n")
                .append("\"" + "execution_count" + "\"" + ": ")
                .append(execution_count)
                .append(",\n")
                .append("\"" + "metadata" + "\"" + ": " + "{")
                .append(metadata)
                .append("},\n")
                .append("\"" + "outputs" + "\"" + ": " + "[")
                .append(outputs)
                .append("],\n")
                .append("\"" + "source" + "\"" + ": " + "[")
                .append(source)
                .append("]\n")
                .insert(0, "{\n")
                .append("}");
        return this;
    }
}
