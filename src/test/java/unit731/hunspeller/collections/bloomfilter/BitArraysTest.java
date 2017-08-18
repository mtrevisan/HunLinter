package unit731.hunspeller.collections.bloomfilter;

import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.bloomfilter.core.BitArray;
import unit731.hunspeller.collections.bloomfilter.core.JavaBitSetArray;


public class BitArraysTest{

	private static final int MILLION_ELEMENTS = 1 * 1000 * 1000;


	@Test
	public void javaBitArray(){
		BitArray bitArray = new JavaBitSetArray(MILLION_ELEMENTS);
		testArray(bitArray, MILLION_ELEMENTS);
	}

	private void testArray(BitArray bitArray, int maxElements){
		for(int index = 0; index < maxElements; index ++){
			Assert.assertFalse(bitArray.getBit(index));
			bitArray.setBit(index);
			Assert.assertTrue(bitArray.getBit(index));
			bitArray.clearBit(index);
			Assert.assertFalse(bitArray.getBit(index));
		}
	}

}
