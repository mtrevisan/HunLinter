package unit731.hunspeller.parsers.strategies;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
public class UTF8ParsingStrategy implements FlagParsingStrategy{

	@Override
	public String[] parseFlags(String textFlags){
		if(Objects.isNull(textFlags))
			return new String[0];

		if(!StandardCharsets.UTF_8.newEncoder().canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in UTF-8 encoding: " + textFlags);

		String[] flags = (!textFlags.isEmpty()? removeDuplicates(textFlags.split(StringUtils.EMPTY)): new String[0]);
		for(String flag : flags)
			if(StringUtils.isBlank(flag))
				throw new IllegalArgumentException("Flag must be a valid UTF-8 character: " + flag + " from " + textFlags);
		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(Objects.isNull(textFlags) || textFlags.length == 0)
			return StringUtils.EMPTY;

		for(String flag : textFlags){
			if(Objects.isNull(flag) || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!StandardCharsets.UTF_8.newEncoder().canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in UTF-8 encoding");
		}

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
