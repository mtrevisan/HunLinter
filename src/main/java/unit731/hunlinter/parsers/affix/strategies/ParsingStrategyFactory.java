package unit731.hunlinter.parsers.affix.strategies;

import unit731.hunlinter.parsers.workers.exceptions.HunLintException;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


public class ParsingStrategyFactory{

	private static final MessageFormat UNKNOWN_TYPE = new MessageFormat("Unknown strategy type: {0}");


	private static final Map<String, FlagParsingStrategy> STRATEGIES = new HashMap<>();
	static{
		STRATEGIES.put(null, CharsetParsingStrategy.getASCIIInstance());
		STRATEGIES.put("UTF-8", CharsetParsingStrategy.getUTF8Instance());
		STRATEGIES.put("long", DoubleASCIIParsingStrategy.getInstance());
		STRATEGIES.put("num", NumericalParsingStrategy.getInstance());
	}


	private ParsingStrategyFactory(){}

	public static FlagParsingStrategy createFromFlag(final String flag){
		final FlagParsingStrategy strategy = STRATEGIES.get(flag);
		if(strategy == null)
			throw new HunLintException(UNKNOWN_TYPE.format(new Object[]{flag}));

		return strategy;
	}

	public static FlagParsingStrategy createASCIIParsingStrategy(){
		return CharsetParsingStrategy.getASCIIInstance();
	}

	public static FlagParsingStrategy createDoubleASCIIParsingStrategy(){
		return DoubleASCIIParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createNumericalParsingStrategy(){
		return NumericalParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createUTF8ParsingStrategy(){
		return CharsetParsingStrategy.getUTF8Instance();
	}

}
