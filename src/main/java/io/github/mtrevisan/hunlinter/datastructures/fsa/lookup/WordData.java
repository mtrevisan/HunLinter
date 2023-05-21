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
package io.github.mtrevisan.hunlinter.datastructures.fsa.lookup;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * Stem and tag data associated with a given word.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class WordData{

	/** inflected word form data. */
	private byte[] word;
	private byte[] stem;
	private byte[] tag;


	WordData(){}

	WordData(final byte[] stem, final byte[] tag){
		this.stem = stem;
		this.tag = tag;
	}

	/**
	 * @return	Inflected word form data. Usually the parameter passed to {@link DictionaryLookup#lookup(String)}.
	 */
	public final byte[] getWord(){
		return word;
	}

	/**
	 * @return	Stem data decoded to a character sequence or {@code null} if no associated stem data exists.
	 */
	public final byte[] getStem(){
		return stem;
	}

	/**
	 * @return	Tag data decoded to a character sequence or {@code null} if no associated tag data exists.
	 */
	public final byte[] getTag(){
		return tag;
	}

	final void setWord(final byte[] word){
		this.word = word;
	}

	final void setStem(final byte[] stem){
		this.stem = stem;
	}

	final void setTag(final byte[] tag){
		this.tag = tag;
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final WordData rhs = (WordData)obj;
		return (Arrays.equals(word, rhs.word)
			&& Arrays.equals(stem, rhs.stem)
			&& Arrays.equals(tag, rhs.tag));
	}

	@Override
	public final int hashCode(){
		int result = Arrays.hashCode(word);
		result = 31 * result + Arrays.hashCode(stem);
		result = 31 * result + Arrays.hashCode(tag);
		return result;
	}

	@Override
	public final String toString(){
		return "WordData[word:" + new String(word, StandardCharsets.UTF_8)
			+ ", stem:" + (stem != null? new String(stem, StandardCharsets.UTF_8): "null")
			+ ", tag:" + (tag != null? new String(tag, StandardCharsets.UTF_8): "null")
			+ "]";
	}

}
