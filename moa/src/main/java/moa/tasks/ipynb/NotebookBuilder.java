package moa.tasks.ipynb;

import java.util.ArrayList;

/**
 * Manage the list of all cells
 * Add new cells
 * Create a Jupyter NotebookBuilder as IPYNB file
 *
 * @author Truong To (todinhtruong at gmail dot com)
 */
public class NotebookBuilder {
    private ArrayList<NotebookCellBuilder> notebookCells;
    private int executionCount;

    public NotebookBuilder(){
        notebookCells = new ArrayList<>();
        executionCount = 1;
    }

    public String build() {
        StringBuilder notebook = new StringBuilder();

        // Open the notebook object and the cells array
        notebook.append(
              "{\n" +
              "\"cells\": [\n"
        );

        for (NotebookCellBuilder cell : notebookCells) {
            notebook.append(cell.build())
                    .append(",\n");
        }

        // Delete the last trailing comma
        if (notebookCells.size() != 0) notebook.deleteCharAt(notebook.length() - 2);

        // Close the cells array and the notebook object
        notebook.append("],\n")
                .append(
                      "\"metadata\": {},\n" +
                      "\"nbformat\": 4,\n" +
                      "\"nbformat_minor\": 0\n" +
                      "}\n"
                );

        return notebook.toString();
    }

    public NotebookCellBuilder addCode(){
        CodeCellBuilder cell = new CodeCellBuilder();
        cell.executionCount = executionCount++;
        notebookCells.add(cell);

        return cell;
    }

    public NotebookCellBuilder addMarkdown(){
        NotebookCellBuilder cell = new MarkDownCellBuilder();
        notebookCells.add(cell);

        return cell;
    }

    public NotebookCellBuilder addRaw(){
        NotebookCellBuilder cell = new RawCellBuilder();
        notebookCells.add(cell);

        return cell;
    }

    public NotebookCellBuilder getLastCell() {
        return notebookCells.get(notebookCells.size() - 1);
    }

    public NotebookCellBuilder getCellByIndex(int index){
        return notebookCells.get(index);
    }
}
