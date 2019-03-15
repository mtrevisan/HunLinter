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
	public String[] parseFlags(String flags){
		if(StringUtils.isBlank(flags))
			return null;

		checkValidity(flags);

		String[] singleFlags = extractFlags(flags);

		checkForDuplication(singleFlags, flags);

		return singleFlags;
	}

	private void checkValidity(String flags) throws IllegalArgumentException{
		if(!canEncode(flags))
			throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + flags);
	}

	private String[] extractFlags(String flags){
		int size = flags.length();
		String[] singleFlags = new String[size];
		for(int i = 0; i < size; i ++)
			singleFlags[i] = Character.toString(flags.charAt(i));
		return singleFlags;
	}

	private void checkForDuplication(String[] flags, String originalFlags) throws IllegalArgumentException{
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < originalFlags.length())
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
		for(String flag : flags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + flag
					+ (flags.length > 1? " in " + originalFlags: StringUtils.EMPTY));
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
