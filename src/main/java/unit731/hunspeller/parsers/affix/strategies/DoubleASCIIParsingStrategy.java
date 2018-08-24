package unit731.hunspeller.parsers.affix.strategies;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
public class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern PATTERN = PatternService.pattern("(?<=\\G.{2})");

	private static final Matcher COMPOUND_RULE = PatternService.matcher("\\(..\\)[*?]?");


	@Override
	@SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Deliberate")
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		if(textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + textFlags);

		int size = (textFlags.length() >>> 1);
		String[] flags = PatternService.split(textFlags, PATTERN);
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < size)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Each flag must be of length two: " + flag + " from " + Arrays.toString(textFlags));

		return String.join(StringUtils.EMPTY, textFlags);
	}

	@Override
	public List<String> extractCompoundRule(String compoundRule){
		return PatternService.extract(compoundRule, COMPOUND_RULE);
	}

}
