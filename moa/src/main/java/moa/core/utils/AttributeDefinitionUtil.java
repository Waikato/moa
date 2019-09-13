package moa.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AttributeDefinitionUtil {
	//public static String definitionTotal = "(!?-?[0-9]+?)|(!?[0-9]+?-[0-9]+?)|(!?-?[0-9]+?~-?[0-9]+?)";
	
	public static String nonIgnoredDefinition = "0";
	
	public static String definitionSingle = "-?[0-9]+?";
	public static String definitionPositiveRange = "[0-9]+?-[0-9]+?";
	public static String definitionArbitraryRange = "-?[0-9]+?~-?[0-9]+?";
	
	public static String getDefinitionTotal() {
		return "(!?" + definitionSingle + ")|" + "(!?" + definitionPositiveRange + ")|" + "(!?" + definitionArbitraryRange + ")";
	}
	
	public static List<Integer> parseDefinition(String definition, int numAttributes, List<Integer> ignoredAttributes) {
		List<Integer> ret = new ArrayList<Integer>();
		if (Pattern.matches(definitionArbitraryRange, definition)) {
			String[] split = definition.split("~");
			int start = Integer.valueOf(split[0]);
			int end = Integer.valueOf(split[1]);
			if (start < 0) {
				start = numAttributes + start + 1; // + 1 for so that -1 maps to last attribute 
			}
			if (end < 0) {
				end = numAttributes + end + 1; // + 1 for so that -1 maps to last attribute 
			}
			start = Math.max(0, start);
			end = Math.max(0, end);
			if (start == end) {
				System.err.println("[Warning] Strage attribute range definition '" + definition + "', start equals end");
			}
			if (start <= end) {
				Integer i = start;
				while(i <= end) {
					if (!ignoredAttributes.contains(i - 1)) ret.add(i - 1);
					i++;
				}
			}
			else 
				System.err.println("[Warning] Strage attribute range definition '" + definition + "', end smaller than start, ignoring...");
		} else if (Pattern.matches(definitionPositiveRange, definition)) {
			String[] split = definition.split("-");
			int start = Integer.valueOf(split[0]);
			int end = Integer.valueOf(split[1]);
			if (start == 0 || end == 0) {
				throw new UnsupportedOperationException("Attribute definition range error - start or end equals 0: " + definition);
			}
			if (start == end) {
				System.err.println("[Warning] Strage attribute range definition '" + definition + "', start equals end");
			}
			Integer i = start;
			if (start <= end)
				while(i <= end) {
					if (!ignoredAttributes.contains(i - 1)) ret.add(i - 1);
					i++;
				}
			else 
				System.err.println("[Warning] Strage attribute range definition '" + definition + "', end smaller of start, ignoring...");
		} else if (Pattern.matches(definitionSingle, definition)) {
			int index = Integer.valueOf(definition);
			if (index == 0) {
	    		// Add all non-ignored attributes
				for (Integer i = 0; i < numAttributes; i++)
	   				if (!ignoredAttributes.contains(i)) ret.add(i);
			} else {
				if (index < 0){
					index = numAttributes + index + 1;
					if (index <= 0) {
						throw new UnsupportedOperationException("Attribute definition range error - negative index overflows the number of attributes: " + definition);
					}
				}
				if (!ignoredAttributes.contains(index - 1)) ret.add(index - 1);
			}
		}
		return ret;
	}
	
    public static List<Integer> parseAttributeDefinition(String attributeDefinition, int numAttributes, List<Integer> ignoredAttributes) {
    	List<Integer> ret = new ArrayList<Integer>();
    	if (ignoredAttributes == null) ignoredAttributes = new ArrayList<Integer>();
    	if (!attributeDefinition.isEmpty()) {
	    	String[] definitions = attributeDefinition.split("[,;]");
	    	Pattern definitionsPattern = Pattern.compile(getDefinitionTotal());
	    	for (int i = 0; i < definitions.length; i++) {
	    		String definition = definitions[i];
	    		boolean negative = false;
	    		if (definition.isEmpty()) {
	    			continue;
	    		}
	    		if (!definitionsPattern.matcher(definition).matches()) throw new UnsupportedOperationException("The attribute definition '" + definition + "' is not recognised.");
	
	    		if (definition.startsWith("!")) {
	    			definition = definition.substring(1);
	    			negative = true;
	    		}
	
	    		List<Integer> candidates = parseDefinition(definition, numAttributes, ignoredAttributes);
	    		if (negative) {
	    			for (Integer j : candidates) {
	    				if (ret.contains(j)) ret.remove(j);
	    			}
	    		} else {
	    			for (Integer j : candidates) {
	    				if (!ret.contains(j)) ret.add(j);
	    			}
	    		}
	    	}
    	} else {
    		// Add all non-ignored attributes (this is the default behavior for input attributes)
 			for (Integer i = 0; i < numAttributes; i++)
   				if (!ignoredAttributes.contains(i)) ret.add(i);
    	}
    	return ret;
    }
	
    public static List<Integer> remapAttributeDefitinion(List<Integer> attributeList, List<Integer> indexMap) {
    	// Remaps the attribute list to only the included attributes, i.e., skip unincluded attributes
    	
    	// Example:
    	// If input attributes = 1 - 11, output attributes = 14, 14 get remapped to 12

    	List<Integer> ret = new ArrayList<Integer>();
    	for (int i = 0; i < attributeList.size(); i++) {
    		ret.add(indexMap.indexOf(attributeList.get(i)));
    	}
    	return ret;
    		
    }
	
}
