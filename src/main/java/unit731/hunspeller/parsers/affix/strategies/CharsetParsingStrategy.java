package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.SetHelper;


class CharsetParsingStrategy implements FlagParsingStrategy{

	private static final MessageFormat BAD_FORMAT = new MessageFormat("Each flag must be in {0} encoding: was ''{1}''");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule must be in {0} encoding: was ''{1}''");
	private static final MessageFormat FLAG_MUST_BE_OF_LENGTH_ONE = new MessageFormat("Flag must be of length one and in {0} encoding: was ''{1}''");
	private static final MessageFormat DUPLICATED_FLAG = new MessageFormat("Flags must not be duplicated: ''{0}''");


	private final Charset charset;


	private static class SingletonHelperASCII{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.US_ASCII);
	}

	private static class SingletonHelperUTF8{
		private static final CharsetParsingStrategy INSTANCE = new CharsetParsingStrategy(StandardCharsets.UTF_8);
	}

	public static CharsetParsingStrategy getASCIIInstance(){
		return SingletonHelperASCII.INSTANCE;
	}

	public static CharsetParsingStrategy getUTF8Instance(){
		return SingletonHelperUTF8.INSTANCE;
	}

	private CharsetParsingStrategy(final Charset charset){
		this.charset = charset;
	}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		if(!canEncode(flags))
			throw new IllegalArgumentException(BAD_FORMAT.format(new Object[]{charset.displayName(), flags}));

		final String[] singleFlags = extractFlags(flags);

		checkForDuplicates(singleFlags);

		return singleFlags;
	}

	@Override
	public void validate(final String flag) throws IllegalArgumentException{
		if(flag == null || flag.length() != 1 || !canEncode(flag))
			throw new IllegalArgumentException(FLAG_MUST_BE_OF_LENGTH_ONE.format(new Object[]{charset.displayName(), flag}));
	}

	private String[] extractFlags(final String flags){
		final int size = flags.length();
		final String[] singleFlags = new String[size];
		for(int i = 0; i < size; i ++)
			singleFlags[i] = Character.toString(flags.charAt(i));
		return singleFlags;
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

		return String.join(StringUtils.EMPTY, flags);
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
			throw new IllegalArgumentException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{charset.displayName(), compoundRule}));
	}

	public boolean canEncode(final String cs){
		final CharsetEncoder encoder = charset.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
