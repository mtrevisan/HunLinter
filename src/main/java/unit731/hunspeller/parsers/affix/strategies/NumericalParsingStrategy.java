package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.services.PatternHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final int MAX_NUMERICAL_FLAG = 65_000;

	private static final String COMMA = ",";

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((\\d+)\\)|([?*])");

	private static class SingletonHelper{
		private static final NumericalParsingStrategy INSTANCE = new NumericalParsingStrategy();
	}


	public static synchronized NumericalParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private NumericalParsingStrategy(){}

	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		//extract flags
		String[] flags = StringUtils.split(textFlags, COMMA);

		checkForDuplication(flags, textFlags);

		checkForBounds(flags, textFlags);

		return flags;
	}

	private void checkForDuplication(String[] flags, String textFlags) throws IllegalArgumentException{
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < flags.length)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);
	}

	private void checkForBounds(String[] flags, String textFlags) throws IllegalArgumentException{
		for(String flag : flags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from " + textFlags);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Flag must be an integer number: " + flag + " from " + textFlags);
			}
		}
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		checkJoinValidity(textFlags);

		return String.join(COMMA, textFlags);
	}

	private void checkJoinValidity(String[] textFlags) throws IllegalArgumentException{
		for(String flag : textFlags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from "
						+ Arrays.deepToString(textFlags));
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Each flag must be an integer number: " + flag + " from " + Arrays.deepToString(textFlags));
			}
		}
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(String[] parts, String compoundRule) throws IllegalArgumentException{
		for(String part : parts)
			if((part.length() != 1 || part.charAt(0) != '*' && part.charAt(0) != '?') && !NumberUtils.isCreatable(part))
				throw new IllegalArgumentException("Compound rule must be composed by numbers and the optional operators '*' and '?': "
					+ compoundRule);
	}

}
