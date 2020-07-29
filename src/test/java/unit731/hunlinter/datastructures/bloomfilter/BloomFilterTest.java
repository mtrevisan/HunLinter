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
package unit731.hunlinter.datastructures.bloomfilter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


class BloomFilterTest{

	private static final int MAX = 100 * 100;

	private static final double FPP = 0.01;


	@Test
	void defaultFilter(){
		BloomFilterParameters params = new BloomFilterParameters(){
			@Override
			public int getExpectedNumberOfElements(){
				return 10 * MAX;
			}

			@Override
			public double getFalsePositiveProbability(){
				return FPP;
			}
		};
		BloomFilterInterface<String> filter = new BloomFilter<>(StandardCharsets.UTF_8, params);

		//generate two one-million uuid arrays
		List<String> contained = new ArrayList<>();
		List<String> unused = new ArrayList<>();
		for(int index = 0; index < MAX; index ++){
			contained.add(UUID.randomUUID().toString());
			unused.add(UUID.randomUUID().toString());
		}

		//now add to filter
		contained.forEach(filter::add);

		//now start checking
		contained.stream()
			.map(filter::contains)
			.forEach(Assertions::assertTrue);
		int fpp = 0;
		for(String uuid : unused){
			boolean present = filter.contains(uuid);
			if(present){
				//false positive
				Assertions.assertFalse(contained.contains(uuid));
				fpp ++;
			}
		}

		//add another one million more uuids
		List<String> more = new ArrayList<>();
		for(int index = 0; index < MAX; index ++)
			more.add(UUID.randomUUID().toString());
		more.forEach(filter::add);

		//check again
		contained.stream()
			.map(filter::contains)
			.forEach(Assertions::assertTrue);
		for(int index = 0; index < MAX; index ++){
			String uuid = UUID.randomUUID().toString();
			boolean present = filter.contains(uuid);
			if(present){
				// false positive
				Assertions.assertFalse(contained.contains(uuid));
				fpp ++;
			}
		}
		System.out.println("False positives found in two millions: " + fpp);
	}

}
