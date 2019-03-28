package unit731.hunspeller.parsers.hyphenation.hyphenators;

import unit731.hunspeller.parsers.hyphenation.HyphenationParser;



public class HyphenatorFactory{

	public static enum Type{STANDARD, AHO_CORASICK};


	private HyphenatorFactory(){}

	public static HyphenatorInterface createEmptyHyphenator(){
		return EmptyHyphenator.getInstance();
	}

	public static HyphenatorInterface createHyphenator(HyphenationParser hypParser, String breakCharacter){
		if(hypParser.getRadixTreeType() == Type.AHO_CORASICK)
			return createAhoCorasickHyphenator(hypParser, breakCharacter);
		return createStandardHyphenator(hypParser, breakCharacter);
	}

	public static HyphenatorInterface createStandardHyphenator(HyphenationParser hypParser, String breakCharacter){
		return new Hyphenator(hypParser, breakCharacter);
	}

	public static HyphenatorInterface createAhoCorasickHyphenator(HyphenationParser hypParser, String breakCharacter){
		return new AhoCorasickHyphenator(hypParser, breakCharacter);
	}

}
