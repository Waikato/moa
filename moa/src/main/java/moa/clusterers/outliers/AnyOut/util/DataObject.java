package moa.clusterers.outliers.AnyOut.util;

import com.yahoo.labs.samoa.instances.Instance;



/**
 * This object encapsulates a data point.
 * @author Fernando Sanchez Villaamil
 */

public class DataObject {
    
    private int id;
    private Instance inst;
    private double[] features;
    private int classLabel;
    private boolean isOutiler;

    /**
     * Returns the features (label attribute excluded).
     * @return An array <code>double[]</code> with the features.
     */
    public double[] getFeatures() {
    	double[] filteredFeatures = new double[features.length-1];
    	for (int i = 0; i<filteredFeatures.length; i++)
    		filteredFeatures[i]=features[i];
        return filteredFeatures;
    }

    /**
     * Returns the id for the <code>DataObject</code>.
     * @return An <code>int</code> with the id of the <code>DataObject</code>.
     */
    public int getId() {
        return id;
    }
    public void setId(int id){
    	this.id=id;
    }
    
    /**
     * Return the label for the <code>DataObject</code>.
     * @return An <code>int</code> which codes the label.
     */
    public int getClassLabel() {
        return classLabel;
    }
    
    /**
     * Return the <code>Instance</code> of the <code>DataObject</code>.
     * @return An <code>Instance</code>.
     */
    public Instance getInstance() {
        return inst;
    }
    /**
     * Standard constructor for <code>DataObject</code>.
     * @param idCounter The id for the <code>DataObject</code>.
     * @param features The feature as a <code>double[]</code>
     * @param classLabel The label id for the <code>DataObject</code>.
     */
    public DataObject(int idCounter, Instance inst){
        this.id = idCounter;
        this.inst = inst;
        this.features = inst.toDoubleArray();
        this.classLabel = (int)inst.classValue();
        this.isOutiler = false;
    }
    
    /**
     * Returns the number of features (label attribute excluded).
     * @return The number of features in the point.
     */
    public int getNrOfDimensions(){
        return this.features.length-1;
    }
    
    /**
     * Returns a <code>String</code> representation of the point. The features are written comma
     * separated between parenthesis and the label id is written after the closing parenthesis
     * surrounded by squared brackets.
     * @return A <code>String</code> representation of the point.
     */
    @Override
    public String toString(){
        
        String res = "(";
        for (int i = 0; i < features.length; i++) {
            double d = features[i];
            res += d;
            if (i != features.length - 1)
                res += ",";
        }
        res += ")";
        
        res += "[" + this.classLabel + "]";
        
        return res;
        
    }

    
    public void setOutiler(boolean val) {
		isOutiler = val;
	}
    
    public boolean isOutiler() {
		return isOutiler;
	}
    
}
