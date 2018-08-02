package unit731.hunspeller.languages;

import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


@AllArgsConstructor
@Getter
@Slf4j
public class CorrectnessChecker{

	@NonNull
	protected AffixParser affParser;
	@NonNull
	protected final AbstractHyphenator hyphenator;


	//correctness worker:
	public void checkProduction(RuleProductionEntry production) throws IllegalArgumentException{}

	//minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(RuleProductionEntry production){
		return true;
	}

}
