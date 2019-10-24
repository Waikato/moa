package moa.tasks.JSON;

/**
 * Implement a raw cell
 */
public class RawCell extends NotebookCell {
    public RawCell() {
        cell_type = new StringBuilder("raw");
    }
}
