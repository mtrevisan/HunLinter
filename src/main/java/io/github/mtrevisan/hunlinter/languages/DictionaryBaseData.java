/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;


public final class DictionaryBaseData extends BloomFilterParameters{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 40_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 1. / EXPECTED_NUMBER_OF_ELEMENTS;
	private static final double GROWTH_RATE_WHEN_FULL = 1.3;


	private static class SingletonHelper{
		private static final DictionaryBaseData INSTANCE = new DictionaryBaseData();
	}


	public static DictionaryBaseData getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DictionaryBaseData(){}

	@Override
	public int getExpectedNumberOfElements(){
		return EXPECTED_NUMBER_OF_ELEMENTS;
	}

	@Override
	public double getFalsePositiveProbability(){
		return FALSE_POSITIVE_PROBABILITY;
	}

	@Override
	public double getGrowthRateWhenFull(){
		return GROWTH_RATE_WHEN_FULL;
	}

}
