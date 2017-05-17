package driftmodelintegration.core;

import java.io.Serializable;

/**
 * This interface defines the learning step of a machine learning 
 * algorithm. All learners must implement this class.
 * 
 * @author beckers, homburg, mueller, schulte 
 *
 */
public interface Learner<T, M extends Model> extends Serializable {

	/**
	 * This method is called after the learner has been created and
	 * all parameters have been set.
	 */
	public void init();


	/**
	 * Starts or continues to train a model.
	 *
	 * @param item The input for the learning process
	 */
	public void learn(T item);

	/**
	 * Returns the result of the training process.
	 *
         * @see Model
	 * @return Returns the current {@link Model} of the algorithm
	 */
	public M getModel();
}