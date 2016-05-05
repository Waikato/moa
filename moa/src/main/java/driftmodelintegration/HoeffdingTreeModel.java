package driftmodelintegration;

import java.io.Serializable;
import java.util.Map;

import driftmodelintegration.core.Data;
import driftmodelintegration.core.DescriptionModel;
import driftmodelintegration.core.PredictionModel;

/**
 * Represents a Hoeffding Tree. The type of the nodes in this tree is {@link HoeffdingTreeNode}
 * @author Tobias Beckers
 *
 */
public class HoeffdingTreeModel implements PredictionModel<Data, Serializable> , DescriptionModel<String> {

	private static final long serialVersionUID = 1L;

	/** the root of this tree */
	private final HoeffdingTreeNode root;



	/** defines a {@link FeatureType} for each feature */
	private final Map<String, Class<?>> featureTypes;


	/**
	 * Creates a new Hoeffding Tree containing only an empty root with an initial label.<br/>
	 * Defines a type for each feature.
	 * @param featureTypes a declaration of types for all features
	 * @param label an initial Label for the root
	 */
	public HoeffdingTreeModel(Map<String, Class<?>> featureTypes, Serializable label) {
		this.featureTypes = featureTypes;
		this.root = new HoeffdingTreeNode(label);
	}

	/**
	 * Returns the {@link FeatureType} for the specified feature
	 * @param feature a feature which type is requested
	 * @return the {@link FeatureType} for the specified feature
	 */
	public Class<?> getType(String feature) {
		return this.featureTypes.get(feature);
	}

	/**
	 * Returns the predicted class label for the specified example
	 * @param example an {@link Example} for which a prediction should be made
	 * @return the predicted class label for the specified example
	 */
	@Override
	public Serializable predict(Data example) {
		return this.getLeaf(example).getLabel();
	}

	/**
	 * Returns the leaf at which the path through the tree ends for the specified {@link Example}.
	 * @param example an {@link Example}
	 * @return the reached leaf
	 */
	public HoeffdingTreeNode getLeaf(Data example) {
		HoeffdingTreeNode currentNode = this.root;
		while (!currentNode.isLeaf()) {
			Class<?> clazz = this.getType( currentNode.getFeature() );
			if( clazz == Double.class )
				currentNode = currentNode.getchild(example.get(currentNode.getFeature()), Double.class );
			else
				currentNode = currentNode.getchild( example.get(currentNode.getFeature() ), String.class );
		}
		return currentNode;
	}

	/**
	 * Returns a string representation of the current tree
	 * @return a string representation of the current tree
	 */
	@Override
	public String describe() {
		return this.toString();
	}

	/**
	 * Returns a string representation of the current tree
	 * @return a string representation of the current tree
	 */
	@Override
	public String toString() {
		return this.root.toString();
	}
}