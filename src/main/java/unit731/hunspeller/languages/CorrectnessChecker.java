package unit731.hunspeller.languages;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessChecker{

	protected AffixParser affParser;
	protected final AbstractHyphenator hyphenator;


	public CorrectnessChecker(AffixParser affParser, AbstractHyphenator hyphenator){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.hyphenator = hyphenator;
	}

	public AffixParser getAffParser(){
		return affParser;
	}

	public AbstractHyphenator getHyphenator(){
		return hyphenator;
	}

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
