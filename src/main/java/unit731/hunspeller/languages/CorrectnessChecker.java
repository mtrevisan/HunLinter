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

	@NonNull
	protected AffixParser affParser;
	@NonNull
	protected final AbstractHyphenator hyphenator;


	//correctness worker:
	public void checkProduction(Production production) throws IllegalArgumentException{
		String forbidCompoundFlag = affParser.getForbidCompoundFlag();
		if(!production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag))
			throw new IllegalArgumentException("Non-affix entry contains COMPOUNDFORBIDFLAG");
	}

	//minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(Production production){
		return true;
	}

}
