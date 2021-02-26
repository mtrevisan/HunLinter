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
package unit731.hunlinter.services.text;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.RegexHelper;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collector;


public final class StringHelper{

	private static final Pattern PATTERN_COMBINING_DIACRITICAL_MARKS = RegexHelper.pattern("\\p{InCombiningDiacriticalMarks}+");

	public enum Casing{
		/** All lower case or neutral case, e.g. "hello java" */
		LOWER_CASE,
		/** Start upper case, rest lower case, e.g. "Hello java" */
		TITLE_CASE,
		/** All upper case, e.g. "UPPERCASE" or "HELLO JAVA" */
		ALL_CAPS,
		/** Camel case, start lower case, e.g. "helloJava" */
		CAMEL_CASE,
		/** Pascal case, start upper case, e.g. "HelloJava" */
		PASCAL_CASE
	}


	private StringHelper(){}

	public static boolean isWord(final CharSequence text){
		for(int i = 0; i < text.length(); i ++){
			final char chr = text.charAt(i);
			if(Character.isLetter(chr) || Character.isDigit(chr))
				return true;
		}
		return false;
	}

	public static long countUppercases(final CharSequence text){
		return text.chars()
			.filter(Character::isUpperCase)
			.count();
	}

	//Classify the casing of a given string (ignoring characters for which no upper-/lowercase distinction exists)
	public static Casing classifyCasing(final CharSequence text){
		if(StringUtils.isBlank(text))
			return Casing.LOWER_CASE;

		final long upper = text.chars()
			.filter(chr -> Character.isLetter(chr) && Character.isUpperCase(chr))
			.count();
		if(upper == 0l)
			return Casing.LOWER_CASE;

		final boolean startsWithUppercase = Character.isUpperCase(text.charAt(0));
		if(startsWithUppercase && upper == 1l)
			return Casing.TITLE_CASE;

		final long lower = text.chars()
			//Unicode modifier letter apostrophe is considered as an uppercase letter, but should be regarded as caseless,
			//so it has to be excluded
			.filter(chr -> Character.isLetter(chr) && chr != HyphenationParser.MODIFIER_LETTER_APOSTROPHE
				&& Character.isLowerCase(chr))
			.count();
		if(lower == 0l)
			return Casing.ALL_CAPS;

		return (startsWithUppercase? Casing.PASCAL_CASE: Casing.CAMEL_CASE);
	}

	public static String longestCommonPrefix(final String... texts){
		return longestCommonAffix(StringHelper::commonPrefix, texts);
	}

	public static String longestCommonSuffix(final String... texts){
		return longestCommonAffix(StringHelper::commonSuffix, texts);
	}

	private static String longestCommonAffix(final BiFunction<String, String, String> commonAffix, final String... texts){
		String lcs = null;
		if(texts.length > 0){
			int offset = 0;
			lcs = texts[offset ++];
			while(!lcs.isEmpty() && offset < texts.length)
				lcs = commonAffix.apply(lcs, texts[offset ++]);
		}
		return lcs;
	}

	/**
	 * Returns the longest string {@code suffix} such that {@code a.endsWith(suffix) &&
	 * b.endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common suffix, returns the empty string.
	 */
	private static String commonSuffix(final CharSequence a, final CharSequence b){
		int s = 0;
		final int aLength = a.length();
		final int bLength = b.length();
		final int maxSuffixLength = Math.min(aLength, bLength);
		while(s < maxSuffixLength && a.charAt(aLength - s - 1) == b.charAt(bLength - s - 1))
			s ++;
		if(validSurrogatePairAt(a, aLength - s - 1) || validSurrogatePairAt(b, bLength - s - 1))
			s --;
		return a.subSequence(aLength - s, aLength).toString();
	}

