package unit731.hunspeller.parsers.affix.strategies;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	/**
	 * Parses the given String into multiple flags
	 *
	 * @param flags	String to parse into flags
	 * @return Parsed flags
	 */
	String[] parseFlags(final String flags);


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param flags	Array of String to compose into flags
	 * @return Composed flags
	 */
	String joinFlags(final String[] flags);

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a*", "b", "c?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	String[] extractCompoundRule(final String compoundRule);

}
