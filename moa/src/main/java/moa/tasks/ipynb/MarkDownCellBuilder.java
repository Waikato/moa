package moa.tasks.ipynb;

/**
 * Implement a markdown cell
 */
public class MarkDownCellBuilder extends NotebookCellBuilder {

    @Override
    protected String cellType() {
        return "markdown";
    }

}
