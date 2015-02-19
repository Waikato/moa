package moa.classifiers.rules.featureranking;

import moa.classifiers.rules.featureranking.messages.WeightedMajorityAddMessage;
import moa.classifiers.rules.featureranking.messages.WeightedMajorityCleanMessage;
import moa.classifiers.rules.featureranking.messages.WeightedMajorityMessage;
import moa.classifiers.rules.featureranking.messages.WeightedMajorityRemoveMessage;
import moa.classifiers.rules.multilabel.core.MultiLabelRule;
import moa.classifiers.rules.multilabel.core.ObservableMOAObject;
import moa.core.DoubleVector;

import com.github.javacliparser.FloatOption;

public class WeightedMajorityFeatureRanking_ extends AbstractFeatureRanking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected DoubleVector attributeImportance;

	public FloatOption meritThresholdOption = new FloatOption(
			"meritThreshold",
			'm',
			"Merit threshold value. If the merit of an input attribute is below the threshold its importance will decrease",
			0.01, 0.0, 1.0);



	public void update(ObservableMOAObject o, Object arg) {
		if (arg instanceof WeightedMajorityAddMessage) {
			double th=meritThresholdOption.getValue();


			double [] merits = ((WeightedMajorityAddMessage) arg).getValues();
			int numLiterals = ((WeightedMajorityAddMessage) arg).getNumberOfLiterals();
			double [] decreases= new double[merits.length];
			System.out.println("\nWMFR Rule added atribute");

			//send the reduction amount back to rule (the rule will accumulate reductions to compensate in the feature if a change is detected)
			if (o instanceof MultiLabelRule){
				if(attributeImportance==null){
					double [] init= new double[merits.length];
					for(int i=0; i<merits.length;i++)
						init[i]=1;
					attributeImportance= new DoubleVector(init);
				}

				for (int i=0; i<merits.length;i++){
					if(merits[i]<th){
						double currentValue=attributeImportance.getValue(i);
						attributeImportance.setValue(i, currentValue*(1+numLiterals)/(2+numLiterals));
						decreases[i]=currentValue/(2+numLiterals);
					}
				}
				//((MultiLabelRule) o).receiveFeedback(this, new WeightedMajorityMessage(decreases));
			}
		}
		else if (arg instanceof WeightedMajorityRemoveMessage) {
			double [] values = ((WeightedMajorityRemoveMessage) arg).getValues();
			System.out.println("\nWMFR Rule removed atribute");
			if(values!=null)
				attributeImportance.addValues(values);
		}
		else if (arg instanceof WeightedMajorityCleanMessage) {
			WeightedMajorityCleanMessage msg=((WeightedMajorityCleanMessage) arg);
			int attrib = msg.getAttribute();
			double val= msg.getValue();
			System.out.println("\nWMFR Rule cleaned atribute " + attrib);
			attributeImportance.addToValue(attrib, val);
		}


	}

	@Override
	public DoubleVector getFeatureRankings() {
		/*DoubleVector normRankings=null;
		if(attributeImportance!=null){
			double total=0;
			for (int i=0; i<attributeImportance.numValues();i++)
				total+=attributeImportance.getValue(i);
			normRankings=new DoubleVector();
			double[] aux= new double[attributeImportance.numValues()];
			for (int i=0; i<attributeImportance.numValues();i++)
				aux[i]=attributeImportance.getValue(i)/total;	
			normRankings= new DoubleVector(aux);
		}
		return normRankings;
		 */
		return attributeImportance;
	}



}
