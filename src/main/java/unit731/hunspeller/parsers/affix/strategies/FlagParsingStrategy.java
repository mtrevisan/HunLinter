package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	static enum Type{
		ASCII(null, new ASCIIParsingStrategy()),
		UTF_8("UTF-8", new UTF8ParsingStrategy()),
		LONG("long", new DoubleASCIIParsingStrategy()),
		NUMERIC("num", new NumericalParsingStrategy());

		private final String code;
		private final FlagParsingStrategy stategy;

		Type(String code, FlagParsingStrategy stategy){
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

	};


	/**
	 * Parses the given String into multiple flags
	 *
	 * @param textFlags	String to parse into flags
	 * @return Parsed flags
	 */
	String[] parseFlags(String textFlags);


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param textFlags	Array of String to compose into flags
	 * @return Composed flags
	 */
	String joinFlags(String[] textFlags);

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a*", "b", "c?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	String[] extractCompoundRule(String compoundRule);

}
