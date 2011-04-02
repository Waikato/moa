/*
 *    TaskManagerPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import moa.core.StringUtils;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.tasks.LearnModel;
import moa.tasks.MainTask;
import moa.tasks.Task;
import moa.tasks.TaskThread;

/**
 * This panel displays the running tasks.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class TaskManagerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int MILLISECS_BETWEEN_REFRESH = 600;

    public class ProgressCellRenderer extends JProgressBar implements
            TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public ProgressCellRenderer() {
            super(SwingConstants.HORIZONTAL, 0, 10000);
            setBorderPainted(false);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            if (value instanceof Double) {
                double frac = ((Double) value).doubleValue();
                if (frac >= 0.0) {
                    setIndeterminate(false);
                    setValue((int) (frac * 10000.0));
                    setString(StringUtils.doubleToString(frac * 100.0, 2, 2));
                } else {
                    setValue(0);
                    setIndeterminate(true);
                    setString("?");
                }
            }
            return this;
        }

        @Override
        public void validate() {
        }

        @Override
        public void revalidate() {
        }

        @Override
        protected void firePropertyChange(String propertyName, Object oldValue,
                Object newValue) {
        }

        @Override
        public void firePropertyChange(String propertyName, boolean oldValue,
                boolean newValue) {
        }
    }

    protected class TaskTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "command";
                case 1:
                    return "status";
                case 2:
                    return "time elapsed";
                case 3:
                    return "current activity";
                case 4:
                    return "% complete";
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return TaskManagerPanel.this.taskList.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            TaskThread thread = TaskManagerPanel.this.taskList.get(row);
            switch (col) {
                case 0:
                    return ((OptionHandler) thread.getTask()).getCLICreationString(MainTask.class);
                case 1:
                    return thread.getCurrentStatusString();
                case 2:
                    return StringUtils.secondsToDHMSString(thread.getCPUSecondsElapsed());
                case 3:
                    return thread.getCurrentActivityString();
                case 4:
                    return new Double(thread.getCurrentActivityFracComplete());
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    protected MainTask currentTask = new LearnModel();

    protected List<TaskThread> taskList = new ArrayList<TaskThread>();

    protected JButton configureTaskButton = new JButton("Configure");

    protected JTextField taskDescField = new JTextField();

    protected JButton runTaskButton = new JButton("Run");

    protected TaskTableModel taskTableModel;

    protected JTable taskTable;

    protected JButton pauseTaskButton = new JButton("Pause");

    protected JButton resumeTaskButton = new JButton("Resume");

    protected JButton cancelTaskButton = new JButton("Cancel");

    protected JButton deleteTaskButton = new JButton("Delete");

    protected PreviewPanel previewPanel;

    public TaskManagerPanel() {
        this.taskDescField.setText(this.currentTask.getCLICreationString(MainTask.class));
        this.taskDescField.setEditable(false);
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BorderLayout());
        configPanel.add(this.configureTaskButton, BorderLayout.WEST);
        configPanel.add(this.taskDescField, BorderLayout.CENTER);
        configPanel.add(this.runTaskButton, BorderLayout.EAST);
        this.taskTableModel = new TaskTableModel();
        this.taskTable = new JTable(this.taskTableModel);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.taskTable.getColumnModel().getColumn(1).setCellRenderer(
                centerRenderer);
        this.taskTable.getColumnModel().getColumn(2).setCellRenderer(
                centerRenderer);
        this.taskTable.getColumnModel().getColumn(4).setCellRenderer(
                new ProgressCellRenderer());
        JPanel controlPanel = new JPanel();
        controlPanel.add(this.pauseTaskButton);
        controlPanel.add(this.resumeTaskButton);
        controlPanel.add(this.cancelTaskButton);
        controlPanel.add(this.deleteTaskButton);
        setLayout(new BorderLayout());
        add(configPanel, BorderLayout.NORTH);
        add(new JScrollPane(this.taskTable), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        this.taskTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent arg0) {
                        taskSelectionChanged();
                    }
                });
        this.configureTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String newTaskString = ClassOptionSelectionPanel.showSelectClassDialog(TaskManagerPanel.this,
                        "Configure task", MainTask.class,
                        TaskManagerPanel.this.currentTask.getCLICreationString(MainTask.class),
                        null);
                setTaskString(newTaskString);
            }
        });
        this.runTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                runTask((Task) TaskManagerPanel.this.currentTask.copy());
            }
        });
        this.pauseTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                pauseSelectedTasks();
            }
        });
        this.resumeTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                resumeSelectedTasks();
            }
        });
        this.cancelTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                cancelSelectedTasks();
            }
        });
        this.deleteTaskButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                deleteSelectedTasks();
            }
        });
        javax.swing.Timer updateListTimer = new javax.swing.Timer(
                MILLISECS_BETWEEN_REFRESH, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TaskManagerPanel.this.taskTable.repaint();
            }
        });
        updateListTimer.start();
        setPreferredSize(new Dimension(0, 200));
    }

    public void setPreviewPanel(PreviewPanel previewPanel) {
        this.previewPanel = previewPanel;
    }

    public void setTaskString(String cliString) {
        try {
            this.currentTask = (MainTask) ClassOption.cliStringToObject(
                    cliString, MainTask.class, null);
            this.taskDescField.setText(this.currentTask.getCLICreationString(MainTask.class));
        } catch (Exception ex) {
            GUIUtils.showExceptionDialog(this, "Problem with task", ex);
        }
    }

    public void runTask(Task task) {
        TaskThread thread = new TaskThread(task);
        this.taskList.add(0, thread);
        this.taskTableModel.fireTableDataChanged();
        this.taskTable.setRowSelectionInterval(0, 0);
        thread.start();
    }

    public void taskSelectionChanged() {
        TaskThread[] selectedTasks = getSelectedTasks();
        if (selectedTasks.length == 1) {
            setTaskString(((OptionHandler) selectedTasks[0].getTask()).getCLICreationString(MainTask.class));
            if (this.previewPanel != null) {
                this.previewPanel.setTaskThreadToPreview(selectedTasks[0]);
            }
        } else {
            this.previewPanel.setTaskThreadToPreview(null);
        }
    }

    public TaskThread[] getSelectedTasks() {
        int[] selectedRows = this.taskTable.getSelectedRows();
        TaskThread[] selectedTasks = new TaskThread[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            selectedTasks[i] = this.taskList.get(selectedRows[i]);
        }
        return selectedTasks;
    }

    public void pauseSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.pauseTask();
        }
    }

    public void resumeSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.resumeTask();
        }
    }

    public void cancelSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.cancelTask();
        }
    }

    public void deleteSelectedTasks() {
        TaskThread[] selectedTasks = getSelectedTasks();
        for (TaskThread thread : selectedTasks) {
            thread.cancelTask();
            this.taskList.remove(thread);
        }
        this.taskTableModel.fireTableDataChanged();
    }

    private static void createAndShowGUI() {

        // Create and set up the window.
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JPanel panel = new TaskManagerPanel();
        panel.setOpaque(true); // content panes must be opaque
        frame.setContentPane(panel);

        // Display the window.
        frame.pack();
        // frame.setSize(400, 400);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
