package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import java.util.HashSet;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	static final String SLASH = "/";


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
		return (new HashSet<>(Arrays.asList(continuationFlags))).toArray(new String[0]);
	}

}
