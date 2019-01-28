package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;


class CharsetParsingStrategy implements FlagParsingStrategy{

	private final Charset charset;


	private static class SingletonHelperASCII{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.US_ASCII);
	}

	private static class SingletonHelperUTF8{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.UTF_8);
	}

	public static synchronized CharsetParsingStrategy getASCIIInstance(){
		return SingletonHelperASCII.INSTANCE;
	}

	public static synchronized CharsetParsingStrategy getUTF8Instance(){
		return SingletonHelperUTF8.INSTANCE;
	}

	private CharsetParsingStrategy(Charset charset){
		this.charset = charset;
	}

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
		if(!canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + textFlags);
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
			if(!canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + flag
					+ (textFlags.length > 1? " in " + String.join(",", textFlags): StringUtils.EMPTY));
		}
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		checkCompoundValidity(compoundRule);

		return compoundRule.split(StringUtils.EMPTY);
	}

	private void checkCompoundValidity(String compoundRule) throws IllegalArgumentException{
		if(!canEncode(compoundRule))
			throw new IllegalArgumentException("Compound rule must be in " + charset.displayName() + " encoding: " + compoundRule);
	}

	public boolean canEncode(String cs){
		CharsetEncoder encoder = charset.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
