package unit731.hunspeller.collections.bloomfilter.core;

import org.junit.Assert;
import org.junit.Test;


public class JavaBitArrayTest{

	private static final int MAX = 100 * 100;


	@Test
	public void javaBitSetArray(){
		JavaBitArray bits = new JavaBitArray(MAX);
		for(int i = 0; i < MAX; i ++){
			Assert.assertFalse(bits.get(i));
			bits.set(i);
			Assert.assertTrue(bits.get(i));
			bits.clear(i);
			Assert.assertFalse(bits.get(i));
		}
	}

}
