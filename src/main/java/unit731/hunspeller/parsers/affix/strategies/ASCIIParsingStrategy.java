package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
class ASCIIParsingStrategy implements FlagParsingStrategy{

	private static final CharsetEncoder ASCII_ENCODER = StandardCharsets.US_ASCII.newEncoder();

	private static class SingletonHelper{
		private static final ASCIIParsingStrategy INSTANCE = new ASCIIParsingStrategy();
	}


	public static synchronized ASCIIParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private ASCIIParsingStrategy(){}

	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		checkValidity(textFlags);

		String[] flags = extractFlags(textFlags);

		checkForDuplication(flags, textFlags);

		return flags;
	}

	private void checkValidity(String textFlags) throws IllegalArgumentException{
		if(!ASCII_ENCODER.canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in ASCII encoding: " + textFlags);
	}

	private String[] extractFlags(String textFlags){
		int size = textFlags.length();
		String[] flags = new String[size];
		for(int i = 0; i < size; i ++)
			flags[i] = Character.toString(textFlags.charAt(i));
		return flags;
	}

	private void checkForDuplication(String[] flags, String textFlags) throws IllegalArgumentException{
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < textFlags.length())
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		checkJoinValidity(textFlags);

		return String.join(StringUtils.EMPTY, textFlags);
	}

	private void checkJoinValidity(String[] textFlags) throws IllegalArgumentException{
		for(String flag : textFlags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!ASCII_ENCODER.canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in ASCII encoding");
		}
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		checkCompoundValidity(compoundRule);

		return compoundRule.split(StringUtils.EMPTY);
	}

	private void checkCompoundValidity(String compoundRule) throws IllegalArgumentException{
		if(!ASCII_ENCODER.canEncode(compoundRule))
			throw new IllegalArgumentException("Compound rule must be in ASCII encoding: " + compoundRule);
	}

}
