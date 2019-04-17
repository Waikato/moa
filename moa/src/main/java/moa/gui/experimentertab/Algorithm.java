/*
 *    Algorithm.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.DoubleVector;

/**
 * This class calculates the different measures for each algorithm
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class Algorithm {

    /**
     * The name of the algorithms
     */
    public String name;
    
    public String path;
    /**
     * The list of measures per algorithm
     */
    public List<Measure> measures = new ArrayList<>();

    /**
     * The results file for the algorithm
     */
    public BufferedReader buffer;

    /**
     * The same size that the measure list
     */
    public int measureStdSize = 0;

    /**
     * Algorithm constructor
     *
     * @param name
     * @param measures
     * @param buffer
     * @param path
     */
    public Algorithm(String name, List<Measure> measures, BufferedReader buffer,String path) {

        this.name = name;
        this.path = path;
        this.measureStdSize = measures.size();
        measures.stream().map((measure) -> {
            int index = ReadFile.getMeasureIndex(path,measure.getFileName());
            this.measures.add(new Measure(measure.getName(),measure.getFileName(), measure.isType(), index));
            return measure;
        }).filter((measure) -> (measure.isType())).forEach((_item) -> {
            this.measureStdSize++;
        });

        this.buffer = buffer;
        try {
            calculateMeasures();
        } catch (IOException ex) {
            Logger.getLogger(Algorithm.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * calculates the different measures for each algorithm.
     *
     */
    private void calculateMeasures() throws IOException {
        int cont = 0;
        DoubleVector values[] = new DoubleVector[this.measures.size()];
        for (int i = 0; i < this.measures.size(); i++) {
            values[i] = new DoubleVector();
        }
        String lines;
        lines = this.buffer.readLine();
        while ((lines = this.buffer.readLine()) != null) {

            String line[] = lines.split(",");
            for (int i = 0; i < this.measures.size(); i++) {
                if (this.measures.get(i).isType()) {

                   try{
                       values[i].setValue(cont, Double.parseDouble(line[this.measures.get(i).getIndex()]));
                   }catch(NumberFormatException exp){
                       values[i].setValue(cont,0);
                   }

                } else {
                    try{
                        this.measures.get(i).setValue(Double.parseDouble(line[this.measures.get(i).getIndex()]));
                    }catch(NumberFormatException exp){
                       this.measures.get(i).setValue(0.0);
                   }
                }
            }
            cont++;
        }
        //compute values
        for (int i = 0; i < this.measures.size(); i++) {
            this.measures.get(i).computeValue(values[i]);
        }

    }

    /**
     * Returns a list of measures per dataset.
     *
     * @param stream
     * @return a list of measures per dataset
     */
    public List<Measure>[] getMeasuresPerData(List<Stream> stream) {
        List<Measure> measures[] = new ArrayList[stream.size()];
        for (int i = 0; i < stream.size(); i++) {
            measures[i] = new ArrayList<>();
            for (int j = 0; j < stream.get(i).algorithm.size(); j++) {
                if (stream.get(i).algorithm.get(j).name.equals(name)) {
                    measures[i] = stream.get(i).algorithm.get(j).measures;
                }
            }
        }
        return measures;
    }

    /**
     * Rounds to two decimal places and returns the rounded value as a string.
     *
     * @return a formated value.
     */
    static String format(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Returns the closest long to the argument, with ties rounding to positive
     * infinity.
     *
     * @return the value of the argument rounded to the nearest long value.
     */
    static String format1(double x) {
        String s = "" + Math.round(x);
        return s;
    }

    /**
     * Rounds to two decimal places and returns the rounded value as a double.
     *
     * @return a formated value.
     */
    static double Round(double x) {
        return (Math.floor((x + 0.005) * 100)) / 100;
    }

}
