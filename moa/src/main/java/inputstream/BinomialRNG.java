/*
 * Original code taken from COLT CERN Binomial.java and Arithmetic.java
 */

package inputstream;

public class BinomialRNG
{
    protected int n;
    protected double p;

    // cache vars for method generateBinomial(...)
    private int    n_last = -1,  n_prev = -1;
    private double par,np,p0,q,p_last = -1.0, p_prev = -1.0;
    private int    b,m,nm;
    private double pq, rc, ss, xm, xl, xr, ll, lr, c, p1, p2, p3, p4, ch;

    private Arithmetic Arithmetic = new Arithmetic();

    public BinomialRNG(int n, double p)
    {
	this.n = n;
	this.p = p;
    }
    
    public int generateBinomial(int n, double p) {
	final double C1_3 = 0.33333333333333333;
	final double C5_8 = 0.62500000000000000;
	final double C1_6 = 0.16666666666666667;
	final int DMAX_KM = 20;

	int     bh,i, K, Km, nK;
	double  f, rm, U, V, X, T, E;

	if (n != n_last || p != p_last) {                 // set-up 
	    n_last = n;
	    p_last = p;
	    par=Math.min(p,1.0-p);
	    q=1.0-par;
	    np = n*par;

	    // Check for invalid input values

	    if( np <= 0.0 ) return -1;

	    rm = np + par;
	    m  = (int) rm;                      		  // mode, integer 
	    if (np<10) {
		p0=Math.exp(n*Math.log(q));               // Chop-down
		bh=(int)(np+10.0*Math.sqrt(np*q));
		b=Math.min(n,bh);
	    }
	    else {
		rc = (n + 1.0) * (pq = par / q);          // recurr. relat.
		ss = np * q;                              // variance  
		i  = (int) (2.195*Math.sqrt(ss) - 4.6*q); // i = p1 - 0.5
		xm = m + 0.5;
		xl = (double) (m - i);                    // limit left
		xr = (double) (m + i + 1L);               // limit right
		f  = (rm - xl) / (rm - xl*par);  ll = f * (1.0 + 0.5*f);
		f  = (xr - rm) / (xr * q);     lr = f * (1.0 + 0.5*f);
		c  = 0.134 + 20.5/(15.3 + (double) m);    // parallelogram
		// height
		p1 = i + 0.5;
		p2 = p1 * (1.0 + c + c);                  // probabilities
		p3 = p2 + c/ll;                           // of regions 1-4
		p4 = p3 + c/lr;
	    }
	}

	if (np<10) {                                      //Inversion Chop-down
	    double pk;

	    K=0;
	    pk=p0;
	    U=Math.random();
	    while (U>pk) {
		++K;
		if (K>b) {
		    U=Math.random();
		    K=0;
		    pk=p0;
		}
		else {
		    U-=pk;
		    pk=(double)(((n-K+1)*par*pk)/(K*q));
		}
	    }
	    return ((p>0.5) ? (n-K):K);
	}

	for (;;) {
	    V = Math.random();
	    if ((U = Math.random() * p4) <= p1) {    // triangular region
		K=(int) (xm - U + p1*V);
		return (p>0.5) ? (n-K):K;  // immediate accept
	    }
	    if (U <= p2) {                               	 // parallelogram
		X = xl + (U - p1)/c;
		if ((V = V*c + 1.0 - Math.abs(xm - X)/p1) >= 1.0)  continue;
		K = (int) X;
	    }
	    else if (U <= p3) {                           	 // left tail
		if ((X = xl + Math.log(V)/ll) < 0.0)  continue;
		K = (int) X;
		V *= (U - p2) * ll;
	    }
	    else {                                        	 // right tail
		if ((K = (int) (xr - Math.log(V)/lr)) > n)  continue;
		V *= (U - p3) * lr;
	    }

	    // acceptance test :  two cases, depending on |K - m|
	    if ((Km = Math.abs(K - m)) <= DMAX_KM || Km + Km + 2L >= ss) {

		// computation of p(K) via recurrence relationship from the mode
		f = 1.0;                              // f(m)
		if (m < K) {
		    for (i = m; i < K; ) {
			if ((f *= (rc / ++i - pq)) < V)  break;  // multiply  f
		    }
		}
		else {
		    for (i = K; i < m; ) {
			if ((V *= (rc / ++i - pq)) > f)  break;  // multiply  V
		    }
		}
		if (V <= f)  break;                       		 // acceptance test
	    }
	    else {

		// lower and upper squeeze tests, based on lower bounds for log p(K)
		V = Math.log(V);
		T = - Km * Km / (ss + ss);
		E =  (Km / ss) * ((Km * (Km * C1_3 + C5_8) + C1_6) / ss + 0.5);
		if (V <= T - E)  break;
		if (V <= T + E) {
		    if (n != n_prev || par != p_prev) {
			n_prev = n;
			p_prev = par;

			nm = n - m + 1;
			ch = xm * Math.log((m + 1.0)/(pq * nm)) +
				Arithmetic.stirlingCorrection(m + 1) + Arithmetic.stirlingCorrection(nm);
		    }
		    nK = n - K + 1;

		    // computation of log f(K) via Stirling's formula
		    // final acceptance-rejection test
		    if (V <= ch + (n + 1.0)*Math.log((double) nm / (double) nK) +
			    (K + 0.5)*Math.log(nK * pq / (K + 1.0)) -
			    Arithmetic.stirlingCorrection(K + 1) - Arithmetic.stirlingCorrection(nK))  break;
		}
	    }
	}
	return (p>0.5) ? (n-K):K;
    }

