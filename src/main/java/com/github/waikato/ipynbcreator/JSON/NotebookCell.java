package moa.tasks.JSON;

/**
 * Abstract class of a cell
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public abstract class NotebookCell {
    public enum Mod {
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
    protected Mod workingMode;

    public NotebookCell() {
        cell = new StringBuilder();
        cell_type = new StringBuilder();
        metadata = new StringBuilder();
        source = new StringBuilder();
        workingMode = Mod.SOURCE;
    }

    public NotebookCell setWorkingMode(Mod workingMode) {
        this.workingMode = workingMode;
        return this;
    }

    /**
     * Adds a string to cell in a new separate line
     * Depending on the working mode, the string @param st will be added to particular field
     * @param st the string to be added
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
                cell_type.insert(cell_type.length() - 3, st);
                break;
            case METADATA:
                metadata.insert(metadata.length() - 3, st);
                break;
            case SOURCE:
                source.insert(source.length() - 3, st);
                break;
        }

        return this;
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
