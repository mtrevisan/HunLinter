package unit731.hunlinter.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.system.LoopHelper;


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
		return extract(text, pattern, -1);
	}

	public static String[] extract(final String text, final Pattern pattern, final int limit){
		final Matcher matcher = pattern.matcher(text);
		return (limit >= 0? extractWithLimit(matcher, limit): extractUnlimited(matcher));
	}

	private static String[] extractWithLimit(final Matcher matcher, final int limit){
		int index = 0;
		final String[] result = new String[limit];
		while(matcher.find() && index < limit){
			final String component = getNextGroup(matcher);
			result[index ++] = (component != null? component: matcher.group());
		}
		return result;
	}

	private static String[] extractUnlimited(final Matcher matcher){
		String[] result = new String[0];
		while(matcher.find()){
			final String component = getNextGroup(matcher);
			result = ArrayUtils.add(result, (component != null? component: matcher.group()));
		}
		return result;
	}

	private static String getNextGroup(final Matcher matcher){
		String component = null;
		int i = 1;
		final int size = matcher.groupCount();
		while(component == null && i <= size)
			component = matcher.group(i ++);
		return component;
	}

	//FIXME is there a way to optimize this find?
	public static boolean find(final String text, final Pattern pattern){
		return pattern.matcher(text)
			.find();
	}

	public static boolean matches(final String text, final Pattern pattern){
		return pattern.matcher(text)
			.matches();
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
		final List<String> list = new ArrayList<>();
		LoopHelper.forEach(set, v -> list.add(String.valueOf(v)));
		list.sort(comparator);

		final StringBuilder sb = new StringBuilder();
		LoopHelper.forEach(list, sb::append);
		return sb.toString();
	}

}
