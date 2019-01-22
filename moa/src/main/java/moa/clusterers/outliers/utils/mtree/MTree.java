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

import moa.clusterers.outliers.utils.mtree.SplitFunction.SplitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;



/**
 * The main class that implements the M-Tree.
 *
 * @param <DATA> The type of data that will be indexed by the M-Tree. Objects of
 *        this type are stored in HashMaps and HashSets, so their
 *        {@code hashCode()} and {@code equals()} methods must be consistent.
 */
public class MTree<DATA> {

	/**
	 * The type of the results for nearest-neighbor queries.
	 */
	public class ResultItem {
		private ResultItem(DATA data, double distance) {
			this.data = data;
			this.distance = distance;
		}

		/** A nearest-neighbor. */
		public DATA data;

		/** 
		 * The distance from the nearest-neighbor to the query data object
		 * parameter.
		 */
		public double distance;
	}
	
	
	// Exception classes
	private static class SplitNodeReplacement extends Exception {
		// A subclass of Throwable cannot be generic.  :-(
		// So, we have newNodes declared as Object[] instead of Node[].
		private Object newNodes[];
		
		private SplitNodeReplacement(Object... newNodes) {
			this.newNodes = newNodes;
		}
	}
	
	private static class RootNodeReplacement extends Exception {
		// A subclass of Throwable cannot be generic.  :-(
		// So, we have newRoot declared as Object instead of Node.
		private Object newRoot;
		
		private RootNodeReplacement(Object newRoot) {
			this.newRoot = newRoot;
		}
	}

	
	private static class NodeUnderCapacity extends Exception { }
	

	private static class DataNotFound extends Exception { }

	/**
	 * An {@link Iterable} class which can be iterated to fetch the results of a
	 * nearest-neighbors query.
	 * 
	 * <p>The neighbors are presented in non-decreasing order from the {@code
	 * queryData} argument to the {@link MTree#getNearest(Object, double, int)
	 * getNearest*()}
	 * call.
	 * 
	 * <p>The query on the M-Tree is executed during the iteration, as the
	 * results are fetched. It means that, by the time when the <i>n</i>-th
	 * result is fetched, the next result may still not be known, and the
	 * resources allocated were only the necessary to identify the <i>n</i>
	 * first results.
	 */
	public class Query implements Iterable<ResultItem> {

		private class ResultsIterator implements Iterator<ResultItem> {
			
			private class ItemWithDistances <U> implements Comparable<ItemWithDistances<U>> {
				private U item;
				private double distance;
				private double minDistance;

				public ItemWithDistances(U item, double distance, double minDistance) {
					this.item = item;
					this.distance = distance;
					this.minDistance = minDistance;
				}

				@Override
				public int compareTo(ItemWithDistances<U> that) {
					if(this.minDistance < that.minDistance) {
						return -1;
					} else if(this.minDistance > that.minDistance) {
						return +1;
					} else {
						return 0;
					}
				}
			}
			
			
			private ResultItem nextResultItem = null;
			private boolean finished = false;
			private PriorityQueue<ItemWithDistances<Node>> pendingQueue = new PriorityQueue<ItemWithDistances<Node>>();
			private double nextPendingMinDistance;
			private PriorityQueue<ItemWithDistances<Entry>> nearestQueue = new PriorityQueue<ItemWithDistances<Entry>>();
			private int yieldedCount;
			
			private ResultsIterator() {
				if(MTree.this.root == null) {
					finished = true;
					return;
				}
				
				double distance = MTree.this.distanceFunction.calculate(Query.this.data, MTree.this.root.data);
				double minDistance = Math.max(distance - MTree.this.root.radius, 0.0);
				
				pendingQueue.add(new ItemWithDistances<Node>(MTree.this.root, distance, minDistance));
				nextPendingMinDistance = minDistance;
			}
			
			
			@Override
			public boolean hasNext() {
				if(finished) {
					return false;
				}
				
				if(nextResultItem == null) {
					fetchNext();
				}
				
				if(nextResultItem == null) {
					finished = true;
					return false;
				} else {
					return true;
				}
			}
			
