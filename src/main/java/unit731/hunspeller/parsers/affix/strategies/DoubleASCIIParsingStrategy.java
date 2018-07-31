package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
public class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern PATTERN = PatternService.pattern("(?<=\\G.{2})");


	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return new String[0];

		if(textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + textFlags);

		int size = (textFlags.length() >>> 1);
		Set<String> flags = new HashSet<>(size);
		flags.addAll(Arrays.asList(PatternService.split(textFlags, PATTERN)));
		if(flags.size() < size)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

		return flags.toArray(new String[size]);
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Each flag must be of length two: " + flag + " from " + Arrays.toString(textFlags));

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
