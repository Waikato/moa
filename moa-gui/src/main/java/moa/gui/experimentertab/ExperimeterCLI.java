/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.experimentertab;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Alberto
 */
public class ExperimeterCLI {
    
    private String algorithms[];
    private String algorithmsID[];
    private String streams[];
    private String streamsID[];
    private String task;
    private String resultsFolder;
    private String saveExperimentsPath;
    private String args[];
    private int threads = 1;
    
    public int[] measures = null;
    public String[] types = null;
    Options options = new Options();
    Options optionsm = new Options();
    
    public ExperimeterCLI(String[] args) {
        this.args = args;
        //Config the options

        options.addOption("ls", true, "The names of the algorithms separated by commas");
        options.addOption("lss", true, "ID of the algorithms separated by commas");
        options.addOption("ds", true, "The names of the streams separated by commas");
        options.addOption("dss", true, "ID of the streams separated by commas");
        options.addOption("rf", true, "Results folder");
        options.addOption("th", true, "Number of threads");
        options.addOption("ts", true, "Task");
        options.addOption("h", "help", false, "Prints the help message");
        
        optionsm.addOption("h", "help", false, "Prints the help message");
        optionsm.addOption("m", true, "The number of measures separated by commas");
        optionsm.addOption("tm", true, "The types of measures separated by commas, the types are Mean and Last");
    }
    
    public boolean summary1CMD(String[] args) {
        
        CommandLineParser parser = null;
        CommandLine cmdLine = null;
        
        try {
            parser = new BasicParser();
            cmdLine = parser.parse(optionsm, args);
            if (cmdLine.hasOption("h")) {
                new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), optionsm);
                return false;
            }
            String measure = cmdLine.getOptionValue("m");
            if (measure == null) {
                System.out.println("The measures are required");
                return false;
            }
            if (measure.contains(",")) {
                String[] m = measure.split(",");
                measures = new int[m.length];
                for (int i = 0; i < m.length; i++) {
                    measures[i] = Integer.parseInt(m[i]);
                }
            } else {
                measures = new int[1];
                measures[0] = Integer.parseInt(measure);
            }
            
