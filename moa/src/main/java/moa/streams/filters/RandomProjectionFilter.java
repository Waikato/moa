

package moa.streams.filters;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.streams.InstanceStream;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 *    Filter to perform random projection to reduce the number of attributes. It applies
 *    a Gaussian matrix on features to project them into a lower-dimensional space.
 *
 *   @author Maroua Bahri
 */

public class RandomProjectionFilter extends AbstractStreamFilter {
    private static final long serialVersionUID = 1L;

    public IntOption dim = new IntOption("OutputFeatureDimension", 'd',
            "the target feature dimension.", 10);
    protected InstancesHeader streamHeader;
    protected double[][] GaussMatrix ;


    public String getPurposeString() { return "Reduces the number of input features using random projection.";  }

    @Override
    protected void restartImpl() {
        this.streamHeader = null;
    }

    @Override
    public InstancesHeader getHeader() {
        Instance sparseInstance = (Instance) this.inputStream.nextInstance().getData();
        Random r = new Random(System.currentTimeMillis());
        this.GaussMatrix = new double[this.dim.getValue()][sparseInstance.numAttributes()-1] ;
        for(int i = 0 ; i < this.dim.getValue() ; i++){
            for(int j = 0; j < sparseInstance.numAttributes()-1 ; j++){
                this.GaussMatrix[i][j]= r.nextGaussian();
            }
        }

        if (streamHeader == null) {
            //Create a new header
            FastVector attributes = new FastVector();
            for (int i = 0; i < this.dim.getValue(); i++) {
                attributes.addElement(new Attribute("numeric" + (i + 1)));
            }

            attributes.addElement(sparseInstance.classAttribute());
            this.streamHeader = new InstancesHeader(new Instances(
                    getCLICreationString(InstanceStream.class), attributes, 0));
            this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);

        }
        return this.streamHeader;

    }

    @Override
    public InstanceExample nextInstance() {
        Instance sparseInstance = (Instance) this.inputStream.nextInstance().getData();

        return new InstanceExample(transformedInstance(sparseInstance,
                randomProjection(sparseInstance,this.GaussMatrix)));
    }


    public DenseInstance transformedInstance(Instance sparseInst, double [] val) {

        Instances header = this.streamHeader;
        double[] attributeValues = new double[header.numAttributes()];

        System.arraycopy(val, 0, attributeValues, 0, header.numAttributes()-1);

        attributeValues[attributeValues.length-1] = sparseInst.classValue();
        DenseInstance newInstance = new DenseInstance(1.0, attributeValues);
        newInstance.setDataset(header);
        return newInstance;
    }


    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    public  double[] randomProjection(Instance instance, double[][] gm) {

        double [] denseValues;

        double[] ins = new double [instance.numAttributes()-1];
        for(int i = 0 ; i < instance.numAttributes()-1 ; i++) {
            ins[i] = instance.value(i);
        }
        denseValues = multiply(gm, ins);

        return denseValues;
    }

    public static double[] multiply(double[][] matrix, double[] vector) {
        return Arrays.stream(matrix)
                .mapToDouble(row ->
                        IntStream.range(0, row.length)
                                .mapToDouble(col -> row[col] * vector[col])
                                .sum()
                ).toArray();
    }


}