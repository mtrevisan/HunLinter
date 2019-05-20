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

	private CharsetParsingStrategy(final Charset charset){
		this.charset = charset;
	}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		checkValidity(flags);

		final String[] singleFlags = extractFlags(flags);

		checkForDuplication(singleFlags, flags);

		return singleFlags;
	}

	private void checkValidity(final String flags) throws IllegalArgumentException{
		if(!canEncode(flags))
			throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + flags);
	}

	private String[] extractFlags(final String flags){
		final int size = flags.length();
		final String[] singleFlags = new String[size];
		for(int i = 0; i < size; i ++)
			singleFlags[i] = Character.toString(flags.charAt(i));
		return singleFlags;
	}

	private void checkForDuplication(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		final Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < originalFlags.length())
			throw new IllegalArgumentException("Flags must not be duplicated: " + originalFlags);
	}

	@Override
	public String joinFlags(final String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		final String originalFlags = Arrays.toString(flags);
		checkValidity(flags, originalFlags);

		return String.join(StringUtils.EMPTY, flags);
	}

	private void checkValidity(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		for(final String flag : flags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in " + charset.displayName() + " encoding: " + flag
					+ (flags.length > 1? " in " + originalFlags: StringUtils.EMPTY));
		}
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		checkCompoundValidity(compoundRule);

		//NOTE: same as compoundRule.split(StringUtils.EMPTY) but faster
		final int size = compoundRule.length();
		final String[] result = new String[size];
		for(int i = 0; i < size; i ++)
			result[i] = String.valueOf(compoundRule.charAt(i));
		return result;
	}

	private void checkCompoundValidity(final String compoundRule) throws IllegalArgumentException{
		if(!canEncode(compoundRule))
			throw new IllegalArgumentException("Compound rule must be in " + charset.displayName() + " encoding: " + compoundRule);
	}

	public boolean canEncode(final String cs){
		final CharsetEncoder encoder = charset.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
