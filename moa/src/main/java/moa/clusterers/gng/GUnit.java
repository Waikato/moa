/*
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package moa.clusterers.gng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class GUnit implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	public double w[];
	private int label;	
	private double errorAccum;
	
	public ArrayList<GEdge> neighbors = new ArrayList<GEdge>();
	
	public GUnit(double w[], int label){
		this.w = w.clone();
		this.label = label;
		this.errorAccum = 0;
	}

	public GUnit(GUnit other){
		this.w = (other.w != null) ? other.w.clone() : null;
		this.label = other.label;
		this.errorAccum = other.errorAccum;
		this.neighbors = new ArrayList<GEdge>();
	}

	@Override
	public GUnit clone(){
		return new GUnit(this);
	}
	
	public void setError(double error){
		this.errorAccum = error;		
	}
	
	public double getError(){
		return this.errorAccum;
	}
	
	public int getLabel(){
		return this.label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.label;
		result = prime * result + Arrays.hashCode(w);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GUnit other = (GUnit) obj;
		if (this.label != other.label)
			return false;
		if (!Arrays.equals(w, other.w))
			return false;
		return true;
	}
	
	public void updateW(double ep, double pattern[]){
		for (int i = 0; i < this.w.length; i++) {
			this.w[i] = this.w[i] + ep * (pattern[i] - this.w[i]);
		}
	}
		
	public static int getNumberOfGPrototypes(ArrayList<GUnit> list){
		return list.size();
	}
	
	public static double distSquared(double w1[], double w2[]){
		double sum = 0;
		for (int i = 0; i < w1.length; i++) {
			sum += Math.pow(w1[i]-w2[i],2);
		}
		return sum;
	}

	public static double dist(double w1[],double w2[]){
		return Math.sqrt(distSquared(w1, w2));
	}
	
	public void addNeighbor(GUnit s2){
		GEdge nueva = new GEdge(this,s2);
		this.neighbors.add(nueva);
		s2.neighbors.add(nueva);
	}
	
	public String toString(){
		return Arrays.toString(this.w)+":"+this.label;
	}
	
	public GEdge searchEdge(GUnit s2){
		for (GEdge c : this.neighbors) {
			if(c.p1.equals(s2)||c.p0.equals(s2)) return c;
		}
		return null;
	}
	
	public void updateAges(){
		for (GEdge c : this.neighbors) {
			c.age++;
		}
	}
	
	public void purgueGEdges(double age){
		purgueGEdgesAndCheck(age);
	}

	public boolean purgueGEdgesAndCheck(double age){
		boolean removedAny = false;
		for (int i = 0;i<this.neighbors.size();i++){
			GEdge c = this.neighbors.get(i);
			if(c.age>=age){
				removedAny = true;
				this.neighbors.remove(i);
				i--;
				if(c.p0.equals(this)){
					c.p1.neighbors.remove(c);
				} else {
					c.p0.neighbors.remove(c);
				}
			}
		}
		return removedAny;
	}
	
	public void removeNeighbor(GUnit p){
		for (int i = 0 ; i<this.neighbors.size();i++) {
			GEdge c = this.neighbors.get(i);
			if(c.p0.equals(p) || c.p1.equals(p)) {
				this.neighbors.remove(i);
				i--;
			}
		}
		for (int i = 0 ; i<p.neighbors.size();i++) {
			GEdge c = p.neighbors.get(i);
			if(c.p0.equals(this) || c.p1.equals(this)) {
				p.neighbors.remove(i);
				i--;
			}
		}
	}
	
	public GUnit[] getNeighborhood(){
		GUnit v[] = new GUnit[this.neighbors.size()];
		for (int i = 0; i < v.length; i++) {
			if(this.neighbors.get(i).p0!=this)	
				v[i] = this.neighbors.get(i).p0;
			else 
				v[i] = this.neighbors.get(i).p1;
			
		}return v;
	}

}
