/*
 *    Cramer.java
 *    Copyright (C) 2017 Instituto Federal de Pernambuco
 *    @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
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
package moa.classifiers.core.statisticaltests;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import org.apache.commons.math3.complex.Complex;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.ArffLoader;
import com.yahoo.labs.samoa.instances.Instance;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the Multivariate Non-parametric Cramer Von Mises Statistical Test.
 *
 * @author Paulo Gonçalves
 *
 */
public class Cramer extends AbstractOptionHandler implements StatisticalTest {

    private List<Instance> sample1i;
    private List<Instance> sample2i;

    public FloatOption confidenceLevelOption = new FloatOption(
            "confidenceLevel",
            'q',
            "The confidence level to use in the Cramer test.",
            0.95, 0, 1);

    public IntOption replicatesOption = new IntOption("replicates", 'r',
            "Number of replications.", 1000, 1,
            Integer.MAX_VALUE);

    public MultiChoiceOption kernelOption = new MultiChoiceOption("kernel", 'f',
            "Kernel function to use.", new String[]{"CRAMER", "BAHR", "LOG", "FRAC A", "FRAC B"},
            new String[]{"CRAMER", "BAHR", "LOG", "FRAC A", "FRAC B"},
            0);

    public FloatOption maxMOption = new FloatOption(
            "maxM",
            'm',
            "Maximum M.",
            Math.pow(2, 14), 1, Float.MAX_VALUE);

    public IntOption kOption = new IntOption("k", 'k',
            "K value.", 160, 1,
            Integer.MAX_VALUE);

    public static final int CRAMER = 0;
    public static final int BAHR = 1;
    public static final int LOG = 2;
    public static final int FRACA = 3;
    public static final int FRACB = 4;

    // compute the FFT of x[], assuming its length is a power of 2
    private Complex[] fft(Complex[] x) {
        int N = x.length;

        // base case
        if (N == 1) {
            return new Complex[]{x[0]};
        }

        // radix 2 Cooley-Tukey FFT
        if (N % 2 != 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // fft of even terms
        Complex[] even = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++) {
            even[k] = x[2 * k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd = even;  // reuse the array
        for (int k = 0; k < N / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] yy = new Complex[N];
        for (int k = 0; k < N / 2; k++) {
            double kth = -2 * k * Math.PI / N;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            yy[k] = q[k].add(wk.multiply(r[k]));
            yy[k + N / 2] = q[k].subtract(wk.multiply(r[k]));
        }
        return yy;
    }

    private double phiCramer(double x) {
        return (Math.sqrt(x) / 2);
    }

    private double phiBahr(double x) {
        return (1 - Math.exp(-x / 2));
    }

    private double phiLog(double x) {
        return (Math.log(1 + x));
    }

    private double phiFracA(double x) {
        return (1 - 1 / (1 + x));
    }

    private double phiFracB(double x) {
        return (1 - 1 / ((1 + x) * (1 + x)));
    }

    private double subtractRows(double[][] matrix, int i, int j) {
        double sum = 0;
        for (int k = 0; k < matrix[i].length; k++) {
            sum += (matrix[i][k] - matrix[j][k]) * (matrix[i][k] - matrix[j][k]);
        }
        return sum;
    }

    private void kernel(int kernel, double[][] lookup) {
        for (double[] lookup1 : lookup) {
            for (int j = 0; j < lookup1.length; j++) {
                switch (kernel) {
                    case CRAMER:
                        lookup1[j] = this.phiCramer(lookup1[j]);
                        break;
                    case BAHR:
                        lookup1[j] = this.phiBahr(lookup1[j]);
                        break;
                    case FRACA:
                        lookup1[j] = this.phiFracA(lookup1[j]);
                        break;
                    case FRACB:
                        lookup1[j] = this.phiFracB(lookup1[j]);
                        break;
                    case LOG:
                        lookup1[j] = this.phiLog(lookup1[j]);
                        break;
                }
            }
        }
    }

    private double sumCells(double[][] lookup, int[] xind, int[] yind) {
        double sum = 0;
        for (int i = 0; i < xind.length; i++) {
            for (int j = 0; j < yind.length; j++) {
                sum += lookup[xind[i]][yind[j]];
            }
        }
        return sum;
    }

    private double cramerStatistic(int m, int n, double[][] lookup) {
        int[] xind = new int[m];
        for (int i = 0; i < m; i++) {
            xind[i] = i;
        }
        int[] yind = new int[n];
        for (int i = m, j = 0; i < m + n; i++, j++) {
            yind[j] = i;
        }
        double mm = m, nn = n;
        return mm * nn / (mm + nn) * (2 * this.sumCells(lookup, xind, yind) / (mm * nn) - this.sumCells(lookup, xind, xind) / (mm * mm) - this.sumCells(lookup, yind, yind) / (nn * nn));
    }

    class Boot {

        double t0;
        double[][] t;
    }

    private double rank(double t0, double[][] t) {
        double[] temp = new double[t.length * t[0].length + 1];
        temp[0] = t0;
        int p = 1;
        for (double[] t1 : t) {
            for (int j = 0; j < t1.length; j++) {
                temp[p++] = t1[j];
            }
        }
        double[] ordTemp = temp.clone();
        Arrays.sort(ordTemp);
        Map<Double, Double> map = new TreeMap();
        for (int i = 0; i < ordTemp.length; i++) {
            double xTemp = ordTemp[i], sum = i;
            int count = 1;
            while (i + 1 < ordTemp.length && xTemp == ordTemp[++i]) {
                sum += i;
                count++;
            }
            map.put(xTemp, 1 + sum / count);
            if (i + 1 == ordTemp.length) {
                map.put(ordTemp[i], 1.0 + i);
                break;
            } else {
                i--;
            }
        }
        return map.get(t0);
    }

    private double[] linearize(double[][] matrix) {
        double[] vector = new double[matrix.length * matrix[0].length];
        int p = 0;
        for (double[] matrix1 : matrix) {
            for (int j = 0; j < matrix1.length; j++) {
                vector[p++] = matrix1[j];
            }
        }
        return vector;
    }

    private double[] createVector(int replicates) {
        double[] result = new double[replicates];
        for (int i = 0; i < replicates; i++) {
            result[i] = (i + 1) / replicates;
        }
        return result;
    }

    private void divide(double[] vector, double divisor) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= divisor;
        }
    }

