package unit731.hunspeller.parsers.hyphenation.hyphenators;

import unit731.hunspeller.parsers.hyphenation.HyphenationParser;



public class HyphenatorFactory{

	private HyphenatorFactory(){}

	public static HyphenatorInterface createEmptyHyphenator(){
		return EmptyHyphenator.getInstance();
	}

	public static HyphenatorInterface createHyphenator(HyphenationParser hypParser, String breakCharacter){
		return new Hyphenator(hypParser, breakCharacter);
	}

	public static HyphenatorInterface createAhoCorasickHyphenator(HyphenationParser hypParser, String breakCharacter){
		return new AhoCorasickHyphenator(hypParser, breakCharacter);
	}

}
