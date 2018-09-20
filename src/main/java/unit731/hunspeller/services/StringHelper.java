package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;


public class StringHelper{

	public static enum Casing{
		/** All lower case or neutral case, e.g. "lowercase" or "123" */
		LOWER_CASE,
		/** Start upper case, rest lower case, e.g. "Initcap" */
		TITLE_CASE,
		/** All upper case, e.g. "UPPERCASE" or "ALL4ONE" */
		ALL_CAPS,
		/** Camel case, start lower case, e.g. "camelCase" */
		CAMEL_CASE,
		/** Pascal case, start upper case, e.g. "PascalCase" */
		PASCAL_CASE
	}


	private StringHelper(){}

	public static Casing classifyCasing(String text){
		if(StringUtils.isBlank(text))
			return Casing.LOWER_CASE;

		int lower = 0;
		int upper = 0;
		for(char chr : text.toCharArray())
			if(Character.isAlphabetic(chr)){
				if(Character.isLowerCase(chr))
					lower ++;
				else if(Character.isUpperCase(chr))
					upper ++;
			}
		if(upper == 0)
			return Casing.LOWER_CASE;

		boolean fistCapital = (Character.isUpperCase(text.charAt(0)));
		if(fistCapital && upper == 1)
			return Casing.TITLE_CASE;

		if(lower == 0)
			return Casing.ALL_CAPS;

		return (fistCapital? Casing.PASCAL_CASE: Casing.CAMEL_CASE);
	}

}
