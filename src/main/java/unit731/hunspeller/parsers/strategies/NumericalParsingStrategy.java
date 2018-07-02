package unit731.hunspeller.parsers.strategies;

import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
public class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final int MAX_NUMERICAL_FLAG = 65_000;

	private static final String COMMA = ",";


	@Override
	public String[] parseFlags(String textFlags){
		String[] flags = (Objects.nonNull(textFlags) && !textFlags.isEmpty()? removeDuplicates(StringUtils.split(textFlags, COMMA)):
			new String[0]);
		for(String flag : flags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from " + textFlags);
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Flag must be an integer number: " + flag + " from " + textFlags);
			}
		}
		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(Objects.isNull(textFlags) || textFlags.length == 0)
			return StringUtils.EMPTY;

		for(String flag : textFlags){
			try{
				int numericalFlag = Integer.parseInt(flag);
				if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
					throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from " + Arrays.deepToString(textFlags));
			}
			catch(NumberFormatException e){
				throw new IllegalArgumentException("Each flag must be an integer number: " + flag + " from " + Arrays.deepToString(textFlags));
			}
		}

		return SLASH + String.join(COMMA, textFlags);
	}

}
