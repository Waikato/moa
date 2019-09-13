/*
 *    StatisticalTest.java
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
 *    The statistical tests programmed in this class were taken from 
 *    KEEL(Knowledge Extraction based on Evolutionary Learning) software. 
 *    KEEL is an open source (GPLv3) Java software tool that can be used for 
 *    a large number of different knowledge data discovery tasks.
 *    J. Alcalá-Fdez, L. Sánchez, S. García, M.J. del Jesus, S. Ventura, 
 *    J.M. Garrell, J. Otero, C. Romero, J. Bacardit, V.M. Rivas, J.C. Fernández, 
 *    F. Herrera. KEEL: A Software Tool to Assess Evolutionary Algorithms to 
 *    Data Mining Problems. Soft Computing 13:3 (2009) 307-318, 
 *    doi: 10.1007/s00500-008-0323-y.
 *    J. Alcalá-Fdez, A. Fernandez, J. Luengo, J. Derrac, S. García, L. Sánchez, 
 *    F. Herrera. KEEL Data-Mining Software Tool: Data Set Repository, 
 *    Integration of Algorithms and Experimental Analysis Framework. Journal of 
 *    Multiple-Valued Logic and Soft Computing 17:2-3 (2011) 255-287. 
 */
package moa.gui.experimentertab.statisticaltests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import moa.gui.experimentertab.Algorithm;
import moa.gui.experimentertab.Stream;

/**
 *
 * @author Alberto Verdecia Cabrera (averdeciac@gmail.com)
 */
public class StatisticalTest {

    ArrayList algoritmos;
    ArrayList datasets;
    ArrayList datos;
    //String linea, token;
    int i, j, k, m;
    int posicion;
    double mean[][];
    Pareja orden[][];
    Pareja rank[][];
    boolean encontrado;
    int ig;
    double sum;
    boolean visto[];
    ArrayList porVisitar;
    double Rj[];
    double friedman;
    double sumatoria = 0;
    double termino1, termino2, termino3;
    double iman;
    boolean vistos[];
    int pos, tmp;
    double min;
    double maxVal;
    double rankingRef;
    double Pi[];
    double ALPHAiHolm[];
    double ALPHAiShaffer[];
    String ordenAlgoritmos[];
    double ordenRankings[];
    int order[];
    double adjustedP[][];
    double Ci[];
    double SE;
    boolean parar, otro;
    ArrayList indices = new ArrayList();
    ArrayList exhaustiveI = new ArrayList();
    boolean[][] cuadro;
    double minPi, tmpPi, maxAPi, tmpAPi;
    Relation[] parejitas;
    int lineaN = 0;
    int columnaN = 0;
    ArrayList T;
    int Tarray[];
    ArrayList<RankPerAlgorithm> rankAlg;
    double pFriedman, pIman;
    public List<Stream> streams = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param streams
     */
    public StatisticalTest(List<Stream> streams) {
        this.streams = streams;
        algoritmos = new ArrayList();
        datasets = new ArrayList();
        datos = new ArrayList();
        rankAlg = new ArrayList<>();

    }

    /**
     * Read a csv file from an path.
     *
     * @param path
     */
    public void readCSV(String path) {
        String cadena, linea, token;
        StringTokenizer lineas, tokens;
        cadena = Fichero.leeFichero(path);
        lineas = new StringTokenizer(cadena, "\n\r");
        while (lineas.hasMoreTokens()) {
            linea = lineas.nextToken();
            tokens = new StringTokenizer(linea, ",\t");
            columnaN = 0;
            while (tokens.hasMoreTokens()) {
                if (lineaN == 0) {
                    if (columnaN == 0) {
                        token = tokens.nextToken();
                    } else {
                        token = tokens.nextToken();
                        algoritmos.add(token);
                        datos.add(new ArrayList());
                    }
                } else {
                    if (columnaN == 0) {
                        token = tokens.nextToken();
                        datasets.add(token);
                    } else {
                        token = tokens.nextToken();
                        ((ArrayList) datos.get(columnaN - 1)).add(new Double(token));
                    }
                }
                columnaN++;
            }
            lineaN++;
        }
    }

