package moa.tasks.JSON;

/**
 * Implement a markdown cell
 */
public class MarkDownCell extends NotebookCell {
    public MarkDownCell() {
        cell = new StringBuilder();
        metadata = new StringBuilder();
        source = new StringBuilder();
        cell_type = new StringBuilder("markdown");
    }
}
