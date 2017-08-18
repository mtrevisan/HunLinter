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
		return (textFlags != null && !textFlags.isEmpty()? removeDuplicates(textFlags.split(StringUtils.EMPTY)): new String[0]);
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
