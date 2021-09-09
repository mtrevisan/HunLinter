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
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.core.BitArrayBuilder;


public abstract class BloomFilterParameters{

	private static final String INVALID_NUMBER_OF_ELEMENTS = "Number of elements must be strict positive";
	private static final String INVALID_FALSE_POSITIVE_PROBABILITY = "False positive probability must be in ]0, 1[ interval";
	private static final String INVALID_GROW_RATIO = "Grow ratio when full must be strictly greater than one";
	private static final String INVALID_TIGHTENING_RATIO = "Tightening ratio must be in the interval ]0, 1[";
	private static final String INVALID_BIT_ARRAY_TYPE = "Bit array type must be valued";

	public static final double GROWTH_RATE_WHEN_FULL_DEFAULT = 2.;
	public static final double TIGHTENING_RATIO_DEFAULT = 0.85;
	public static final BitArrayBuilder.Type BIT_ARRAY_TYPE_DEFAULT = BitArrayBuilder.Type.JAVA;


	/**
	 * Expected (maximum) number of elements to be added without transcending the {@code falsePositiveProbability}.
	 *
	 * @return	The expected number of elements.
	 */
	public abstract int getExpectedNumberOfElements();

	/**
	 * The maximum false positive probability rate that the bloom filter can give.
	 *
	 * @return	The false positive probability.
	 */
	public abstract double getFalsePositiveProbability();

	/**
	 * Defaults to 2.
	 *
	 * @return	The growth rate when the filter is full.
	 */
	@SuppressWarnings("DesignForExtension")
	public double getGrowthRateWhenFull(){
		return GROWTH_RATE_WHEN_FULL_DEFAULT;
	}

	/**
	 * Defaults to 0.85.
	 *
	 * @return	The tightening ratio.
	 */
	public double getTighteningRatio(){
		return TIGHTENING_RATIO_DEFAULT;
	}

	/**
	 * Defaults to {@link BitArrayBuilder.Type#JAVA}.
	 *
	 * @return	The bit array type.
	 */
	public BitArrayBuilder.Type getBitArrayType(){
		return BIT_ARRAY_TYPE_DEFAULT;
	}

	public final void validate(){
		if(getExpectedNumberOfElements() <= 0)
			throw new IllegalArgumentException(INVALID_NUMBER_OF_ELEMENTS);
		if(getFalsePositiveProbability() <= 0. || getFalsePositiveProbability() >= 1.)
			throw new IllegalArgumentException(INVALID_FALSE_POSITIVE_PROBABILITY);
		if(getGrowthRateWhenFull() <= 1.)
			throw new IllegalArgumentException(INVALID_GROW_RATIO);
		if(getTighteningRatio() <= 0. || getTighteningRatio() >= 1.)
			throw new IllegalArgumentException(INVALID_TIGHTENING_RATIO);
		if(getBitArrayType() == null)
			throw new IllegalArgumentException(INVALID_BIT_ARRAY_TYPE);
	}

}
