package unit731.hunlinter.languages.vec;

import unit731.hunlinter.collections.bloomfilter.BloomFilterParameters;


public class DictionaryBaseDataVEC extends BloomFilterParameters{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 50_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 1. / EXPECTED_NUMBER_OF_ELEMENTS;
	private static final double GROW_RATIO_WHEN_FULL = 1.3;


	private static class SingletonHelper{
		private static final DictionaryBaseDataVEC INSTANCE = new DictionaryBaseDataVEC();
	}


	public static DictionaryBaseDataVEC getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DictionaryBaseDataVEC(){}

	@Override
	public int getExpectedNumberOfElements(){
		return EXPECTED_NUMBER_OF_ELEMENTS;
	}

	@Override
	public double getFalsePositiveProbability(){
		return FALSE_POSITIVE_PROBABILITY;
	}

	@Override
	public double getGrowRatioWhenFull(){
		return GROW_RATIO_WHEN_FULL;
	}

}