            if (cmdLine.hasOption("tm")) {
                String type = cmdLine.getOptionValue("tm");
                if (type.contains(",")) {
                    types = type.split(",");
                } else {
                    types = new String[1];
                    types[0] = type;
                }
            } else {
                types = new String[measures.length];
                for (int i = 0; i < types.length; i++) {
                    types[i] = "Mean";
                }
            }
        } catch (org.apache.commons.cli.ParseException ex) {
            System.out.println(ex.getMessage());
            
            new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), optionsm);    // Error, imprimimos la ayuda  
        } catch (java.lang.NumberFormatException ex) {
            new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), optionsm);    // Error, imprimimos la ayuda  
        }
        return true;
    }
    
    public boolean proccesCMD(){
        int threads = 1;
        String algNames = null;
        String algShortNames = null;
        String streamNames = null;
        String streamShortNames = null;
        String task = null;
        String resultsFolder = null;
        
        CommandLineParser parser = null;
        CommandLine cmdLine = null;
        
        try {
            //Parse the input with the set configuration
            parser = new BasicParser();
            cmdLine = parser.parse(options, args);
            if (cmdLine.hasOption("h")) {
                new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), options);
                return false;
            }
            
            if (cmdLine.hasOption("th")) {
                threads = Integer.parseInt(cmdLine.getOptionValue("th"));
                this.setThreads(threads);
            }
            
            task = cmdLine.getOptionValue("ts");
            if (task == null) {
                throw new org.apache.commons.cli.ParseException("The task is required");
            }
            this.setTask(task);
            //Algorithms names
            algNames = cmdLine.getOptionValue("ls");
            
            if (algNames == null) {
                throw new org.apache.commons.cli.ParseException("The name of the algorithms are required");
            }
            
            try {
                if (algNames.contains(",")) {
                    this.setAlgorithms(algNames.split(","));
                } else {
                    String alg[] = new String[1];
                    alg[0] = algNames;
                    this.setAlgorithms(alg);
                }
                
            } catch (Exception e) {
                System.out.println("Problems with algortihms ls options");
                new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), options);
            }

            //Agorithms ID 
            if (cmdLine.hasOption("lss")) {
                
                algShortNames = cmdLine.getOptionValue("lss");
                if (algShortNames.contains(",")) {
                    this.setAlgorithmsID(algShortNames.split(","));
                } else {
                    String ash[] = new String[1];
                    ash[0] = algShortNames;
                    this.setAlgorithmsID(ash);
                }
                
            } else {
                this.setAlgorithmsID(this.getAlgorithms());
            }
            //Streams names
            streamNames = cmdLine.getOptionValue("ds");
            
            if (streamNames == null) {
                throw new org.apache.commons.cli.ParseException("The name of the streams are required");
            }
            
            if (streamNames.contains(",")) {
                this.setStreams(streamNames.split(","));
                for (int i = 0; i < this.getStreams().length; i++) {
                    String ds = this.getStreams()[i];
                    if (ds.contains(":")) {
                        String dir = ds.split(":")[0];
                        if (dir.contains(File.separator)) {
                            dir = dir.split(File.separator + File.separator)[0];
                            ds = dir + ":" + ds.split(":")[1];
                            this.setStreamIndex(i, ds);
                        }
                        
                    }
                }
            } else {
                String str[] = new String[1];
                str[0] = FilenameUtils.separatorsToSystem(streamNames);
                this.setStreams(str);
                
            }

            //stream ID 
            if (cmdLine.hasOption("dss")) {
                streamShortNames = cmdLine.getOptionValue("dss");
                if (streamShortNames.contains(",")) {
                    this.setStreamsID(streamShortNames.split(","));
                } else {
                    String strh[] = new String[1];
                    strh[0] = streamShortNames;
                    this.setStreamsID(strh);
                }
                
            } else {
                this.setStreamsID(this.getStreams());
            }
            //Results folder
            resultsFolder = cmdLine.getOptionValue("rf");
            
            if (resultsFolder == null) {
                //throw new org.apache.commons.cli.ParseException("The resuts folder are required");
                File excPath = new File(".");
                try {
                    resultsFolder = excPath.getCanonicalPath();
                } catch (IOException ex) {
                    Logger.getLogger(ExperimeterCLI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (resultsFolder.contains(":")) {
                String dir = resultsFolder.split(":")[0];
                if (dir.contains(File.separator)) {
                    dir = dir.split(File.separator + File.separator)[0];
                    resultsFolder = dir + ":" + resultsFolder.split(":")[1];
                }
            }
            this.setResultsFolder(FilenameUtils.separatorsToSystem(resultsFolder));
            // System.out.println("OK");  
            // System.out.println(task);
            // System.out.println(algNames);
            // System.out.println(streamNames);

        } catch (org.apache.commons.cli.ParseException ex) {
            System.out.println(ex.getMessage());
            new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), options);    // Error, print help
            return false;
        } catch (java.lang.NumberFormatException ex) {
            new HelpFormatter().printHelp(ExperimeterCLI.class.getCanonicalName(), options);    // Error, print help 
            return false;
        }
        return true;
    }
    
    public String[] getAlgorithms() {
        return algorithms;
    }
    
    public String[] getAlgorithmsID() {
        return algorithmsID;
    }
    
    public String[] getArgs() {
        return args;
    }
    
    public String getResultsFolder() {
        return resultsFolder;
    }
    
    public String getSaveExperimentsPath() {
        return saveExperimentsPath;
    }
    
    public String[] getStreams() {
        return streams;
    }
    
    public String[] getStreamsID() {
        return streamsID;
    }
    
    public String getTask() {
        return task;
    }
    
    public int getThreads() {
        return threads;
    }
    
    public void setAlgorithms(String[] algorithms) {
        this.algorithms = algorithms;
    }
    
    public void setAlgorithmsID(String[] algorithmsID) {
        this.algorithmsID = algorithmsID;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    public void setResultsFolder(String resultsFolder) {
        this.resultsFolder = resultsFolder;
    }
    
    public void setSaveExperimentsPath(String saveExperimentsPath) {
        this.saveExperimentsPath = saveExperimentsPath;
    }
    
    public void setStreams(String[] streams) {
        for(int i = 0; i < streams.length; i++){
            streams[i] =  FilenameUtils.separatorsToSystem(streams[i]);
        }
        this.streams = streams;
    }
    
    public void setStreamsID(String[] streamsID) {
        this.streamsID = streamsID;
    }
     public void setStreamIndex(int index, String streamID) {
        this.streams[index] = streamID;
    }
    public void setTask(String task) {
        this.task = task;
    }
    
    public void setThreads(int threads) {
        this.threads = threads;
    }
    
}
