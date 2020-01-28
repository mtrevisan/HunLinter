package unit731.hunlinter.services;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


public class StringHelper{

	private static final Pattern PATTERN_COMBINING_DIACRITICAL_MARKS = PatternHelper.pattern("\\p{InCombiningDiacriticalMarks}+");

	public enum Casing{
		/** All lower case or neutral case, e.g. "lowercase" or "123" */
		LOWER_CASE,
		/** Start upper case, rest lower case, e.g. "Initcap" */
		TITLE_CASE,
		/** All upper case, e.g. "UPPERCASE" or "ALL4ONE" */
		ALL_CAPS,
		/** Camel case, start lower case, e.g. "camelCase" */
		CAMEL_CASE,
		/** Pascal case, start upper case, e.g. "PascalCase" */
		PASCAL_CASE
	}


	private StringHelper(){}

	public static long countUppercases(final String text){
		return text.chars()
			.filter(Character::isUpperCase)
			.count();
	}

	public static Casing classifyCasing(final String text){
		if(StringUtils.isBlank(text))
			return Casing.LOWER_CASE;

		int lower = 0;
		int upper = 0;
		for(final char chr : text.toCharArray())
			if(Character.isAlphabetic(chr)){
				if(Character.isLowerCase(chr))
					lower ++;
				else if(Character.isUpperCase(chr))
					upper ++;
			}
		if(upper == 0)
			return Casing.LOWER_CASE;

		final boolean fistCapital = (Character.isUpperCase(text.charAt(0)));
		if(fistCapital && upper == 1)
			return Casing.TITLE_CASE;

		if(lower == 0)
			return Casing.ALL_CAPS;

		return (fistCapital? Casing.PASCAL_CASE: Casing.CAMEL_CASE);
	}

	/**
	 * Finds the length and the index at which starts the Longest Common Substring
	 *
	 * @param keyA	Character sequence A
	 * @param keyB	Character sequence B
	 * @return	The indexes of keyA and keyB of the start of the longest common substring between <code>A</code> and <code>B</code>
	 */
	public static Pair<Integer, Integer> longestCommonSubstring(final String keyA, final String keyB){
		final int m = keyA.length();
		final int n = keyB.length();

		if(m < n){
			final Pair<Integer, Integer> indexes = longestCommonSubstring(keyB, keyA);
			return Pair.of(indexes.getRight(), indexes.getLeft());
		}

		//matrix to store result of two consecutive rows at a time
		final int[][] len = new int[2][n];
		int currentRow = 0;

		//for a particular value of i and j, len[currRow][j] stores length of LCS in string X[0..i] and Y[0..j]
		int lcsMaxLength = 0;
		int lcsIndexA = -1;
		int lcsIndexB = -1;
		for(int i = 0; i < m; i ++){
			for(int j = 0; j < n; j ++){
				final int cost;
				if(keyA.charAt(i) != keyB.charAt(j))
					cost = 0;
				else if(i == 0 || j == 0)
					cost = 1;
				else
					cost = len[currentRow][j - 1] + 1;
				len[1 - currentRow][j] = cost;

				if(cost > lcsMaxLength){
					lcsMaxLength = cost;

					lcsIndexA = i;
					lcsIndexB = j;
				}
			}

			//make current row as previous row and previous row as new current row
			currentRow = 1 - currentRow;
		}
		return Pair.of(lcsIndexA, lcsIndexB);
	}

	public static String longestCommonPrefix(final Collection<String> texts){
		return longestCommonAffix(texts, StringHelper::commonPrefix);
	}

	public static String longestCommonSuffix(final Collection<String> texts){
		return longestCommonAffix(texts, StringHelper::commonSuffix);
	}

	private static String longestCommonAffix(final Collection<String> texts, final BiFunction<String, String, String> commonAffix){
		String lcs = null;
		if(!texts.isEmpty()){
			final Iterator<String> itr = texts.iterator();
			lcs = itr.next();
			while(!lcs.isEmpty() && itr.hasNext())
				lcs = commonAffix.apply(lcs, itr.next());
		}
		return lcs;
	}

	/**
	 * Returns the longest string {@code suffix} such that {@code a.toString().endsWith(suffix) &&
	 * b.toString().endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common suffix, returns the empty string.
	 */
	private static String commonSuffix(final String a, final String b){
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
	private static String commonPrefix(final String a, final String b){
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

	public static int getLastCommonLetterIndex(final String word1, final String word2){
		int lastCommonLetter;
		final int minWordLength = Math.min(word1.length(), word2.length());
		for(lastCommonLetter = 0; lastCommonLetter < minWordLength; lastCommonLetter ++)
			if(word1.charAt(lastCommonLetter) != word2.charAt(lastCommonLetter))
				break;
		return lastCommonLetter;
	}

	public static String removeCombiningDiacriticalMarks(final String word){
		return PatternHelper.replaceAll(Normalizer.normalize(word, Normalizer.Form.NFKD), PATTERN_COMBINING_DIACRITICAL_MARKS, StringUtils.EMPTY);
	}

	public static Collector<String, List<String>, String> limitingJoin(final String delimiter, final int limit, final String ellipsis){
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

	public static String byteCountToHumanReadable(final long bytes){
		final long b = (bytes == Long.MIN_VALUE? Long.MAX_VALUE: Math.abs(bytes));
		return (b < 1024l? bytes + " B":
			b <= 0xFFFCCCCCCCCCCCCl >> 40? String.format(Locale.ROOT, "%.1f KiB", bytes / 0x1p10):
			b <= 0xFFFCCCCCCCCCCCCl >> 30? String.format(Locale.ROOT, "%.1f MiB", bytes / 0x1p20):
			b <= 0xFFFCCCCCCCCCCCCl >> 20? String.format(Locale.ROOT, "%.1f GiB", bytes / 0x1p30):
			b <= 0xFFFCCCCCCCCCCCCl >> 10? String.format(Locale.ROOT, "%.1f TiB", bytes / 0x1p40):
			b <= 0xFFFCCCCCCCCCCCCl? String.format(Locale.ROOT, "%.1f PiB", (bytes >> 10) / 0x1p40):
			String.format(Locale.ROOT, "%.1f EiB", (bytes >> 20) / 0x1p40));
	}

}
