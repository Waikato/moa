/*
 *    ImageChart.java
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/**
 * This class allows to handle the properties of the graph created by
 * JFreeChart.
 *
 * @author Alberto
 */
public class ImageChart {

    private String name;

    private JFreeChart chart;

    private int width;

    private int height;

    /**
     * Default constructor.
     */
    public ImageChart() {
    }

    /**
     * Constructor.
     *
     * @param name
     * @param chart
     * @param width
     * @param height
     */
    public ImageChart(String name, JFreeChart chart, int width, int height) {
        this.name = name;
        this.chart = chart;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructor.
     *
     * @param name
     * @param chart
     */
    public ImageChart(String name, JFreeChart chart) {
        this.name = name;
        this.chart = chart;
    }

    /**
     * Set the image name.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set chart.
     *
     * @param chart
     */
    public void setChart(JFreeChart chart) {
        this.chart = chart;
    }

    /**
     * Set chart height.
     *
     * @param height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Set chart width.
     *
     * @param width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Return the chart.
     *
     * @return chart
     */
    public JFreeChart getChart() {
        return chart;
    }

    /**
     * Return the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the height.
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the width.
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        return name; //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Export the image to formats JPG, PNG, SVG and EPS.
     *
     * @param path
     * @param type
     * @throws IOException
     */
    public void exportIMG(String path, String type) throws IOException {

        switch (type) {
            case "JPG":
                try {
                    ChartUtilities.saveChartAsJPEG(new File(path + File.separator + name + ".jpg"), chart, width, height);
                } catch (IOException e) {

                }
                break;
            case "PNG":
                try {
                    ChartUtilities.saveChartAsPNG(new File(path + File.separator + name + ".png"), chart, width, height);
                } catch (IOException e) {

                }
                break;
            case "SVG":
                String svg = generateSVG(width, height);
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(new File(path + File.separator + name + ".svg")));
                    writer.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
                    writer.write(svg + "\n");
                    writer.flush();
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                break;

        }

    }

    private String generateSVG(int width, int height) {
        Graphics2D g2 = createSVGGraphics2D(width, height);
        if (g2 == null) {
            throw new IllegalStateException("JFreeSVG library is not present.");
        }
        // we suppress shadow generation, because SVG is a vector format and
        // the shadow effect is applied via bitmap effects...
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        String svg = null;
        Rectangle2D drawArea = new Rectangle2D.Double(0, 0, width, height);
        this.chart.draw(g2, drawArea);
        try {
            Method m = g2.getClass().getMethod("getSVGElement");
            svg = (String) m.invoke(g2);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // null will be returned
        }
        return svg;
    }

    private Graphics2D createSVGGraphics2D(int w, int h) {
        try {
            Class svgGraphics2d = Class.forName("org.jfree.graphics2d.svg.SVGGraphics2D");
            Constructor ctor = svgGraphics2d.getConstructor(int.class, int.class);
            return (Graphics2D) ctor.newInstance(w, h);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
    }

}
