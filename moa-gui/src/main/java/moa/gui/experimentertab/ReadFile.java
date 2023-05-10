/*
 *    ReadFile.java
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 * This class processes the results files of the algorithms in each directory.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com).
 */
public class ReadFile {

    private String path;
    private LinkedList<String> stream;
    private LinkedList<String> algNames;
    private LinkedList<String> measures;
    private ArrayList<String> algShortNames;
    private List<Algorithm> algorithm = new ArrayList<>();
    /**
     * File Constructor
     *
     * @param path
     */
    public ReadFile(String path) {
        this.path = path;
        this.stream = new LinkedList<>();
        this.algNames = new LinkedList<>();
        this.measures = new LinkedList<>();
        this.algShortNames = new ArrayList<>();
    }

    /**
     * Processes the results files of the algorithms in each directory.
     *
     * @return If all the files were processed correctly it returns an empty
     * string, else return the problem file.
     */
    public String processFiles() {

        //read files
        File file = new File(path);
        File listFiles[] = file.listFiles();
        boolean addFirst = true;
        for (int i = 0; i < listFiles.length; i++) {

            if (listFiles[i].isDirectory()) {
                stream.add(listFiles[i].getName());
                File files = new File(listFiles[i].getAbsolutePath());
                File algorithm[] = files.listFiles();
                for (int j = 0; j < algorithm.length; j++) {
                    if (algorithm[j].isFile()) {
                        if (algNames.remove(algorithm[j].getName())) {
                            algNames.add(algorithm[j].getName());
                        } else {
                            algNames.add(algorithm[j].getName());
                        }
                        FileReader fr = null;
                        try {
                            fr = new FileReader(algorithm[j].getAbsolutePath());
                        } catch (FileNotFoundException ex) {
                            return "Problem with file: " + listFiles[j].getAbsolutePath();

                        }
                        BufferedReader br = new BufferedReader(fr);
                        try {
                            String line = br.readLine();
                            if (addFirst) {
                                measures.add(line);
                                addFirst = false;
                            } else if (measures.remove(line)) {
                                measures.add(line);
                            } else {
                                String lineArray[] = line.split(",");
                                String measureArray[] = measures.getFirst().split(",");
                                String newMeasure = "";
                                for (int l = 0; l < lineArray.length; l++) {
                                    for (int m = 0; m < measureArray.length; m++) {
                                        if (lineArray[l].equals(measureArray[m])) {
                                            newMeasure += lineArray[l] + ",";
                                        }

                                    }
                                }
                                String s[];
                                if (newMeasure.endsWith(",")) {
                                    s = newMeasure.split(",");
                                    newMeasure = "";
                                    for (int k = 0; k < s.length; k++) {
                                        newMeasure += s[k];
                                        if (k != s.length - 1) {
                                            newMeasure += ",";
                                        }
                                    }
                                }

                                measures.removeFirst();
                                measures.add(newMeasure);
                            }

                        } catch (IOException ex) {
                            return "Problem with file: " + listFiles[j].getAbsolutePath();

                        }
                    }

                }
            }
        }//end files
        algNames.stream().forEach((algName) -> {
            this.algShortNames.add(FilenameUtils.getBaseName(algName));
        });
        return "";
    }

    public String updateMeasures(String algNames[], String stream) {
        this.measures = new LinkedList<>();
        //read files
        boolean addFirst = true;
        for (int i = 0; i < algNames.length; i++) {
            File algorithm = new File(path + File.separator + stream + File.separator + algNames[i]);
            FileReader fr = null;
            try {
                fr = new FileReader(algorithm.getAbsolutePath());
            } catch (FileNotFoundException ex) {
                return "Problem with file: " + algorithm.getAbsolutePath();
            }
            BufferedReader br = new BufferedReader(fr);
            try {
                String line = br.readLine();
                if (addFirst) {
                    measures.add(line);
                    addFirst = false;
                } else if (measures.remove(line)) {
                    measures.add(line);
                } else {
                    String lineArray[] = line.split(",");
                    String measureArray[] = measures.getFirst().split(",");
                    String newMeasure = "";
                    for (int l = 0; l < lineArray.length; l++) {
                        for (int m = 0; m < measureArray.length; m++) {
                            if (lineArray[l].equals(measureArray[m])) {
                                newMeasure += lineArray[l] + ",";
                            }

                        }
                    }
                    String s[];
                    if (newMeasure.endsWith(",")) {
                        s = newMeasure.split(",");
                        newMeasure = "";
                        for (int k = 0; k < s.length; k++) {
                            newMeasure += s[k];
                            if (k != s.length - 1) {
                                newMeasure += ",";
                            }
                        }
                    }

                    measures.removeFirst();
                    measures.add(newMeasure);
                }

            } catch (IOException ex) {
                return "Problem with file: " + algorithm.getAbsolutePath();

            }
        }
        return null;
    }
    
    static int getMeasureIndex(String algPath, String mesasure){
          
             FileReader fr = null;
        try {
            fr = new FileReader(new File(algPath));
            BufferedReader br = new BufferedReader(fr);
             String line = br.readLine();
                 String measures[] = line.split(",");
                 for(int i = 0; i < measures.length; i++){
                      if(measures[i].equals(mesasure)==true)
                          return i;
                 }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return 0;
    }
    
  /*  public ArrayList<Algorithm> getAlgorithms(List<String> algPath,List<String> algNames, List<Measure> measures){
        
         for(int i = 0; i < algPath.size(); i++){
             FileReader fr = null;
             try {
                 fr = new FileReader(new File(algPath.get(i)));
             } catch (FileNotFoundException ex) {
                 Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
             }
             BufferedReader br = new BufferedReader(fr); 
            
             try {
                 String line = br.readLine();
                 String measures1[] = line.split(",");
                 for(int j = 0; j < measures1.length; j++){
                     
                 }
             } catch (IOException ex) {
                 Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
             }
             Algorithm algorithm = new Algorithm(algNames.get(i), measures, br);
             this.algorithm.add(algorithm);
         }
    }*/
    /**
     * Returns the name of the algorithms.
     *
     * @return a LinkedList with the name of the algorithms.
     */
    public LinkedList<String> getAlgNames() {
        return algNames;
    }

    /**
     * Returns the common measures to all algorithms.
     *
     * @return a LinkedList with the measures.
     */
    public LinkedList<String> getMeasures() {
        return measures;
    }

    /**
     * Returns the name of the streams.
     *
     * @return a LinkedList with the streams.
     */
    public LinkedList<String> getStream() {
        return stream;
    }

    /**
     * Returns the short name of the algorithms.
     *
     * @return an ArrayList with the short name of the algorithms.
     */
    public ArrayList<String> getAlgShortNames() {
        return algShortNames;
    }

    /**
     * Returns the path of the results.
     *
     * @return the path of the results.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the directory of the results file.
     *
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Delete the selected directory.
     *
     * @param directory
     */
    public static void deleteDrectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDrectory(file);
            }
            file.delete();
        }
    }

    /**
     * Allow to read a csv file.
     *
     * @param path
     * @return
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<String[]> readCSV(String path)
            throws UnsupportedEncodingException, FileNotFoundException, IOException {

        ArrayList<String[]> data = new ArrayList<>();

        try (FileInputStream csv = new FileInputStream(path);
                InputStreamReader reader = new InputStreamReader(csv);
                BufferedReader br = new BufferedReader(reader)) {

            String linea = br.readLine();

            while ((linea = br.readLine()) != null) {

                data.add(linea.split(","));

            }

        }

        return data;

    }

}