    /**
     * Returns a random number from the distribution.
     */
    public int nextBinomial() {
    	return generateBinomial(n,p);
    }
    
    /**
     * Returns a random number from the distribution with the given parameters n and p; bypasses the internal state.
     * @param n the number of trials
     * @param p the probability of success.
     * @throws IllegalArgumentException if <tt>n*Math.min(p,1-p) &lt;= 0.0</tt>
     */
    public int nextBinomial(int n, double p) {
    	if (n*Math.min(p,1-p) <= 0.0) throw new IllegalArgumentException();
    	return generateBinomial(n,p);
    }
    
    public class Arithmetic
    {	
	private final double[] stirlingCorrection =  {   
		0.0,
		8.106146679532726e-02, 4.134069595540929e-02,
		2.767792568499834e-02, 2.079067210376509e-02,
		1.664469118982119e-02, 1.387612882307075e-02,
		1.189670994589177e-02, 1.041126526197209e-02,
		9.255462182712733e-03, 8.330563433362871e-03,
		7.573675487951841e-03, 6.942840107209530e-03,
		6.408994188004207e-03, 5.951370112758848e-03,
		5.554733551962801e-03, 5.207655919609640e-03,
		4.901395948434738e-03, 4.629153749334029e-03,
		4.385560249232324e-03, 4.166319691996922e-03,
		3.967954218640860e-03, 3.787618068444430e-03,
		3.622960224683090e-03, 3.472021382978770e-03,
		3.333155636728090e-03, 3.204970228055040e-03,
		3.086278682608780e-03, 2.976063983550410e-03,
		2.873449362352470e-03, 2.777674929752690e-03,
	};

	public double stirlingCorrection(int k) {
	    final double C1 =  8.33333333333333333e-02;     //  +1/12 
	    final double C3 = -2.77777777777777778e-03;     //  -1/360
	    final double C5 =  7.93650793650793651e-04;     //  +1/1260
	    final double C7 = -5.95238095238095238e-04;     //  -1/1680

	    double r, rr;

	    if (k > 30) {
		r = 1.0 / (double) k;
		rr = r * r;
		return r*(C1 + rr*(C3 + rr*(C5 + rr*C7)));
	    }
	    else return stirlingCorrection[k];
	}
    }

}
