package moa.tasks.ipynb;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class of a cell
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public abstract class NotebookCellBuilder {

    private StringBuilder source;

    NotebookCellBuilder() {
        source = new StringBuilder();
    }

    /**
     * Gets the cell-type string of this type of cell.
     *
     * @return
     *          The cell-type string.
     */
    protected abstract String cellType();

    /**
     * Appends a line of source to cell on a new separate line.
     *
     * @param line
     *          The line of source to be added.
     */
    final public NotebookCellBuilder addSource(String line) {
        if(source.length() > 0) source.append(",\n");
        source
              .append("\"")
              .append(line.replace("\"", "\\\""))
              .append("\\n\"");
        return this;
    }

    /**
     * Defines the fields of this cell and their contents. Does not
     * include the "source" field, as this is handled separately.
     *
     * @return
     *          A map from field name to field contents.
     */
    protected Map<String, String> fields() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("cell_type", "\"" + cellType() + "\"");
        fields.put("metadata", "{}");
        return fields;
    }

    /**
     * Create a cell with the right format
     */
    final public StringBuilder build() {
        // Create the buffer of the entire cell's contents
        StringBuilder cell = new StringBuilder();

        // Add the opening brace
        cell.append("{\n");

        // Add the fields of the cell
        for (Map.Entry<String, String> fieldEntry : fields().entrySet()) {
            String fieldName = fieldEntry.getKey();
            String fieldContents = fieldEntry.getValue();

            cell.append("\"")
                  .append(fieldName)
                  .append("\": ")
                  .append(fieldContents)
                  .append(",\n");
        }

        // Add the source of the cell
        cell.append("\"source\": [\n")
                .append(source)
                .append("\n]\n");

        // Add the closing brace
        cell.append("}");

        return cell;
    }
}
