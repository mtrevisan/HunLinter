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

import java.util.Arrays;
import java.util.Objects;


public class Hyphenation{

	private final String[] syllabes;
	private final String[] compounds;
	private final String[] rules;
	private final String breakCharacter;


	public Hyphenation(final String[] syllabes, final String[] compounds, final String[] rules, final String breakCharacter){
		Objects.requireNonNull(syllabes, "Syllabes cannot be null");
		Objects.requireNonNull(compounds, "Compounds cannot be null");
		Objects.requireNonNull(rules, "Rules cannot be null");
		Objects.requireNonNull(breakCharacter, "Break character cannot be null");

		this.syllabes = syllabes;
		this.compounds = compounds;
		this.rules = rules;
		this.breakCharacter = breakCharacter;
	}

	public String[] getSyllabes(){
		return syllabes;
	}

	public String[] getCompounds(){
		return compounds;
	}

	public String[] getRules(){
		return rules;
	}

	public String getBreakCharacter(){
		return breakCharacter;
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the (relative) index of the syllabe at the given (global) index
	 */
	public int getSyllabeIndex(int idx){
		int k = -1;
		final int size = countSyllabes();
		for(int i = 0; i < size; i ++){
			final String syllabe = syllabes[i];
			idx -= syllabe.length();
			if(idx < 0){
				k = i;
				break;
			}
		}
		return k;
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the syllabe at the given (global) index
	 */
	public String getSyllabe(final int idx){
		return syllabes[getSyllabeIndex(idx)];
	}

	public int countSyllabes(){
		return syllabes.length;
	}

	/**
	 * @param idx	Index of syllabe to extract, if negative then it's relative to the last syllabe
	 * @return the syllabe at the given (relative) index
	 */
	public String getAt(final int idx){
		return syllabes[restoreRelativeIndex(idx)];
	}

	private int restoreRelativeIndex(final int idx){
		return (idx + countSyllabes()) % countSyllabes();
	}

	public boolean isHyphenated(){
		return (rules.length > 0);
	}

	public boolean isCompound(){
		return (compounds.length > 1);
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Hyphenation rhs = (Hyphenation)obj;
		return (Arrays.equals(syllabes, rhs.syllabes)
			&& breakCharacter.equals(rhs.breakCharacter));
	}

	@Override
	public int hashCode(){
		int result = Arrays.hashCode(syllabes);
		result = 31 * result + breakCharacter.hashCode();
		return result;
	}

}