			@Override
			public ResultItem next() {
				if(hasNext()) {
					ResultItem next = nextResultItem;
					nextResultItem = null;
					return next;
				} else {
					throw new NoSuchElementException();
				}
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			
			private void fetchNext() {
				assert !finished;
				
				if(finished  ||  yieldedCount >= Query.this.limit) {
					finished = true;
					return;
				}
				
				while(!pendingQueue.isEmpty()  ||  !nearestQueue.isEmpty()) {
					if(prepareNextNearest()) {
						return;
					}
					
					assert !pendingQueue.isEmpty();
					
					ItemWithDistances<Node> pending = pendingQueue.poll();
					Node node = pending.item;
					
					for(IndexItem child : node.children.values()) {
						if(Math.abs(pending.distance - child.distanceToParent) - child.radius <= Query.this.range) {
							double childDistance = MTree.this.distanceFunction.calculate(Query.this.data, child.data);
							double childMinDistance = Math.max(childDistance - child.radius, 0.0);
							if(childMinDistance <= Query.this.range) {
								if(child instanceof MTree.Entry) {
									@SuppressWarnings("unchecked")
									Entry entry = (Entry)child;
									nearestQueue.add(new ItemWithDistances<Entry>(entry, childDistance, childMinDistance));
								} else {
									@SuppressWarnings("unchecked")
									Node childNode = (Node)child;
									pendingQueue.add(new ItemWithDistances<Node>(childNode, childDistance, childMinDistance));
								}
							}
						}
					}
					
					if(pendingQueue.isEmpty()) {
						nextPendingMinDistance = Double.POSITIVE_INFINITY;
					} else {
						nextPendingMinDistance = pendingQueue.peek().minDistance;
					}
				}

				finished = true;
			}

			
			private boolean prepareNextNearest() {
				if(!nearestQueue.isEmpty()) {
					ItemWithDistances<Entry> nextNearest = nearestQueue.peek();
					if(nextNearest.distance <= nextPendingMinDistance) {
						nearestQueue.poll();
						nextResultItem = new ResultItem(nextNearest.item.data, nextNearest.distance);
						++yieldedCount;
						return true;
					}
				}
				
				return false;
			}
			
		}
		
		
		private Query(DATA data, double range, int limit) {
			this.data = data;
			this.range = range;
			this.limit = limit;
		}
		
		
		@Override
		public Iterator<ResultItem> iterator() {
			return new ResultsIterator();
		}

		
		private DATA data;
		private double range;
		private int limit;
	}


	
	/**
	 * The default minimum capacity of nodes in an M-Tree, when not specified in
	 * the constructor call.
	 */
	public static final int DEFAULT_MIN_NODE_CAPACITY = 50;


	protected int minNodeCapacity;
	protected int maxNodeCapacity;
	protected DistanceFunction<? super DATA> distanceFunction;
	protected SplitFunction<DATA> splitFunction;
	protected Node root;
	
	
	/**
	 * Constructs an M-Tree with the specified distance function.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 */
	public MTree(DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction) {
		this(DEFAULT_MIN_NODE_CAPACITY, distanceFunction, splitFunction);
	}
	
	/**
	 * Constructs an M-Tree with the specified minimum node capacity and
	 * distance function.
	 * @param minNodeCapacity The minimum capacity for the nodes of the tree.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 * @param splitFunction The object used to process the split of nodes if
	 *        they are full when a new child must be added.
	 */
	public MTree(int minNodeCapacity,
			DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction) {
		this(minNodeCapacity, 2 * minNodeCapacity - 1, distanceFunction, splitFunction);
	}
	
