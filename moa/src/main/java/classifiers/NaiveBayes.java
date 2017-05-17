package classifiers;

public class NaiveBayes
{
    int classIndex = -1;
    int numAtts = -1;
    double[][] means;
    double[][] variances;
    
    public NaiveBayes(int numAtts, int classIndex)
    {
	this.numAtts = numAtts;
	this.classIndex = classIndex == -1 ? numAtts : classIndex;
	this.means = new double[2][numAtts];
	this.variances = new double[2][numAtts];
    }
    
    public void train(String[] instances) throws Exception
    {
	double[][] trainValues0 = new double[numAtts][instances.length];
	double[][] trainValues1 = new double[numAtts][instances.length];
//	int[] classLabels = new int[instances.length];
	    
	for(int j = 0; j < instances.length; j++)
	{
	    String[] inst = instances[j].split(",");
	    if(inst.length != numAtts+1){throw new Exception();} // number of attributes from instance do not match 

	    for(int i = 0; i < numAtts; i++)
	    {
		if(inst[classIndex].equals("0"))
		{
		    trainValues0[i][j] = Double.parseDouble(inst[i]);
		}
		else
		{
		    trainValues1[i][j] = Double.parseDouble(inst[i]);
		}
		
	    }
	}
	
	for(int k = 0; k < numAtts; k++)
	{
	    this.means[0][k] = sum(trainValues0[k]) / instances.length;
	    this.variances[0][k] = stdev(trainValues0[k], this.means[0][k]);
	    this.means[1][k] = sum(trainValues1[k]) / instances.length;
	    this.variances[1][k] = stdev(trainValues1[k], this.means[1][k]);
	}
    }

    
    public String predict(String instance)
    {
	double class0 = 0.0; 
	double class1 = 0.0;
	
	String[] inst = instance.split(",");
	for(int i = 0; i < inst.length-1; i++)
	{
	    double v = Double.parseDouble(inst[i]);
	    // class 0
	    double z0 = (v - means[0][i]) / variances[0][i];
	    z0 = z0 < 0.0 ? z0 : z0 * -1.0;
	    double p0 = normCDF(z0);
	    class0 += (0.5 - p0) * 2.0;
	    
	    // class 1
	    double z1 = (v - means[1][i]) / variances[1][i];
	    z1 = z1 < 0.0 ? z1 : z1 * -1.0;
	    double p1 = normCDF(z1);
	    class1 += (0.5 - p1) * 2.0;
	    
	}
	return class0 > class1 ? "0" : "1";
    }
    
    public double stdev(double[] values, double mean)
    {
	double sum = 0;
	int count = 0;
	for (Double d : values)
	{
	    if (d > 0)
	    {
		count++;
		sum += Math.pow(d - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double sum(double[] values)
    {
	double sum = 0.0;
	for (double d : values)
	{
	    sum += d;
	}

	return sum;
    }
    
  //Source: http://www1.fpl.fs.fed.us/distributions.html
    public static double normCDF(double z) {

	double zabs;
	double p;
	double expntl,pdf;

	final double p0 = 220.2068679123761;
	final double p1 = 221.2135961699311;
	final double p2 = 112.0792914978709;
	final double p3 = 33.91286607838300;
	final double p4 = 6.373962203531650;
	final double p5 = .7003830644436881;
	final double p6 = .3526249659989109E-01;

	final double q0 = 440.4137358247522;
	final double q1 = 793.8265125199484;
	final double q2 = 637.3336333788311;
	final double q3 = 296.5642487796737;
	final double q4 = 86.78073220294608;
	final double q5 = 16.06417757920695;
	final double q6 = 1.755667163182642;
	final double q7 = .8838834764831844E-1;

	final double cutoff = 7.071;
	final double root2pi = 2.506628274631001;

	zabs = Math.abs(z);

	//  |z| > 37

	if (z > 37.0) {

	    p = 1.0;

	    return p;

	}

	if (z < -37.0) {

	    p = 0.0;

	    return p;

	}

	//  |z| <= 37.

	expntl = Math.exp(-.5*zabs*zabs);

	pdf = expntl/root2pi;

	//  |z| < cutoff = 10/sqrt(2).

	if (zabs < cutoff) {

	    p = expntl*((((((p6*zabs + p5)*zabs + p4)*zabs + p3)*zabs +
		    p2)*zabs + p1)*zabs + p0)/(((((((q7*zabs + q6)*zabs +
			    q5)*zabs + q4)*zabs + q3)*zabs + q2)*zabs + q1)*zabs +
			    q0);

	} else {

	    p = pdf/(zabs + 1.0/(zabs + 2.0/(zabs + 3.0/(zabs + 4.0/
		    (zabs + 0.65)))));

	}

	if (z < 0.0) {

	    return p;

	} else {

	    p = 1.0 - p;

	    return p;

	}

    }
}
