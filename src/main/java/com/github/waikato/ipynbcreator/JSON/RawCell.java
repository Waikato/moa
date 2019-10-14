package moa.tasks.JSON;

/**
 * Implement a raw cell
 */
public class RawCell extends NotebookCell {
    public RawCell() {
        cell = new StringBuilder();
        metadata = new StringBuilder();
        source = new StringBuilder();
        cell_type = new StringBuilder("raw");
    }
}
