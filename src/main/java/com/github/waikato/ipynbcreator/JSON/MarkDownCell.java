package moa.tasks.JSON;

/**
 * Implement a markdown cell
 */
public class MarkDownCell extends NotebookCell {
    public MarkDownCell() {
        cell_type = new StringBuilder("markdown");
    }
}
