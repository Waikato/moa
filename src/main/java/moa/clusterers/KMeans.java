/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package moa.clusterers;

import java.util.ArrayList;
import java.util.List;
import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;

/**
 *
 * @author jansen
 */
public class KMeans {

    public static Clustering kMeans(Cluster[] centers, List<? extends Cluster> data ) {
        int k = centers.length;

	int dimensions = centers[0].getCenter().length;

	ArrayList<ArrayList<Cluster>> clustering =
		new ArrayList<ArrayList<Cluster>>();
	for ( int i = 0; i < k; i++ ) {
	    clustering.add( new ArrayList<Cluster>() );
	}

	int repetitions = 100;
	while ( repetitions-- >= 0 ) {
	    // Assign points to clusters
	    for ( Cluster point : data ) {
		double minDistance = distance( point.getCenter(), centers[0].getCenter() );
		int closestCluster = 0;
		for ( int i = 1; i < k; i++ ) {
		    double distance = distance( point.getCenter(), centers[i].getCenter() );
		    if ( distance < minDistance ) {
			closestCluster = i;
			minDistance = distance;
		    }
		}

		clustering.get( closestCluster ).add( point );
	    }

	    // Calculate new centers and clear clustering lists
	    SphereCluster[] newCenters = new SphereCluster[centers.length];
	    for ( int i = 0; i < k; i++ ) {
		newCenters[i] = calculateCenter( clustering.get( i ), dimensions );
		clustering.get( i ).clear();
	    }
	    centers = newCenters;
	}

	return new Clustering( centers );
    }

    private static double distance(double[] pointA, double [] pointB){
        double distance = 0.0;
        for (int i = 0; i < pointA.length; i++) {
            double d = pointA[i] - pointB[i];
            distance += d * d;
        }
        return Math.sqrt(distance);
    }


    private static SphereCluster calculateCenter( ArrayList<Cluster> cluster, int dimensions ) {
	double[] res = new double[dimensions];
	for ( int i = 0; i < res.length; i++ ) {
	    res[i] = 0.0;
	}

	if ( cluster.size() == 0 ) {
	    return new SphereCluster( res, 0.0 );
	}

	for ( Cluster point : cluster ) {
            double [] center = point.getCenter();
            for (int i = 0; i < res.length; i++) {
               res[i] += center[i];
            }
	}

	// Normalize
	for ( int i = 0; i < res.length; i++ ) {
	    res[i] /= cluster.size();
	}

	// Calculate radius
	double radius = 0.0;
	for ( Cluster point : cluster ) {
	    double dist = distance( res, point.getCenter() );
	    if ( dist > radius ) {
		radius = dist;
	    }
	}

	return new SphereCluster( res, radius );
    }

    public static Clustering gaussianMeans(Clustering gtClustering, Clustering clustering) {
        ArrayList<CFCluster> microclusters = new ArrayList<CFCluster>();
        for (int i = 0; i < clustering.size(); i++) {
            if (clustering.get(i) instanceof CFCluster) {
                microclusters.add((CFCluster)clustering.get(i));
            }
            else{
                System.out.println("Unsupported Cluster Type:"+clustering.get(i).getClass()
                        +". Cluster needs to extend moa.cluster.CFCluster");
            }
        }
        Cluster[] centers = new Cluster[gtClustering.size()];
        for (int i = 0; i < centers.length; i++) {
            centers[i] = gtClustering.get(i);

        }

        int k = centers.length;
	if ( microclusters.size() < k ) {
	    return new Clustering( new Cluster[0]);
	}

	Clustering kMeansResult = kMeans( centers, microclusters );

	k = kMeansResult.size();
	CFCluster[] res = new CFCluster[ k ];

	for ( CFCluster microcluster : microclusters) {
	    // Find closest kMeans cluster
	    double minDistance = Double.MAX_VALUE;
	    int closestCluster = 0;
	    for ( int i = 0; i < k; i++ ) {
		double distance = distance( kMeansResult.get(i).getCenter(), microcluster.getCenter() );
		if ( distance < minDistance ) {
		    closestCluster = i;
		    minDistance = distance;
		}
	    }

	    // Add to cluster
	    if ( res[closestCluster] == null ) {
		res[closestCluster] = (CFCluster)microcluster.copy();
	    } else {
		res[closestCluster].add( microcluster );
	    }
	}

	// Clean up res
	int count = 0;
	for ( int i = 0; i < res.length; i++ ) {
	    if ( res[i] != null )
		++count;
	}

	CFCluster[] cleaned = new CFCluster[count];
	count = 0;
	for ( int i = 0; i < res.length; i++ ) {
	    if ( res[i] != null )
		cleaned[count++] = res[i];
	}

	return new Clustering( cleaned );
    }

}
