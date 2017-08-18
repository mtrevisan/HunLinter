package unit731.hunspeller.parsers.strategies;

import org.apache.commons.lang3.StringUtils;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
public class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final String COMMA = ",";
	private static final String SLASH = "/";


	@Override
	public String[] parseRuleFlags(String textFlags){
		return (textFlags != null && !textFlags.isEmpty()? removeDuplicates(textFlags.split(COMMA)): new String[0]);
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		return SLASH + String.join(COMMA, textFlags);
	}

}
