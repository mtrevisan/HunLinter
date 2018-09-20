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

	public static Matcher matcher(String pattern){
		return matcher(pattern, StringUtils.EMPTY);
	}

	public static Matcher matcher(String pattern, String text){
		return pattern(pattern).matcher(text);
	}

	/** Returns the delimiters along with the splitted elements */
	public static Pattern splitterWithDelimiters(String delimitersRegex){
		return pattern(String.format(SPLITTER_PATTERN_WITH_DELIMITER, delimitersRegex));
	}

	public static String[] split(String text, Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(String text, Pattern pattern, int limit){
		return pattern.split(text, limit);
	}

	public static String[] extract(String text, Matcher matcher){
		List<String> result = new ArrayList<>();
		matcher.reset(text);
		while(matcher.find()){
			String component = null;
			int i = 1;
			int size = matcher.groupCount();
			while(component == null && i < size)
				component = matcher.group(i ++);
			result.add(component != null? component: matcher.group());
		}
		return result.toArray(new String[result.size()]);
	}

	public static boolean find(String text, Matcher matcher){
		return matcher.reset(text).find();
	}

	public static String replaceAll(String text, Matcher matcher, String replacement){
		return matcher.reset(text).replaceAll(replacement);
	}

	public static String clear(String text, Matcher matcher){
		return replaceAll(text, matcher, StringUtils.EMPTY);
	}

}
