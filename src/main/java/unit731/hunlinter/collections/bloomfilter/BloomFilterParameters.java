package unit731.hunlinter.collections.bloomfilter;

import unit731.hunlinter.collections.bloomfilter.core.BitArrayBuilder;

import java.text.MessageFormat;


public abstract class BloomFilterParameters{

	private static final MessageFormat INVALID_NUMBER_OF_ELEMENTS = new MessageFormat("Number of elements must be strict positive");
	private static final MessageFormat INVALID_FALSE_POSITIVE_PROBABILITY = new MessageFormat("False positive probability must be in ]0, 1[ interval");
	private static final MessageFormat INVALID_GROW_RATIO = new MessageFormat("Grow ratio when full must be strictly greater than one");
	private static final MessageFormat INVALID_TIGHTENING_RATIO = new MessageFormat("Tightening ratio must be in the interval ]0, 1[");
	private static final MessageFormat INVALID_BIT_ARRAY_TYPE = new MessageFormat("Bit array type must be valued");

	public static final double GROW_RATIO_WHEN_FULL_DEFAULT = 2.;
	public static final double TIGHTENING_RATIO_DEFAULT = 0.85;
	public static final BitArrayBuilder.Type BIT_ARRAY_TYPE_DEFAULT = BitArrayBuilder.Type.JAVA;


	/**
	 * Expected (maximum) number of elements to be added without transcending the <code>falsePositiveProbability</code>
	 *
	 * @return	The expected number of elements
	 */
	public abstract int getExpectedNumberOfElements();

	/**
	 * The maximum false positive probability rate that the bloom filter can give
	 *
	 * @return	The false positive probability
	 */
	public abstract double getFalsePositiveProbability();

	/**
	 * Defaults to 2
	 *
	 * @return	The grow ratio when the filter is full
	 */
	public double getGrowRatioWhenFull(){
		return GROW_RATIO_WHEN_FULL_DEFAULT;
	}

	/**
	 * Defaults to 0.85
	 *
	 * @return	The tightening ratio
	 */
	public double getTighteningRatio(){
		return TIGHTENING_RATIO_DEFAULT;
	}

	/**
	 * Defaults to {@link BitArrayBuilder.Type#JAVA}
	 *
	 * @return	The bit array type
	 */
	public BitArrayBuilder.Type getBitArrayType(){
		return BIT_ARRAY_TYPE_DEFAULT;
	}

	public void validate(){
		if(getExpectedNumberOfElements() <= 0)
			throw new IllegalArgumentException(INVALID_NUMBER_OF_ELEMENTS.format(new Object[0]));
		if(getFalsePositiveProbability() <= 0. || getFalsePositiveProbability() >= 1.)
			throw new IllegalArgumentException(INVALID_FALSE_POSITIVE_PROBABILITY.format(new Object[0]));
		if(getGrowRatioWhenFull() <= 1.)
			throw new IllegalArgumentException(INVALID_GROW_RATIO.format(new Object[0]));
		if(getTighteningRatio() <= 0. && getTighteningRatio() >= 1.)
			throw new IllegalArgumentException(INVALID_TIGHTENING_RATIO.format(new Object[0]));
		if(getBitArrayType() == null)
			throw new IllegalArgumentException(INVALID_BIT_ARRAY_TYPE.format(new Object[0]));
	}

}