	/**
	 * Constructs an M-Tree with the specified minimum and maximum node
	 * capacities and distance function.
	 * @param minNodeCapacity The minimum capacity for the nodes of the tree.
	 * @param maxNodeCapacity The maximum capacity for the nodes of the tree.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 * @param splitFunction The object used to process the split of nodes if
	 *        they are full when a new child must be added.
	 */
	public MTree(int minNodeCapacity, int maxNodeCapacity,
			DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction)
	{
		if(minNodeCapacity < 2  ||  maxNodeCapacity <= minNodeCapacity  ||
		   distanceFunction == null) {
			throw new IllegalArgumentException();
		}
		
		if(splitFunction == null) {
			splitFunction = new ComposedSplitFunction<DATA>(
					new PromotionFunctions.RandomPromotion<DATA>(),
					new PartitionFunctions.BalancedPartition<DATA>()
				);
		}
		
		this.minNodeCapacity = minNodeCapacity;
		this.maxNodeCapacity = maxNodeCapacity;
		this.distanceFunction = distanceFunction;
		this.splitFunction = splitFunction;
		this.root = null;
	}
	
	
	/**
	 * Adds and indexes a data object.
	 * 
	 * <p>An object that is already indexed should not be added. There is no
	 * validation regarding this, and the behavior is undefined if done.
	 * 
	 * @param data The data object to index.
	 */
	public void add(DATA data) {
		if(root == null) {
			root = new RootLeafNode(data);
			try {
				root.addData(data, 0);
			} catch (SplitNodeReplacement e) {
				throw new RuntimeException("Should never happen!");
			}
		} else {
			double distance = distanceFunction.calculate(data, root.data);
			try {
				root.addData(data, distance);
			} catch(SplitNodeReplacement e) {
				Node newRoot = new RootNode(data);
				root = newRoot;
				for(int i = 0; i < e.newNodes.length; i++) {
					@SuppressWarnings("unchecked")
					Node newNode = (Node) e.newNodes[i];
					distance = distanceFunction.calculate(root.data, newNode.data);
					root.addChild(newNode, distance);
				}
			}
		}
	}


	/**
	 * Removes a data object from the M-Tree.
	 * @param data The data object to be removed.
	 * @return {@code true} if and only if the object was found.
	 */
	public boolean remove(DATA data) {
		if(root == null) {
			return false;
		}
		
		double distanceToRoot = distanceFunction.calculate(data, root.data);
		try {
			root.removeData(data, distanceToRoot);
		} catch(RootNodeReplacement e) {
			@SuppressWarnings("unchecked")
			Node newRoot = (Node) e.newRoot;
			root = newRoot;
		} catch(DataNotFound e) {
			return false;
		} catch (NodeUnderCapacity e) {
			throw new RuntimeException("Should have never happened", e);
		}
		return true;
	}

