package moa.classifiers.core.statisticaltests;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.List;
import java.util.concurrent.Callable;

import moa.options.OptionHandler;

public interface StatisticalTest extends OptionHandler, Callable<Double> {
    public double test(List<Instance> x, List<Instance> y);
    public void set(List<Instance> x, List<Instance> y);
}
