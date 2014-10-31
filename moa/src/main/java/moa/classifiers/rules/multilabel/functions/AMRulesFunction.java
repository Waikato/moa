package moa.classifiers.rules.multilabel.functions;

public interface AMRulesFunction {
	public void resetWithMemory();
	public void selectOutputsToLearn(int [] outtputAtributtes);
}
