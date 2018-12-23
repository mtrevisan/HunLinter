package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;


public class ParsingStrategyFactory{

	private static enum Type{
		ASCII(null, ASCIIParsingStrategy.getInstance()),
		UTF_8("UTF-8", UTF8ParsingStrategy.getInstance()),
		LONG("long", DoubleASCIIParsingStrategy.getInstance()),
		NUMERIC("num", NumericalParsingStrategy.getInstance());

		private final String code;
		private final FlagParsingStrategy stategy;

		private Type(String code, FlagParsingStrategy stategy){
			this.code = code;
			this.stategy = stategy;
		}

		public static Type toEnum(String flag){
			Type type = ASCII;
			if(!StringUtils.isBlank(flag))
				type = Arrays.stream(values())
					.filter(tag -> flag.equals(tag.code))
					.findFirst()
					.orElse(ASCII);
			return type;
		}

		public FlagParsingStrategy getStategy(){
			return stategy;
		}

	}


	private ParsingStrategyFactory(){}

	public static FlagParsingStrategy createFromFlag(String flag){
		return ParsingStrategyFactory.Type.toEnum(flag).getStategy();
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
