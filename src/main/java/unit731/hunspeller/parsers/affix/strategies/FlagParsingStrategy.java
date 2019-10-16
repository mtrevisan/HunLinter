package unit731.hunspeller.parsers.affix.strategies;

import unit731.hunspeller.services.SetHelper;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public abstract class FlagParsingStrategy{

	private static final MessageFormat DUPLICATED_FLAG = new MessageFormat("Flags must not be duplicated: {0}");


	public abstract void validate(final String flag) throws IllegalArgumentException;

	/**
	 * Parses the given String into multiple flags
	 *
	 * @param flags	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] parseFlags(final String flags);

	protected void checkForDuplicates(final String[] flags) throws IllegalArgumentException{
		final Set<String> notDuplicatedFlags = SetHelper.setOf(flags);
		if(notDuplicatedFlags.size() < flags.length)
			throw new IllegalArgumentException(DUPLICATED_FLAG.format(new Object[]{Arrays.toString(flags)}));
	}


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param flags	Array of String to compose into flags
	 * @return Composed flags
	 */
	public abstract String joinFlags(final String[] flags);

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a*", "b", "c?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] extractCompoundRule(final String compoundRule);

}
