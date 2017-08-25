package unit731.hunspeller.collections.bloomfilter;

import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.bloomfilter.core.BitArray;
import unit731.hunspeller.collections.bloomfilter.core.JavaBitArray;


public class BitArraysTest{

	private static final int MAX = 100 * 100;


	@Test
	public void javaBitArray(){
		BitArray bitArray = new JavaBitArray(MAX);
		testArray(bitArray, MAX);
	}

	private void testArray(BitArray bitArray, int maxElements){
		for(int index = 0; index < maxElements; index ++){
			Assert.assertFalse(bitArray.get(index));
			bitArray.set(index);
			Assert.assertTrue(bitArray.get(index));
			bitArray.clear(index);
			Assert.assertFalse(bitArray.get(index));
		}
	}

}
