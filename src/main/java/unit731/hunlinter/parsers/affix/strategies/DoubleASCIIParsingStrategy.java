package unit731.hunlinter.parsers.affix.strategies;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.RegexHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
class DoubleASCIIParsingStrategy extends FlagParsingStrategy{

	private static final MessageFormat FLAG_MUST_BE_EVEN_IN_LENGTH = new MessageFormat("Flag must be of length multiple of two: ''{0}''");
	private static final MessageFormat FLAG_MUST_BE_OF_LENGTH_TWO = new MessageFormat("Flag must be of length two: ''{0}''");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule must be composed by double-characters flags, or the optional operators '*' or '?: was ''{0}''");

	private static final Pattern PATTERN = RegexHelper.pattern("(?<=\\G.{2})");

	private static final Pattern COMPOUND_RULE_SPLITTER = RegexHelper.pattern("\\((..)\\)|([?*])");

	private static class SingletonHelper{
		private static final DoubleASCIIParsingStrategy INSTANCE = new DoubleASCIIParsingStrategy();
	}


	public static DoubleASCIIParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DoubleASCIIParsingStrategy(){}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		if(flags.length() % 2 != 0)
			throw new LinterException(FLAG_MUST_BE_EVEN_IN_LENGTH.format(new Object[]{flags}));

		final String[] singleFlags = extractFlags(flags);

		checkForDuplicates(singleFlags);

		return singleFlags;
	}

	private String[] extractFlags(final String flags){
		return RegexHelper.split(flags, PATTERN);
	}

	@Override
	public void validate(final String flag){
		if(flag == null || flag.length() != 2)
			throw new LinterException(FLAG_MUST_BE_OF_LENGTH_TWO.format(new Object[]{flag}));
	}

	@Override
	protected String joinFlags(final String[] flags, final int size){
		if(flags == null || size == 0)
			return StringUtils.EMPTY;

		for(int i = 0; i < size; i ++)
			validate(flags[i]);

		return StringUtils.join(flags, StringUtils.EMPTY);
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = RegexHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(final String[] parts, final String compoundRule){
		for(final String part : parts){
			final int size = part.length();
			final boolean isFlag = (size != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
			if(size != 2 && isFlag)
				throw new LinterException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{compoundRule}));
		}
	}

}
