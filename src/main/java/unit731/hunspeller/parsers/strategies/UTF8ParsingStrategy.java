package unit731.hunspeller.parsers.strategies;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
public class UTF8ParsingStrategy implements FlagParsingStrategy{

	@Override
	public String[] parseRuleFlags(String textFlags){
		if(textFlags == null)
			return new String[0];

		if(!StandardCharsets.UTF_8.newEncoder().canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in UTF-8 encoding");

		String[] flags = (!textFlags.isEmpty()? removeDuplicates(textFlags.split(StringUtils.EMPTY)): new String[0]);
		for(String flag : flags)
			if(StringUtils.isBlank(flag))
				throw new IllegalArgumentException("Flag must be a valid UTF-8 character");
		return flags;
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!StandardCharsets.UTF_8.newEncoder().canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in UTF-8 encoding");
		}

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
