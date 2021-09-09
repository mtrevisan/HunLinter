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

import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class HyphenationBreak{

	@FunctionalInterface
	public interface NoHyphenationManageFunction{
		void manage(final Map<Integer, IndexDataPair<String>> indexesAndRules, final List<String> syllabes, final String nohyp,
			final int wordLength);
	}

	private static final Map<String, NoHyphenationManageFunction> NO_HYPHENATION_MANAGE_METHODS = new HashMap<>(4);
	static{
		NO_HYPHENATION_MANAGE_METHODS.put("  ", HyphenationBreak::manageInside);
		NO_HYPHENATION_MANAGE_METHODS.put("^ ", HyphenationBreak::manageStartsWith);
		NO_HYPHENATION_MANAGE_METHODS.put(" $", HyphenationBreak::manageEndsWith);
		NO_HYPHENATION_MANAGE_METHODS.put("^$", HyphenationBreak::manageWhole);
	}

	public static final IndexDataPair<String> EMPTY_PAIR = IndexDataPair.of(0, null);


	private final Map<Integer, IndexDataPair<String>> indexesAndRules;


	public HyphenationBreak(final Map<Integer, IndexDataPair<String>> indexesAndRules){
		Objects.requireNonNull(indexesAndRules, "Indexes and rules cannot be null");

		this.indexesAndRules = indexesAndRules;
	}


	public final boolean isBreakpoint(final int index){
		return (indexesAndRules.getOrDefault(index, EMPTY_PAIR).getIndex() % 2 != 0);
	}

	public final String getRule(final int index){
		return indexesAndRules.getOrDefault(index, EMPTY_PAIR).getData();
	}

	public final List<String> getRules(){
		final Collection<IndexDataPair<String>> pairs = indexesAndRules.values();
		final List<String> list = new ArrayList<>(pairs.size());
		for(final IndexDataPair<String> pair : pairs)
			list.add(pair.getData());
		return list;
	}

	public final void enforceNoHyphens(final List<String> syllabes, final Iterable<String> noHyphen){
		if(syllabes.size() > 1){
			int wordLength = 0;
			for(final String syllabe : syllabes)
				wordLength += syllabe.length();
			for(final String nohyp : noHyphen){
				final String reducedKey = reduceKey(nohyp);
				final NoHyphenationManageFunction fun = NO_HYPHENATION_MANAGE_METHODS.get(reducedKey);
				fun.manage(indexesAndRules, syllabes, nohyp, wordLength);
				if(syllabes.size() <= 1)
					break;
			}
		}
	}

	private static void manageInside(final Map<Integer, IndexDataPair<String>> indexesAndRules, final List<String> syllabes,
			final CharSequence nohyp, final int wordLength){
		final int nohypLength = nohyp.length();

		int index = 0;
		for(int i = 0; syllabes.size() > 1 && i < syllabes.size(); i ++){
			final String syllabe = syllabes.get(i);

			if(syllabe.contentEquals(nohyp)){
				indexesAndRules.remove(index);
				indexesAndRules.remove(index + nohypLength);

				if(i == 0)
					mergeIndexWithFollowing(syllabes, 0);
				else if(i == syllabes.size() - 1)
					mergeIndexWithPrevious(syllabes, i);
				else if(syllabes.size() == 2)
					mergeIndexWithPrevious(syllabes, syllabes.size() - 1);
				else
					mergeIndexAndNextWithPrevious(syllabes, i);

				i --;
			}

			index += syllabe.length();
		}
	}

	private static void manageStartsWith(final Map<Integer, IndexDataPair<String>> indexesAndRules, final List<String> syllabes,
			final String nohyp, final int wordLength){
		if(syllabes.get(0).equals(nohyp.substring(1))){
			indexesAndRules.remove(1);
			indexesAndRules.remove(nohyp.length());

			if(syllabes.size() > 1)
				mergeIndexWithFollowing(syllabes, 0);
		}
	}

	private static void manageEndsWith(final Map<Integer, IndexDataPair<String>> indexesAndRules, final List<String> syllabes,
			final String nohyp, final int wordLength){
		final int nohypLength = nohyp.length();
		if(syllabes.get(syllabes.size() - 1).equals(nohyp.substring(0, nohypLength - 1))){
			indexesAndRules.remove(wordLength - nohypLength - 1);
			indexesAndRules.remove(wordLength - 1);

			if(syllabes.size() > 1)
				mergeIndexWithPrevious(syllabes, syllabes.size() - 1);
		}
	}

	private static void manageWhole(final Map<Integer, IndexDataPair<String>> indexesAndRules, final List<String> syllabes,
			String nohyp, final int wordLength){
		nohyp = nohyp.substring(1, nohyp.length() - 1);
		manageInside(indexesAndRules, syllabes, nohyp, wordLength);
	}

	private static void mergeIndexWithFollowing(final List<String> array, final int index){
		array.set(index + 1, array.get(index) + array.get(index + 1));
		array.remove(index);
	}

	private static void mergeIndexWithPrevious(final List<String> array, final int index){
		array.set(index - 1, array.get(index - 1) + array.get(index));
		array.remove(index);
	}

	private static void mergeIndexAndNextWithPrevious(final List<String> array, final int index){
		array.set(index - 1, array.get(index - 1) + array.get(index) + array.get(index + 1));
		array.remove(index);
		array.remove(index);
	}

	private static String reduceKey(final CharSequence key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private static boolean isStarting(final CharSequence key){
		return (key.charAt(0) == '^');
	}

	private static boolean isEnding(final CharSequence key){
		return (key.charAt(key.length() - 1) == '$');
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final HyphenationBreak rhs = (HyphenationBreak)obj;
		return indexesAndRules.equals(rhs.indexesAndRules);
	}

	@Override
	public final int hashCode(){
		return indexesAndRules.hashCode();
	}

}