    /**
     * Read data from experiments sumaries.
     */
    public void readData() {

        int cont = 0;
        int algorithmSize = this.streams.get(0).algorithm.size();
        int streamSize = this.streams.size();
        int measureSize = this.streams.get(0).algorithm.get(0).measures.size();

        for (int i = 0; i < algorithmSize; i++) {
            algoritmos.add(this.streams.get(0).algorithm.get(i).name);
            datos.add(new ArrayList());
        }
        for (int i = 0; i < streamSize; i++) {
            List<Algorithm> alg = this.streams.get(i).algorithm;
            datasets.add(this.streams.get(i).name);
            for (int j = 0; j < algorithmSize; j++) {
                ((ArrayList) datos.get(j)).add(alg.get(j).measures.get(cont).getValue());
            }

        }

    }

    /**
     * Compute the average ranking of the algorithms.
     */
    public void avgPerformance() {
        mean = new double[datasets.size()][algoritmos.size()];

        /*Compute the average performance per algorithm for each data set*/
        for (i = 0; i < datasets.size(); i++) {
            for (j = 0; j < algoritmos.size(); j++) {
                mean[i][j] = ((Double) ((ArrayList) datos.get(j)).get(i));
            }
        }

        /*We use the pareja structure to compute and order rankings*/
        orden = new Pareja[datasets.size()][algoritmos.size()];
        for (i = 0; i < datasets.size(); i++) {
            for (j = 0; j < algoritmos.size(); j++) {
                orden[i][j] = new Pareja(j, mean[i][j]);
            }
            Arrays.sort(orden[i]);
        }

        /*building of the rankings table per algorithms and data sets*/
        rank = new Pareja[datasets.size()][algoritmos.size()];
        posicion = 0;
        for (i = 0; i < datasets.size(); i++) {
            for (j = 0; j < algoritmos.size(); j++) {
                encontrado = false;
                for (k = 0; k < algoritmos.size() && !encontrado; k++) {
                    if (orden[i][k].indice == j) {
                        encontrado = true;
                        posicion = k + 1;
                    }
                }
                rank[i][j] = new Pareja(posicion, orden[i][posicion - 1].valor);
            }
        }

        /*In the case of having the same performance, the rankings are equal*/
        for (i = 0; i < datasets.size(); i++) {
            visto = new boolean[algoritmos.size()];
            porVisitar = new ArrayList();

            Arrays.fill(visto, false);
            for (j = 0; j < algoritmos.size(); j++) {
                porVisitar.clear();
                sum = rank[i][j].indice;
                visto[j] = true;
                ig = 1;
                for (k = j + 1; k < algoritmos.size(); k++) {
                    if (rank[i][j].valor == rank[i][k].valor && !visto[k]) {
                        sum += rank[i][k].indice;
                        ig++;
                        porVisitar.add(new Integer(k));
                        visto[k] = true;
                    }
                }
                sum /= (double) ig;
                rank[i][j].indice = sum;
                for (k = 0; k < porVisitar.size(); k++) {
                    rank[i][((Integer) porVisitar.get(k))].indice = sum;
                }
            }
        }
        avgRankingPerAlgorithm();
    }

    private void avgRankingPerAlgorithm() {

        Rj = new double[algoritmos.size()];
        for (i = 0; i < algoritmos.size(); i++) {
            Rj[i] = 0;
            for (j = 0; j < datasets.size(); j++) {
                Rj[i] += rank[j][i].indice / ((double) datasets.size());
            }
        }
        /*Print the average ranking per algorithm*/
        for (i = 0; i < algoritmos.size(); i++) {
            rankAlg.add(new RankPerAlgorithm((String) algoritmos.get(i), Rj[i]));
        }
        //Order de Algorithms with rank
        Collections.sort(rankAlg, new ComparatorImpl());

        /*Compute the Friedman statistic*/
        termino1 = (12 * (double) datasets.size()) / ((double) algoritmos.size()
                * ((double) algoritmos.size() + 1));
        termino2 = (double) algoritmos.size() * ((double) algoritmos.size() + 1)
                * ((double) algoritmos.size() + 1) / (4.0);
        for (i = 0; i < algoritmos.size(); i++) {
            sumatoria += Rj[i] * Rj[i];
        }
        friedman = (sumatoria - termino2) * termino1;

        pFriedman = ChiSq(friedman, (algoritmos.size() - 1));

        /*Compute the Iman-Davenport statistic*/
        iman = ((datasets.size() - 1) * friedman) / (datasets.size() * (algoritmos.size() - 1) - friedman);
        pIman = FishF(iman, (algoritmos.size() - 1), (algoritmos.size() - 1) * (datasets.size() - 1));
        //System.out.print("P-value computed by Iman and Daveport Test: " + pIman + ".\\newline\n\n");

        termino3 = Math.sqrt((double) algoritmos.size() * ((double) algoritmos.size() + 1)
                / (6.0 * (double) datasets.size()));
        //Inicialize values
        inicialize();

    }

