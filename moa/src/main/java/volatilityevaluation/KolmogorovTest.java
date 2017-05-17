package volatilityevaluation;

public class KolmogorovTest
{

    public double test(double[] a, double[] b)
    {
	int na = a.length;
	int nb = b.length;
	double rna = na;
	double rnb = nb;
	double sa = 1.0 / rna;
	double sb = 1.0 / rnb;
	double rdiff = 0;
	double rdmax = 0;
	int ia = 0;
	int ib = 0;

	double prob = 0.0;

	boolean ok = false;

	for (int i = 0; i < na + nb; i++)
	{
	    if (a[ia] < b[ib])
	    {
		rdiff -= sa;
		ia++;
		if (ia >= na)
		{
		    ok = true;
		    break;
		}
	    } else if (a[ia] > b[ib])
	    {
		rdiff += sb;
		ib++;
		if (ib >= nb)
		{
		    ok = true;
		    break;
		}
	    } else
	    {
		double x = a[ia];
		while (ia < na && a[ia] == x)
		{
		    rdiff -= sa;
		    ia++;
		}
		while (ib < nb && b[ib] == x)
		{
		    rdiff += sb;
		    ib++;
		}
		if (ia >= na)
		{
		    ok = true;
		    break;
		}
		if (ib >= nb)
		{
		    ok = true;
		    break;
		}
	    }
	    rdmax = Math.max(rdmax, Math.abs(rdiff));
	}

	if (ok)
	{
	    rdmax = Math.max(rdmax, Math.abs(rdiff));
	    double z = rdmax * Math.sqrt(rna * rnb / (rna + rnb));
	    prob = KolmogorovProb(z);
	}

	return prob;
    }

    public double KolmogorovProb(double z)
    {
	double[] fj = { -2, -8, -18, -32 };
	double[] r = new double[4];

	final double w = 2.50662827;
	final double c1 = -1.2337005501361697;
	final double c2 = -11.103304921225528;
	final double c3 = -30.842513753404244;

	double u = Math.abs(z);
	double p;
	if (u < 0.2)
	{
	    p = 1;
	} else if (u < 0.755)
	{
	    double v = 1.0 / (u * u);
	    p = 1 - w
		    * (Math.exp(c1 * v) + Math.exp(c2 * v) + Math.exp(c3 * v))
		    / u;
	} else if (u < 6.8116)
	{
	    r[1] = 0;
	    r[2] = 0;
	    r[3] = 0;
	    double v = u * u;
	    int maxj = Math.max(1, Math.round((float) (3.0 / u)));
	    for (int j = 0; j < maxj; j++)
	    {
		r[j] = Math.exp(fj[j] * v);
	    }
	    p = 2 * (r[0] - r[1] + r[2] - r[3]);
	} else
	{
	    p = 0;
	}

	return p;

    }

}
