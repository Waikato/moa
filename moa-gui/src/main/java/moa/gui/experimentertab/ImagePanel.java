/*
 *    ImagePanel.java
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

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import weka.gui.ExtensionFileFilter;

/**
 * This class creates a panel with an image.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class ImagePanel extends ChartPanel {

    JFreeChart chart;

    /**
     * Class Constructor.
     *
     * @param chart
     */
    public ImagePanel(JFreeChart chart) {
        super(chart);
        this.chart = chart;
    }

    /**
     * Method for save the images.
     *
     * @throws IOException
     */
    @Override
    public void doSaveAs() throws IOException {

        JFileChooser fileChooser = new JFileChooser();
        ExtensionFileFilter filterPNG = new ExtensionFileFilter("PNG Image Files", ".png");
        fileChooser.addChoosableFileFilter(filterPNG);

        ExtensionFileFilter filterJPG = new ExtensionFileFilter("JPG Image Files", ".jpg");
        fileChooser.addChoosableFileFilter(filterJPG);

        ExtensionFileFilter filterEPS = new ExtensionFileFilter("EPS Image Files", ".eps");
        fileChooser.addChoosableFileFilter(filterEPS);

        ExtensionFileFilter filterSVG = new ExtensionFileFilter("SVG Image Files", ".svg");
        fileChooser.addChoosableFileFilter(filterSVG);
        fileChooser.setCurrentDirectory(null);
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            String fileDesc = fileChooser.getFileFilter().getDescription();
            if (fileDesc.startsWith("PNG")) {
                if (!fileChooser.getSelectedFile().getName().toUpperCase().endsWith("PNG")) {
                    ChartUtilities.saveChartAsPNG(new File(fileChooser.getSelectedFile().getAbsolutePath() + ".png"), this.chart, this.getWidth(), this.getHeight());
                } else {
                    ChartUtilities.saveChartAsPNG(fileChooser.getSelectedFile(), this.chart, this.getWidth(), this.getHeight());
                }
            } else if (fileDesc.startsWith("JPG")) {
                if (!fileChooser.getSelectedFile().getName().toUpperCase().endsWith("JPG")) {
                    ChartUtilities.saveChartAsJPEG(new File(fileChooser.getSelectedFile().getAbsolutePath() + ".jpg"), this.chart, this.getWidth(), this.getHeight());
                } else {
                    ChartUtilities.saveChartAsJPEG(fileChooser.getSelectedFile(), this.chart, this.getWidth(), this.getHeight());
                }
            }

        }//else
    }

    /**
     *
     * @param properties
     * @param copy
     * @param save
     * @param print
     * @param zoom
     * @return JPopupMenu
     */
    @Override
    protected JPopupMenu createPopupMenu(boolean properties,
            boolean copy, boolean save, boolean print, boolean zoom) {
        JPopupMenu result = new JPopupMenu(localizationResources.getString("Chart") + ":");
        boolean separator = false;

        if (properties) {
            JMenuItem propertiesItem = new JMenuItem(
                    localizationResources.getString("Properties..."));
            propertiesItem.setActionCommand(PROPERTIES_COMMAND);
            propertiesItem.addActionListener(this);
            result.add(propertiesItem);
            separator = true;
        }

        if (copy) {
            if (separator) {
                result.addSeparator();
            }
            JMenuItem copyItem = new JMenuItem(
                    localizationResources.getString("Copy"));
            copyItem.setActionCommand(COPY_COMMAND);
            copyItem.addActionListener(this);
            result.add(copyItem);
            separator = !save;
        }

        if (save) {
            if (separator) {
                result.addSeparator();
            }
            JMenu saveSubMenu = new JMenu(localizationResources.getString(
                    "Save_as"));
            JMenuItem pngItem = new JMenuItem(localizationResources.getString(
                    "PNG..."));

            separator = true;
        }

        if (print) {
            if (separator) {
                result.addSeparator();
            }
            JMenuItem printItem = new JMenuItem(
                    localizationResources.getString("Print..."));
            printItem.setActionCommand(PRINT_COMMAND);
            printItem.addActionListener(this);
            result.add(printItem);
            separator = true;
        }

        return result;
    }

}
