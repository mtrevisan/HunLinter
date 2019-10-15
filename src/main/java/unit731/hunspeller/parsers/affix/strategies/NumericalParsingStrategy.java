package unit731.hunspeller.parsers.affix.strategies;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final MessageFormat FLAG_MUST_BE_IN_RANGE = new MessageFormat("Flag must be in the range [1, {0}]: was ''{1}''");
	private static final MessageFormat BAD_FORMAT = new MessageFormat("Flag must be an integer number: was ''{0}''");
	private static final MessageFormat DUPLICATED_FLAG = new MessageFormat("Flags must not be duplicated: ''{0}''");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule must be composed by numbers and the optional operators '*' and '?': was ''{0}''");


	private static final int MAX_NUMERICAL_FLAG = 65_000;

	private static final String COMMA = ",";

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((\\d+)\\)|([?*])");

	private static class SingletonHelper{
		private static final NumericalParsingStrategy INSTANCE = new NumericalParsingStrategy();
	}


	public static NumericalParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private NumericalParsingStrategy(){}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		final String[] singleFlags = extractFlags(flags);

		checkForDuplicates(singleFlags);

		for(final String flag : singleFlags)
			validate(flag);

		return singleFlags;
	}

	@Override
	public void validate(final String flag) throws IllegalArgumentException{
		try{
			final int numericalFlag = Integer.parseInt(flag);
			if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
				throw new IllegalArgumentException(FLAG_MUST_BE_IN_RANGE.format(new Object[]{MAX_NUMERICAL_FLAG, flag}));
		}
		catch(final NumberFormatException e){
			throw new IllegalArgumentException(BAD_FORMAT.format(new Object[]{flag}));
		}
	}

	private String[] extractFlags(final String flags){
		return StringUtils.split(flags, COMMA);
	}

	private void checkForDuplicates(final String[] flags) throws IllegalArgumentException{
		final Set<String> notDuplicatedFlags = SetHelper.setOf(flags);
		if(notDuplicatedFlags.size() < flags.length)
			throw new IllegalArgumentException(DUPLICATED_FLAG.format(new Object[]{Arrays.toString(flags)}));
	}

	@Override
	public String joinFlags(final String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		for(final String flag : flags)
			validate(flag);

		return String.join(COMMA, flags);
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(final String[] parts, final String compoundRule) throws IllegalArgumentException{
		for(final String part : parts)
			checkCompoundValidity(part, compoundRule);
	}

	private void checkCompoundValidity(final String part, final String compoundRule) throws IllegalArgumentException{
		final boolean isNumber = (part.length() != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
		if(isNumber && !NumberUtils.isCreatable(part))
			throw new IllegalArgumentException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{compoundRule}));
	}

}
