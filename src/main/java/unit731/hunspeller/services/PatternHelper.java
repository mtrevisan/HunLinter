package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


public class PatternHelper{

	private static final String SPLITTER_PATTERN_WITH_DELIMITER = "(?=(?!^)%1$s)(?<!%1$s)|(?!%1$s)(?<=%1$s)";


	private PatternHelper(){}

	public static Pattern pattern(String pattern){
		return Pattern.compile(pattern);
	}

	public static Pattern pattern(String pattern, int flags){
		return Pattern.compile(pattern, flags);
	}

	/**
	 * Returns the delimiters along with the splitted elements
	 * 
	 * @param delimitersRegex	regex stating the delimiters
	 * @return	The pattern to be used to split a string
	 */
	public static Pattern splitterWithDelimiters(String delimitersRegex){
		return pattern(String.format(SPLITTER_PATTERN_WITH_DELIMITER, delimitersRegex));
	}

	public static String[] split(String text, Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(String text, Pattern pattern, int limit){
		return pattern.split(text, limit);
	}

	public static String[] extract(String text, Pattern pattern){
		List<String> result = new ArrayList<>();
		Matcher m = pattern.matcher(text);
		while(m.find()){
			String component = null;
			int i = 1;
			int size = m.groupCount();
			while(component == null && i < size)
				component = m.group(i ++);
			result.add(component != null? component: m.group());
		}
		return result.toArray(new String[result.size()]);
	}

	public static boolean find(String text, Pattern pattern){
		return pattern.matcher(text)
			.find();
	}

	public static String replaceAll(String text, Pattern pattern, String replacement){
		return pattern.matcher(text)
			.replaceAll(replacement);
	}

	public static String clear(String text, Pattern pattern){
		return replaceAll(text, pattern, StringUtils.EMPTY);
	}

}
