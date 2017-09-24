package unit731.hunspeller.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


public class PatternService{

	public static Pattern pattern(String pattern){
		return Pattern.compile(pattern);
	}

	public static Matcher matcher(String pattern){
		return matcher(pattern, StringUtils.EMPTY);
	}

	public static Matcher matcher(String pattern, String text){
		return pattern(pattern).matcher(text);
	}

	public static String[] split(String text, Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(String text, Pattern pattern, int limit){
		return pattern.split(text, limit);
	}

	public static boolean find(String text, Matcher matcher){
		return matcher.reset(text).find();
	}

	public static String replaceFirst(String text, Matcher matcher, String replacement){
		return matcher.reset(text).replaceFirst(replacement);
	}

	public static String replaceAll(String text, Matcher matcher, String replacement){
		return matcher.reset(text).replaceAll(replacement);
	}

}
