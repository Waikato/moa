package moa.classifiers.multilabel.trees;

import java.util.List;
import java.util.Vector;

import com.github.javacliparser.MultiChoiceOption;

public class ISOUPTreeRF extends ISOUPTree {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3145263568676115808L;
	public MultiChoiceOption radndomForestFunAttrsOption = new MultiChoiceOption(
			"randomForestFunAttr",
			'z',
			"The function of the number of input attribtues to use in the random forest construction.", 
			new String[]{"sqrt", "log", "percent"}, new String[]{"Square root", "Logarithm", "10% percent"}, 0);
	
	
	public int numRFAttrs() {
		switch (radndomForestFunAttrsOption.getChosenLabel()) {
		case "sqrt": return (int) Math.ceil(Math.sqrt(this.getModelContext().numInputAttributes()));
		case "log": return (int) Math.ceil(Math.log(this.getModelContext().numInputAttributes())) + 1;
		case "percent": return (int) Math.ceil(0.1 * this.getModelContext().numInputAttributes());
		}
		return 0;
	}
	
	@Override
	public List<Integer> newInputIndexes() {
		List<Integer> indexes = new Vector<Integer>();
		int numAttrs = this.getModelContext().numInputAttributes();
		for (int i = 0; i < numAttrs; i++) {
			indexes.add(i, i);
		}
		for (int i = 0; i < Math.ceil(numRFAttrs()); i++) {
			int swap = i + this.classifierRandom.nextInt(numAttrs - i);
			int temp = indexes.get(i);
			indexes.set(i, indexes.get(swap));
			indexes.set(swap, temp);
		}
		return indexes.subList(0, numRFAttrs());
	}
	
}
