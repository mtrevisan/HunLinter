package unit731.hunspeller.parsers.affix.strategies;

import java.util.HashMap;
import java.util.Map;


public class ParsingStrategyFactory{

	private static final Map<String, FlagParsingStrategy> STRATEGIES = new HashMap<>();
	static{
		STRATEGIES.put(null, CharsetParsingStrategy.getASCIIInstance());
		STRATEGIES.put("UTF-8", CharsetParsingStrategy.getUTF8Instance());
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