	/**
	 * Performs a nearest-neighbors query on the M-Tree, constrained by distance.
	 * @param queryData The query data object.
	 * @param range     The maximum distance from {@code queryData} to fetched
	 *                  neighbors.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearestByRange(DATA queryData, double range) {
		return getNearest(queryData, range, Integer.MAX_VALUE);
	}
	
	
	/**
	 * Performs a nearest-neighbors query on the M-Tree, constrained by the
	 * number of neighbors.
	 * @param queryData The query data object.
	 * @param limit     The maximum number of neighbors to fetch.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearestByLimit(DATA queryData, int limit) {
		return getNearest(queryData, Double.POSITIVE_INFINITY, limit);
	}

	/**
	 * Performs a nearest-neighbor query on the M-Tree, constrained by distance
	 * and/or the number of neighbors.
	 * @param queryData The query data object.
	 * @param range     The maximum distance from {@code queryData} to fetched
	 *                  neighbors.
	 * @param limit     The maximum number of neighbors to fetch.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearest(DATA queryData, double range, int limit) {
		return new Query(queryData, range, limit);
	}

	/**
	 * Performs a nearest-neighbor query on the M-Tree, without constraints.
	 * @param queryData The query data object.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearest(DATA queryData) {
		return new Query(queryData, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
	}
	
	
	protected void _check() {
		if(root != null) {
			root._check();
		}
	}
	

	private class IndexItem {
		DATA data;
		protected double radius;
		double distanceToParent;

		private IndexItem(DATA data) {
			this.data = data;
			this.radius = 0;
			this.distanceToParent = -1;
		}

		int _check() {
			_checkRadius();
			_checkDistanceToParent();
			return 1;
		}

		private void _checkRadius() {
			assert radius >= 0;
		}

		protected void _checkDistanceToParent() {
			assert !(this instanceof MTree.RootLeafNode);
			assert !(this instanceof MTree.RootNode);
			assert distanceToParent >= 0;
		}
	}

	
	
	private abstract class Node extends IndexItem {

		protected Map<DATA, IndexItem> children = new HashMap<DATA, IndexItem>();
		protected Rootness       rootness;
		protected Leafness<DATA> leafness;
		
		private
		<R extends NodeTrait & Rootness, L extends NodeTrait & Leafness<DATA>>
		Node(DATA data, R rootness, L leafness) {
			super(data);
			
			rootness.thisNode = this;
			this.rootness = rootness;
			
			leafness.thisNode = this;
			this.leafness = leafness;
		}

		private final void addData(DATA data, double distance) throws SplitNodeReplacement {
			doAddData(data, distance);
			checkMaxCapacity();
		}

		int _check() {
			super._check();
			_checkMinCapacity();
			_checkMaxCapacity();

			int childHeight = -1;
			for(Map.Entry<DATA, IndexItem> e : children.entrySet()) {
				DATA data = e.getKey();
				IndexItem child = e.getValue();
				assert child.data.equals(data);

				_checkChildClass(child);
				_checkChildMetrics(child);

				int height = child._check();
				if(childHeight < 0) {
					childHeight = height;
				} else {
					assert childHeight == height;
				}
			}

			return childHeight + 1;
		}

		protected void doAddData(DATA data, double distance) {
			leafness.doAddData(data, distance);
		}

		protected void doRemoveData(DATA data, double distance) throws DataNotFound {
			leafness.doRemoveData(data, distance);
		}

		private final void checkMaxCapacity() throws SplitNodeReplacement {
			if(children.size() > MTree.this.maxNodeCapacity) {
				DistanceFunction<? super DATA> cachedDistanceFunction = DistanceFunctions.cached(MTree.this.distanceFunction);
				SplitResult<DATA> splitResult = MTree.this.splitFunction.process(children.keySet(), cachedDistanceFunction);
				
				Node newNode0 = null;
				Node newNode1 = null;
				for(int i = 0; i < 2; ++i) {
					DATA promotedData   = splitResult.promoted.get(i);
					Set<DATA> partition = splitResult.partitions.get(i);
					
					Node newNode = newSplitNodeReplacement(promotedData);
					for(DATA data : partition) {
						IndexItem child = children.get(data);
						children.remove(data);
						double distance = cachedDistanceFunction.calculate(promotedData, data);
						newNode.addChild(child, distance);
					}

					if(i == 0) {
						newNode0 = newNode;
					} else {
						newNode1 = newNode;
					}
				}
				assert children.isEmpty();

				throw new SplitNodeReplacement(newNode0, newNode1);
			}

		}

		protected Node newSplitNodeReplacement(DATA data) {
			return leafness.newSplitNodeReplacement(data);
		}

		protected void addChild(IndexItem child, double distance) {
			leafness.addChild(child, distance);
		}

		void removeData(DATA data, double distance) throws RootNodeReplacement, NodeUnderCapacity, DataNotFound {
			doRemoveData(data, distance);
			if(children.size() < getMinCapacity()) {
				throw new NodeUnderCapacity();
			}
		}

		protected int getMinCapacity() {
			return rootness.getMinCapacity();
		}

		private void updateMetrics(IndexItem child, double distance) {
			child.distanceToParent = distance;
			updateRadius(child);
		}

		private void updateRadius(IndexItem child) {
                        if (child != null) // ### added by mits
                            this.radius = Math.max(this.radius, child.distanceToParent + child.radius);
		}

		void _checkMinCapacity() {
			rootness._checkMinCapacity();
		}

		private void _checkMaxCapacity() {
			assert children.size() <= MTree.this.maxNodeCapacity;
		}

		private void _checkChildClass(IndexItem child) {
			leafness._checkChildClass(child);
		}

		private void _checkChildMetrics(IndexItem child) {
			double dist = MTree.this.distanceFunction.calculate(child.data, this.data);
			assert child.distanceToParent == dist;

			double sum = child.distanceToParent + child.radius;
			assert sum <= this.radius;
		}
		
		protected void _checkDistanceToParent() {
			rootness._checkDistanceToParent();
		}

		private MTree<DATA> mtree() {
			return MTree.this;
		}
	}
	
	
	
	
	private abstract class NodeTrait {
		protected Node thisNode;
	}
	
	private interface Leafness<DATA> {
		void doAddData(DATA data, double distance);
		void addChild(MTree<DATA>.IndexItem child, double distance);
		void doRemoveData(DATA data, double distance) throws DataNotFound;
		MTree<DATA>.Node newSplitNodeReplacement(DATA data);
		void _checkChildClass(MTree<DATA>.IndexItem child);
	}
	
	private interface Rootness {
		int getMinCapacity();
		void _checkDistanceToParent();
		void _checkMinCapacity();
	}
	
	
	
	private class RootNodeTrait extends NodeTrait implements Rootness {
		
		@Override
		public int getMinCapacity() {
			throw new RuntimeException("Should not be called!");
		}
		
		@Override
		public void _checkDistanceToParent() {
			assert thisNode.distanceToParent == -1;
		}

		@Override
		public void _checkMinCapacity() {
			thisNode._checkMinCapacity();
		}
		
	};


	private class NonRootNodeTrait extends NodeTrait implements Rootness {
		
		@Override
		public int getMinCapacity() {
			return MTree.this.minNodeCapacity;
		}
		
		@Override
		public void _checkMinCapacity() {
			assert thisNode.children.size() >= thisNode.mtree().minNodeCapacity;
		}
		
		@Override
		public void _checkDistanceToParent() {
			assert thisNode.distanceToParent >= 0;
		}
	};
	
	
	private class LeafNodeTrait extends NodeTrait implements Leafness<DATA> {
		
		public void doAddData(DATA data, double distance) {
			Entry entry = thisNode.mtree().new Entry(data);
			assert !thisNode.children.containsKey(data);
			thisNode.children.put(data, entry);
			assert thisNode.children.containsKey(data);
			thisNode.updateMetrics(entry, distance);
		}

		public void addChild(IndexItem child, double distance) {
			assert !thisNode.children.containsKey(child.data);
			thisNode.children.put(child.data, child);
			assert thisNode.children.containsKey(child.data);
			thisNode.updateMetrics(child, distance);
		}
		
		public Node newSplitNodeReplacement(DATA data) {
			return thisNode.mtree().new LeafNode(data);
		}
		

		@Override
		public void doRemoveData(DATA data, double distance) throws DataNotFound {
			if(thisNode.children.remove(data) == null) {
				throw new DataNotFound();
			}
		}

		public void _checkChildClass(IndexItem child) {
			assert child instanceof MTree.Entry;
		}
	}


	class NonLeafNodeTrait extends NodeTrait implements Leafness<DATA> {
		
		public void doAddData(DATA data, double distance) {
			class CandidateChild {
				Node node;
				double distance;
				double metric;
				private CandidateChild(Node node, double distance, double metric) {
					this.node = node;
					this.distance = distance;
					this.metric = metric;
				}
			}
			
			CandidateChild minRadiusIncreaseNeeded = new CandidateChild(null, -1.0, Double.POSITIVE_INFINITY);
			CandidateChild nearestDistance         = new CandidateChild(null, -1.0, Double.POSITIVE_INFINITY);
			
			for(IndexItem item : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node child = (Node)item;
				double childDistance = thisNode.mtree().distanceFunction.calculate(child.data, data);
				if(childDistance > child.radius) {
					double radiusIncrease = childDistance - child.radius;
					if(radiusIncrease < minRadiusIncreaseNeeded.metric) {
						minRadiusIncreaseNeeded = new CandidateChild(child, childDistance, radiusIncrease);
					}
				} else {
					if(childDistance < nearestDistance.metric) {
						nearestDistance = new CandidateChild(child, childDistance, childDistance);
					}
				}
			}
			
			CandidateChild chosen = (nearestDistance.node != null)
			                      ? nearestDistance
			                      : minRadiusIncreaseNeeded;
			
			Node child = chosen.node;
			try {
				child.addData(data, chosen.distance);
				thisNode.updateRadius(child);
			} catch(SplitNodeReplacement e) {
				// Replace current child with new nodes
				IndexItem ii = thisNode.children.remove(child.data);
				assert ii != null;
				
				for(int i = 0; i < e.newNodes.length; ++i) {
					@SuppressWarnings("unchecked")
					Node newChild = (Node) e.newNodes[i];
					distance = thisNode.mtree().distanceFunction.calculate(thisNode.data, newChild.data);
					thisNode.addChild(newChild, distance);
				}
			}
		}
		

		public void addChild(IndexItem newChild_, double distance) {
			@SuppressWarnings("unchecked")
			Node newChild = (Node) newChild_;
			
			class ChildWithDistance {
				Node child;
				double distance;
				private ChildWithDistance(Node child, double distance) {
					this.child = child;
					this.distance = distance;
				}
			}
			
			Deque<ChildWithDistance> newChildren = new ArrayDeque<ChildWithDistance>();
			newChildren.addFirst(new ChildWithDistance(newChild, distance));
			
			while(!newChildren.isEmpty()) {
				ChildWithDistance cwd = newChildren.removeFirst();
				
				newChild = cwd.child;
				distance = cwd.distance;
				if(thisNode.children.containsKey(newChild.data)) {
					@SuppressWarnings("unchecked")
					Node existingChild = (Node) thisNode.children.get(newChild.data);
					assert existingChild.data.equals(newChild.data);
					
					// Transfer the _children_ of the newChild to the existingChild
					for(IndexItem grandchild : newChild.children.values()) {
						existingChild.addChild(grandchild, grandchild.distanceToParent);
					}
					newChild.children.clear();
					
					try {
						existingChild.checkMaxCapacity();
					} catch(SplitNodeReplacement e) {
						IndexItem ii = thisNode.children.remove(existingChild.data);
						assert ii != null;
						
						for(int i = 0; i < e.newNodes.length; ++i) {
							@SuppressWarnings("unchecked")
							Node newNode = (Node) e.newNodes[i];
							distance = thisNode.mtree().distanceFunction.calculate(thisNode.data, newNode.data);
							newChildren.addFirst(new ChildWithDistance(newNode, distance));
						}
					}
				} else {
					thisNode.children.put(newChild.data, newChild);
					thisNode.updateMetrics(newChild, distance);
				}
			}
		}


		public Node newSplitNodeReplacement(DATA data) {
			return new InternalNode(data);
		}


		public void doRemoveData(DATA data, double distance) throws DataNotFound {
			for(IndexItem childItem : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node child = (Node)childItem;
				if(Math.abs(distance - child.distanceToParent) <= child.radius) {
					double distanceToChild = thisNode.mtree().distanceFunction.calculate(data, child.data);
					if(distanceToChild <= child.radius) {
						try {
							child.removeData(data, distanceToChild);
							thisNode.updateRadius(child);
							return;
						} catch(DataNotFound e) {
							// If DataNotFound was thrown, then the data was not found in the child
						} catch(NodeUnderCapacity e) {
							Node expandedChild = balanceChildren(child);
							thisNode.updateRadius(expandedChild);
							return;
						} catch (RootNodeReplacement e) {
							throw new RuntimeException("Should never happen!");
						}
					}
				}
			}
			
			throw new DataNotFound();
		}

		
		private Node balanceChildren(Node theChild) {
			// Tries to find anotherChild which can donate a grand-child to theChild.
			
			Node nearestDonor = null;
			double distanceNearestDonor = Double.POSITIVE_INFINITY;
			
			Node nearestMergeCandidate = null;
			double distanceNearestMergeCandidate = Double.POSITIVE_INFINITY;

			for(IndexItem child : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node anotherChild = (Node)child;
				if(anotherChild == theChild) continue;

				double distance = thisNode.mtree().distanceFunction.calculate(theChild.data, anotherChild.data);
				if(anotherChild.children.size() > anotherChild.getMinCapacity()) {
					if(distance < distanceNearestDonor) {
						distanceNearestDonor = distance;
						nearestDonor = anotherChild;
					}
				} else {
					if(distance < distanceNearestMergeCandidate) {
						distanceNearestMergeCandidate = distance;
						nearestMergeCandidate = anotherChild;
					}
				}
			}

			if(nearestDonor == null) {
				// Merge
				for(IndexItem grandchild : theChild.children.values()) {
                                    //if (thisNode == null) System.out.println("thisNode is null");
                                    //if (thisNode.mtree() == null) System.out.println("thisNode.mtree() is null");
                                    //if (grandchild == null) System.out.println("grandchild is null");
                                    //if (grandchild.data == null) System.out.println("grandchild.data is null");                                
                                    //if (nearestMergeCandidate == null) System.out.println("nearestMergeCandidate is null");                                
                                    //if (nearestMergeCandidate.data == null) System.out.println("nearestMergeCandidate.data is null");

                                    if (nearestMergeCandidate != null) { // ### added by mits
                                        double distance = thisNode.mtree().distanceFunction.calculate(grandchild.data, nearestMergeCandidate.data);
                                        nearestMergeCandidate.addChild(grandchild, distance); 
                                    }                                    
				}

				IndexItem removed = thisNode.children.remove(theChild.data);
				assert removed != null;
				return nearestMergeCandidate;
			} else {
				// Donate
				// Look for the nearest grandchild
				IndexItem nearestGrandchild = null;
				double nearestGrandchildDistance = Double.POSITIVE_INFINITY;
				for(IndexItem grandchild : nearestDonor.children.values()) {
					double distance = thisNode.mtree().distanceFunction.calculate(grandchild.data, theChild.data);
					if(distance < nearestGrandchildDistance) {
						nearestGrandchildDistance = distance;
						nearestGrandchild = grandchild;
					}
				}

				IndexItem ii = nearestDonor.children.remove(nearestGrandchild.data);
				assert ii != null;
				theChild.addChild(nearestGrandchild, nearestGrandchildDistance);
				return theChild;
			}
		}


		public void _checkChildClass(IndexItem child) {
			assert child instanceof MTree.InternalNode
			    || child instanceof MTree.LeafNode;
		}
	}


	private class RootLeafNode extends Node {
		
		private RootLeafNode(DATA data) {
			super(data, new RootNodeTrait(), new LeafNodeTrait());
		}
		
		void removeData(DATA data, double distance) throws RootNodeReplacement, DataNotFound {
			try {
				super.removeData(data, distance);
			} catch (NodeUnderCapacity e) {
				assert children.isEmpty();
				throw new RootNodeReplacement(null);
			}
		}
		
		protected int getMinCapacity() {
			return 1;
		}

		void _checkMinCapacity() {
			assert children.size() >= 1;
		}
	}

	private class RootNode extends Node {

		private RootNode(DATA data) {
			super(data, new RootNodeTrait(), new NonLeafNodeTrait());
		}
		
		void removeData(DATA data, double distance) throws RootNodeReplacement, NodeUnderCapacity, DataNotFound {
			try {
				super.removeData(data, distance);
			} catch(NodeUnderCapacity e) {
				// Promote the only child to root
				@SuppressWarnings("unchecked")
				Node theChild = (Node)(children.values().iterator().next());
				Node newRoot;
				if(theChild instanceof MTree.InternalNode) {
					newRoot = new RootNode(theChild.data);
				} else {
					assert theChild instanceof MTree.LeafNode;
					newRoot = new RootLeafNode(theChild.data);
				}

				for(IndexItem grandchild : theChild.children.values()) {
					distance = MTree.this.distanceFunction.calculate(newRoot.data, grandchild.data);
					newRoot.addChild(grandchild, distance);
				}
				theChild.children.clear();

				throw new RootNodeReplacement(newRoot);
			}
		}
		
		
		@Override
		protected int getMinCapacity() {
			return 2;
		}
		
		@Override
		void _checkMinCapacity() {
			assert children.size() >= 2;
		}
	}


	private class InternalNode extends Node {
		private InternalNode(DATA data) {
			super(data, new NonRootNodeTrait(), new NonLeafNodeTrait());
		}
	};


	private class LeafNode extends Node {

		public LeafNode(DATA data) {
			super(data, new NonRootNodeTrait(), new LeafNodeTrait());
		}
	}


	private class Entry extends IndexItem {
		private Entry(DATA data) {
			super(data);
		}
	}
}