    /**
     * Return the p-value computed by Friedman test.
     *
     * @return pFriedman
     */
    public double getFriedmanPValue() {
        return pFriedman;
    }

    /**
     * Return the p-value Iman and Daveport test.
     *
     * @return pIman
     */
    public double getImanPValue() {
        return pIman;
    }

    /**
     * Return the ranking of the algorithms.
     *
     * @return rankAlg
     */
    public ArrayList<RankPerAlgorithm> getRankAlg() {
        return rankAlg;
    }

    private void inicialize() {
        /*Compute the unadjusted p_i value for each comparison alpha=0.10*/
        Pi = new double[(int) combinatoria(2, algoritmos.size())];
        ordenAlgoritmos = new String[(int) combinatoria(2, algoritmos.size())];
        ordenRankings = new double[(int) combinatoria(2, algoritmos.size())];
        order = new int[(int) combinatoria(2, algoritmos.size())];
        parejitas = new Relation[(int) combinatoria(2, algoritmos.size())];
        T = new ArrayList();
        T = trueHShaffer(algoritmos.size());
        Tarray = new int[T.size()];
        for (i = 0; i < T.size(); i++) {
            Tarray[i] = ((Integer) T.get(i));
        }
        Arrays.sort(Tarray);
        SE = termino3;
        vistos = new boolean[(int) combinatoria(2, algoritmos.size())];
        for (i = 0, k = 0; i < algoritmos.size(); i++) {
            for (j = i + 1; j < algoritmos.size(); j++, k++) {
                ordenRankings[k] = Math.abs(Rj[i] - Rj[j]);
                ordenAlgoritmos[k] = (String) algoritmos.get(i) + " vs. " + (String) algoritmos.get(j);
                parejitas[k] = new Relation(i, j);
            }
        }

        Arrays.fill(vistos, false);
        for (i = 0; i < ordenRankings.length; i++) {
            for (j = 0; vistos[j] == true; j++);
            pos = j;
            maxVal = ordenRankings[j];
            for (j = j + 1; j < ordenRankings.length; j++) {
                if (vistos[j] == false && ordenRankings[j] > maxVal) {
                    pos = j;
                    maxVal = ordenRankings[j];
                }
            }
            vistos[pos] = true;
            order[i] = pos;
        }

        /*Computing the logically related hypotheses tests (Shaffer and Bergmann-Hommel)*/
        pos = 0;
        tmp = Tarray.length - 1;
        for (i = 0; i < order.length; i++) {
            Pi[i] = 2 * CDF_Normal.normp((-1) * Math.abs((ordenRankings[order[i]]) / SE));

        }

    }

    /**
     * Return the p-values computed by the Holm test.
     *
     * @return algPValues
     */
    public ArrayList<PValuePerTwoAlgorithm> holmTest() {
        ArrayList<PValuePerTwoAlgorithm> algPValues = new ArrayList<>();
        double[] holmPValues;
        holmPValues = new double[Pi.length];

        for (i = 0; i < holmPValues.length; i++) {
            holmPValues[i] = Pi[i] * (double) (holmPValues.length - i);
        }
        for (i = 1; i < holmPValues.length; i++) {
            if (holmPValues[i] < holmPValues[i - 1]) {
                holmPValues[i] = holmPValues[i - 1];
            }
        }
        for (i = 0; i < Pi.length; i++) {
            algPValues.add(new PValuePerTwoAlgorithm(algoritmos.get(parejitas[order[i]].i).toString(),
                    algoritmos.get(parejitas[order[i]].j).toString(), holmPValues[i]));
        }

        return algPValues;
    }

