/*
 *    AssetNegotiationGenerator.java
 *    Copyright (C) 2016 Pontifícia Universidade Católica do Paraná, Curitiba, Brazil
 *    @author Jean Paul Barddal (jean.barddal@ppgia.pucpr.br)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

package moa.streams.generators;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import java.util.Arrays;
import java.util.Random;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 *
 * @author Jean Paul Barddal
 * @author Fabrício Enembreck
 *
 * @version 1.0
 * Originally discussed in F. Enembreck, B. C. Ávila, E. E. Scalabrin {@literal &}
 * J-P. Barthès. LEARNING DRIFTING NEGOTIATIONS. In Applied Artificial
 * Intelligence: An International Journal. Volume 21, Issue 9, 2007. DOI:
 * 10.1080/08839510701526954
 * First used in the data stream configuration in J. P. Barddal, H. M.
 * Gomes, F. Enembreck, B. Pfahringer {@literal &} A. Bifet. ON DYNAMIC FEATURE WEIGHTING
 * FOR FEATURE DRIFTING DATA STREAMS. In European Conference on Machine Learning
 * and Principles and Practice of Knowledge Discovery (ECML/PKDD'16). 2016.
 */

public class AssetNegotiationGenerator
	extends AbstractOptionHandler
	implements InstanceStream {

    /*
     * OPTIONS
     */        
    public IntOption functionOption = new IntOption("function", 'f',
	    "Classification function used, as defined in the original paper.",
	    1, 1, 5);
    public FloatOption noisePercentage = new FloatOption("noise", 'n',
	    "% of class noise.", 0.05, 0.0, 1.0f);

    public IntOption instanceRandomSeedOption = new IntOption(
	    "instanceRandomSeed", 'i',
	    "Seed for random generation of instances.", 1);

    /*
     * INTERNALS
     */
    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected boolean nextClassShouldBeZero;
    
    protected ClassFunction classFunction;
    

    /*
     * FEATURE DEFINITIONS
     */
    protected static String colorValues[] = {"black",
	"blue",
	"cyan",
	"brown",
	"red",
	"green",
	"yellow",
	"magenta"};

    protected static String priceValues[] = {"veryLow",
	"low",
	"normal",
	"high",
	"veryHigh",
	"quiteHigh",
	"enormous",
	"non_salable"};

    protected static String paymentValues[] = {"0",
	"30",
	"60",
	"90",
	"120",
	"150",
	"180",
	"210",
	"240"};

    protected static String amountValues[] = {"veryLow",
	"low",
	"normal",
	"high",
	"veryHigh",
	"quiteHigh",
	"enormous",
	"non_ensured"};

    protected static String deliveryDelayValues[] = {"veryLow",
	"low",
	"normal",
	"high",
	"veryHigh"};

    protected static String classValues[] = {"interested", "notInterested"};

    /*
     * Labeling functions
     */
    protected interface ClassFunction {

	public int determineClass(String color,
		String price,
		String payment,
		String amount,
		String deliveryDelay);

	public Instance makeTrue(Instance intnc);
    }

    protected static ClassFunction concepts[] = {
	new ClassFunction() {
	    Random r = new Random(Integer.MAX_VALUE);

	    @Override
	    public int determineClass(String color,
		    String price,
		    String payment,
		    String amount,
		    String deliveryDelay) {
		if ((price.equals("normal") && amount.equals("high")
			|| (color.equals("brown") && price.equals("veryLow")
			&& deliveryDelay.equals("high")))) {
		    return indexOfValue("interested", classValues);
		}
		return indexOfValue("notInterested", classValues);
	    }

	    @Override
	    public Instance makeTrue(Instance intnc) {
		int part = r.nextInt(2);
		if (part == 0) {

		    intnc.setValue(1, indexOfValue("normal", priceValues));
		    intnc.setValue(3, indexOfValue("high", amountValues));
		} else {
		    intnc.setValue(0, indexOfValue("brown", colorValues));
		    intnc.setValue(1, indexOfValue("veryLow", priceValues));
		    intnc.setValue(4, indexOfValue("high", deliveryDelayValues));
		}
		intnc.setClassValue(indexOfValue("interested", classValues));
		return intnc;
	    }

	},
	new ClassFunction() {
	    Random r = new Random(Integer.MAX_VALUE);

	    @Override
	    public int determineClass(String color,
		    String price,
		    String payment,
		    String amount,
		    String deliveryDelay) {
		if (price.equals("high") && amount.equals("veryHigh")
			&& deliveryDelay.equals("high")) {
		    return indexOfValue("interested", classValues);
		}
		return indexOfValue("notInterested", classValues);
	    }

	    @Override
	    public Instance makeTrue(Instance intnc) {
		intnc.setValue(1, indexOfValue("high", priceValues));
		intnc.setValue(3, indexOfValue("veryHigh", amountValues));
		intnc.setValue(4, indexOfValue("high", deliveryDelayValues));
		intnc.setClassValue(Arrays.asList(classValues).indexOf("interested"));
		return intnc;
	    }	    
	},
	new ClassFunction() {
	    Random r = new Random(Integer.MAX_VALUE);

	    @Override
	    public int determineClass(String color,
		    String price,
		    String payment,
		    String amount,
		    String deliveryDelay) {
		if ((price.equals("veryLow")
			&& payment.equals("0") && amount.equals("high"))
			|| (color.equals("red") && price.equals("low")
			&& payment.equals("30"))) {
		    return indexOfValue("interested", classValues);
		}
		return indexOfValue("notInterested", classValues);
	    }

	    @Override
	    public Instance makeTrue(Instance intnc) {
		int part = r.nextInt(2);
		if (part == 0) {
		    intnc.setValue(1, indexOfValue("veryLow", priceValues));
		    intnc.setValue(2, indexOfValue("0", paymentValues));
		    intnc.setValue(3, indexOfValue("high", amountValues));
		} else {
		    intnc.setValue(0, indexOfValue("red", colorValues));
		    intnc.setValue(1, indexOfValue("low", priceValues));
		    intnc.setValue(2, indexOfValue("30", paymentValues));
		}
		intnc.setClassValue(Arrays.asList(classValues).indexOf("interested"));
		return intnc;
	    }	    
	},
	new ClassFunction() {
	    Random r = new Random(Integer.MAX_VALUE);

	    @Override
	    public int determineClass(String color,
		    String price,
		    String payment,
		    String amount,
		    String deliveryDelay) {
		if ((color.equals("black")
			&& payment.equals("90")
			&& deliveryDelay.equals("veryLow"))
			|| (color.equals("magenta")
			&& price.equals("high")
			&& deliveryDelay.equals("veryLow"))) {
		    return indexOfValue("interested", classValues);
		}
		return indexOfValue("notInterested", classValues);
	    }

	    @Override
	    public Instance makeTrue(Instance intnc) {
		int part = r.nextInt(2);
		if (part == 0) {
		    intnc.setValue(0, indexOfValue("black", colorValues));
		    intnc.setValue(2, indexOfValue("90", paymentValues));
		    intnc.setValue(4, indexOfValue("veryLow", deliveryDelayValues));
		} else {
		    intnc.setValue(0, indexOfValue("magenta", colorValues));
		    intnc.setValue(1, indexOfValue("high", priceValues));
		    intnc.setValue(4, indexOfValue("veryLow", deliveryDelayValues));
		}
		intnc.setClassValue(Arrays.asList(classValues).indexOf("interested"));
		return intnc;
	    }	    
	},
	new ClassFunction() {
	    Random r = new Random(Integer.MAX_VALUE);

	    @Override
	    public int determineClass(String color,
		    String price,
		    String payment,
		    String amount,
		    String deliveryDelay) {
		if ((color.equals("blue")
			&& payment.equals("60")
			&& amount.equals("low")
			&& deliveryDelay.equals("normal"))
			|| (color.equals("cyan")
			&& amount.equals("low")
			&& deliveryDelay.equals("normal"))) {
		    return indexOfValue("interested", classValues);
		}
		return indexOfValue("notInterested", classValues);
	    }

	    @Override
	    public Instance makeTrue(Instance intnc) {
		int part = r.nextInt(2);
		if (part == 0) {
		    intnc.setValue(0, indexOfValue("blue", colorValues));
		    intnc.setValue(2, indexOfValue("60", paymentValues));
		    intnc.setValue(3, indexOfValue("low", amountValues));
		    intnc.setValue(4, indexOfValue("normal", deliveryDelayValues));
		} else {
		    intnc.setValue(0, indexOfValue("cyan", colorValues));
		    intnc.setValue(3, indexOfValue("low", amountValues));
		    intnc.setValue(4, indexOfValue("normal", deliveryDelayValues));
		}
		intnc.setClassValue(Arrays.asList(classValues).indexOf("interested"));
		return intnc;
	    }	    
	}
    };
    
    /*
     * Generator core
     */

    @Override
    public void getDescription(StringBuilder sb, int indent) {
	sb.append("Generates instances based on 5 different concept functions "
		+ "that describe whether another agent is "
		+ "interested or not in an item.");
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor tm, ObjectRepository or) {
	
	classFunction = concepts[this.functionOption.getValue() - 1];

	FastVector attributes = new FastVector();
	attributes.addElement(new Attribute("color",
		Arrays.asList(colorValues)));
	attributes.addElement(new Attribute("price",
		Arrays.asList(priceValues)));
	attributes.addElement(new Attribute("payment",
		Arrays.asList(paymentValues)));
	attributes.addElement(new Attribute("amount",
		Arrays.asList(amountValues)));
	attributes.addElement(new Attribute("deliveryDelay",
		Arrays.asList(deliveryDelayValues)));

	this.instanceRandom = new Random(System.currentTimeMillis());

	FastVector classLabels = new FastVector();
	for (int i = 0; i < classValues.length; i++) {
	    classLabels.addElement(classValues[i]);
	}

	attributes.addElement(new Attribute("class", classLabels));
	this.streamHeader = new InstancesHeader(new Instances(
		getCLICreationString(InstanceStream.class), attributes, 0));
	this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);

	restart();
    }

    @Override
    public InstancesHeader getHeader() {
	return streamHeader;
    }

    @Override
    public long estimatedRemainingInstances() {
	return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasMoreInstances() {
	return true;
    }

    @Override
    public InstanceExample nextInstance() {
	Instance instnc = null;

	boolean classFound = false;
	while (!classFound) {
	    //randomize indexes for new instance
	    int indexColor = this.instanceRandom.nextInt(colorValues.length);
	    int indexPrice = this.instanceRandom.nextInt(priceValues.length);
	    int indexPayment = this.instanceRandom.nextInt(paymentValues.length);
	    int indexAmount = this.instanceRandom.nextInt(amountValues.length);
	    int indexDelivery = this.instanceRandom.nextInt(deliveryDelayValues.length);
	    //retrieve values
	    String color = colorValues[indexColor];
	    String price = priceValues[indexPrice];
	    String payment = paymentValues[indexPayment];
	    String amount = amountValues[indexAmount];
	    String delivery = deliveryDelayValues[indexDelivery];
	    int classValue = classFunction.
		    determineClass(color, price, payment, amount, delivery);

	    instnc = new DenseInstance(streamHeader.numAttributes());
	    //set values
	    instnc.setDataset(this.getHeader());
	    instnc.setValue(0, Arrays.asList(colorValues).indexOf(color));
	    instnc.setValue(1, Arrays.asList(priceValues).indexOf(price));
	    instnc.setValue(2, Arrays.asList(paymentValues).indexOf(payment));
	    instnc.setValue(3, Arrays.asList(amountValues).indexOf(amount));
	    instnc.setValue(4, Arrays.asList(deliveryDelayValues).indexOf(delivery));

	    if (classValue == 0 && !nextClassShouldBeZero) {
		instnc = classFunction.makeTrue(instnc);
		classValue = 1;
		nextClassShouldBeZero = !nextClassShouldBeZero;
		classFound = true;
	    } else if (classValue == 0 && nextClassShouldBeZero) {
		nextClassShouldBeZero = !nextClassShouldBeZero;
		classFound = true;
	    } else if (classValue == 1 && !nextClassShouldBeZero) {
		nextClassShouldBeZero = !nextClassShouldBeZero;
		classFound = true;
	    }
	    instnc.setClassValue((int) classValue);
	    
	}
	//add noise
	int newClassValue = addNoise((int) instnc.classValue());
	instnc.setClassValue(newClassValue);
	return new InstanceExample(instnc);
    }

    @Override
    public boolean isRestartable() {
	return true;
    }

    @Override
    public void restart() {
	this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
	this.nextClassShouldBeZero = false;
    }

    int addNoise(int classObtained) {
	if (this.instanceRandom.nextFloat() <= this.noisePercentage.getValue()) {
	    classObtained = classObtained == 0 ? 1 : 0;
	}
	return classObtained;
    }

    private static int indexOfValue(String value, Object[] arr) {
	int index = Arrays.asList(arr).indexOf(value);
	return index;
    }
}