package moa.streams.filters;

import static org.junit.Assert.assertEquals;
import moa.streams.MultiTargetArffFileStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * Test SelectAttributesFilter
 *
 * @author João Duarte (joaomaiaduarte@gmail.com)
 */

public class SelectAttributesFilterTest {
	private static double EPS=0.00000001;
	private static SelectAttributesFilter filter;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		MultiTargetArffFileStream stream=new MultiTargetArffFileStream(ClassLoader.getSystemResource("moa/classifiers/data/small_regression.arff").getPath(), "4-6");
		filter= new SelectAttributesFilter();
		filter.setInputStream(stream);   
		filter.inputStringOption.setValue("2-5,8");
		filter.outputStringOption.setValue("1,5-7");

	}
	@Test
	public void testNextInstance(){
		Instance inst=filter.nextInstance().getData();
		assertEquals("F2", inst.inputAttribute(0).name());
		assertEquals("N1", inst.inputAttribute(1).name());
		assertEquals("R1", inst.inputAttribute(2).name());
		assertEquals("R2", inst.inputAttribute(3).name());
		assertEquals("N2", inst.inputAttribute(4).name());
		
		assertEquals("F1", inst.outputAttribute(0).name());
		assertEquals("R2", inst.outputAttribute(1).name());
		assertEquals("R3", inst.outputAttribute(2).name());
		assertEquals("F3", inst.outputAttribute(3).name());
		assertEquals(9, inst.numAttributes());
		//Original instance
		//1.1, -2.3, val3, 1, 2, 3, 3.3, cat1
		//Filtered instance
		//-2.3, val3, 1, 2, cat1, 1.1, 2, 3, 3.3
		assertEquals(-2.3, inst.value(0),EPS);
		assertEquals(2, inst.value(1),EPS);
		assertEquals(1, inst.value(2),EPS);
		assertEquals(2, inst.value(3),EPS);
		assertEquals(0, inst.value(4),EPS);
		assertEquals(1.1, inst.value(5),EPS);
		assertEquals(2, inst.value(6),EPS);
		assertEquals(3, inst.value(7),EPS);
		assertEquals(3.3, inst.value(8),EPS);
	}

}
