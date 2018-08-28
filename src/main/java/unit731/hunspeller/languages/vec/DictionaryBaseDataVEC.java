package unit731.hunspeller.languages.vec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.languages.DictionaryBaseData;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DictionaryBaseDataVEC extends DictionaryBaseData{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 50_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 1. / EXPECTED_NUMBER_OF_ELEMENTS;
	private static final double GROW_RATIO_WHEN_FULL = 1.3;


	private static class SingletonHelper{
		private static final DictionaryBaseData INSTANCE = new DictionaryBaseDataVEC();
	}


	public static synchronized DictionaryBaseData getInstance(){
		return SingletonHelper.INSTANCE;
	}

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
