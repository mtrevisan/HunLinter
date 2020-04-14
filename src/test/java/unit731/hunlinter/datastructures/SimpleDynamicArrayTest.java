package unit731.hunlinter.datastructures;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.datastructures.dynamicarray.DynamicArray;


public class SimpleDynamicArrayTest{

	@Test
	void testAdd(){
		DynamicArray<Integer> array = new DynamicArray<>();

		for(int i = 0; i < 1_000_000; i ++)
			array.add(i);

		for(int i = 0; i < 1_000_000; i ++)
			Assertions.assertEquals(i, array.get(i));
	}

	@Test
	void testRemove(){
		DynamicArray<Integer> array = new DynamicArray<>();

		for(int i = 0; i < 1_000_000; i ++)
			array.add(i);

		for(int i = 0; i < 900_000; i ++)
			array.remove();

		for(int i = 0; i < 100_000; i ++)
			Assertions.assertEquals(i, array.get(i));
	}

}
