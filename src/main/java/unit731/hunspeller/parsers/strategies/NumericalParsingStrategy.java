package unit731.hunspeller.parsers.strategies;

import org.apache.commons.lang3.StringUtils;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
public class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final int MAX_NUMERICAL_FLAG = 65510;

	private static final String COMMA = ",";
	private static final String SLASH = "/";


	@Override
	public String[] parseRuleFlags(String textFlags){
		String[] flags = (textFlags != null && !textFlags.isEmpty()? removeDuplicates(textFlags.split(COMMA)): new String[0]);
		for(String flag : flags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]");
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Flag must be an integer number");
			}
		}
		return flags;
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		return SLASH + String.join(COMMA, textFlags);
	}

}
