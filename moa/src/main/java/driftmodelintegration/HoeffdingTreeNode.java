package driftmodelintegration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Represents a node in a {@link HoeffdingTreeModel}
 * @author Tobias Beckers
 *
 */
public class HoeffdingTreeNode implements Serializable {

	private static final long serialVersionUID = 1L;

	/** the feature associated with this node (split feature) */
	private String feature;
	/** contains a child for each possible branch (/value) for this node (/feature) */
	private final Map<Serializable, HoeffdingTreeNode> children;

	/** only set for leafs: The predicted class for all examples sorted to this node */
	private Serializable label;

	/**
	 * Initializes a new {@link HoeffdingTreeNode} with no feature and no children but a label.
	 * @param label the initial label for the new node
	 */
	public HoeffdingTreeNode(Serializable label) {
		this.feature = null;
		this.label = label;
		this.children = new HashMap<Serializable, HoeffdingTreeNode>();
	}

	/**
	 * Sets the label for this node. Overwrites the previous label if there was one.
	 * @param label the (new) label
	 */
	public void setLabel(Serializable label) {
		this.label = label;
	}

	/**
	 * Returns the label of this node.
	 * @return the label
	 */
	public Serializable getLabel() {
		return this.label;
	}

	/**
	 * Declares this node to be a split on the specified feature.
	 * @param feature the split feature
	 */
	public void setFeature(String feature) {
		this.feature = feature;
	}

	/**
	 * Returns the feature on which this node tests.
	 * @return the split feature
	 */
	public String getFeature() {
		return this.feature;
	}

	/**
	 * Returns true iff this node actually represents a leaf in the contained tree.
	 * @return true iff this node is a leaf
	 */
	public boolean isLeaf() {
		return (this.children == null || this.children.isEmpty());
	}

	/**
	 * Adds a child to this node (which is then the parent of the specified node) for the specified branch.
	 * @param child a node which should be a child of this node
	 * @param value a value for the feature of this node to specify the branch
	 */
	public void addChild(HoeffdingTreeNode child, Serializable value) {
		this.children.put(value, child);
	}

	/**
	 * Returns the node following the branch induced by the specified value.
	 * @param value a valid value for the feature represented by this node
	 * @return the next node following the path of the given value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HoeffdingTreeNode getchild(Serializable value, Class<?> featureType) {

		if( featureType == Double.class )
			return this.children.get(HoeffdingTree.numericToNominal((Set)(this.children.keySet()), (Comparable)value));

		return children.get( value );
	}

	/**
	 * Returns a string representation of the subtree beginning at this node.
	 * @return a string representation of the subtree beginning at this node
	 */
	@Override
    public String toString() {
		return this.toString(0);
	}

	/**
	 * Returns a string representation of the subtree beginning at this node. Adds the specified number of blanks to each line.
	 * @param spaceNumber the number of blanks for each line
	 * @return a string representation of the subtree beginning at this node
	 */
	protected String toString(int spaceNumber) {
		StringBuffer out = new StringBuffer();
		out.append("["+this.feature+"|"+this.label+"]");
		for (Serializable value : this.children.keySet()) {
			out.append("\n");
			int labelLength = 0;
			if (this.label == null) { labelLength = 4;}
			else { labelLength = this.label.toString().length();	}
			int spaceLength = spaceNumber+this.feature.length()+labelLength+3;
			for (int i = 0; i < spaceLength; i++) {
				out.append(" ");
			}
			out.append("-- "+value.toString()+" --");
			out.append(this.children.get(value).toString(spaceLength+value.toString().length()+6));
		}
		return out.toString();
	}
}