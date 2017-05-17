package driftmodelintegration.core;

/**
 * <p>
 * An output extension to the model interface which returns
 * a prediction for a given input. E.g. for clustering, regression, 
 * classification etcpp.
 * </p>
 * 
 * @author beckers, homburg, mueller, schulte, skirzynski 
 *
 */
public interface PredictionModel<T, R> extends Model {

	/**
	 * <p>
	 * This method returns a prediction for the given input.
	 * </p>
	 * 
	 * @param item to predict for
	 * @return	a prediction for the given item
	 */
	R predict(T item);	
}