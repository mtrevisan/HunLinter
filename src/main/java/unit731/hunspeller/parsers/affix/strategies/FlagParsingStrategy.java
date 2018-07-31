package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	static final String SLASH = "/";

	@AllArgsConstructor
	@Getter
	static enum Type{
		ASCII(null, new ASCIIParsingStrategy()),
		UTF_8("UTF-8", new UTF8ParsingStrategy()),
		LONG("long", new DoubleASCIIParsingStrategy()),
		NUMERIC("num", new NumericalParsingStrategy());

		private final String code;
		private final FlagParsingStrategy stategy;

		public static Type toEnum(String flag){
			Type type = ASCII;
			if(!StringUtils.isBlank(flag)){
				type = null;
				for(Type t : values())
					if(flag.equals(t.getCode())){
						type = t;
						break;
					}
			}
			return type;
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

	default String[] removeDuplicates(String[] continuationFlags){
		Set<String> set = new HashSet<>(Arrays.asList(continuationFlags));
		return set.toArray(new String[set.size()]);
	}

}
