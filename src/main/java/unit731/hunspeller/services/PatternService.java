package unit731.hunspeller.services;

import java.util.regex.Matcher;


public class PatternService{

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
