package unit731.hunspeller.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PatternService{

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
