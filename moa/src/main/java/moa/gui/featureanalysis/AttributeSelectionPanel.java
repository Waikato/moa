/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *   AttributeSelectionPanel.java
 *   Copyright (C) 1999-2020 University of Waikato, Hamilton, New Zealand
 */
package moa.gui.featureanalysis;

import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.regex.Pattern;

/**
 * A sub panel in visualizeFeatures tab. It shows all the attributes in a data stream so that a user selects to show the attribute's
 * statistical information, graphs. This panel refers to weka.gui.AttributeSelectionPanel.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @author Yongheng Ma (2560653665@qq.com)
 */
public class AttributeSelectionPanel extends JPanel {

  /** for serialization */
  private static final long serialVersionUID = 627131485290359194L;

  /**
   * A table model that looks at the names of attributes and maintains a list of
   * attributes that have been "selected".
   */
  class AttributeTableModel extends AbstractTableModel {

    /** for serialization */
    private static final long serialVersionUID = -4152987434024338064L;

    /** The instances who's attribute structure we are reporting */
    protected Instances m_Instances;

    /** The flag for whether the instance will be included */
    protected boolean[] m_Selected;

    /**
     * Creates the tablemodel with the given set of instances.
     * 
     * @param instances the initial set of Instances
     */
    public AttributeTableModel(Instances instances) {
      setInstances(instances);
    }

    /**
     * Sets the tablemodel to look at a new set of instances.
     * 
     * @param instances the new set of Instances.
     */
    public void setInstances(Instances instances) {
      m_Instances = instances;
      m_Selected = new boolean[m_Instances.numAttributes()];
    }

    /**
     * Gets the number of attributes.
     * 
     * @return the number of attributes.
     */
    @Override
    public int getRowCount() {
      return m_Selected.length;
    }

    /**
     * Gets the number of columns: 3
     * 
     * @return 3
     */
    @Override
    public int getColumnCount() {
      return 3;
    }

    /**
     * Gets a table cell
     * 
     * @param row the row index
     * @param column the column index
     * @return the value at row, column
     */
    @Override
    public Object getValueAt(int row, int column) {

      switch (column) {
      case 0:
        return new Integer(row + 1);
      case 1:
        return new Boolean(m_Selected[row]);
      case 2:
        return m_Instances.attribute(row).name();
      default:
        return null;
      }
    }

    /**
     * Gets the name for a column.
     * 
     * @param column the column index.
     * @return the name of the column.
     */
    @Override
    public String getColumnName(int column) {

      switch (column) {
      case 0:
        return new String("No.");
      case 1:
        return new String("");
      case 2:
        return new String("Name");
      default:
        return null;
      }
    }


    /**
     * Sets the value at a cell.
     * 
     * @param value the new value.
     * @param row the row index.
     * @param col the column index.
     */
    @Override
    public void setValueAt(Object value, int row, int col) {
      if (col == 1) {
        m_Selected[row] = ((Boolean) value).booleanValue();
        fireTableRowsUpdated(0, m_Selected.length);
      }
    }

    /**
     * Gets the class of elements in a column.
     * 
     * @param col the column index.
     * @return the class of elements in the column.
     */
    @Override
    public Class<?> getColumnClass(int col) {
      return getValueAt(0, col).getClass();
    }

    /**
     * Returns true if the column is the "selected" column.
     * 
     * @param row ignored
     * @param col the column index.
     * @return true if col == 1.
     */
    @Override
    public boolean isCellEditable(int row, int col) {

      if (col == 1) {
        return true;
      }
      return false;
    }

    /**
     * Gets an array containing the indices of all selected attributes.
     * 
     * @return the array of selected indices.
     */
    public int[] getSelectedAttributes() {

      int[] r1 = new int[getRowCount()];
      int selCount = 0;
      for (int i = 0; i < getRowCount(); i++) {
        if (m_Selected[i]) {
          r1[selCount++] = i;
        }
      }
      int[] result = new int[selCount];
      System.arraycopy(r1, 0, result, 0, selCount);
      return result;
    }

    /**
     * Sets the state of all attributes to selected.
     */
    public void includeAll() {

      for (int i = 0; i < m_Selected.length; i++) {
        m_Selected[i] = true;
      }
      fireTableRowsUpdated(0, m_Selected.length);
    }

    /**
     * Deselects all attributes.
     */
    public void removeAll() {

      for (int i = 0; i < m_Selected.length; i++) {
        m_Selected[i] = false;
      }
      fireTableRowsUpdated(0, m_Selected.length);
    }

