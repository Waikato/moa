

package moa.streams.filters;
import com.github.javacliparser.IntOption;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.streams.InstanceStream;

/**
 *    Filter to perform feature hashing to reduce the number of attributes by applying
 *    a hash function to features.
 *
 *   @author Maroua Bahri
 */

public class HashingTrickFilter extends AbstractStreamFilter {

    private static final long serialVersionUID = 1L;

    public IntOption dim = new IntOption("OutputFeatureDimension", 'd',
            "the target feature dimension.", 10);

    protected InstancesHeader streamHeader;

    protected FastVector attributes;

    @Override
    public String getPurposeString() { return "Reduces the number of input features using a hash function.";  }

    @Override
    protected void restartImpl() {
        this.streamHeader = null;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public InstanceExample nextInstance() {

        Instance sparseInstance = (Instance) this.inputStream.nextInstance().getData();

        if (streamHeader == null) {
            //Create a new header
            this.attributes = new FastVector();
            for (int i = 0; i < this.dim.getValue(); i++) {
                this.attributes.addElement(new Attribute("numeric" + (i + 1)));
            }
            this.attributes.addElement(sparseInstance.classAttribute());
            this.streamHeader = new InstancesHeader(new Instances(
                    getCLICreationString(InstanceStream.class), this.attributes, 0));
            this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
        }


        double [] hashVal = hashVector(sparseInstance,this.dim.getValue(), Hashing.murmur3_128());

        return new InstanceExample(transformedInstance(sparseInstance, hashVal));
    }



    public DenseInstance transformedInstance(Instance sparseInst, double [] hashVal) {

        Instances header = this.streamHeader;
        double[] attributeValues = new double[header.numAttributes()];

        for(int i = 0 ; i < header.numAttributes()-1 ; i++) {
            attributeValues[i] = hashVal[i];
        }

        attributeValues[attributeValues.length-1] = sparseInst.classValue();
        DenseInstance newInstance = new DenseInstance(1.0, attributeValues);
        newInstance.setDataset(header);
        return newInstance;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }


    public  double[] hashVector(Instance instance, int n, HashFunction hashFunction) {

        double [] denseValues = new double [n];
        for (int i = 0 ; i < n ; i++) {
            denseValues[i] = 0d;
        }
        for (int i = 0; i < instance.numAttributes()-1 ; i++){
                double diff = Math.abs(instance.value(i));
                if( diff  > Double.MIN_NORMAL) {
                    int  hash = hashFunction.hashInt(i).asInt();
                    int bucket = Math.abs(hash) % n;
                    denseValues[bucket] += (hash < 0 ? -1d : 1d);
                }
        }
        
        return denseValues;
    }


}