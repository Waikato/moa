package lin.junit.test;

import org.junit.Test;

import junit.framework.Assert;
import volatilityevaluation.Buffer;

public class TestBuffer
{
	@Test
	public void testGetIntervalsMeanNotFull()
	{
		Buffer buffer = new Buffer(4);
		buffer.add(1);
		buffer.add(5);
		buffer.add(7);
		double mean = buffer.getIntervalsMean();
		Assert.assertEquals(3.0, mean);
	}
	@Test
	public void testGetIntervalsMeanFull()
	{
		Buffer buffer = new Buffer(4);
		buffer.add(1);
		buffer.add(5);
		buffer.add(7);
		buffer.add(11);
		double mean = buffer.getIntervalsMean();
		Assert.assertEquals(10/3.0, mean);
	}
	@Test
	public void testGetIntervalsMeanNoElement()
	{
		Buffer buffer = new Buffer(4);
		double mean = buffer.getIntervalsMean();
		Assert.assertTrue(mean<=0.0);
	}

}
