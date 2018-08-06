package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.services.PatternService;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
public class UTF8ParsingStrategy implements FlagParsingStrategy{

	private static final Matcher COMPOUND_RULE = PatternService.matcher(".[*?]?");


	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return new String[0];

		if(!StandardCharsets.UTF_8.newEncoder().canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in UTF-8 encoding: " + textFlags);

		int size = textFlags.length();
		String[] flags = new String[size];
		for(int i = 0; i < size; i ++)
			flags[i] = Character.toString(textFlags.charAt(i));
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < size)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		for(String flag : textFlags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!StandardCharsets.UTF_8.newEncoder().canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in UTF-8 encoding");
		}

		return AffixEntry.SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

	@Override
	public List<String> extractCompoundRule(String compoundRule){
		if(!StandardCharsets.UTF_8.newEncoder().canEncode(compoundRule))
			throw new IllegalArgumentException("Compound rule must be in UTF-8 encoding: " + compoundRule);

		return PatternService.extract(compoundRule, COMPOUND_RULE);
	}

}
