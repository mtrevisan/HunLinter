/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.hyphenation;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class HyphenationBreak{

	@FunctionalInterface
	public interface NoHyphenationManageFunction{
		String[] manage(final Map<Integer, Pair<Integer, String>> indexesAndRules, final String[] syllabes, final String nohyp,
			final int wordLength);
	}

	private static final Map<String, NoHyphenationManageFunction> NO_HYPHENATION_MANAGE_METHODS = new HashMap<>(4);
	static{
		NO_HYPHENATION_MANAGE_METHODS.put("  ", HyphenationBreak::manageInside);
		NO_HYPHENATION_MANAGE_METHODS.put("^ ", HyphenationBreak::manageStartsWith);
		NO_HYPHENATION_MANAGE_METHODS.put(" $", HyphenationBreak::manageEndsWith);
		NO_HYPHENATION_MANAGE_METHODS.put("^$", HyphenationBreak::manageWhole);
	}

	public static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	private final Map<Integer, Pair<Integer, String>> indexesAndRules;


	public HyphenationBreak(final Map<Integer, Pair<Integer, String>> indexesAndRules){
		Objects.requireNonNull(indexesAndRules);

		this.indexesAndRules = indexesAndRules;
	}


	public boolean isBreakpoint(final int index){
		return (indexesAndRules.getOrDefault(index, EMPTY_PAIR).getKey() % 2 != 0);
	}

	public String getRule(final int index){
		return indexesAndRules.getOrDefault(index, EMPTY_PAIR).getValue();
	}

	public String[] getRules(){
		int offset = 0;
		final String[] list = new String[indexesAndRules.size()];
		for(final Pair<Integer, String> pair : indexesAndRules.values())
			list[offset ++] = pair.getValue();
		return list;
	}

	public String[] enforceNoHyphens(String[] syllabes, final Iterable<String> noHyphen){
		if(syllabes.length > 1){
			int wordLength = 0;
			for(final String syllabe : syllabes)
				wordLength += syllabe.length();
			for(final String nohyp : noHyphen){
				final String reducedKey = reduceKey(nohyp);
				final NoHyphenationManageFunction fun = NO_HYPHENATION_MANAGE_METHODS.get(reducedKey);
				syllabes = fun.manage(indexesAndRules, syllabes, nohyp, wordLength);
				if(syllabes.length <= 1)
					break;
			}
		}
		return syllabes;
	}

	private static String[] manageInside(final Map<Integer, Pair<Integer, String>> indexesAndRules, String[] syllabes,
			final CharSequence nohyp, final int wordLength){
		final int nohypLength = nohyp.length();

		int index = 0;
		for(int i = 0; syllabes.length > 1 && i < syllabes.length; i ++){
			final String syllabe = syllabes[i];

			if(syllabe.contentEquals(nohyp)){
				indexesAndRules.remove(index);
				indexesAndRules.remove(index + nohypLength);

				if(i == 0)
					syllabes = mergeIndexWithFollowing(syllabes, 0);
				else if(i == syllabes.length - 1)
					syllabes = mergeIndexWithPrevious(syllabes, i);
				else if(syllabes.length == 2)
					syllabes = mergeIndexWithPrevious(syllabes, syllabes.length - 1);
				else
					syllabes = mergeIndexAndNextWithPrevious(syllabes, i);

				i --;
			}

			index += syllabe.length();
		}
		return syllabes;
	}

	private static String[] manageStartsWith(final Map<Integer, Pair<Integer, String>> indexesAndRules, String[] syllabes,
			final String nohyp, final int wordLength){
		if(syllabes[0].equals(nohyp.substring(1))){
			indexesAndRules.remove(1);
			indexesAndRules.remove(nohyp.length());

			if(syllabes.length > 1)
				syllabes = mergeIndexWithFollowing(syllabes, 0);
		}
		return syllabes;
	}

	private static String[] manageEndsWith(final Map<Integer, Pair<Integer, String>> indexesAndRules, String[] syllabes,
			final String nohyp, final int wordLength){
		final int nohypLength = nohyp.length();
		if(syllabes[syllabes.length - 1].equals(nohyp.substring(0, nohypLength - 1))){
			indexesAndRules.remove(wordLength - nohypLength - 1);
			indexesAndRules.remove(wordLength - 1);

			if(syllabes.length > 1)
				syllabes = mergeIndexWithPrevious(syllabes, syllabes.length - 1);
		}
		return syllabes;
	}

	private static String[] manageWhole(final Map<Integer, Pair<Integer, String>> indexesAndRules, final String[] syllabes,
			String nohyp, final int wordLength){
		nohyp = nohyp.substring(1, nohyp.length() - 1);
		manageInside(indexesAndRules, syllabes, nohyp, wordLength);
		return syllabes;
	}

	private static String[] mergeIndexWithFollowing(final String[] array, final int index){
		array[index + 1] = array[index] + array[index + 1];
		return ArrayUtils.remove(array, index);
	}

	private static String[] mergeIndexWithPrevious(final String[] array, final int index){
		array[index - 1] += array[index];
		return ArrayUtils.remove(array, index);
	}

	private static String[] mergeIndexAndNextWithPrevious(final String[] array, final int index){
		array[index - 1] = array[index - 1] + array[index] + array[index + 1];
		return ArrayUtils.removeAll(array, index, index + 1);
	}

	private String reduceKey(final CharSequence key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private boolean isStarting(final CharSequence key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(final CharSequence key){
		return (key.charAt(key.length() - 1) == '$');
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final HyphenationBreak other = (HyphenationBreak)obj;
		return new EqualsBuilder()
			.append(indexesAndRules, other.indexesAndRules)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(indexesAndRules)
			.toHashCode();
	}

}
