package core;

public class Centroid
{
    private double[] centers;
    private double weight;
    private double stdev;
    private int classLabel;

    public Centroid()
    {
    }

    public Centroid(double[] centers, double weight, double stdev,
	    int classLabel)
    {
	this.centers = centers;
	this.weight = weight;
	this.stdev = stdev;
	this.classLabel = classLabel;
    }

    public void setCenters(double[] centers)
    {
	this.centers = centers;
    }

    public double[] getCenters()
    {
	return centers;
    }

    public void setCenterAtPos(int pos, double value)
    {
	this.centers[pos] = value;
    }

    public double getCenterAtPos(int pos)
    {
	return this.centers[pos];
    }

    public void setWeight(double weight)
    {
	this.weight = weight;
    }

    public double getWeight()
    {
	return weight;
    }

    public void setStdev(double stdev)
    {
	this.stdev = stdev;
    }

    public double getStdev()
    {
	return stdev;
    }

    public void setClassLabel(int classLabel)
    {
	this.classLabel = classLabel;
    }

    public int getClassLabel()
    {
	return classLabel;
    }

}
