package moa.classifiers.multilabel.trees;

import java.util.List;
import java.util.Vector;

import com.github.javacliparser.MultiChoiceOption;

import moa.core.DoubleVector;

public class ISOUPTreeRF extends ISOUPTree {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public MultiChoiceOption subspaceSizeOption = new MultiChoiceOption("randomForestFunAttr", 'z',
			"The function of the number of input attribtues to use in the random forest construction.",
			new String[] { "sqrt", "log", "percent" }, new String[] { "Square root", "Logarithm", "10% percent" }, 0);

	public Integer subspaceSize = null;

	public int numRFAttrs() {
		if (subspaceSize == null) {
			switch (subspaceSizeOption.getChosenLabel()) {
			case "sqrt":
				this.subspaceSize = 1 + (int) Math.ceil(Math.sqrt(this.getModelContext().numInputAttributes()));
			case "log":
				this.subspaceSize = 1 + (int) Math.ceil(Math.log(this.getModelContext().numInputAttributes()));
			case "percent":
				this.subspaceSize = (int) Math.ceil(0.1 * this.getModelContext().numInputAttributes());
			}
		}
		return this.subspaceSize;
	}

	@Override
	public List<Integer> newInputIndexes() {
		List<Integer> indexes = new Vector<>();
		int numAttrs = this.getModelContext().numInputAttributes();
		for (int i = 0; i < numAttrs; i++) {
			indexes.add(i, i);
		}
		for (int i = 0; i < numRFAttrs(); i++) {
			int swap = i + this.classifierRandom.nextInt(numAttrs - i);
			int temp = indexes.get(i);
			indexes.set(i, indexes.get(swap));
			indexes.set(swap, temp);
		}
		return indexes.subList(0, numRFAttrs());
	}

	public DoubleVector getFeatureScores() {
		return getNodeFeatureScore(treeRoot);
	}

	public DoubleVector getNodeFeatureScore(ISOUPTree.Node node) {
		if (node instanceof ISOUPTree.SplitNode) {
			DoubleVector scores = new DoubleVector(new double[getModelContext().numInputAttributes()]);
			for (ISOUPTree.Node child : ((ISOUPTree.SplitNode) node).children) {
				scores.addValues(getNodeFeatureScore(child));
			}
			scores.setValue(((ISOUPTree.SplitNode) node).predicate.getAttributeIndex(), Math.pow(0.5, node.getLevel()));
			return scores;
		} else {
			return new DoubleVector();
		}
	}

}
