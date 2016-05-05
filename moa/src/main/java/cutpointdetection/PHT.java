package cutpointdetection;
/**
 *  
 * <p>
 * E. S. Page. Continuous inspection schemes. Biometrika, 41(1/2):100-115, 
 * June 1954. URL http://www.jstor.org/stable/2333009.
 * </p>
 * 
 * @author Paulo Gonçalves (paulomgj at gmail dot com)
 * 		Modified by David Huang 
 *
 */
public class PHT implements CutPointDetector{

    private static final int PHT_MINNUMINST = 30;

    private double detectionThreshold = 10; //10
    private double warningThreshold = 0.8 * detectionThreshold;
    private double magnitudeThreshold = 0.02;

    protected double Mt;
    protected double mt;
    protected long nt;
    protected double mean;
    protected double p;

    public PHT(double detectionThreshold) {
	this.detectionThreshold = detectionThreshold;
	initialize();    	
    }

    private void initialize() {
	this.mt = 0;
	this.Mt = Double.MAX_VALUE;
	this.nt = 1;
	this.mean = 0;
	this.p = 1;
    }

    public boolean setInput(double value) {
/*	if (prediction == false) {
	    p = p + (1.0 - p) / nt;
	} else {
	    p = p - (p) / nt;
	}
*/	
	p = value;
	nt++;			
	this.mean = ((nt-1)*this.mean+p)/nt;
	this.mt += p - this.mean - magnitudeThreshold;
	if(this.mt <= this.Mt) {
	    this.Mt = this.mt;
	}
	double PHt = mt - Mt;
//	System.out.println(PHt);
	if(nt > PHT_MINNUMINST && PHt > detectionThreshold) 
	{
	    this.initialize();
	    return true;
	    //	    return DriftDetectionMethod.DDM_OUTCONTROL_LEVEL;
	} 
/*	else if(nt > PHT_MINNUMINST && PHt > warningThreshold) 
	{
	    //	    return DriftDetectionMethod.DDM_WARNING_LEVEL;
	} 
	else 
	{
	    //return DriftDetectionMethod.DDM_INCONTROL_LEVEL;
	}
*/	return false;
    }

}

