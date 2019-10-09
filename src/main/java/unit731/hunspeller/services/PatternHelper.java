package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;


public class PatternHelper{

	private static final String SPLITTER_PATTERN_WITH_DELIMITER = "(?=(?!^)%1$s)(?<!%1$s)|(?!%1$s)(?<=%1$s)";

	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	public static final String GROUP_END = "]";


	private PatternHelper(){}

	public static Pattern pattern(final String pattern){
		return Pattern.compile(pattern);
	}

	public static Pattern pattern(final String pattern, final int flags){
		return Pattern.compile(pattern, flags);
	}

	/**
	 * Returns the delimiters along with the split elements
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

	public static String[] extract(final String text, final Pattern pattern){
		return extract(pattern.matcher(text));
	}

	public static String[] extract(final Matcher matcher){
		final List<String> result = new ArrayList<>();
		while(matcher.find()){
			String component = null;
			int i = 1;
			final int size = matcher.groupCount();
			while(component == null && i < size)
				component = matcher.group(i ++);
			result.add(component != null? component: matcher.group());
		}
		return result.toArray(String[]::new);
	}

	//FIXME is there a way to optimize this find?
	public static boolean find(final String text, final Pattern pattern){
		return find(pattern.matcher(text));
	}

	public static boolean find(final Matcher matcher){
		return matcher.find();
	}

	public static boolean matches(final Matcher matcher){
		return matcher.matches();
	}

	//FIXME is there a way to optimize this replaceAll?
	public static String replaceAll(final String text, final Pattern pattern, final String replacement){
		return pattern.matcher(text)
			.replaceAll(replacement);
	}

	public static String clear(final String text, final Pattern pattern){
		return replaceAll(text, pattern, StringUtils.EMPTY);
	}

	public static String makeGroup(final Set<Character> group, final Comparator<String> comparator){
		final String merge = mergeSet(group, comparator);
		return (group.size() > 1? GROUP_START + merge + GROUP_END: merge);
	}

	public static String makeNotGroup(final Set<Character> group, final Comparator<String> comparator){
		final String merge = mergeSet(group, comparator);
		return NOT_GROUP_START + merge + GROUP_END;
	}

	public static <V> String mergeSet(final Set<V> set, final Comparator<String> comparator){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

}
