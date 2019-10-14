package moa.tasks.JSON;

/**
 * Implement a code cell
 */
public class CodeCell extends NotebookCell {
    private int execution_count;
    private StringBuilder outputs;

    public CodeCell() {
        cell = new StringBuilder();
        metadata = new StringBuilder();
        source = new StringBuilder();
        cell_type = new StringBuilder("code");
        outputs = new StringBuilder();
        execution_count = 0;
    }

    /**
     * Adds a string to cell in a new separate line
     *
     * @param st   the string to be added
     */
    public NotebookCell addNewLine(String st) {
        switch (workingMode) {
            case CELL_TYPE:
                if(cell_type.length() > 0)
                    cell_type.append(",\n");
                cell_type.append("\"" + st + "\\n\"");
                break;
            case METADATA:
                if(metadata.length() > 0)
                    metadata.append(",\n");
                metadata.append("\"" + st + "\\n\"");
                break;
            case SOURCE:
                if(source.length() > 0)
                    source.append(",\n");
                source.append("\"" + st + "\\n\"");
                break;
            case OUTPUTS:
                if(outputs.length() > 0)
                    outputs.append(",\n");
                outputs.append("\"" + st + "\\n\"");
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
                cell_type.insert(cell_type.length() - 3, st);
                break;
            case METADATA:
                metadata.insert(metadata.length() - 3, st);
                break;
            case SOURCE:
                source.insert(source.length() - 3, st);
                break;
            case OUTPUTS:
                outputs.insert(source.length() - 3, st);
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