	/**
	 * Returns the longest string {@code prefix} such that {@code a.toString().startsWith(prefix) &&
	 * b.toString().startsWith(prefix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common prefix, returns the empty string.
	 */
	private static String commonPrefix(final CharSequence a, final CharSequence b){
		int p = 0;
		final int maxPrefixLength = Math.min(a.length(), b.length());
		while(p < maxPrefixLength && a.charAt(p) == b.charAt(p))
			p ++;
		if(validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1))
			p --;
		return a.subSequence(0, p).toString();
	}

	/**
	 * True when a valid surrogate pair starts at the given {@code index} in the given {@code string}.
	 * Out-of-range indexes return false.
	 */
	private static boolean validSurrogatePairAt(final CharSequence string, final int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	public static int getLastCommonLetterIndex(final CharSequence word1, final CharSequence word2){
		int lastCommonLetter;
		final int minWordLength = Math.min(word1.length(), word2.length());
		for(lastCommonLetter = 0; lastCommonLetter < minWordLength; lastCommonLetter ++)
			if(word1.charAt(lastCommonLetter) != word2.charAt(lastCommonLetter))
				break;
		return lastCommonLetter;
	}

	public static String removeCombiningDiacriticalMarks(final CharSequence word){
		return RegexHelper.replaceAll(Normalizer.normalize(word, Normalizer.Form.NFKD), PATTERN_COMBINING_DIACRITICAL_MARKS,
			StringUtils.EMPTY);
	}

	public static Collector<String, List<String>, String> limitingJoin(final CharSequence delimiter, final int limit,
			final String ellipsis){
		return Collector.of(ArrayList::new,
			(l, e) -> {
				if(l.size() < limit)
					l.add(e);
				else if(l.size() == limit)
					l.add(ellipsis);
			},
			(l1, l2) -> {
				l1.addAll(l2.subList(0, Math.min(l2.size(), Math.max(0, limit - l1.size()))));
				if(l1.size() == limit)
					l1.add(ellipsis);
				return l1;
			},
			l -> String.join(delimiter, l)
		);
	}


	public static byte[] getRawBytes(final String text){
		return text.getBytes(StandardCharsets.UTF_8);
	}

	public static int rawBytesLength(final CharSequence sequence){
		int count = 0;
		for(int i = 0; i < sequence.length(); i ++){
			final char ch = sequence.charAt(i);
			if(ch <= 0x7F)
				count ++;
			else if(ch <= 0x07FF)
				count += 2;
			else if(Character.isHighSurrogate(ch)){
				count += 4;
				i ++;
			}
			else
				count += 3;
		}
		return count;
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order
	 *
	 * @param byteArray	Array to be converted to hexadecimal characters
	 * @return	The hexadecimal characters
	 */
	public static String byteArrayToHexString(final byte[] byteArray){
		final StringBuilder sb = new StringBuilder(byteArray.length << 1);
		for(final byte b : byteArray){
			sb.append(Character.forDigit((b >> 4) & 0x0F, 16));
			sb.append(Character.forDigit((b & 0x0F), 16));
		}
		return sb.toString();
	}

	//NOTE: `bytes` should be non-negative (no check is done)
	public static String byteCountToHumanReadable(final long bytes){
		if(bytes < 1024l)
			return bytes + " B";

		final int exponent = (int)(Math.log(bytes) / Math.log(1024.));
		final char prefix = "KMGTPE".charAt(exponent - 1);
		final double divisor = Math.pow(1024., exponent);
		final double result = bytes / divisor;
		return String.format(Locale.ROOT, (result < 100? "%.1f": "%.0f") + " %ciB", result, prefix);
	}

	//Find the maximum consecutive repeating character in given string
	public static int maxRepeating(final CharSequence text, final char chr){
		final int n = text.length();
		int count = 0;
		int currentCount = 1;
		//traverse string except last character
		for(int i = 0; i < n; i ++){
			//if current character matches with next
			if(i < n - 1 && text.charAt(i) == chr && text.charAt(i + 1) == chr)
				currentCount ++;
			//if doesn't match, update result (if required) and reset count
			else{
				if(currentCount > count)
					count = currentCount;
				currentCount = 1;
			}
		}
		return count;
	}

	public static String removeAll(final String text, final char charToRemove){
		final String strToRemove = Character.toString(charToRemove);
		final StringBuilder sb = new StringBuilder(text);
		int index = 0;
		while((index = sb.indexOf(strToRemove, index)) >= 0)
			sb.deleteCharAt(index --);
		return sb.toString();
	}

}
