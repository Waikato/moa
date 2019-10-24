package moa.tasks.JSON;

/**
 * Abstract class of a cell
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public abstract class NotebookCell {
    public enum Mode {
        CELL_TYPE,
        EXECUTION_COUNT,
        METADATA,
        OUTPUTS,
        SOURCE;
    }
/** All fields of a cell
 * **/
    protected StringBuilder cell_type;
    protected StringBuilder metadata;
    protected StringBuilder source;
    protected StringBuilder cell;
    protected Mode workingMode;

    public NotebookCell() {
        cell = new StringBuilder();
        cell_type = new StringBuilder();
        metadata = new StringBuilder();
        source = new StringBuilder();
        workingMode = Mode.SOURCE;
    }

    public NotebookCell setWorkingMode(Mode workingMode) {
        this.workingMode = workingMode;
        return this;
    }

    /**
     * Appends a string to cell in a new separate line
     * Depending on the working mode, the string @param st will be added to particular field
     * @param st the string to be added
     */
    public NotebookCell addNewLine(String st) {
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
        }
        return this;
    }

    /**
     * Adds a string to cell in a current line at the last position before the return (\n) character
     *
     * @param st the string to be added
     */
    public NotebookCell add(String st) {
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
        }

        return this;
    }

    public void addNewLineToStringBuilder(String st, StringBuilder builder){
        if(builder.length() > 0)
            builder.append(",\n");
        builder.append("\"" + st.replace("\"", "\\\"") + "\\n\"");
    }

    public void addToStringBuilder(String st, StringBuilder builder){
        builder.insert(cell_type.length() - 3, st.replace("\"", "\\\""));
    }

    /**
     * Create a cell with the right format
     */
    public NotebookCell createCell() {
        cell.append("\"" + "cell_type" + "\"" + ": " + "\"")
                .append(cell_type)
                .append("\",\n")
                .append("\"" + "metadata" + "\"" + ": " + "{")
                .append(metadata)
                .append("},\n")
                .append("\"" + "source" + "\"" + ": " + "[")
                .append(source)
                .append("]\n")
                .insert(0, "{\n")
                .append("}");
        return this;
    }

    public StringBuilder getCell() {
        return this.cell;
    }
}
