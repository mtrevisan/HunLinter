package unit731.hunspeller.languages;

import unit731.hunspeller.collections.bloomfilter.BloomFilterParameters;


public class DictionaryBaseData extends BloomFilterParameters{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 40_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 1. / EXPECTED_NUMBER_OF_ELEMENTS;
	private static final double GROW_RATIO_WHEN_FULL = 1.3;


	private static class SingletonHelper{
		private static final DictionaryBaseData INSTANCE = new DictionaryBaseData();
	}


	public static DictionaryBaseData getInstance(){
		return SingletonHelper.INSTANCE;
	}

	protected DictionaryBaseData(){}

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
