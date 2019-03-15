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
	public String[] parseFlags(String flags){
		if(StringUtils.isBlank(flags))
			return null;

		checkValidity(flags);

		String[] singleFlags = extractFlags(flags);

		checkForDuplication(singleFlags, flags);

		return singleFlags;
	}

	private void checkValidity(String flags) throws IllegalArgumentException{
		if(flags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + flags);
	}

	private String[] extractFlags(String flags){
		return PatternHelper.split(flags, PATTERN);
	}

	private void checkForDuplication(String[] flags, String originalFlags) throws IllegalArgumentException{
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if((unduplicatedFlags.size() << 1) < originalFlags.length())
			throw new IllegalArgumentException("Flags must not be duplicated: " + originalFlags);
	}

	@Override
	public String joinFlags(String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		String originalFlags = Arrays.toString(flags);
		checkValidity(flags, originalFlags);

		return String.join(StringUtils.EMPTY, flags);
	}

	private void checkValidity(String[] flags, String originalFlags) throws IllegalArgumentException{
		for(String flag : flags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Flag must be of length two: " + flag + " from " + originalFlags);
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(String[] parts, String compoundRule) throws IllegalArgumentException{
		for(String part : parts)
			checkCompoundValidity(part, compoundRule);
	}

	private void checkCompoundValidity(String part, String compoundRule) throws IllegalArgumentException{
		int size = part.length();
		boolean isFlag = (size != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
		if(size != 2 && isFlag)
			throw new IllegalArgumentException("Compound rule must be composed by double-characters flags, or the optional operators '*' or '? : "
				+ compoundRule);
	}

}
