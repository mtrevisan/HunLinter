package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;


class CharsetParsingStrategy extends FlagParsingStrategy{

	private static final MessageFormat BAD_FORMAT = new MessageFormat("Each flag must be in {0} encoding: was ''{1}''");
	private static final MessageFormat BAD_FORMAT_COMPOUND_RULE = new MessageFormat("Compound rule must be in {0} encoding: was ''{1}''");
	private static final MessageFormat FLAG_MUST_BE_OF_LENGTH_ONE = new MessageFormat("Flag must be of length one and in {0} encoding: was ''{1}''");


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
			throw new HunspellException(BAD_FORMAT.format(new Object[]{charset.displayName(), flags}));

		final String[] singleFlags = extractFlags(flags);

		checkForDuplicates(singleFlags);

		return singleFlags;
	}

	private String[] extractFlags(final String flags){
		return IntStream.range(0, flags.length())
			.mapToObj(i -> Character.toString(flags.charAt(i)))
			.toArray(String[]::new);
	}

	@Override
	public void validate(final String flag){
		if(flag == null || flag.length() != 1 || !canEncode(flag))
			throw new HunspellException(FLAG_MUST_BE_OF_LENGTH_ONE.format(new Object[]{charset.displayName(), flag}));
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

	private void checkCompoundValidity(final String compoundRule){
		if(!canEncode(compoundRule))
			throw new HunspellException(BAD_FORMAT_COMPOUND_RULE.format(new Object[]{charset.displayName(), compoundRule}));
	}

	public boolean canEncode(final String cs){
		final CharsetEncoder encoder = charset.newEncoder();
		//NOTE: encoder.canEncode is not thread-safe!
		return encoder.canEncode(cs);
	}

}
