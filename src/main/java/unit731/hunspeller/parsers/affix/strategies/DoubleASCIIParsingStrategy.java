package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern PATTERN = PatternHelper.pattern("(?<=\\G.{2})");

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((..)\\)|([?*])");

	private static class SingletonHelper{
		private static final DoubleASCIIParsingStrategy INSTANCE = new DoubleASCIIParsingStrategy();
	}


	public static synchronized DoubleASCIIParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DoubleASCIIParsingStrategy(){}

	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		if(textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + textFlags);

		String[] flags = extractFlags(textFlags);

		checkForDuplication(flags, textFlags);

		return flags;
	}

	private String[] extractFlags(String textFlags){
		return PatternHelper.split(textFlags, PATTERN);
	}

	private void checkForDuplication(String[] flags, String textFlags) throws IllegalArgumentException{
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if((unduplicatedFlags.size() << 1) < textFlags.length())
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		checkValidity(textFlags);

		return String.join(StringUtils.EMPTY, textFlags);
	}

	private void checkValidity(String[] textFlags) throws IllegalArgumentException{
		for(String flag : textFlags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Each flag must be of length two: " + flag + " from " + Arrays.toString(textFlags));
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		//check compound validity
		for(String part : parts){
			int size = part.length();
			if(size != 2 && (size != 1 || part.charAt(0) != '*' && part.charAt(0) != '?'))
				throw new IllegalArgumentException("Compound rule must be composed by double-characters flags, or the optional operators '*' or '? : " + compoundRule);
		}

		return parts;
	}

}
