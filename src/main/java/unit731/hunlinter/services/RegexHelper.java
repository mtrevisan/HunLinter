/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.services;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public final class RegexHelper{

	private static final String SPLITTER_PATTERN_WITH_DELIMITER = "(?=(?!^)%1$s)(?<!%1$s)|(?!%1$s)(?<=%1$s)";

	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	public static final String GROUP_END = "]";


	private RegexHelper(){}

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
	public static Pattern splitterWithDelimiters(final String delimitersRegex){
		return pattern(String.format(SPLITTER_PATTERN_WITH_DELIMITER, delimitersRegex));
	}

	public static String[] split(final CharSequence text, final Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(final CharSequence text, final Pattern pattern, final int limit){
		return pattern.split(text, limit);
	}

	public static String[] extract(final CharSequence text, final Pattern pattern){
		return extract(text, pattern, -1);
	}

	public static String[] extract(final CharSequence text, final Pattern pattern, final int limit){
		final Matcher matcher = matcher(text, pattern);
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


	public static Matcher matcher(final CharSequence text, final Pattern pattern){
		return pattern.matcher(text);
	}

	public static boolean find(final CharSequence text, final Pattern pattern){
		return matcher(text, pattern)
			.find();
	}

	public static boolean matches(final CharSequence text, final Pattern pattern){
		return matcher(text, pattern)
			.matches();
	}

	public static String replaceAll(final CharSequence text, final Pattern pattern, final String replacement){
		return matcher(text, pattern)
			.replaceAll(replacement);
	}

	public static String clear(final CharSequence text, final Pattern pattern){
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

	public static <V> String mergeSet(final Collection<V> set, final Comparator<String> comparator){
		final List<String> list = new ArrayList<>(set.size());
		forEach(set, v -> list.add(String.valueOf(v)));
		list.sort(comparator);

		final StringBuilder sb = new StringBuilder();
		forEach(list, sb::append);
		return sb.toString();
	}

}
