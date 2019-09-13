/*
 *    Stream.java
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains the name of a stream and a list of algorithms.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com).
 */
public class Stream {

    /**
     * The name of the stream
     */
    public String name;

    /**
     * The list of algorithms within of the stream
     */
    public List<Algorithm> algorithm = new ArrayList<>();

    /**
     * Stream Constructor
     * @param name
     * @param algPath
     * @param algNames
     * @param measures
     */
    public Stream(String name, List<String> algPath, List<String> algNames, List<Measure> measures) {
        this.name = name;
        readBuffer(algPath, algNames, measures);
    }

    /**
     * Read each algorithm file.
     * @param algPath
     * @param algNames
     * @param measures
     */
    public void readBuffer(List<String> algPath, List<String> algNames, List<Measure> measures) {
        BufferedReader buffer = null;
        for (int i = 0; i < algPath.size(); i++) {
            try {
                buffer = new BufferedReader(new FileReader(algPath.get(i)));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
            }
            Algorithm algorithm = new Algorithm(algNames.get(i), measures, buffer,algPath.get(i));
            this.algorithm.add(algorithm);
        }

    }

    /**
     * Sets the name of stream
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the stream
     * @return the name of the stream
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of the algorithms
     * @return the list of  the algorithms
     */
    public List<Algorithm> getAlgorithm() {
        return algorithm;
    }

}
