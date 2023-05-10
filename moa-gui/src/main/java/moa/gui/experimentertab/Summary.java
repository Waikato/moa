/*
 *    Summary.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class performs the different summaries.
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com).
 */
public class Summary {

    /**
     * The list of the streams
     */
    public List<Stream> streams = new ArrayList<>();

    /**
     * The path of the results
     */
    public  String path = "";
    public  SummaryTable []summary;    

    /**
     * Summary Constructor
     *
     * @param streams
     * @param path
     */
    public Summary(List<Stream> streams, String path) {
        this.streams = streams;
        this.path = path;
//        generateHTML();
//        generateCSV();
//        generateLatex();
//        computeWinsTiesLossesLatex();
//        invertedSumariesPerMeasure();
//        computeWinsTiesLossesHTML();
    }

    /**
     * Generates a latex summary, in which the rows are the algorithms and the
     * columns the datasets.
     */
    public void generateLatex(String path) {
        String output = "";
        output += "\\documentclass{article}\n";
        output += "\\usepackage{multirow}\n\\usepackage{booktabs}\n\\begin{document}\n\\begin{table}[htbp]\n\\caption{Add caption}";
        output += "\\begin{tabular}";
        output += "{";
        for (int i = 0; i < this.streams.size() + 4; i++) {
            output += "r";
        }
        output += "}\n\\toprule\nAlgorithm & \\multicolumn{2}{r}{Measure}";
        for (int i = 0; i < this.streams.size(); i++) {
            output += "& " + this.streams.get(i).getName();
        }
        output += "& AVG\\\\\n\\midrule\n";
        int algorithmSize = this.streams.get(0).algorithm.size();
        String name = "";
        for (int i = 0; i < algorithmSize; i++) {
            List<Algorithm> alg = this.streams.get(0).algorithm;
            output += "\\multirow{" + alg.get(i).measureStdSize + "}[" + 6 + "]{*}{" + alg.get(i).name + "}";
            List<Measure> measures[] = alg.get(i).getMeasuresPerData(streams);
            int cont = 0;
            double sum = 0.0;
            boolean isType = true;
            while (cont != this.streams.get(0).algorithm.get(i).measures.size()) {
                sum = 0;
                name = measures[0].get(cont).getName();
                if (measures[0].get(cont).isType()) {
                    output += "&\\multirow{" + 2 + "}[4]{*}{" + name + "} & mean";

                } else {
                    output += " & " + name;
                    output += "& Last value";
                }
                for (int j = 0; j < measures.length; j++) {
                    //sum =0;
                    if (measures[j].get(cont).isType()) {
                        output += " & " + Algorithm.format(measures[j].get(cont).getValue());

                        sum += measures[j].get(cont).getValue();
                        isType = true;
                    } else {
                        output += " & " + Algorithm.format1(measures[j].get(cont).getValue());
                        isType = false;
                    }

                }
                if (isType) {
                    double size = (double) this.streams.size();
                    output += "& " + Algorithm.format(sum / size);
                    output += "\\\\\n";
                    output += " & & std";
                    for (int j = 0; j < measures.length; j++) {
                        output += " & " + Algorithm.format(measures[j].get(cont).getStd());
                    }
                    output += " & -";
                } else {
                    output += " & -";

                }
                output += "\\\\\n";
                cont++;
            }
        }
        output += "\\bottomrule\n\\end{tabular}%\n\\label{tab:addlabel}%\n\\end{table}%\n\\end{document}";
//        PrintStream out = null;
//        try {
//            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + "summary.tex")));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.setOut(out);
//        System.out.println(output);
//        out.close();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + "summary.tex"));
            out.write(output);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println(" Error saving summary.tex");

        }
    }

    /**
     * Generates a latex summary, in which the rows are the datasets and the
     * columns the algorithms.
     */
    public void invertedSumariesPerMeasure( String path) {

        int cont = 0;
        int algorithmSize = this.streams.get(0).algorithm.size();
        int streamSize = this.streams.size();
        int measureSize = this.streams.get(0).algorithm.get(0).measures.size();
        while (cont != measureSize) {
            String output = "";
            output += "\\documentclass{article}\n";
            output += "\\usepackage{multirow}\n\\usepackage{booktabs}\n\\begin{document}\n\\begin{table}[htbp]\n\\caption{Add caption}";
            output += "\\begin{tabular}";
            output += "{";
            for (int i = 0; i < algorithmSize + 1; i++) {
                output += "r";
            }
           
            output += "}\n\\toprule\nAlgorithm";
            for (int i = 0; i < algorithmSize; i++) {
                output += "& " + this.streams.get(0).algorithm.get(i).name;
            }
            output += "\\\\";
            output += "\n\\midrule\n";

            for (int i = 0; i < streamSize; i++) {
                List<Algorithm> alg = this.streams.get(i).algorithm;
                output += this.streams.get(i).name;
                for (int j = 0; j < algorithmSize; j++) {
                    if (alg.get(j).measures.get(cont).isType()) {
                        output += "&" + Algorithm.format(alg.get(j).measures.get(cont).getValue()) + "$\\,\\pm$"
                                + Algorithm.format(alg.get(j).measures.get(cont).getStd());
                    } else {      
                        output += "&" + Algorithm.format1(alg.get(j).measures.get(cont).getValue());
                    }
                }
                output += "\\\\\n";
            }
            output += "\\bottomrule\n\\end{tabular}%\n\\label{tab:addlabel}%\n\\end{table}%\n\\end{document}";
            //PrintStream out = null;
            String name = this.streams.get(0).algorithm.get(0).measures.get(cont).getName();
//            try {
//                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + name + ".tex")));
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            System.setOut(out);
//            System.out.println(output);
//            out.close();
             try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + name + ".tex"));
            out.write(output);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println("Error saving "+name + ".tex");

        }
            cont++;
        }

    }
    /**
     * The summaries are performed for each measure to be displayed in the user interface
     * 
     * @return SummaryTable
     */
    public SummaryTable[] showSummary(){
        int cont = 0;
        int algorithmSize = this.streams.get(0).algorithm.size();
        int streamSize = this.streams.size();
        int measureSize = this.streams.get(0).algorithm.get(0).measures.size();
        summary = new SummaryTable[measureSize];
        while (cont != measureSize) {
           
            summary[cont] = new SummaryTable();
            summary[cont].measureName = this.streams.get(0).algorithm.get(0).measures.get(cont).getName();
            summary[cont].algNames = new String[algorithmSize+1];
            summary[cont].algNames[0] ="Algorithm";
            summary[cont].value = new Object[streamSize][algorithmSize+1];
           
            for (int i = 0; i < algorithmSize; i++) {
              
                summary[cont].algNames[i+1] = this.streams.get(0).algorithm.get(i).name;
            }
            for (int i = 0; i < streamSize; i++) {
                List<Algorithm> alg = this.streams.get(i).algorithm;
                 summary[cont].value[i][0] = this.streams.get(i).name;  
                for (int j = 0; j < algorithmSize; j++) {
                    if (alg.get(j).measures.get(cont).isType()) {
                        summary[cont].value[i][j+1] = Algorithm.format(alg.get(j).measures.get(cont).getValue())+"±"+
                                    Algorithm.format(alg.get(j).measures.get(cont).getStd());
                    } else {
                        summary[cont].value[i][j+1] = Algorithm.format1(alg.get(j).measures.get(cont).getValue());
                       
                    }
                }
               
            }
                   
            cont++;
        }
        return summary;
    }

    /**
     * Generates an HTML summary, in which the rows are the datasets and the
     * columns the algorithms.
     */
    public void generateHTML(String path) {
      
        String output = "";
        output += "<TABLE BORDER=1 WIDTH=\"100%\" ALIGN=CENTER>\n";
        output += "<CAPTION> Experiment";
        output += "<TR> <TD>Algorithm <TD COLSPAN = 2>Measure";

        //set algorithms names
       
        for (int i = 0; i < this.streams.size(); i++) {
            output += "<TD>" + this.streams.get(i).getName();
        }
        output += "<TD>AVG";
        int algorithmSize = this.streams.get(0).algorithm.size();
        String name = "";
        for (int i = 0; i < algorithmSize; i++) {
            List<Algorithm> alg = this.streams.get(0).algorithm;
            output += "<TR><TD ROWSPAN = " + /*alg.get(i).measures.size()*/ alg.get(i).measureStdSize + ">"
                    + alg.get(i).name;
            List<Measure> measures[] = alg.get(i).getMeasuresPerData(streams);
            int cont = 0;
            double sum = 0.0;
            boolean isType = true;
            while (cont != this.streams.get(0).algorithm.get(i).measures.size()) {
                sum = 0;
                name = measures[0].get(cont).getName();
                if (measures[0].get(cont).isType()) {
                    output += "<TD ROWSPAN = 2>" + name;
                    output += "<TD>mean";
                } else {
                    output += "<TD>" + name;
                    output += "<TD>Last value";
                }
                for (int j = 0; j < measures.length; j++) {
                    //sum =0;
                    if (measures[j].get(cont).isType()) {
                        output += "<TD>" + Algorithm.format(measures[j].get(cont).getValue());

                        sum += measures[j].get(cont).getValue();
                        isType = true;
                    } else {
                        output += "<TD>" + Algorithm.format1(measures[j].get(cont).getValue());
                        isType = false;
                    }

                }
                if (isType) {
                    double size = (double) this.streams.size();
                    output += "<TD>" + Algorithm.format(sum / size);
                    output += "<TR>";
                    output += "<TD>std";
                    for (int j = 0; j < measures.length; j++) {
                        output += "<TD>" + Algorithm.format(measures[j].get(cont).getStd());
                    }
                    output += "<TD>" + "-";
                } else {
                    output += "<TD>" + "-";

                }
                output += "<TR>";
                cont++;
            }
        }

        output += "</TABLE>";
//        PrintStream out = null;
//        try {
//            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + "summary.html")));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.setOut(out);
//        System.out.println(output);
//        out.close();
         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + "summary.html"));
            out.write(output);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println("Error saving summary.html");

        }

    }

    /**
     * Generates a latex summary that shows the gains, loses or ties of each
     * algorithm against each other, in a specific measure..
     */
    public void computeWinsTiesLossesLatex(String path) {

        List<Algorithm> alg = this.streams.get(0).algorithm;
        int algorithmSize = this.streams.get(0).algorithm.size();
        String output = "";
        output += "\\documentclass{article}\n";
        output += "\\usepackage[latin9]{inputenc}\n"
                + "\\usepackage{array}\n"
                + "\\usepackage{rotfloat}\n"
                + "\\usepackage{multirow}\n"
                + "\n"
                + "\\makeatletter\n"
                + "\\providecommand{\\tabularnewline}{\\\\}\n"
                + "\\usepackage{multirow}\n"
                + "\\usepackage{booktabs}\n"
                + "\\makeatother\n"
                + "\n"
                + "\\begin{document}\n"
                + "\\begin{sidewaystable}\n"
                + "\\centering \\caption{Add caption}\n\\begin{tabular}";
        output += "{|r|r|";

        for (int i = 2; i <= algorithmSize * 3; i++) {
            if (i <= algorithmSize) {
                output += "r";
            } else {
                if (i == algorithmSize * 3) {
                    output += "|";
                } else {
                    output += "|r";
                }
            }
        }
        output += "}\n\\hline\n";
        output += "\\multirow{2}{*}{Algorithm } & \\multirow{2}{*}{PM} &";
        output += "\\multicolumn{3}{r|}{" + alg.get(1).name + "}";
        for (int i = 2; i < algorithmSize; i++) {
            output += " & \\multicolumn{3}{r|}{" + alg.get(i).name + "}";
        }
        output += "& \\multirow{2}{*}{AVG}\\tabularnewline\n";
        output += "\\cline{3-" + (algorithmSize * 3 - 1) + "}\n";
        output += " & & ";
        output += "\\multicolumn{1}{r|}{W} & \\multicolumn{1}{r|}{L} & \\multicolumn{1}{r|}{T} & ";
        for (int i = 2; i < algorithmSize; i++) {
            output += "\\multicolumn{1}{r|}{W} & \\multicolumn{1}{r|}{L} & \\multicolumn{1}{r|}{T} & ";
        }
        output += "\\tabularnewline\n\\hline\n";
        int range = 3;
        int measuresSize = this.streams.get(0).algorithm.get(0).measures.size();
        for (int i = 0; i < algorithmSize; i++) {
            output += "\\multirow{" + alg.get(i).measures.size() + "}{*}" + "{" + alg.get(i).name + "}";
            List<Measure> measureRow[] = alg.get(i).getMeasuresPerData(streams);
            int cont = 0;

            while (cont != measuresSize) {

                //String name = measureRow[i].get(cont).getName();
                String name = alg.get(i).measures.get(cont).getName();
                output += " & " + name;
                double sum = 0.0;

                for (int j = 1; j < algorithmSize; j++) {
                    List<Measure> measureCol[] = alg.get(j).getMeasuresPerData(streams);
                    int win = 0, losses = 0, ties = 0;
                    for (int k = 0; k < measureCol.length; k++) {
                        double alg1 = measureRow[k].get(cont).getValue();
                        double alg2 = measureCol[k].get(cont).getValue();
                        if (j == 1) {
                            sum += measureRow[k].get(cont).getValue();
                        }
                        if (measureRow[k].get(cont).isType()) {

                            if (Algorithm.Round(alg1) > Algorithm.Round(alg2)) {
                                win++;
                            } else if (Algorithm.Round(alg1) < Algorithm.Round(alg2)) {
                                losses++;
                            } else {
                                ties++;
                            }
                        } else {
                            if (alg1 < alg2) {
                                win++;
                            } else if (alg1 > alg2) {
                                losses++;
                            } else {
                                ties++;
                            }
                        }

                    }
                    if (i < j) {

                        output += " & \\multicolumn{1}{r|}{" + win + "}";
                        output += " & \\multicolumn{1}{r|}{" + losses + "}";
                        output += " & \\multicolumn{1}{r|}{" + ties + "}";

                    } else if (i == j) {
                        output += " & \\multicolumn{1}{r}{}";
                        output += " & \\multicolumn{1}{r}{}";
                        output += " & \\multicolumn{1}{r|}{}";
                    } else {
                        output += " & \\multicolumn{1}{r}{}";
                        output += " & \\multicolumn{1}{r}{}";
                        output += " & \\multicolumn{1}{r}{}";

                    }

                }

                sum = (double) sum / measureRow.length;
                output += " & " + Algorithm.format(sum);

                if (cont < measuresSize - 1) {
                    output += "\\tabularnewline\n \\cline{2-2} \\cline{" + range + "-" + (algorithmSize * 3) + "}\n";
                } else {
                    if (i != algorithmSize - 1) {
                        output += "\\tabularnewline\n \\cline{1-2} \\cline{" + range + "-" + (algorithmSize * 3) + "}\n";
                    } else {
                        output += "\\tabularnewline\n \\cline{1-" + (algorithmSize * 3) + "}\n";
                    }
                }

                cont++;

            }

            range += 3;
        }
        output += "\\end{tabular}\\label{tab:addlabel}\n"
                + "\\end{sidewaystable}\n"
                + "\n"
                + "\\end{document}";
//        PrintStream out = null;
//        try {
//            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + "summary.win.ties.losses.tex")));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.setOut(out);
//        System.out.println(output);
//        out.close();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + "summary.win.ties.losses.tex"));
            out.write(output);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println("Error saving summary.win.ties.losses.tex");

        }

    }

    /**
     * Generate a csv file for the statistical analysis.
     */
    public void generateCSV() {
        
        int cont = 0;
        int algorithmSize = this.streams.get(0).algorithm.size();
        int streamSize = this.streams.size();
        int measureSize = this.streams.get(0).algorithm.get(0).measures.size();
        
        while (cont != measureSize) {
            String output = "";
            output += "Algorithm,";
            output += this.streams.get(0).algorithm.get(0).name;
            //Inicialize summary table
            
            for (int i = 1; i < algorithmSize; i++) {
                output += "," + this.streams.get(0).algorithm.get(i).name;
               
            }
            output += "\n";

            for (int i = 0; i < streamSize; i++) {
                List<Algorithm> alg = this.streams.get(i).algorithm;
                output += this.streams.get(i).name;
                for (int j = 0; j < algorithmSize; j++) {
                    output += "," + alg.get(j).measures.get(cont).getValue();
                    
                }
                output += "\n";
            }
            //PrintStream out = null;
            String name = this.streams.get(0).algorithm.get(0).measures.get(cont).getName();
//            try {
//                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + name + ".csv")));
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            System.setOut(out);
//            System.out.println(output);
//            out.close();
            try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + name + ".csv"));
            out.write(output);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println(name + ".csv");

        }
            cont++;
        }

    }

    /**
     * Generates a HTML summary that shows the gains, loses or ties of each
     * algorithm against each other, in a specific measure..
     */
    public void computeWinsTiesLossesHTML(String path) {

        List<Algorithm> alg = this.streams.get(0).algorithm;
        int algorithmSize = this.streams.get(0).algorithm.size();
        String tablaSalida = "";
        tablaSalida += "<TABLE BORDER=1 WIDTH=\"100%\" ALIGN=CENTER>\n";
        tablaSalida += "<CAPTION> Experiment";
        tablaSalida += "<TR> <TD ROWSPAN = 2>Algorithm <TD TD ROWSPAN = 2>PM";

        for (int i = 1; i < algorithmSize; i++) {
            tablaSalida += "<TD COLSPAN = 3>" + alg.get(i).name;
        }
        tablaSalida += "<TD>AVG";
        tablaSalida += "<TR>";
        for (int i = 1; i < algorithmSize; i++) {
            tablaSalida += "<TD>" + "Wins" + "<TD>" + "Losses" + "<TD>" + "Ties";
        }
        for (int i = 0; i < algorithmSize; i++) {
            tablaSalida += "<TR><TD ROWSPAN = " + alg.get(i).measures.size() + ">" + alg.get(i).name;
            List<Measure> measureRow[] = alg.get(i).getMeasuresPerData(streams);
            int cont = 0;
            while (cont != this.streams.get(0).algorithm.get(i).measures.size()) {

                //String name = measureRow[i].get(cont).getName();
                String name = alg.get(i).measures.get(cont).getName();
                tablaSalida += "<TD>" + name;
                double sum = 0.0;

                for (int j = 1; j < algorithmSize; j++) {
                    List<Measure> measureCol[] = alg.get(j).getMeasuresPerData(streams);
                    int win = 0, losses = 0, ties = 0;
                    for (int k = 0; k < measureCol.length; k++) {
                        double alg1 = measureRow[k].get(cont).getValue();
                        double alg2 = measureCol[k].get(cont).getValue();
                        if (j == 1) {
                            sum += measureRow[k].get(cont).getValue();
                        }
                        if (measureRow[k].get(cont).isType()) {
                            if (Algorithm.Round(alg1) > Algorithm.Round(alg2)) {
                                win++;
                            } else if (Algorithm.Round(alg1) < Algorithm.Round(alg2)) {
                                losses++;
                            } else {
                                ties++;
                            }
                        } else {
                            if (alg1 < alg2) {
                                win++;
                            } else if (alg1 > alg2) {
                                losses++;
                            } else {
                                ties++;
                            }
                        }

                    }

                    if (i < j) {
                        tablaSalida += "<TD>" + win;
                        tablaSalida += "<TD>" + losses;
                        tablaSalida += "<TD>" + ties;
                    } else {
                        tablaSalida += "<TD> ";
                        tablaSalida += "<TD> ";
                        tablaSalida += "<TD> ";
                    }

                }

                sum = (double) sum / measureRow.length;
                tablaSalida += "<TD>" + Algorithm.format(sum);
                tablaSalida += "<TR>";
                cont++;
            }

        }
        tablaSalida += "</TABLE>";
//        PrintStream salida = null;
//        try {
//            salida = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + "summary.win.ties.losses.html")));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Summary.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.setOut(salida);
//        System.out.println(tablaSalida);
//        salida.close();
          try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path + "summary.win.ties.losses.html"));
            out.write(tablaSalida);  //Replace with the string 
            //you are trying to write  
            out.close();
        } catch (IOException e) {
            System.out.println("Error saving summary.win.ties.losses.html");

        }
        

    }

}
