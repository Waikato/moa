package moa.classifiers.meta.AutoML;

// interface allows us to maintain a single list of parameters
public interface IParameter {
    public void sampleNewConfig(double lambda, double reset, int verbose);

    public IParameter copy();

    public String getCLIString();

    public String getCLIValueString();

    public double getValue();

    public String getParameter();
}
