package unit731.hunspeller.parsers.affix.strategies;

import java.util.HashMap;
import java.util.Map;


public class ParsingStrategyFactory{

	private static final Map<String, FlagParsingStrategy> STRATEGIES = new HashMap<>();
	static{
		STRATEGIES.put(null, ASCIIParsingStrategy.getInstance());
		STRATEGIES.put("UTF-8", UTF8ParsingStrategy.getInstance());
		STRATEGIES.put("long", DoubleASCIIParsingStrategy.getInstance());
		STRATEGIES.put("num", NumericalParsingStrategy.getInstance());
	}


	private ParsingStrategyFactory(){}

	public static FlagParsingStrategy createFromFlag(String flag){
		FlagParsingStrategy strategy = STRATEGIES.get(flag);
		if(strategy == null)
			throw new IllegalArgumentException("Unknown strategy type: " + flag);

		return strategy;
	}

	public static FlagParsingStrategy createASCIIParsingStrategy(){
		return ASCIIParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createDoubleASCIIParsingStrategy(){
		return DoubleASCIIParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createNumericalParsingStrategy(){
		return NumericalParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createUTF8ParsingStrategy(){
		return UTF8ParsingStrategy.getInstance();
	}

}
