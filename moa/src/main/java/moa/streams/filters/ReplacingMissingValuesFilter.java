/*
 *    ReplacingMissingValuesFilter.java
 *    Copyright (C) 2014 Manuel Martin Salvador
 *    @author Manuel Martin Salvador (draxus@gmail.com)
 *
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
package moa.streams.filters;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import moa.core.InstanceExample;



/**
 * Replaces the missing values with another value according to the selected
 * strategy. Available strategies for numerical attributes are: 
 * 1. Nothing: Does nothing (doesn't replace missing values) 
 * 2. LastKnownValue: Replaces with the last non missing value
 * 3. Mean: Replaces with mean of the processed instances so far
 * 4. Max: Replaces with maximum of the processed instances so far
 * 5. Min: Replaces with minimum of the processed instances so far
 * 6. Constant: Replaces with a constant value (default: zero)
 * 
 * Available strategies for nominal attributes are:
 * 1. Nothing: Does nothing (doesn't replace missing values) 
 * 2. LastKnownValue: Replaces with the last non missing value
 * 3. Mode: Replaces with the mode of the processed instances so far (most frequent value)
 * 
 * Beware of numerical strategies 2 to 5: if no previous non-missing values were processed,
 * missing values will be replaced by 0.
 *   
 * @author Manuel Martin Salvador <draxus@gmail.com>
 * 
 */
public class ReplacingMissingValuesFilter extends AbstractStreamFilter {


	private static final long serialVersionUID = 1470772215201414815L;

	public MultiChoiceOption numericReplacementStrategyOption = new MultiChoiceOption(
			"numericReplacementStrategy", 's', "Replacement strategy for numeric attributes", 
			new String[]{"Nothing", "LastKnownValue", "Mean", "Max", "Min", "Constant"},
			new String[]{"Does nothing (doesn't replace missing values)",
					     "Replaces with the last non missing value",
					     "Replaces with mean of the processed instances so far",
					     "Replaces with maximum of the processed instances so far",
					     "Replaces with minimum of the processed instances so far",
					     "Replaces with a constant value (default: zero)"}
			, 0);
	
	public MultiChoiceOption nominalReplacementStrategyOption = new MultiChoiceOption(
			"nominalReplacementStrategy", 't', "Replacement strategy for nominal attributes", 
			new String[]{"Nothing", "LastKnownValue", "Mode"},
			new String[]{"Does nothing (doesn't replace missing values)",
					     "Replaces with the last non missing value",
					     "Replaces with the mode of the processed instances so far (most frequent value)"}
			, 0);

	public FloatOption numericalConstantValueOption = new FloatOption("numericalConstantValue", 'c', 
			"Value used to replace missing values during the numerical constant strategy", 0.0);
	
	protected int numAttributes = -1;
	
	protected double columnsStatistics[] = null; 
	
	protected long numberOfSamples[] = null;
	
	protected double lastNominalValues[] = null;
	
	protected HashMap<Double, Integer> frequencies[] = null;
	
	protected int numericalSelectedStrategy = 0;
	protected int nominalSelectedStrategy = 0;
	
	 @Override
    public String getPurposeString() {
        return "Replaces the missing values with another value according to the selected strategy.";
    }
	 
	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

    @Override
    public InstanceExample nextInstance() {
        Instance inst = (Instance) ((Instance) this.inputStream.nextInstance().getData()).copy();
 		
		// Initialization
		if (numAttributes < 0){
			numAttributes = inst.numAttributes();
			columnsStatistics = new double[numAttributes];
			numberOfSamples = new long[numAttributes];
			lastNominalValues = new double[numAttributes];
                        for(int i=0;i <numAttributes;i++){
                            lastNominalValues[i]=-1;
                        }
			frequencies = new HashMap[numAttributes];
			for(int i=0; i< inst.numAttributes(); i++){
				if(inst.attribute(i).isNominal())
					frequencies[i] = new HashMap<Double, Integer>();
			}
			
			numericalSelectedStrategy = this.numericReplacementStrategyOption.getChosenIndex();
			nominalSelectedStrategy = this.nominalReplacementStrategyOption.getChosenIndex();
		}
		
		
		
		for (int i = 0; i < numAttributes; i++) {
			
			// ---- Numerical values ----
			if (inst.attribute(i).isNumeric()) {
				// Handle missing value
				if (inst.isMissing(i)) {
					switch(numericalSelectedStrategy){
					case 0: // NOTHING
						break;
					case 1: // LAST KNOWN VALUE
					case 2: // MEAN
					case 3: // MAX
					case 4: // MIN
						inst.setValue(i, columnsStatistics[i]);
						break;
					case 5: // CONSTANT
						inst.setValue(i, numericalConstantValueOption.getValue());
						break;
					default: continue;
					}
				}
				// Update statistics with non-missing values
				else{
					switch(numericalSelectedStrategy){
					case 1: // LAST KNOWN VALUE
						columnsStatistics[i] = inst.value(i);
						break;
					case 2: // MEAN
						numberOfSamples[i]++;
						columnsStatistics[i] = columnsStatistics[i] + (inst.value(i) - columnsStatistics[i])/numberOfSamples[i];
						break;
					case 3: // MAX
						columnsStatistics[i] = columnsStatistics[i] < inst.value(i) ? inst.value(i) : columnsStatistics[i];
						break;
					case 4: // MIN
						columnsStatistics[i] = columnsStatistics[i] > inst.value(i) ? inst.value(i) : columnsStatistics[i];
						break;
					default: continue;
					}
				}
			}
			// ---- Nominal values ----
			else if(inst.attribute(i).isNominal()){
				// Handle missing value
				if (inst.isMissing(i)) {
					switch(nominalSelectedStrategy){
					case 0: // NOTHING
						break;
					case 1: // LAST KNOWN VALUE
						if(lastNominalValues[i] != -1){ //null){
							inst.setValue(i, lastNominalValues[i]);
						}
						break;
					case 2: // MODE
						if(!frequencies[i].isEmpty()){
							// Sort the map to get the most frequent value
							Map<Double, Integer> sortedMap = MapUtil.sortByValue( frequencies[i] );
							inst.setValue(i, sortedMap.entrySet().iterator().next().getKey());
						}
						break;
					default: continue;
					}
				}
				// Update statistics with non-missing values
				else{
					switch(nominalSelectedStrategy){
					case 1: // LAST KNOWN VALUE
						lastNominalValues[i] = inst.value(i);
						break;
					case 2: // MODE
						Integer previousCounter = frequencies[i].get(inst.value(i));
						if(previousCounter == null) previousCounter = 0;
						frequencies[i].put(inst.value(i), ++previousCounter);
						break;
					default: continue;
					}
				}
			}
		}
		
        return new InstanceExample(inst);
    }

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void restartImpl() {
		numAttributes = -1;
		columnsStatistics = null;
		numberOfSamples = null;
		lastNominalValues = null;
		frequencies = null;
	}
	
	// Solution from http://stackoverflow.com/a/2581754/2022620
	public static class MapUtil {
	    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
	        
	    	List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
	        Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
	            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
	                return -(o1.getValue()).compareTo( o2.getValue() );
	            }
	        });

	        Map<K, V> result = new LinkedHashMap<K, V>();
	        for (Map.Entry<K, V> entry : list) {
	            result.put( entry.getKey(), entry.getValue() );
	        }
	        return result;
	    }
	}

}
