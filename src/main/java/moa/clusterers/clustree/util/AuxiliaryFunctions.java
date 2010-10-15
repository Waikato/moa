package moa.clusterers.clustree.util;

/**
 * Helpful static functions.
 * @author Fernando Sanchez Villaamil
 */
public class AuxiliaryFunctions {

    /**
     * Number of iterations used to calculate the incomplete gamma function,
     * see <code>AuxiliaryFunctions</code>.
     * XXX: A good precision value is said to be 100 iterations, but this
     * results in double overflows.
     */
    public static final int GAMMA_ITERATIONS = 100;


    /**
     * Private constructor to hinder instantiation.
     */
    private AuxiliaryFunctions() {
    }

    /**
     * Adds the second array to the first array element by element. The arrays
     * must have the same length.
     * @param a1 Array to which the second array is added.
     * @param a2 Array to be addded. This array is not changed.
     */
    public static void addIntegerArrays(int[] a1, int[] a2) {
        assert (a1.length == a2.length);

        for (int i = 0; i < a1.length; i++) {
            a1[i] += a2[i];
        }
    }

    /**
     * Overwrites the values of the first array with the values of the second
     * array.  The arrays must have the same length.
     * @param a1 Array to be overwritten.
     * @param a2 Array whose values will be used. This array is not changed.
     */
    public static void overwriteDoubleArray(double[] a1, double[] a2) {
        assert (a1.length == a2.length);

        for (int i = 0; i < a1.length; i++) {
            a1[i] = a2[i];
        }
    }

    /**
     * Overwrites the values of the first array with the values of the second
     * array.  The arrays must have the same length.
     * @param a1 Array to be overwritten.
     * @param a2 Array whose values will be used. This array is not changed.
     */
    public static void overwriteIntegerArray(int[] a1, int[] a2) {
        assert (a1.length == a2.length);

        for (int i = 0; i < a1.length; i++) {
            a1[i] = a2[i];
        }
    }



    /**
     * Function used to weight the entries.
     * @param negLambda Parameter of the weighting function. See paper.
     * @param timeDifference The difference between the current time and the
     * time at which the entry was updated before.
     * @return A double to multiply the weighted N, LS and SS in a Cluster.
     */
    public static double weight(double negLambda, long timeDifference) {
        assert (negLambda < 0);
        assert (timeDifference > 0);
        return Math.pow(2.0, negLambda * timeDifference);
    }

    /**
     * Print a <code>double</code> array.
     * @param a The array to be printed.
     */
    public static void printArray(double[] a) {
        System.out.println(formatArray(a));
    }

    /**
     * Writes a <code>double</code> array into a string. The double values
     * are rounded to 3 decimals for better readability.
     * @param a The array to be formated
     * @return a textual representation of the array
     */
    public static String formatArray(double[] a) {
        if (a.length == 0) {
            return "[]";
        }

        String res = "[";
        for (int i = 0; i < a.length - 1; i++) {
            res += Math.round(a[i] * 1000.0) / 1000.0 + ", ";
        }
        res += Math.round(a[a.length - 1] * 1000.0) / 1000.0 + "]";
        return res;
    }

    /**
     * Print a <code>String</code> array.
     * @param a The array to be printed.
     */
    public static void printArray(String[] a) {
        if (a.length == 0) {
            System.out.println("[]");
            return;
        }

        System.out.print("[");
        for (int i = 0; i < a.length - 1; i++) {
            System.out.print(a[i] + ", ");
        }
        System.out.print(a[a.length - 1]);
        System.out.println("]");
    }

    /**
     * Calculate the incomplete gamma function on x, a numerically.
     * @param x
     * @param a
     * @return gamma(x,a)
     */
    private static double incompleteGamma(double a, double x) {
        assert (!Double.isNaN(a));
        assert (!Double.isNaN(x));
        double sum = 0.0;

        double xPow = 1; // Start with x^0
        for (int n = 0; n < GAMMA_ITERATIONS; n++) {
            double denom = a;
            for (int i = 1; i <= n; i++) {
                denom *= (a + i);
            }

            assert (denom != 0.0);
            sum += xPow / denom;
            xPow *= x;
        }

        double res = Math.pow(x, a) * Math.exp(-x) * sum;

        if (Double.isNaN(res)) {
            System.err.println("a " + a);
            System.err.println("x " + x);
            System.err.println("x^a " + Math.pow(x, a));
            System.err.println("e^-x " + Math.exp(-x));
            System.err.println("sum " + sum);
            assert (false);
        }

        return res;
    }