    private void divide(double[][] matrix, double divisor) {
        for (double[] matrix1 : matrix) {
            for (int j = 0; j < matrix1.length; j++) {
                matrix1[j] /= divisor;
            }
        }
    }

    private double[] createArray(int M, double k) {
        double[] result = new double[M];
        for (int i = 0; i < M; i++) {
            result[i] = i * k / M;
        }
        return result;
    }

    private double[] createArray2(int M, double k) {
        double[] result = new double[M];
        for (int i = 0; i < M; i++) {
            result[i] = i * 2 * Math.PI / k;
        }
        return result;
    }

    private Complex[][] complex(Complex[] v, double[] t) {
        Complex[][] complex = new Complex[v.length][t.length];
        for (int i = 0; i < v.length; i++) {
            for (int j = 0; j < t.length; j++) {
                complex[i][j] = v[i].multiply(t[j]).add(new Complex(1, 0)).log().multiply(-0.5);
            }
        }
        return complex;
    }

    private Complex[] characteristic(double[] lambdasquare, double[] t) {
        // z<--0.5*log(1-2i*lambdasquare%*%t(t));
        Complex c = new Complex(0, -2);
        Complex[] temp = new Complex[lambdasquare.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = c.multiply(lambdasquare[i]);
        }
        Complex[][] z = this.complex(temp, t);
        // return(exp(complex(length(t),rowsum(Re(z),rep(1,length(lambdasquare))),rowsum(Im(z),rep(1,length(lambdasquare))))))
        double[] real = new double[t.length];
        double[] imag = new double[t.length];
        for (int j = 0; j < t.length; j++) {
            for (Complex[] z1 : z) {
                real[j] += z1[j].getReal();
                imag[j] += z1[j].getImaginary();
            }
        }
        Complex[] result = new Complex[t.length];
        for (int i = 0; i < t.length; i++) {
            result[i] = new Complex(real[i], imag[i]);
            result[i] = result[i].exp();
        }
        return result;
    }

