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
package io.github.mtrevisan.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.charset.StandardCharsets;


/** Stem and tag data associated with a given word */
public class WordData{

	/** inflected word form data */
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
	public byte[] getWord(){
		return word;
	}

	/**
	 * @return	Stem data decoded to a character sequence or <code>null</code> if no associated stem data exists.
	 */
	public byte[] getStem(){
		return stem;
	}

	/**
	 * @return	Tag data decoded to a character sequence or <code>null</code> if no associated tag data exists.
	 */
	public byte[] getTag(){
		return tag;
	}

	void setWord(final byte[] word){
		this.word = word;
	}

	void setStem(final byte[] stem){
		this.stem = stem;
	}

	void setTag(final byte[] tag){
		this.tag = tag;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final WordData rhs = (WordData)obj;
		return new EqualsBuilder()
			.append(word, rhs.word)
			.append(stem, rhs.stem)
			.append(tag, rhs.tag)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(word)
			.append(stem)
			.append(tag)
			.toHashCode();
	}

	@Override
	public String toString(){
		return "WordData[" + new String(word, StandardCharsets.UTF_8)
			+ "," + new String(stem, StandardCharsets.UTF_8)
			+ "," + new String(tag, StandardCharsets.UTF_8) + "]";
	}

}
