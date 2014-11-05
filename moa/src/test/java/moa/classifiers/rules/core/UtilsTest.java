package moa.classifiers.rules.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testComplementSet() {
		int [] result=Utils.complementSet(new int []{1,3,4,5,6}, new int []{4,5});
		assertArrayEquals(new int []{1,3,6},result);
		
		result=Utils.complementSet(new int []{1,3,4,5,6}, new int []{1,3,4,5,6});
		assertArrayEquals(new int []{},result);
		
		result=Utils.complementSet(new int []{1,3,4,5,6}, new int []{1});
		assertArrayEquals(new int []{3,4,5,6},result);
		
		result=Utils.complementSet(new int []{1,3,4,5,6}, new int []{4,6});
		assertArrayEquals(new int []{1,3,5},result);
	}

}
