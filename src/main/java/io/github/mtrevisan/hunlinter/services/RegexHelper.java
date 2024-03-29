/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class RegexHelper{

	private static final String SPLITTER_PATTERN_WITH_DELIMITER = "(?=(?!^)%1$s)(?<!%1$s)|(?!%1$s)(?<=%1$s)";

	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	public static final String GROUP_END = "]";
	private static final String DOT = ".";


	private RegexHelper(){}

	public static Pattern pattern(final String pattern){
		return Pattern.compile(pattern);
	}

	public static Pattern pattern(final String pattern, final int flags){
		return Pattern.compile(pattern, flags);
	}

	public static int conditionLength(final String pattern){
		int length = 0;
		boolean insideGroup = false;
		for(int i = 0; i < pattern.length(); i ++){
			final char chr = pattern.charAt(i);
			if(chr == '[')
				insideGroup = true;
			else if(chr == ']'){
				insideGroup = false;
				length ++;
			}
			else if(!insideGroup)
				length ++;
		}
		return length;
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

	public static String quoteReplacement(final String text){
		return Matcher.quoteReplacement(text);
	}

	public static String[] split(final CharSequence text, final Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(final CharSequence text, final Pattern pattern, final int limit){
		return pattern.split(text, limit);
	}

	public static String[] extract(final CharSequence text, final Pattern pattern){
		final List<String> result = extract(text, pattern, - 1);
		return result.toArray(new String[result.size()]);
	}

	public static List<String> extract(final CharSequence text, final Pattern pattern, final int limit){
		final Matcher matcher = matcher(text, pattern);
		return (limit >= 0? extractWithLimit(matcher, limit): extractUnlimited(matcher));
	}

	private static List<String> extractWithLimit(final Matcher matcher, final int limit){
		int index = 0;
		final List<String> result = new ArrayList<>(limit);
		while(matcher.find() && index ++ < limit){
			final String component = getNextGroup(matcher);
			result.add(component != null? component: matcher.group());
		}
		return result;
	}

	private static List<String> extractUnlimited(final Matcher matcher){
		final List<String> result = new ArrayList<>(0);
		while(matcher.find()){
			final String component = getNextGroup(matcher);
			result.add(component != null? component: matcher.group());
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

	public static int indexOf(final CharSequence text, final Pattern pattern){
		final Matcher m = matcher(text, pattern);
		m.find();
		return m.start();
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

	/**
	 * NOTE: the empty set produce an empty result.
	 */
	public static String makeGroup(final char[] group, final Comparator<String> comparator){
		final String merge = sortAndMergeSet(group, comparator);
		return (group.length > 1? GROUP_START + merge + GROUP_END: merge);
	}

	/**
	 * NOTE: the empty set produce an empty result.
	 */
	public static String makeGroup(final Collection<Character> group, final Comparator<String> comparator){
		final String merge = sortAndMergeSet(group, comparator);
		return (group.size() > 1? GROUP_START + merge + GROUP_END: merge);
	}

	public static String makeNotGroup(final char[] group, final Comparator<String> comparator){
		final String notGroup;
		if(group.length == 0)
			//the negation of an empty set is everything
			notGroup = DOT;
		else{
			final String merge = sortAndMergeSet(group, comparator);
			notGroup = NOT_GROUP_START + merge + GROUP_END;
		}
		return notGroup;
	}

	public static String makeNotGroup(final Collection<Character> group, final Comparator<String> comparator){
		final String notGroup;
		if(group.isEmpty())
			//the negation of an empty set is everything
			notGroup = DOT;
		else{
			final String merge = sortAndMergeSet(group, comparator);
			notGroup = NOT_GROUP_START + merge + GROUP_END;
		}
		return notGroup;
	}

	public static String sortAndMergeSet(final char[] set, final Comparator<String> comparator){
		final List<String> list = new ArrayList<>(set.length);
		for(int i = 0; i < set.length; i ++)
			list.add(String.valueOf(set[i]));
		list.sort(comparator);

		final StringBuilder sb = new StringBuilder();
		for(final String elem : list)
			sb.append(elem);
		return sb.toString();
	}

	public static <V> String sortAndMergeSet(final Collection<V> set, final Comparator<String> comparator){
		final List<String> list = new ArrayList<>(set.size());
		for(final V v : set)
			list.add(String.valueOf(v));
		list.sort(comparator);

		final StringBuilder sb = new StringBuilder();
		for(final String elem : list)
			sb.append(elem);
		return sb.toString();
	}

}
