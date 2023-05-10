/*
 *    ImageTreePanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand 
 *    @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.gui.experimentertab;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 * This class creates a JTree panel to show the images generated with
 * JFreeChart.
 *
 * @author Alberto
 */
public class ImageTreePanel extends JPanel
        implements TreeSelectionListener {

    private JPanel imgPanel;
    private JTree tree;
    private ImageChart chart[];
    private ImagePanel chartPanel[];

    /**
     * Constructor.
     * @param chart
     */
    public ImageTreePanel(ImageChart chart[]) {
        super(new GridLayout(1, 0));
        this.chart = chart;
        //Create the nodes.
        DefaultMutableTreeNode top
                = new DefaultMutableTreeNode("Images");
        imgPanel = new JPanel();
        imgPanel.setLayout(new GridLayout(1, 0));
        createNodes(top);
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        tree.setSelectionRow(1);

        tree.addTreeSelectionListener(this);
        ImageIcon leafIcon = new ImageIcon("icon/img.png");
        if (leafIcon != null) {
            DefaultTreeCellRenderer renderer
                    = new DefaultTreeCellRenderer();
            renderer.setLeafIcon(leafIcon);
            tree.setCellRenderer(renderer);
        }
        imgPanel.updateUI();
        JScrollPane treeView = new JScrollPane(tree);
        treeView.setMinimumSize(new Dimension(100, 50));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(imgPanel);
        splitPane.setDividerLocation(100);
        splitPane.setPreferredSize(new Dimension(500, 300));
        add(splitPane);

    }

    private void createNodes(DefaultMutableTreeNode top) {

        DefaultMutableTreeNode child = null;

        for (ImageChart chart1 : chart) {
            child = new DefaultMutableTreeNode(chart1);
            ImagePanel chPanel = new ImagePanel(chart1.getChart());
            chPanel.setMouseWheelEnabled(true);
            chPanel.setMouseZoomable(true);
            chPanel.repaint();
            this.imgPanel.removeAll();
            this.imgPanel.add(chPanel);
            this.imgPanel.updateUI();
            top.add(child);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            ImageChart chart = (ImageChart) nodeInfo;
            ImagePanel chPanel = new ImagePanel(chart.getChart());
            chPanel.setMouseWheelEnabled(true);
            chPanel.setMouseZoomable(true);
            chPanel.repaint();
            this.imgPanel.removeAll();
            this.imgPanel.add(chPanel);
            this.imgPanel.updateUI();
        }
    }

    /**
     * Return the ImageChart array.
     *
     * @return chart
     */
    public ImageChart[] getChart() {
        return chart;
    }

}
