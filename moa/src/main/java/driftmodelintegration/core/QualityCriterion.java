package driftmodelintegration.core;

/**
 * 
 */
import java.io.Serializable;

/**
 * <p>
 * Interface to define a quality criterion to be used to construct the tree. Available:<br/>
 * {@link EntropyCriterion} (information gain) as used by ID3<br/>
 * {@link GiniCriterion} (Gini-Index) as used by CART
 * </p>
 * 
 * @author Tobias Beckers
 */
public interface QualityCriterion extends Serializable{

	public final static QualityCriterion INFO_GAIN = new EntropyCriterion();

	public final static QualityCriterion GINI_INDEX = new GiniCriterion();


	/**
	 * Returns the quality for the given probabilities
	 * @param probabilities probability values must sum up to 1
	 * @return the quality for the given probabilities
	 */
	public double getQuality(double ... probabilities);

	/**
	 * Returns the lower bound of gain for this quality criterion
	 * @return the lower bound of gain for this quality criterion
	 */
	public double getLowestGain();

	/**
	 * Returns the upper bound of gain for this quality criterion depending on the number of possible classes
	 * @param numberOfClasses the number of possible classes
	 * @return the upper bound of gain for this quality criterion depending on the number of possible classes
	 */
	public double getHighestGain(int numberOfClasses);

	/**
	 * Returns the name of this quality criterion
	 * @return the name of this quality criterion
	 */
	@Override
	public String toString();





	/** The {@link QualityCriterion} 'Gini-Index' as used by CART */
	public class GiniCriterion implements QualityCriterion {
		private static final long serialVersionUID = 1L;
		/** {@inheritDoc} */
		@Override
		public double getQuality(double... probabilities) {
			double giniIndex = 0d;
			for (int i = 0; i < probabilities.length; i++) {
				giniIndex += (probabilities[i]*probabilities[i]);
			}
			return (1 - giniIndex);
		}
		/** {@inheritDoc} */
		@Override
		public double getHighestGain(int numberOfClasses) {
			return 1d - (1d / numberOfClasses);
		}
		/** {@inheritDoc} */
		@Override
		public double getLowestGain() {
			return 0d;
		}
		/** {@inheritDoc} */
		@Override
		public String toString() {
			return "Gini-Index";
		}
	}



	/**
	 *  The {@link QualityCriterion} 'entropy' or 'information (gain)' as used by ID3 
	 */
	public class EntropyCriterion implements QualityCriterion {
		private static final long serialVersionUID = 1L;
		/** {@inheritDoc} */
		@Override
		public double getQuality(double ... probabilities) {
			double entropy = 0d;
			for (int i = 0; i < probabilities.length; i++) {
				if (probabilities[i] > 0d) {
	                entropy -= probabilities[i] * log2(probabilities[i]);
	            }
			}
			return entropy;
		}
		/** {@inheritDoc} */
		@Override
		public double getHighestGain(int numberOfClasses) {
			return log2(numberOfClasses);
		}
		/** {@inheritDoc} */
		@Override
		public double getLowestGain() {
			return 0d;
		}
		/** {@inheritDoc} */
		@Override
		public String toString() {
			return "information";
		}
		
		public double log2 (double number)
		{
			return Math.log10(number)/Math.log10(2d);
		}
	}
	

}