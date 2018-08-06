package unit731.hunspeller.languages;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


@AllArgsConstructor
@Getter
public class CorrectnessChecker{

	private static final int EXPECTED_NUMBER_OF_ELEMENTS = 40_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY = 0.000_000_01;
	private static final double GROW_RATIO_WHEN_FULL = 1.3;


	@NonNull
	protected AffixParser affParser;
	@NonNull
	protected final AbstractHyphenator hyphenator;


	public int getExpectedNumberOfElements(){
		return EXPECTED_NUMBER_OF_ELEMENTS;
	}

	public double getFalsePositiveProbability(){
		return FALSE_POSITIVE_PROBABILITY;
	}

	public double getGrowRatioWhenFull(){
		return GROW_RATIO_WHEN_FULL;
	}

	//correctness worker:
	public void checkProduction(Production production) throws IllegalArgumentException{}

	//minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(Production production){
		return true;
	}

}
