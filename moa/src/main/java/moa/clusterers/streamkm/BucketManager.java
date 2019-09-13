package moa.clusterers.streamkm;


/**
 *
 * @author Marcel R. Ackermann, Christiane Lammersen, Marcus Maertens, Christoph Raupach, 
Christian Sohler, Kamil Swierkot

Modified by Richard Hugh Moulton (24 Jul 2017)
 */
public class BucketManager  {

	protected class Bucket {
		int cursize;
		Point[] points;
		Point[] spillover;
		
		public Bucket(int d, int maxsize){
			this.cursize = 0;
			this.points = new Point[maxsize];
			this.spillover = new Point[maxsize];
			for(int i=0; i<maxsize; i++){
				this.points[i] = new Point(d);
				this.spillover[i] = new Point(d);
			}
		}
		
	};

	protected int numberOfBuckets;
	protected int maxBucketsize;
	protected Bucket[] buckets;
	protected MTRandom clustererRandom;
	protected TreeCoreset treeCoreset;
	
	
	/**
	initializes a bucketmanager for n points with bucketsize maxsize and dimension d
	**/
	public BucketManager(int n,int d,int maxsize, MTRandom random){
		this.clustererRandom = random;
		this.numberOfBuckets = (int) Math.ceil(Math.log((double)n/(double)maxsize) / Math.log(2) )+2;
		this.maxBucketsize = maxsize;
		this.buckets = new Bucket[this.numberOfBuckets];
		for(int i=0; i<this.numberOfBuckets; i++){
			this.buckets[i] = new Bucket(d,maxsize);
		}
		this.treeCoreset = new TreeCoreset();
		//printf("Created manager with %d buckets of dimension %d \n",this.numberOfBuckets,d);
	}

	/**
	inserts a single point into the bucketmanager
	**/
	void insertPoint(Point p){
		
		//check if there is enough space in the first bucket
		int cursize = this.buckets[0].cursize;	
		if(cursize >= this.maxBucketsize) {
			//printf("Bucket 0 full \n");
			//start spillover process
			int curbucket  = 0;
			int nextbucket = 1;

			//check if the next bucket is empty
			if(this.buckets[nextbucket].cursize == 0){
				//copy the bucket	
				int i;
				for(i=0; i<this.maxBucketsize; i++){
					this.buckets[nextbucket].points[i] = this.buckets[curbucket].points[i].clone();
					//copyPointWithoutInit: we should not copy coordinates? 
				}
				//bucket is now full
				this.buckets[nextbucket].cursize = this.maxBucketsize;
				//first bucket is now empty
				this.buckets[curbucket].cursize = 0;
				cursize = 0;
			} else {
				//printf("Bucket %d full \n",nextbucket);
				//copy bucket to spillover and continue
				int i;
				for(i=0;i<this.maxBucketsize;i++){
					this.buckets[nextbucket].spillover[i] = this.buckets[curbucket].points[i].clone();
					//copyPointWithoutInit: we should not copy coordinates? 
				}
				this.buckets[0].cursize=0;
				cursize = 0;
				curbucket++;
				nextbucket++;
				/*
				as long as the next bucket is full output the coreset to the spillover of the next bucket
				*/
				while(this.buckets[nextbucket].cursize == this.maxBucketsize){
					//printf("Bucket %d full \n",nextbucket);
					this.treeCoreset.unionTreeCoreset(this.maxBucketsize,this.maxBucketsize,
						this.maxBucketsize,p.dimension, 
						this.buckets[curbucket].points,this.buckets[curbucket].spillover,
						this.buckets[nextbucket].spillover, this.clustererRandom);
					//bucket now empty
					this.buckets[curbucket].cursize = 0;
					curbucket++;
					nextbucket++;
				}
				this.treeCoreset.unionTreeCoreset(this.maxBucketsize,this.maxBucketsize,
						this.maxBucketsize,p.dimension, 
						this.buckets[curbucket].points,this.buckets[curbucket].spillover,
						this.buckets[nextbucket].points, this.clustererRandom);
				this.buckets[curbucket].cursize = 0;
				this.buckets[nextbucket].cursize = this.maxBucketsize;
			}
		}
		//insert point into the first bucket
		this.buckets[0].points[cursize] = p.clone();
		//copyPointWithoutInit: we should not copy coordinates? 
		this.buckets[0].cursize++;
	}

	/**
	It may happen that the manager is not full (since n is not always a power of 2). In this case we extract the coreset
	from the manager by computing a coreset of all nonempty buckets

	Case 1: the last bucket is full
	=> n is a power of 2 and we return the contents of the last bucket

	Case2: the last bucket is not full
	=> we compute a coreset of all nonempty buckets 

	this operation should only be called after the streaming process is finished
	**/
	Point[] getCoresetFromManager(int d){
		Point[] coreset = new Point[d];
		int i = 0;
		//if(this.buckets[this.numberOfBuckets-1].cursize == this.maxBucketsize){
			//coreset = this.buckets[this.numberOfBuckets-1].points;

		//} else {
			//find the first nonempty bucket
			for(i=0; i < this.numberOfBuckets; i++){
				if(this.buckets[i].cursize != 0){
					coreset = this.buckets[i].points;
					break;
				}
			}		
			//as long as there is a nonempty bucket compute a coreset
			int j;
			for(j=i+1; j < this.numberOfBuckets; j++){
				if(this.buckets[j].cursize != 0){
					//output the coreset into the spillover of bucket j
					this.treeCoreset.unionTreeCoreset(this.maxBucketsize,this.maxBucketsize,
						this.maxBucketsize,d, 
						this.buckets[j].points,coreset,
						this.buckets[j].spillover, this.clustererRandom); 
					coreset = this.buckets[j].spillover;			
				}
			}
		//}
		return coreset;
	}
}
