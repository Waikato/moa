/*
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

package moa.clusterers.outliers.utils.mtree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Some pre-defined implementations of {@linkplain DistanceFunction distance
 * functions}.
 */
public final class DistanceFunctions {
	
    /**
     * Don't let anyone instantiate this class.
     */
	private DistanceFunctions() {}
	
	
	/**
	 * Creates a cached version of a {@linkplain DistanceFunction distance
	 * function}. This method is used internally by {@link MTree} to create
	 * a cached distance function to pass to the {@linkplain SplitFunction split
	 * function}.
	 * @param distanceFunction The distance function to create a cached version
	 *        of.
	 * @return The cached distance function.
	 */
	public static <Data> DistanceFunction<Data> cached(final DistanceFunction<Data> distanceFunction) {
		return new DistanceFunction<Data>() {
			class Pair {
				Data data1;
				Data data2;
				
				public Pair(Data data1, Data data2) {
					this.data1 = data1;
					this.data2 = data2;
				}

				@Override
				public int hashCode() {
					return data1.hashCode() ^ data2.hashCode();
				}
				
				@Override
				public boolean equals(Object arg0) {
					if(arg0 instanceof Pair) {
						Pair that = (Pair) arg0;
						return this.data1.equals(that.data1)
						    && this.data2.equals(that.data2);
					} else {
						return false;
					}
				}
			}
			
			private final Map<Pair, Double> cache = new HashMap<Pair, Double>();
			
			@Override
			public double calculate(Data data1, Data data2) {
				Pair pair1 = new Pair(data1, data2);
				Double distance = cache.get(pair1);
				if(distance != null) {
					return distance;
				}
				
				Pair pair2 = new Pair(data2, data1);
				distance = cache.get(pair2);
				if(distance != null) {
					return distance;
				}
				
				distance = distanceFunction.calculate(data1, data2);
				cache.put(pair1, distance);
				cache.put(pair2, distance);
				return distance;
			}
		};
	}
	
	
	
	/**
	 * An interface to represent coordinates in Euclidean spaces.
	 * @see <a href="http://en.wikipedia.org/wiki/Euclidean_space">"Euclidean
	 *      Space" article at Wikipedia</a>
	 */
	public interface EuclideanCoordinate {
		/**
		 * The number of dimensions.
		 */
		int dimensions();
		
		/**
		 * A method to access the {@code index}-th component of the coordinate.
		 * 
		 * @param index The index of the component. Must be less than {@link
		 *              #dimensions()}. 
		 */
		double get(int index);
	}
	
	
	/**
	 * Calculates the distance between two {@linkplain EuclideanCoordinate 
	 * euclidean coordinates}.
	 */
	public static double euclidean(EuclideanCoordinate coord1, EuclideanCoordinate coord2) {
		int size = Math.min(coord1.dimensions(), coord2.dimensions());
		double distance = 0;
		for(int i = 0; i < size; i++) {
			double diff = coord1.get(i) - coord2.get(i);
			distance += diff * diff;
		}
		distance = Math.sqrt(distance);
		return distance;
	}


	/**
	 * A {@linkplain DistanceFunction distance function} object that calculates
	 * the distance between two {@linkplain EuclideanCoordinate euclidean
	 * coordinates}.
	 */
	public static final DistanceFunction<EuclideanCoordinate> EUCLIDEAN = new DistanceFunction<DistanceFunctions.EuclideanCoordinate>() {
		@Override
		public double calculate(EuclideanCoordinate coord1, EuclideanCoordinate coord2) {
			return DistanceFunctions.euclidean(coord1, coord2);
		}
	};
	
	
	/**
	 * A {@linkplain DistanceFunction distance function} object that calculates
	 * the distance between two coordinates represented by {@linkplain 
	 * java.util.List lists} of {@link java.lang.Integer}s.
	 */
	public static final DistanceFunction<List<Integer>> EUCLIDEAN_INTEGER_LIST = new DistanceFunction<List<Integer>>() {
		@Override
		public double calculate(List<Integer> data1, List<Integer> data2) {
			class IntegerListEuclideanCoordinate implements EuclideanCoordinate {
				List<Integer> list;
				public IntegerListEuclideanCoordinate(List<Integer> list) { this.list = list; }
				@Override public int dimensions() { return list.size(); }
				@Override public double get(int index) { return list.get(index); }
			};
			IntegerListEuclideanCoordinate coord1 = new IntegerListEuclideanCoordinate(data1);
			IntegerListEuclideanCoordinate coord2 = new IntegerListEuclideanCoordinate(data2);
			return DistanceFunctions.euclidean(coord1, coord2);
		}
	};
	
	/**
	 * A {@linkplain DistanceFunction distance function} object that calculates
	 * the distance between two coordinates represented by {@linkplain 
	 * java.util.List lists} of {@link java.lang.Double}s.
	 */
	public static final DistanceFunction<List<Double>> EUCLIDEAN_DOUBLE_LIST = new DistanceFunction<List<Double>>() {
		@Override
		public double calculate(List<Double> data1, List<Double> data2) {
			class DoubleListEuclideanCoordinate implements EuclideanCoordinate {
				List<Double> list;
				public DoubleListEuclideanCoordinate(List<Double> list) { this.list = list; }
				@Override public int dimensions() { return list.size(); }
				@Override public double get(int index) { return list.get(index); }
			};
			DoubleListEuclideanCoordinate coord1 = new DoubleListEuclideanCoordinate(data1);
			DoubleListEuclideanCoordinate coord2 = new DoubleListEuclideanCoordinate(data2);
			return DistanceFunctions.euclidean(coord1, coord2);
		}
	};
}
