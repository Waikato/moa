package driftmodelintegration.core;


/**
 * <p>
 * An output extension to the model interface which returns
 * a a description of the data. E.g. statistics, frequent items, etcpp.
 * </p>
 * 
 * <p>
 * For a selective descriptive model see {@link SelectiveDescriptionModel}.
 * </p>
 * 
 * <p>
 * The difference between selective and non-selective description
 * models is, that you can specify a parameter at request time for
 * selective description.
 * </p>
 * 
 * @author Marcin Skirzynski
 *
 */
public interface DescriptionModel<R> extends Model {

	/**
	 * <p>
	 * This method returns a description of the data.
	 * </p>
	 * 
	 * @return	a description of the data
	 */
	R describe();

}