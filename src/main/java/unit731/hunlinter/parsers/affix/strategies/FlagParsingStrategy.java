package unit731.hunlinter.parsers.affix.strategies;

import unit731.hunlinter.services.GrowableArray;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.SetHelper;

import java.text.MessageFormat;
import java.util.Set;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public abstract class FlagParsingStrategy{

	private static final MessageFormat DUPLICATED_FLAG = new MessageFormat("Flags must not be duplicated: {0}");


	public abstract void validate(final String flag);

	/**
	 * Parses the given String into multiple flags
	 *
	 * @param flags	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] parseFlags(final String flags);

	protected void checkForDuplicates(final String[] flags){
		final Set<String> notDuplicatedFlags = SetHelper.setOf(flags);
		if(notDuplicatedFlags.size() < flags.length){
			final Set<String> duplicates = SetHelper.getDuplicates(flags);
			throw new LinterException(DUPLICATED_FLAG.format(new Object[]{String.join(", ", duplicates)}));
		}
	}


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param flags	Array of String to compose into flags
	 * @return Composed flags
	 */
	public String joinFlags(final String[] flags){
		return joinFlags(flags, (flags != null? flags.length: 0));
	}

	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param flags	GrowableArray of String to compose into flags
	 * @return Composed flags
	 */
	public String joinFlags(final GrowableArray<String> flags){
		return joinFlags(flags.data, flags.limit);
	}

	protected abstract String joinFlags(final String[] flags, final int size);

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a*", "b", "c?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	public abstract String[] extractCompoundRule(final String compoundRule);

}
