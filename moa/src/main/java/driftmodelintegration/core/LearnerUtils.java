package driftmodelintegration.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author chris
 *
 */
public class LearnerUtils extends DataUtils
{


	public static String detectLabelAttribute( Data item ){
		String label = null;
		for( String key : item.keySet() ){
			if( isLabel( key ) )
				return key;
			else 
				label = key;
		}
		return label;
	}


	public static boolean isLabel( String name ){
		return name.startsWith( Data.ANNOTATION_PREFIX + "label" ) || name.equals( "label" ) || name.startsWith( "class" ) || name.startsWith( "_class" );
	}

	public static boolean isPrediction( String name ){
		return name.startsWith( Data.ANNOTATION_PREFIX + "pred" );
	}


	public static Set<String> getLabelAttributes( Data item ){
		Set<String> set = new LinkedHashSet<String>();
		for( String key : item.keySet() )
			if( isLabel( key ) )
				set.add( key );
		return set;
	}


	public static Set<String> getAttributes( Data item ){
		Set<String> set = new LinkedHashSet<String>();
		for( String key : item.keySet() )
			if( !isLabel( key ) && !isHidden( key ) && !isAnnotation( key ) )
				set.add( key );
		return set;
	}

	public static Set<String> getNumericAttributes( Data item ){
		Set<String> set = new LinkedHashSet<String>();
		for( String key : item.keySet() )
			if( !isLabel( key ) && !isHidden( key ) && isNumerical( key, item ) )
				set.add( key );
		return set;
	}

	public static Map<String,Double> getNumericVector( Data item ){
		Map<String,Double> set = new LinkedHashMap<String,Double>();
		for( String key : item.keySet() )
			if( !isLabel( key ) && !isHidden( key ) && (isNumerical( key, item ) || parseDouble( key, item ) != null ))
				set.put( key, parseDouble( key, item ) );
		return set;
	}

	public static Serializable getLabel( Data item ){
		Set<String> set = getLabelAttributes( item );
		if( set.isEmpty() )
			return null;

		return item.get( set.iterator().next() );
	}

	public static Double parseDouble( String key, Data item ){
		try {
			return new Double( item.get( key ).toString() );
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isNumerical( String key, Data item ){
		return item.containsKey( key ) && item.get( key ).getClass() == Double.class;
	}

	public static boolean isNominal( String key, Data item ){
		return !isNumerical( key, item );
	}


	public static Double getDouble( String key, Data item ){
		if( isNumerical( key, item ))
			return (Double) item.get( key );
		else
			return Double.NaN;
	}


	public static Map<String,Class<?>> getTypes( Data item ){
		Map<String,Class<?>> types = new LinkedHashMap<String,Class<?>>();

		for( String key : item.keySet() ){
			if( isNumerical( key, item ) )
				types.put( key, Double.class );
			else
				types.put( key, String.class );
		}

		return types;
	}


	public static Map<String,Class<?>> getTypes( Collection<Data> items ){
		Map<String,Class<?>> types = new LinkedHashMap<String,Class<?>>();

		for( Data item : items ){
			for( String key : item.keySet() ){
				if( isNumerical( key, item ) )
					types.put( key, Double.class );
				else
					types.put( key, String.class );
			}
		}
		return types;
	}


	public static String getMaximumKey( Map<String,Double> input ){
		String bestFeature  = null;
		Double bestValue = null;
		for (String feature : input.keySet()) {
			if ( bestValue == null || bestValue.compareTo( input.get( feature ) ) < 0 ) {
				bestFeature = feature;
				bestValue = input.get( feature );
			}
		}
		return bestFeature;
	}
}