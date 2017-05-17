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
	@Test
	public void testGetIndexCorrespondence(){
		int[] result=Utils.getIndexCorrespondence(new int []{2,3,5}, new int []{2,3});
		assertArrayEquals(new int []{0,1},result);
		
		result=Utils.getIndexCorrespondence(new int []{1,2,3,4,5}, new int []{1,2,3,4,5});
		assertArrayEquals(new int []{0,1,2,3,4},result);
		
		result=Utils.getIndexCorrespondence(new int []{1,2,5}, new int []{1,5});
		assertArrayEquals(new int []{0,2},result);
		
		result=Utils.getIndexCorrespondence(new int []{1,2,5}, new int []{2});
		assertArrayEquals(new int []{1},result);
	}

}