    private double[] imaginary(Complex[] c) {
        double[] ret = new double[c.length];
        for (int i = 0; i < c.length; i++) {
            ret[i] = c[i].getImaginary();
        }
        return ret;
    }

    private double[] plus(double[] array, double m) {
        double[] ret = new double[array.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = array[i] + m;
        }
        return ret;
    }

    private double sum(double[] lambdasquare) {
        double sum = 0;
        for (int i = 0; i < lambdasquare.length; i++) {
            sum += lambdasquare[i];
        }
        return sum;
    }

    private double[] sum(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    private int whichMin(double[] a, int M, double confLevel) {
        double[] ret = new double[M / 2];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Math.abs(a[i] - confLevel);
        }
        int minIndex = 0;
        double minValue = Double.MAX_VALUE;
        for (int i = 0; i < ret.length; i++) {
            if (ret[i] < minValue) {
                minValue = ret[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    private int whichMin(double[] ret) {
        int minIndex = 0;
        double minValue = Double.MAX_VALUE;
        for (int i = 0; i < ret.length; i++) {
            if (ret[i] < minValue) {
                minValue = ret[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    class Kritwert {

        double quantile;
        double[] x;
        double[] Fx;

        public Kritwert(double quantile, double[] x, double[] fx) {
            super();
            this.quantile = quantile;
            this.x = x;
            Fx = fx;
        }
    }

    private Complex[] multiply(double[] t, Complex c) {
        Complex[] ret = new Complex[t.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = c.multiply(t[i]);
        }
        return ret;
    }

    private void multiply(double[] t, Complex[] c) {
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i].multiply(t[i]);
        }
    }

    private void multiply(Complex[] c, double multiply) {
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i].multiply(multiply);
        }
    }

    private void multiply(double[] c, double multiply) {
        for (int i = 0; i < c.length; i++) {
            c[i] *= multiply;
        }
    }

    private Kritwert kritwertfft(double[] lambdasquare, double confLevel, double maxM, int k) {
        double sumLambasquare = this.sum(lambdasquare);
        // M<-2^11
        int M = (int) Math.pow(2, 11);
        // while (150*pi*M/K^2<(2*sum(lambdasquare)+lambdasquare[1])) M<-M*2
        while (150 * Math.PI * M / (k * k) < (2 * sumLambasquare + lambdasquare[0])) {
            M *= 2;
        }
        // M<-min(c(M,maxM))
        M = (int) Math.min(M, maxM);
        // goodlimit<-150*pi*M/K^2
        double goodlimit = 150 * Math.PI * M / (k * k);
        // a<-0
        // t<-0:(M-1)*K/M
        double[] t = this.createArray(M, k);
        // x<-0:(M-1)*2*pi/K
        double[] xx = this.createArray2(M, k);
        // t[1]<-1
        t[0] = 1;
        // h<-.cramer.characteristicfunction(lambdasquare,t)/t*exp(-a*1i*t);
        Complex[] h = this.characteristic(lambdasquare, t);
        Complex z = new Complex(0, 1).multiply(-0);
        Complex[] hTemp = this.multiply(t, z);
        for (int i = 0; i < hTemp.length; i++) {
            hTemp[i] = hTemp[i].exp();
        }
        this.multiply(t, hTemp);
        for (int i = 0; i < h.length; i++) {
            h[i] = h[i].divide(hTemp[i]);
        }
        // h[1]<-complex(1,0,1)*sum(lambdasquare)
        h[0] = new Complex(0, 1).multiply(sumLambasquare);
        // Fx<-1/2-Im(K/(M*pi)*fft(h,inverse=FALSE))+K/(2*M*pi)*(sum(lambdasquare)+x+a)
        Complex[] temp = fft(h);
        this.multiply(temp, k / (M * Math.PI));
        double[] tempFx = this.imaginary(temp);
        this.multiply(tempFx, -1);
        tempFx = this.plus(tempFx, 0.5);
        double[] tempX = this.plus(xx, sumLambasquare);
        this.multiply(tempX, k / (2 * M * Math.PI));
        double[] Fx = this.sum(tempFx, tempX);
        // xindex<-which.min(abs(Fx[1:(M/2)]-conf.level))
        int xindex = this.whichMin(Fx, M, confLevel);
        // if (Fx[xindex]>conf.level) xindex<-xindex-1
        if (Fx[xindex] > confLevel) {
            xindex--;
        }
        // if (xindex<1) xindex<-1
        if (xindex < 1) {
            xindex = 0;
        }
        // quantile<-x[xindex]+(conf.level-Fx[xindex])*(x[xindex+1]-x[xindex])/(Fx[xindex+1]-Fx[xindex])
        double quantile = xx[xindex] + (confLevel - Fx[xindex]) * (xx[xindex + 1] - xx[xindex]) / (Fx[xindex + 1] - Fx[xindex]);
        if (Fx[M / 2] < confLevel) {
            System.out.println("Quantile calculation discrepance. Try to increase K!");
        }
        if (quantile > goodlimit) {
            System.out.println("Quantile beyond good approximation limit. Try to increase maxM or decrease K!");
        }
        return new Kritwert(quantile, xx, Fx);
    }

    public CramerTest cramerTest(List<Instance> x, List<Instance> y) {
        return this.cramerTest(x, y, this.confidenceLevelOption.getValue(), this.replicatesOption.getValue(), "ordinary", false, this.kernelOption.getChosenIndex(), this.maxMOption.getValue(), this.kOption.getValue());
    }

    public CramerTest cramerTest1(List<List<Double>> x, List<List<Double>> y) {
        return this.cramerTest1(x, y, this.confidenceLevelOption.getValue(), this.replicatesOption.getValue(), "ordinary", false, this.kernelOption.getChosenIndex(), this.maxMOption.getValue(), this.kOption.getValue());
    }

    public CramerTest cramerTest1(List<List<Double>> x, List<List<Double>> y, double confLevel, int replicates, String sim, boolean justStatistic, int kernel, double maxM, int k) {
        CramerTest RVAL = new CramerTest(0, 0, 0, 0, 0, 0, 0, confLevel, replicates, null, null, null);
        // if ((is.matrix(x))&&(is.matrix(y))) if (ncol(x)==ncol(y)) RVAL$d<-ncol(x)
        RVAL.d = x.get(0).size();
        // RVAL$m<-nrow(x)
        RVAL.m = x.size();
        // RVAL$n<-nrow(y)
        RVAL.n = y.size();
        // daten<-matrix(c(t(x),t(y)),ncol=ncol(x),byrow=TRUE)
        double[][] daten = new double[RVAL.m + RVAL.n][];
        for (int i = 0; i < RVAL.m; i++) {
            double[] values = new double[x.get(i).size() - 1];
            System.arraycopy(x.get(i).toArray(), 0, values, 0, values.length);
            daten[i] = values;
        }
        for (int i = 0; i < RVAL.n; i++) {
            double[] values = new double[y.get(i).size() - 1];
            System.arraycopy(y.get(i).toArray(), 0, values, 0, values.length);
            daten[i + RVAL.m] = values;
        }
        return this.compute(RVAL, daten, replicates, sim, justStatistic, kernel, maxM, k);
    }

    private CramerTest compute(CramerTest RVAL, double[][] daten, int replicates, String sim, boolean justStatistic, int kernel, double maxM, int k) {
        // lookup<-matrix(rep(0,(RVAL$m+RVAL$n)^2),ncol=(RVAL$m+RVAL$n))
        double[][] lookup = new double[RVAL.m + RVAL.n][RVAL.m + RVAL.n];
        // for (i in 2:(RVAL$m+RVAL$n)) for (j in 1:(i-1)) { lookup[i,j]<-sum((daten[i,]-daten[j,])^2); lookup[j,i]<-lookup[i,j]; }
        for (int i = 1; i < RVAL.m + RVAL.n; i++) {
            for (int j = 0; j <= i - 1; j++) {
                lookup[i][j] = this.subtractRows(daten, i, j);
                lookup[j][i] = lookup[i][j];
            }
        }
        // lookup<-eval(call(kernel,lookup))
        this.kernel(kernel, lookup);
        if (justStatistic) {
            RVAL.statistic = this.cramerStatistic(RVAL.m, RVAL.n, lookup);
        } else if (sim.equals("eigenvalue")) {
            /* To be implemented! */
            Boot boot = new Boot();
            RVAL.statistic = boot.t0;
            RVAL.pValue = 1 - this.rank(boot.t0, boot.t) / (replicates + 1);
            double[] toSort = this.linearize(boot.t);
            Arrays.sort(toSort);
            RVAL.critValue = toSort[(int) Math.round(RVAL.confLevel * RVAL.replicates)];
            if (RVAL.statistic > RVAL.critValue) {
                RVAL.result = 1;
            }
            RVAL.hypdistX = toSort;
            RVAL.hypdistFx = this.createVector(replicates);
        } else {
            // RVAL$statistic<-.cramer.statistic(daten,1:(RVAL$m+RVAL$n),RVAL$m,RVAL$n,lookup)
            RVAL.statistic = this.cramerStatistic(RVAL.m, RVAL.n, lookup);
            // N<-RVAL$m+RVAL$n
            int N = RVAL.m + RVAL.n;
            // C1<-rep(0,N)
            double[] C1 = new double[N];
            // for (i in 1:N) for (j in 1:N) C1[i]<-C1[i]+lookup[i,j]
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    C1[i] += lookup[i][j];
                }
            }
            // C1<-C1/N
            this.divide(C1, N);
            // C2<-0
            double C2 = 0;
            // for (i in 1:N) for (j in 1:N) C2<-C2+lookup[i,j]
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    C2 += lookup[i][j];
                }
            }
            // C2<-C2/N^2
            C2 /= (N * N);
            // B<-matrix(rep(0,N^2),ncol=N)
            double[][] B = new double[N][N];
            // for (i in 1:N) for (j in 1:N) B[i,j]<-C1[i]+C1[j]-C2-lookup[i,j]
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    B[i][j] = C1[i] + C1[j] - C2 - lookup[i][j];
                }
            }
            // B<-B/N
            this.divide(B, N);
            // RVAL$ev<-eigen(B,FALSE)
            Matrix matrix = new Matrix(B);
            EigenvalueDecomposition evd = matrix.eig();
            double[] lambdasquare = evd.getRealEigenvalues();
            Arrays.sort(lambdasquare);
            this.reverse(lambdasquare);
            RVAL.ev = lambdasquare;
            // kritwert<-.cramer.kritwertfft(Re((RVAL$ev)$values),conf.level,maxM,K)
            Kritwert kw = this.kritwertfft(lambdasquare, RVAL.confLevel, maxM, k);
            // RVAL$p.value<-1-(kritwert$hypdist.Fx)[which.min(abs((kritwert$hypdist.x)[1:(3*length(kritwert$hypdist.x)/4)]-RVAL$statistic))];
            double[] temp = Arrays.copyOf(kw.x, 3 * kw.x.length / 4);
            temp = this.plus(temp, -RVAL.statistic);
            for (int i = 0; i < temp.length; i++) {
                temp[i] = Math.abs(temp[i]);
            }
            RVAL.pValue = 1 - kw.Fx[this.whichMin(temp)];
            // RVAL$crit.value<-kritwert$quantile
            RVAL.critValue = kw.quantile;
            // RVAL$hypdist.x<-kritwert$hypdist.x
            RVAL.hypdistX = kw.x;
            // RVAL$hypdist.Fx<-kritwert$hypdist.Fx
            RVAL.hypdistFx = kw.Fx;
            // if (RVAL$statistic>RVAL$crit.value) RVAL$result<-1
            if (RVAL.statistic > RVAL.critValue) {
                RVAL.result = 1.0;
            }
        }
        return RVAL;
    }

    public CramerTest cramerTest(List<Instance> x, List<Instance> y, double confLevel, int replicates, String sim, boolean justStatistic, int kernel, double maxM, int k) {
        CramerTest RVAL = new CramerTest(0, 0, 0, 0, 0, 0, 0, confLevel, replicates, null, null, null);
        // if ((is.matrix(x))&&(is.matrix(y))) if (ncol(x)==ncol(y)) RVAL$d<-ncol(x)
        RVAL.d = x.get(0).numAttributes();
        // RVAL$m<-nrow(x)
        RVAL.m = x.size();
        // RVAL$n<-nrow(y)
        RVAL.n = y.size();
        // daten<-matrix(c(t(x),t(y)),ncol=ncol(x),byrow=TRUE)
        double[][] daten = new double[RVAL.m + RVAL.n][];
        for (int i = 0; i < RVAL.m; i++) {
            Instance inst = x.get(i);
            double[] values = new double[inst.numAttributes() - 1];
            for (int j = 0; j < values.length; j++) {
                values[j] = inst.value(j);
            }
            daten[i] = values;
        }
        for (int i = 0; i < RVAL.n; i++) {
            Instance inst = y.get(i);
            double[] values = new double[inst.numAttributes() - 1];
            for (int j = 0; j < values.length; j++) {
                values[j] = inst.value(j);
            }
            daten[i + RVAL.m] = values;
        }
        return this.compute(RVAL, daten, replicates, sim, justStatistic, kernel, maxM, k);
    }

    private void reverse(double[] array) {
        double temp;
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            // swap the elements
            temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public class CramerTest {

        int d, m, n;
        double pValue, critValue, statistic, result, confLevel, replicates;
        double[] hypdistX, hypdistFx, ev;

        public CramerTest(int d, int m, int n, double pValue,
                double critValue, double statistic, double result,
                double confLevel, double replicates, double[] hypdistX,
                double[] hypdistFx, double[] ev) {
            super();
            this.d = d;
            this.m = m;
            this.n = n;
            this.pValue = pValue;
            this.critValue = critValue;
            this.statistic = statistic;
            this.result = result;
            this.confLevel = confLevel;
            this.replicates = replicates;
            this.hypdistX = hypdistX;
            this.hypdistFx = hypdistFx;
            this.ev = ev;
        }
    }

    public static List<Instance> fileToInstances(String path) {
        List<Instance> x = new ArrayList();
        try {
            FileReader reader = new FileReader(path);
            ArffLoader arff = new ArffLoader(reader, 1, -1);
            Instance inst = arff.readInstance();
            while (inst != null) {
                x.add(inst);
                inst = arff.readInstance();
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return x;
    }

    public static List<List<Double>> fileToMatrix(String path) {
        List<List<Double>> x = new ArrayList();
        try {
            FileReader reader = new FileReader(path);
            ArffLoader arff = new ArffLoader(reader, 1, -1);
            Instance inst = arff.readInstance();
            while (inst != null) {
                double[] dArray = inst.toDoubleArray();
                List<Double> list = new ArrayList();
                for (double d : dArray) {
                    list.add(d);
                }
                x.add(list);
                inst = arff.readInstance();
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return x;
    }

    public static void main(String[] args) throws Exception {
        List<Instance> x = Cramer.fileToInstances("c:\\Users\\Paulo\\Documents\\test1-x.arff");
        List<Instance> y = Cramer.fileToInstances("c:\\Users\\Paulo\\Documents\\test1-y.arff");

        Cramer c = new Cramer();
        Cramer.CramerTest ct = c.cramerTest(x, y);
        System.out.println("p Value [Resultado esperado: 0.7092907] [Resultado obtido..: " + ct.pValue + "]");
        System.out.println("Critical value [Resultado esperado: 2.379552] [Resultado obtido: " + ct.critValue + "]");
        System.out.println("Statistic [Resultado esperado: 0.8160198] [Resultado obtido: " + ct.statistic + "]");
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    public double test(List<Instance> x, List<Instance> y) {
        return this.cramerTest(x, y).confLevel;
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public Double call() throws Exception {
        return this.test(sample1i, sample2i);
    }

    @Override
    public void set(List<Instance> x, List<Instance> y) {
        this.sample1i = x;
        this.sample2i = y;
    }
}
