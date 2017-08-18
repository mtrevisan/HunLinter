package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	/**
	 * Parses the given String into multiple flags
	 *
	 * @param textFlags	String to parse into flags
	 * @return Parsed flags
	 */
	String[] parseRuleFlags(String textFlags);


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param textFlags	Array of String to compose into flags
	 * @return Composed flags
	 */
	String joinRuleFlags(String[] textFlags);

	default String[] removeDuplicates(String[] ruleFlags){
		return (new HashSet<>(Arrays.asList(ruleFlags))).toArray(new String[0]);
	}

}
