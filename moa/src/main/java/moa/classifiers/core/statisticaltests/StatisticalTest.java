/*
 *    StatisticalTest.java
 *    Copyright (C) 2017 Instituto Federal de Pernambuco
 *    @author Paulo Gonçalves (paulogoncalves@recife.ifpe.edu.br)
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
package moa.classifiers.core.statisticaltests;

import com.yahoo.labs.samoa.instances.Instance;
import java.util.List;
import java.util.concurrent.Callable;
import moa.options.OptionHandler;

/**
 * This interface represents how to perform multivariate statistical tests.
 *
 * @author Paulo Goncalves
 *
 */
public interface StatisticalTest extends OptionHandler, Callable<Double> {
    /**
     * This method performs a test and returns the correspoding p-value.
     * @param x List of instances
     * @param y List of instances
     * @return p-value
     */
    public double test(List<Instance> x, List<Instance> y);
    /**
     * This method sets the instances for later use in concurrent scenarios. 
     * The test is performed by using the call() method.
     * @param x List of instances
     * @param y List of instances
     */
    public void set(List<Instance> x, List<Instance> y);
}
