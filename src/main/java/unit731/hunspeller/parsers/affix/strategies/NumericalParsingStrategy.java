package unit731.hunspeller.parsers.affix.strategies;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
public class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final int MAX_NUMERICAL_FLAG = 65_000;

	private static final String COMMA = ",";

	private static final Matcher COMPOUND_RULE = PatternService.matcher("\\(\\d+\\)[*?]?");


	@Override
	@SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Deliberate")
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		String[] flags = StringUtils.split(textFlags, COMMA);
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < flags.length)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

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
		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		for(String flag : textFlags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from " + Arrays.deepToString(textFlags));
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Each flag must be an integer number: " + flag + " from " + Arrays.deepToString(textFlags));
			}
		}

		return String.join(COMMA, textFlags);
	}

	@Override
	public List<String> extractCompoundRule(String compoundRule){
		return PatternService.extract(compoundRule, COMPOUND_RULE);
	}

}
