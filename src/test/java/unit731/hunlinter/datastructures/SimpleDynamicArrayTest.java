/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.datastructures;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.datastructures.dynamicarray.DynamicArray;


class SimpleDynamicArrayTest{

	@Test
	void add(){
		DynamicArray<Integer> array = new DynamicArray<>();

		for(int i = 0; i < 1_000_000; i ++)
			array.add(i);

		for(int i = 0; i < 1_000_000; i ++)
			Assertions.assertEquals(i, array.get(i));
	}

	@Test
	void remove(){
		DynamicArray<Integer> array = new DynamicArray<>();

		for(int i = 0; i < 1_000_000; i ++)
			array.add(i);

		for(int i = 0; i < 900_000; i ++)
			array.remove();

		for(int i = 0; i < 100_000; i ++)
			Assertions.assertEquals(i, array.get(i));
	}

}
