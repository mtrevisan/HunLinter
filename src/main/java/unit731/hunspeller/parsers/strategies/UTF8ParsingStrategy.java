package unit731.hunspeller.parsers.strategies;

import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 * Can be used with both the ASCII and UTF-8 flag types.
 */
public class UTF8ParsingStrategy implements FlagParsingStrategy{

	private static final String SLASH = "/";


	@Override
	public String[] parseRuleFlags(String textFlags){
		String[] flags = (textFlags != null && !textFlags.isEmpty()? removeDuplicates(textFlags.split(StringUtils.EMPTY)): new String[0]);
		for(String flag : flags)
			if(StringUtils.isBlank(flag))
				throw new IllegalArgumentException("Flag must be a valid UTF-8 character");
		return flags;
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags)
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length two");

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