    /**
     * Inverts the selected status of each attribute.
     */
    public void invert() {

      for (int i = 0; i < m_Selected.length; i++) {
        m_Selected[i] = !m_Selected[i];
      }
      fireTableRowsUpdated(0, m_Selected.length);
    }

    /**
     * applies the perl regular expression pattern to select the attribute names
     * (expects a valid reg expression!)
     * 
     * @param pattern a perl reg. expression
     */
    public void pattern(String pattern) {
      for (int i = 0; i < m_Selected.length; i++) {
        m_Selected[i] = Pattern.matches(pattern, m_Instances.attribute(i)
          .name());
      }
      fireTableRowsUpdated(0, m_Selected.length);
    }

    public void setSelectedAttributes(boolean[] selected) throws Exception {
      if (selected.length != m_Selected.length) {
        throw new Exception("Supplied array does not have the same number "
          + "of elements as there are attributes!");
      }

      for (int i = 0; i < selected.length; i++) {
        m_Selected[i] = selected[i];
      }
      fireTableRowsUpdated(0, m_Selected.length);
    }
  }

  /** Press to select all attributes */
  protected JButton m_IncludeAll = new JButton("All");

  /** Press to deselect all attributes */
  protected JButton m_RemoveAll = new JButton("None");

  /** Press to invert the current selection */
  protected JButton m_Invert = new JButton("Invert");

  /** Press to enter a perl regular expression for selection */
  protected JButton m_Pattern = new JButton("Pattern");

  /** The table displaying attribute names and selection status */
  protected JTable m_Table = new JTable();

  /** The table model containing attribute names and selection status */
  protected AttributeTableModel m_Model;

  /** The current regular expression. */
  protected String m_PatternRegEx = "";

  /**
   * Creates the attribute selection panel with no initial instances.
   */
  public AttributeSelectionPanel() {
    this(true, true, true, true);
  }

