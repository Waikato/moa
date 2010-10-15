package moa.evaluation;


import java.util.ArrayList;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.gui.visualization.DataPoint;
import weka.core.Instance;

/**
 *
 * @author jansen
 *
 */
public class General extends MeasureCollection{
	private static final long serialVersionUID = 1L;
	private int numPoints;
	private int numFClusters;
	private int numDims;
	private double pointInclusionProbThreshold = 0.8;
	private Clustering clustering;
	private ArrayList<DataPoint> points;


	public General() {
		super();
	}


	@Override
	protected String[] getNames() {
		String[] names = {"GPrecision","GRecall","Redundancy","Compactness","Overlap"};
		return names;
	}

	@Override
	public void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points) throws Exception{

		this.points = points;
		this.clustering = clustering;
		numPoints = points.size();
		numFClusters = clustering.size();
		numDims = points.get(0).numAttributes()-1;


		int totalRedundancy = 0;
		int trueCoverage = 0;
		int totalCoverage = 0;

		int numNoise = 0;
		for (int p = 0; p < numPoints; p++) {
			int coverage = 0;
			for (int c = 0; c < numFClusters; c++) {
				//contained in cluster c?
						if(clustering.get(c).getInclusionProbability(points.get(p)) >= pointInclusionProbThreshold){
							coverage++;
						}
			}

			if(points.get(p).classValue()==-1){
				numNoise++;
			}
			else{
				if(coverage>0) trueCoverage++;
			}

			if(coverage>0) totalCoverage++;  //points covered by clustering (incl. noise)
			if(coverage>1) totalRedundancy++; //include noise
		}

		addValue("Redundancy", ((double)totalRedundancy/(double)numPoints));
		addValue("GPrecision", (totalCoverage==0?0:((double)trueCoverage/(double)(totalCoverage))));
		addValue("GRecall", ((double)trueCoverage/(double)(numPoints-numNoise)));
		addValue("Compactness", computeCompactness());
		addValue("Overlap", computeOverlap());
	}

	private double computeOverlap(){
		for (int c = 0; c < numFClusters; c++) {
			if(!(clustering.get(c) instanceof SphereCluster)){
				System.out.println("Compactness only supports Sphere Cluster. Found: "+clustering.get(c).getClass());
				return Double.NaN;
			}
		}

		boolean[] overlap = new boolean[numFClusters];

		for (int c0 = 0; c0 < numFClusters; c0++) {
			if(overlap[c0]) continue;
			SphereCluster s0 = (SphereCluster)clustering.get(c0);
			for (int c1 = c0; c1 < clustering.size(); c1++) {
				if(c1 == c0) continue;
				SphereCluster s1 = (SphereCluster)clustering.get(c1);
				if(s0.overlapRadiusDegree(s1) > 0){
					overlap[c0] = overlap[c1] = true;
				}
			}
		}

		double totalOverlap = 0;
		for (int c0 = 0; c0 < numFClusters; c0++) {
			if(overlap[c0])
				totalOverlap++;
		}

		//        if(totalOverlap/(double)numFClusters > .8) RunVisualizer.pause();
		return totalOverlap/(double)numFClusters;
	}


	private double computeCompactness(){
		if(numFClusters == 0) return 0;
		for (int c = 0; c < numFClusters; c++) {
			if(!(clustering.get(c) instanceof SphereCluster)){
				System.out.println("Compactness only supports Sphere Cluster. Found: "+clustering.get(c).getClass());
				return Double.NaN;
			}
		}

		//TODO weight radius by number of dimensions
		double totalCompactness = 0;
		for (int c = 0; c < numFClusters; c++) {
			ArrayList<Instance> containedPoints = new ArrayList<Instance>();
			for (int p = 0; p < numPoints; p++) {
				//p in c
				if(clustering.get(c).getInclusionProbability(points.get(p)) >= pointInclusionProbThreshold){
					containedPoints.add(points.get(p));
				}
			}
			double compactness = 0;
			if(containedPoints.size()>1){
				//cluster not empty
				SphereCluster minEnclosingCluster = new SphereCluster(containedPoints, numDims);
				double minRadius = minEnclosingCluster.getRadius();
				double cfRadius = ((SphereCluster)clustering.get(c)).getRadius();
				if(Math.abs(minRadius-cfRadius) < 0.1e-10){
					compactness = 1;
				}
				else
					if(minRadius < cfRadius)
						compactness = minRadius/cfRadius;
					else{
						System.out.println("Optimal radius bigger then real one ("+(cfRadius-minRadius)+"), this is really wrong");
						compactness = 1;
					}
			}
			else{
				double cfRadius = ((SphereCluster)clustering.get(c)).getRadius();
				if(cfRadius==0) compactness = 1;
			}

			//weight by weight of cluster???
			totalCompactness+=compactness;
			clustering.get(c).setMeasureValue("Compactness", Double.toString(compactness));
		}
		return (totalCompactness/numFClusters);
	}


}