    /**
     * Return the p-values computed by the Shaffer test.
     *
     * @return algPValues
     */
    public ArrayList<PValuePerTwoAlgorithm> shafferTest() {
        ArrayList<PValuePerTwoAlgorithm> algPValues = new ArrayList<>();
        double[] shafferPValues;
        shafferPValues = new double[Pi.length];
        pos = 0;
        tmp = Tarray.length - 1;
        for (i = 0; i < shafferPValues.length; i++) {
            shafferPValues[i] = Pi[i] * ((double) shafferPValues.length - (double) Math.max(pos, i));
            if (i == pos) {
                tmp--;
                pos = (int) combinatoria(2, algoritmos.size()) - Tarray[tmp];
            }
        }
        for (i = 1; i < shafferPValues.length; i++) {
            if (shafferPValues[i] < shafferPValues[i - 1]) {
                shafferPValues[i] = shafferPValues[i - 1];
            }
            if (shafferPValues[i] < shafferPValues[i - 1]) {
                shafferPValues[i] = shafferPValues[i - 1];
            }
        }

        for (i = 0; i < Pi.length; i++) {
            algPValues.add(new PValuePerTwoAlgorithm(algoritmos.get(parejitas[order[i]].i).toString(),
                    algoritmos.get(parejitas[order[i]].j).toString(), shafferPValues[i]));
        }

        return algPValues;
    }

    /**
     * Return the p-values computed by the Nemenyi test.
     *
     * @return algPValues
     */
    public ArrayList<PValuePerTwoAlgorithm> nemenyiTest() {
        ArrayList<PValuePerTwoAlgorithm> algPValues = new ArrayList<>();
        double[] nemenyiPValues;
        nemenyiPValues = new double[Pi.length];
        pos = 0;
        tmp = Tarray.length - 1;
        for (i = 0; i < nemenyiPValues.length; i++) {
            nemenyiPValues[i] = Pi[i] * (double) (nemenyiPValues.length);
        }

        for (i = 0; i < Pi.length; i++) {
            algPValues.add(new PValuePerTwoAlgorithm(algoritmos.get(parejitas[order[i]].i).toString(),
                    algoritmos.get(parejitas[order[i]].j).toString(), nemenyiPValues[i]));
        }
        return algPValues;
    }

    private static double combinatoria(int m, int n) {

        double result = 1;
        int i;

        if (n >= m) {
            for (i = 1; i <= m; i++) {
                result *= (double) (n - m + i) / (double) i;
            }
        } else {
            result = 0;
        }
        return result;
    }

    private static ArrayList obtainExhaustive(ArrayList indices) {

        ArrayList result = new ArrayList();
        int i, j, k;
        String binario;
        boolean[] number = new boolean[indices.size()];
        ArrayList ind1, ind2;
        ArrayList set = new ArrayList();
        ArrayList res1, res2;
        ArrayList temp;
        ArrayList temp2;
        ArrayList temp3;

        ind1 = new ArrayList();
        ind2 = new ArrayList();
        temp = new ArrayList();
        temp2 = new ArrayList();
        temp3 = new ArrayList();

        for (i = 0; i < indices.size(); i++) {
            for (j = i + 1; j < indices.size(); j++) {
                set.add(new Relation(((Integer) indices.get(i)), ((Integer) indices.get(j))));
            }
        }
        if (set.size() > 0) {
            result.add(set);
        }

        for (i = 1; i < (int) (Math.pow(2, indices.size() - 1)); i++) {
            Arrays.fill(number, false);
            ind1.clear();
            ind2.clear();
            temp.clear();
            temp2.clear();
            temp3.clear();
            binario = Integer.toString(i, 2);
            for (k = 0; k < number.length - binario.length(); k++) {
                number[k] = false;
            }
            for (j = 0; j < binario.length(); j++, k++) {
                if (binario.charAt(j) == '1') {
                    number[k] = true;
                }
            }
            for (j = 0; j < number.length; j++) {
                if (number[j] == true) {
                    ind1.add(((Integer) indices.get(j)));
                } else {
                    ind2.add(((Integer) indices.get(j)));
                }
            }
            res1 = obtainExhaustive(ind1);
            res2 = obtainExhaustive(ind2);
            for (j = 0; j < res1.size(); j++) {
                result.add(new ArrayList((ArrayList) res1.get(j)));
            }
            for (j = 0; j < res2.size(); j++) {
                result.add(new ArrayList((ArrayList) res2.get(j)));
            }
            for (j = 0; j < res1.size(); j++) {
                temp = (ArrayList) ((ArrayList) res1.get(j)).clone();
                for (k = 0; k < res2.size(); k++) {
                    temp2 = (ArrayList) temp.clone();
                    temp3 = (ArrayList) ((ArrayList) res2.get(k)).clone();
                    if (((Relation) temp2.get(0)).i < ((Relation) temp3.get(0)).i) {
                        temp2.addAll((ArrayList) temp3);
                        result.add(new ArrayList(temp2));
                    } else {
                        temp3.addAll((ArrayList) temp2);
                        result.add(new ArrayList(temp3));

                    }
                }
            }
        }
        for (i = 0; i < result.size(); i++) {
            if (((ArrayList) result.get(i)).toString().equalsIgnoreCase("[]")) {
                result.remove(i);
                i--;
            }
        }
        for (i = 0; i < result.size(); i++) {
            for (j = i + 1; j < result.size(); j++) {
                if (((ArrayList) result.get(i)).toString().equalsIgnoreCase(((ArrayList) result.get(j)).toString())) {
                    result.remove(j);
                    j--;
                }
            }
        }
        return result;
    }

