package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
public class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern REGEX_PATTERN_DOUBLE = PatternService.pattern("(?<=\\G.{2})");


	@Override
	public String[] parseFlags(String textFlags){
		if(Objects.nonNull(textFlags) && textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + textFlags);

		return (Objects.nonNull(textFlags) && !textFlags.isEmpty()? removeDuplicates(PatternService.split(textFlags, REGEX_PATTERN_DOUBLE)):
			new String[0]);
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(Objects.isNull(textFlags) || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags)
			if(Objects.isNull(flag) || flag.length() != 2)
				throw new IllegalArgumentException("Each flag must be of length two: " + flag + " from " + Arrays.toString(textFlags));

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
