package unit731.hunspeller.languages;


public class DictionaryBaseData{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 40_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 1. / EXPECTED_NUMBER_OF_ELEMENTS;
	private static final double GROW_RATIO_WHEN_FULL = 1.3;


	private static class SingletonHelper{
		private static final DictionaryBaseData INSTANCE = new DictionaryBaseData();
	}


	protected DictionaryBaseData(){}

	public static synchronized DictionaryBaseData getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public int getExpectedNumberOfElements(){
		return EXPECTED_NUMBER_OF_ELEMENTS;
	}

	public double getFalsePositiveProbability(){
		return FALSE_POSITIVE_PROBABILITY;
	}

	public double getGrowRatioWhenFull(){
		return GROW_RATIO_WHEN_FULL;
	}
	
}
