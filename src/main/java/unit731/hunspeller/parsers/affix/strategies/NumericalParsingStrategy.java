package unit731.hunspeller.parsers.affix.strategies;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.StringHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
class NumericalParsingStrategy extends FlagParsingStrategy{

	private static final MessageFormat FLAG_MUST_BE_IN_RANGE = new MessageFormat("Flag must be in the range [1, {0}]: was ''{1}''");
	private static final MessageFormat BAD_FORMAT = new MessageFormat("Flag must be an integer number: was ''{0}''");
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

		Arrays.stream(singleFlags)
			.forEach(this::validate);

		return singleFlags;
	}

	private String[] extractFlags(final String flags){
		return StringUtils.split(flags, COMMA);
	}

	@Override
	public void validate(final String flag){
		try{
			final int numericalFlag = Integer.parseInt(flag);
			if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
				throw new HunspellException(FLAG_MUST_BE_IN_RANGE.format(new Object[]{MAX_NUMERICAL_FLAG, flag}));
		}
		catch(final NumberFormatException e){
			throw new HunspellException(BAD_FORMAT.format(new Object[]{flag}));
		}
	}

	@Override
	public String joinFlags(final String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		Arrays.stream(flags)
			.forEach(this::validate);

		return StringHelper.join(COMMA, flags);
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(final String[] parts, final String compoundRule){
		for(final String part : parts){
			final boolean isNumber = (part.length() != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
			if(isNumber && !NumberUtils.isCreatable(part))
				throw new HunspellException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{compoundRule}));
		}
	}

}
