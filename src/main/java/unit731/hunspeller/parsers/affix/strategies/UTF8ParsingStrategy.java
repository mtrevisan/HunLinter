package unit731.hunspeller.parsers.affix.strategies;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
public class UTF8ParsingStrategy implements FlagParsingStrategy{

	@Override
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return new String[0];

		if(!StandardCharsets.UTF_8.newEncoder().canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in UTF-8 encoding: " + textFlags);

		int size = textFlags.length();
		Set<String> flags = new HashSet<>(size);
		for(int i = 0; i < size; i ++)
			flags.add(Character.toString(textFlags.charAt(i)));
		if(flags.size() < size)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

		return flags.toArray(new String[size]);
	}

	@Override
	public String joinFlags(String[] textFlags){
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