  /**
   * Creates the attribute selection panel with no initial instances.
   *
   * @param include true if the include button is to be shown
   * @param remove true if the remove button is to be shown
   * @param invert true if the invert button is to be shown
   * @param pattern true if the pattern button is to be shown
   */
  public AttributeSelectionPanel(boolean include, boolean remove,
    boolean invert, boolean pattern) {

    m_IncludeAll.setToolTipText("Selects all attributes");
    m_IncludeAll.setEnabled(false);
    m_IncludeAll.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_Model.includeAll();
      }
    });
    m_RemoveAll.setToolTipText("Unselects all attributes");
    m_RemoveAll.setEnabled(false);
    m_RemoveAll.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_Model.removeAll();
      }
    });
    m_Invert.setToolTipText("Inverts the current attribute selection");
    m_Invert.setEnabled(false);
    m_Invert.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_Model.invert();
      }
    });
    m_Pattern
      .setToolTipText("Selects all attributes that match a reg. expression");
    m_Pattern.setEnabled(false);
    m_Pattern.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String pattern = JOptionPane.showInputDialog(m_Pattern.getParent(),
          "Enter a Perl regular expression", m_PatternRegEx);
        if (pattern != null) {
          try {
            Pattern.compile(pattern);
            m_PatternRegEx = pattern;
            m_Model.pattern(pattern);
          } catch (Exception ex) {
            JOptionPane.showMessageDialog(m_Pattern.getParent(), "'" + pattern
              + "' is not a valid Perl regular expression!\n" + "Error: " + ex,
              "Error in Pattern...", JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    });
    m_Table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    m_Table.setColumnSelectionAllowed(false);
    m_Table.setPreferredScrollableViewportSize(new Dimension(250, 150));
    m_Table.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
          || e.isAltDown()) {
          popupCopyRangeMenu(e.getX(), e.getY());
        }
      }
    });

    /** Set up the layout. */
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    p1.setLayout(new GridLayout(1, 4, 5, 5));
    if (include) {
      p1.add(m_IncludeAll);
    }
    if (remove) {
      p1.add(m_RemoveAll);
    }
    if (invert) {
      p1.add(m_Invert);
    }
    if (pattern) {
      p1.add(m_Pattern);
    }

    setLayout(new BorderLayout());
    if (include || remove || invert || pattern) {
      add(p1, BorderLayout.NORTH);
    }
    add(new JScrollPane(m_Table), BorderLayout.CENTER);
  }

  public Dimension getPreferredScrollableViewportSize() {
    return m_Table.getPreferredScrollableViewportSize();
  }

  public void setPreferredScrollableViewportSize(Dimension d) {
    m_Table.setPreferredScrollableViewportSize(d);
  }

  protected void popupCopyRangeMenu(int x, int y) {
    JPopupMenu popupMenu = new JPopupMenu();
    final JMenuItem copyRangeItem =
      new JMenuItem("Copy checked items to range in clipboard");
    popupMenu.add(copyRangeItem);

    if (getSelectedAttributes().length == 0) {
      copyRangeItem.setEnabled(false);
    }

    copyRangeItem.addActionListener(new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        int[] selected = getSelectedAttributes();
        StringBuilder b = new StringBuilder();
        int prev = -1;
        int lastInString = -1;
        for (int v : selected) {
          if (v == 0) {
            b.append("first-");
            prev = v;
            lastInString = v;
          } else {
            if (prev < 0) {
              prev = v;
              lastInString = v;
              b.append(v + 1).append("-");
            } else {
              if (v - prev == 1) {
                prev = v;
                continue;
              }
              if (b.charAt(b.length() - 1) == '-') {
                if (prev == lastInString) {
                  b.setCharAt(b.length() - 1, ',');
                } else {
                  b.append(prev + 1).append(",");
                }
              }
              if (v == m_Model.getRowCount() - 1) {
                b.append("last");
              } else {
                b.append(v + 1).append("-");
              }
              prev = v;
              lastInString = v;
            }
          }
        }
        if (b.charAt(b.length() - 1) == '-') {
          if (selected.length > 1 &&
            lastInString != selected[selected.length - 1]) {
            if (selected[selected.length - 1] == m_Model.getRowCount() - 1) {
              b.append("last");
            } else {
              b.append(selected[selected.length - 1] + 1);
            }
          } else {
            b.setLength(b.length() - 1);
          }
        }
        StringSelection selection = new StringSelection(b.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
      }
    });

    popupMenu.show(m_Table, x, y);
  }

  /**
   * Sets the instances who's attribute names will be displayed.
   *
   * @param newInstances the new set of instances
   */
  public void setInstances(Instances newInstances) {

    if (m_Model == null) {
      m_Model = new AttributeTableModel(newInstances);
      m_Table.setModel(m_Model);
      TableColumnModel tcm = m_Table.getColumnModel();
      tcm.getColumn(0).setMaxWidth(60);
      tcm.getColumn(1).setMaxWidth(tcm.getColumn(1).getMinWidth());
      tcm.getColumn(2).setMinWidth(100);
    } else {
      m_Model.setInstances(newInstances);
      m_Table.clearSelection();
    }
    m_IncludeAll.setEnabled(true);
    m_RemoveAll.setEnabled(true);
    m_Invert.setEnabled(true);
    m_Pattern.setEnabled(true);
    m_Table.sizeColumnsToFit(2);
    m_Table.revalidate();
    m_Table.repaint();
  }

  /**
   * Gets an array containing the indices of all selected attributes.
   *
   * @return the array of selected indices.
   */
  public int[] getSelectedAttributes() {

    return (m_Model == null) ? null : m_Model.getSelectedAttributes();
  }

  /**
   * Set the selected attributes in the widget. Note that setInstances() must
   * have been called first.
   *
   * @param selected an array of boolean indicating which attributes are to have
   *          their check boxes selected.
   * @throws Exception if the supplied array of booleans does not have the same
   *           number of elements as there are attributes.
   */
  public void setSelectedAttributes(boolean[] selected) throws Exception {
    if (m_Model != null) {
      m_Model.setSelectedAttributes(selected);
    }
  }

  /**
   * Get the table model in use (or null if no instances have been set yet).
   *
   * @return the table model in use or null if no instances have been seen yet.
   */
  public TableModel getTableModel() {
    return m_Model;
  }

  /**
   * Gets the selection model used by the table.
   *
   * @return a value of type 'ListSelectionModel'
   */
  public ListSelectionModel getSelectionModel() {

    return m_Table.getSelectionModel();
  }

  /**
   * Tests the attribute selection panel from the command line.
   *
   * @param args must contain the name of an arff file to load.
   */
  public static void main(String[] args) {
    WekaToSamoaInstanceConverter m_wekaToSamoaInstanceConverter = new WekaToSamoaInstanceConverter();
    try {
      if (args.length == 0) {
        throw new Exception("supply the name of an arff file");
      }
      weka.core.Instances i = new weka.core.Instances(new java.io.BufferedReader(
        new java.io.FileReader(args[0])));
      AttributeSelectionPanel asp = new AttributeSelectionPanel();
      final javax.swing.JFrame jf = new javax.swing.JFrame(
        "Attribute Selection Panel");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(asp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.pack();
      jf.setVisible(true);
      asp.setInstances(m_wekaToSamoaInstanceConverter.samoaInstances(i));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

}