    private static ArrayList trueHShaffer(int k) {

        ArrayList number;
        int j;
        ArrayList tmp, tmp2;
        int p;

        number = new ArrayList();
        tmp = new ArrayList();
        if (k <= 1) {
            number.add(0);
        } else {
            for (j = 1; j <= k; j++) {
                tmp = trueHShaffer(k - j);
                tmp2 = new ArrayList();
                for (p = 0; p < tmp.size(); p++) {
                    tmp2.add(((Integer) (tmp.get(p))) + (int) combinatoria(2, j));
                }
                number = unionVectores(number, tmp2);
            }
        }

        return number;
    }

    private static ArrayList unionVectores(ArrayList a, ArrayList b) {

        int i;

        for (i = 0; i < b.size(); i++) {
            if (a.contains(new Integer((Integer) (b.get(i)))) == false) {
                a.add(b.get(i));
            }
        }

        return a;
    }

    private static double ChiSq(double x, int n) {
        if (n == 1 & x > 1000) {
            return 0;
        }
        if (x > 1000 | n > 1000) {
            double q = ChiSq((x - n) * (x - n) / (2 * n), 1) / 2;
            if (x > n) {
                return q;
            }
            {
                return 1 - q;
            }
        }
        double p = Math.exp(-0.5 * x);
        if ((n % 2) == 1) {
            p = p * Math.sqrt(2 * x / Math.PI);
        }
        double k = n;
        while (k >= 2) {
            p = p * x / k;
            k = k - 2;
        }
        double t = p;
        double a = n;
        while (t > 0.0000000001 * p) {
            a = a + 2;
            t = t * x / a;
            p = p + t;
        }
        return 1 - p;
    }

    private static double FishF(double f, int n1, int n2) {
        double x = n2 / (n1 * f + n2);
        if ((n1 % 2) == 0) {
            return StatCom(1 - x, n2, n1 + n2 - 4, n2 - 2) * Math.pow(x, n2 / 2.0);
        }
        if ((n2 % 2) == 0) {
            return 1
                    - StatCom(x, n1, n1 + n2 - 4, n1 - 2)
                    * Math.pow(1 - x, n1 / 2.0);
        }
        double th = Math.atan(Math.sqrt(n1 * f / (1.0 * n2)));
        double a = th / (Math.PI / 2.0);
        double sth = Math.sin(th);
        double cth = Math.cos(th);
        if (n2 > 1) {
            a = a
                    + sth * cth * StatCom(cth * cth, 2, n2 - 3, -1) / (Math.PI / 2.0);
        }
        if (n1 == 1) {
            return 1 - a;
        }
        double c = 4 * StatCom(sth * sth, n2 + 1, n1 + n2 - 4, n2 - 2) * sth
                * Math.pow(cth, n2) / Math.PI;
        if (n2 == 1) {
            return 1 - a + c / 2.0;
        }
        int k = 2;
        while (k <= (n2 - 1) / 2.0) {
            c = c * k / (k - .5);
            k = k + 1;
        }
        return 1 - a + c;
    }

    private static double StatCom(double q, int i, int j, int b) {
        double zz = 1;
        double z = zz;
        int k = i;
        while (k <= j) {
            zz = zz * q * k / (k - b);
            z = z + zz;
            k = k + 2;
        }
        return z;
    }

    private static class ComparatorImpl implements Comparator<RankPerAlgorithm> {

        public ComparatorImpl() {
        }

        @Override
        public int compare(RankPerAlgorithm r1, RankPerAlgorithm r2) {
            return r1.compareTo(r2);
        }
    }

}
