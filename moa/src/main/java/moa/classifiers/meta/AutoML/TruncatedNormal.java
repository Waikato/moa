package moa.classifiers.meta.AutoML;

import org.apache.commons.math3.distribution.NormalDistribution;
//import umontreal.iro.lecuyer.probdist.NormalDist;
import java.util.Random;

public class TruncatedNormal {
	
		double mean;
		double sd;
		double lb;
		double ub;
		double cdf_a;
		double Z;

	private NormalDistribution normalDist;

		// https://en.wikipedia.org/wiki/Truncated_normal_distribution
		TruncatedNormal(double mean, double sd, double lb, double ub){
			this.mean = mean;
			this.sd = sd;
			this.lb = lb;
			this.ub = ub;


			normalDist = new NormalDistribution(0, 1); // Standard normal distribution
			this.cdf_a = normalDist.cumulativeProbability((lb - mean) / sd);
			double cdf_b = normalDist.cumulativeProbability((ub - mean) / sd);
//			this.cdf_a = NormalDist.cdf01((lb - mean)/sd);
//			double cdf_b = NormalDist.cdf01((ub - mean)/sd);
			this.Z = cdf_b - cdf_a;
		}
			
		public double sample() {	
			//TODO This is the simple sampling strategy. Faster approaches are available
			Random random = new Random();
			double val = random.nextDouble() * Z + cdf_a;
			return mean + sd * normalDist.inverseCumulativeProbability(val);
//			return mean + sd * NormalDistribution.inverseF01(val);
		}
	


	
	public static void main(String[] args) {
		TruncatedNormal trncnorm = new TruncatedNormal(0,10,-5,5);
		double min=Double.MAX_VALUE;
		double max=-Double.MAX_VALUE;
		double sum=0.0;
		for(int i=0; i<100000; i++) {
			double val = trncnorm.sample();
			sum += val;
			if(val > max) max=val;
			if(val < min) min=val;
			System.out.println(val);
		}
		
		System.out.println("Min: " + min);
		System.out.println("Max: " + max);
		System.out.println("Mean: " + sum / 100000);

	}

}
