package unit731.hunspeller.parsers.hyphenation.hyphenators;

import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;


public class EmptyHyphenator extends AbstractHyphenator{

	private static class SingletonHelper{
		private static final EmptyHyphenator INSTANCE = new EmptyHyphenator();
	}


	public static EmptyHyphenator getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private EmptyHyphenator(){
		super(new HyphenationParser("xx"), HyphenationParser.SOFT_HYPHEN);
	}

	private EmptyHyphenator(HyphenationParser hypParser, String breakCharacter){
		super(hypParser, breakCharacter);
	}

	@Override
	protected HyphenationBreak calculateBreakpoints(String word, RadixTree<String, String> patterns, HyphenationOptions options){
		return HyphenationBreak.getEmptyInstance();
	}
	
}
