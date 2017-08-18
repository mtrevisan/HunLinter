package unit731.hunspeller.collections.bloomfilter.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;


public class JavaBitSetArrayTest{

	private static final int MILLION = 1000 * 1000;


	@Test
	public void javaBitSetArray(){
		JavaBitSetArray bits = null;

		try{
			bits = new JavaBitSetArray(MILLION);
			for(int i = 0; i < MILLION; i ++){
				Assert.assertFalse(bits.getBit(i));
				bits.setBit(i);
				Assert.assertTrue(bits.getBit(i));
				bits.clearBit(i);
				Assert.assertFalse(bits.getBit(i));
			}
		}
		finally{
			if(bits != null){
				try{
					bits.close();
				}
				catch(IOException e){
					//eat up
				}
			}
		}
	}

}