    /**
     * Calculates gamma(n/2) (special case!) for small n.
     * @param n
     * @return  gamma(n/2)
     */
    public static double gammaHalf(int n) {
        int[] doubleFac = new int[]{1, 1, 2, 3, 8, 15, 48, 105, 384, 945, 3840,
            10395, 46080, 135135, 645120, 2027025,
            10321920, 34459425, 185794560, 654729075};

        if (n == 0) {
            return Double.POSITIVE_INFINITY;
        }

        // Integers are simple fac(n)
        if ((n % 2) == 0) {
            int v = (n / 2) - 1;
            int res = 1;

            for (int i = 1; i <= v; i++) {
                res *= i;
            }

            return res;
        }

        // First two would yeald negative double factorials
        if (n == 1) {
            return 1.0692226492664116 / 0.6032442812094465;
        }
        if (n == 3) {
            return 0.947573901083827 / 1.0692226492664116;
        }

        return Math.sqrt(Math.PI) * (doubleFac[n - 2]) / (Math.pow(2, (n - 1) * .5));
    }

    /**
     * Calcuates the probabilty that a point sampled from a gaussian kernel
     * has a Malahanobis-distance greater than the given threshold.
     *
     * @param threshold a threshold for the distance
     * @param dimension	the number of dimensions of the kernel
     * @return the above probability
     */
    public static double distanceProbabilty(double threshold, int dimension) {
        // threshold = (threshold*threshold) * .5;
        if (threshold == 0) {
            return 1;
        }

        //return 1 - (incompleteGamma(dimension * .5, threshold) / gammaHalf(dimension));
        return 1 - (Gamma.incompleteGamma(dimension * .5, threshold * .5)
                / gammaHalf(dimension));
    }

    public static double gompertzWeight(double average, double count) {
	if ( average < 2.0 )
	    return 1.0;

        double logT = Math.log(0.97);
        double logt = Math.log(0.0001);

        double denomB = (Math.pow(logT * logT, 1 / (average - 2.0)));

        double b = (Math.pow(logt * logt, 1.0 / (2.0 * (1.0 - (2.0 / average))))) / denomB;
        double c = -(1.0 / average) * Math.log(-(1.0 / b) * logT);

        assert (b >= 0) : "Bad b " + b + ", average " + average;
        assert (c >= 0) : "Bad c " + c + ", average " + average;

	// Should be okay, the following test fails for some numerica
	// bad examples
        // assert (Math.exp(-b * Math.exp(-c * average)) > 0.95);
        // assert (Math.exp(-b * Math.exp(-c * 2)) < 0.001);

        return Math.exp(-b * Math.exp(-c * count));
    }

    /**
     * Sorts the given array
     * @param a double arrray
     */
    public static void sortDoubleArray(double[] a) {
        int i, j;
        double value;
        for (i = 1; i < a.length; i++) {
            j = i;
            value = a[j];
            while (j > 0 && a[j - 1] > value) {
                a[j] = a[j - 1];
                j--;
            }
            a[j] = value;
        }

    }

    /**
     * Approximates the inverse error function. Clustream needs this.
     * @param z
     */
    public static double inverseError(double x) {
        double z = Math.sqrt(Math.PI) * x;
        double res = (z) / 2;

        double z2 = z * z;
        double zProd = z * z2; // z^3
        res += (1.0 / 24) * zProd;

        zProd *= z2;  // z^5
        res += (7.0 / 960) * zProd;

        zProd *= z2;  // z^7
        res += (127 * zProd) / 80640;

        zProd *= z2;  // z^9
        res += (4369 * zProd) / 11612160;

        zProd *= z2;  // z^11
        res += (34807 * zProd) / 364953600;

        zProd *= z2;  // z^13
        res += (20036983 * zProd) / 797058662400d;

        /*
        zProd *= z2;  // z^15
        res += (2280356863 * zProd)/334764638208000;
         */

        // +(49020204823 pi^(17/2) x^17)/26015994740736000+(65967241200001 pi^(19/2) x^19)/124564582818643968000+(15773461423793767 pi^(21/2) x^21)/104634249567660933120000+O(x^22)

        return res;
    }
}
