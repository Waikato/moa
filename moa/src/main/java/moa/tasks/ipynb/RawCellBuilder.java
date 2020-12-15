package moa.tasks.ipynb;

/**
 * Implement a raw cell
 */
public class RawCellBuilder extends NotebookCellBuilder {

    @Override
    protected String cellType() {
        return "raw";
    }

}
