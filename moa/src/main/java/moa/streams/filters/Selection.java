package moa.streams.filters;

import java.util.ArrayList;


public class Selection {
	private ArrayList<Node> selection;
	private int numvalues;
	
	public Selection(){
		selection=new ArrayList<Selection.Node>(1);
	}
	
	public boolean add(int start, int end){
		if (start<=end && start>=0){
			numvalues+=(end-start+1);
			selection.add(new Node(start,end));
			return true;
		}
		else 
			return false;
	}
	
	public boolean add(int value){
		if(value>=0){
			selection.add(new Node(value));
			numvalues++;
			return true;
		}
		return false;	
	}
	
	public int getStart(int numEntry){
		return selection.get(numEntry).getStart();
	}
	
	public int getEnd(int numEntry){
		return selection.get(numEntry).getEnd();
	}
	
	public int numEntries(){
		return selection.size();
	}
	
	public int numValues(){
		return numvalues;
	}
	
	class Node {
		private int start;
		private int end;
		
		public Node(int start, int end){
			this.setStart(start);
			this.setEnd(end);
		}
		
		public Node(int val){
			this(val,val);
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}
	}
}

