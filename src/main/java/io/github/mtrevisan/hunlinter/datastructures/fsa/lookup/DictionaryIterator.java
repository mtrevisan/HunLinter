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

import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.Dictionary;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.NoEncoder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.Iterator;


/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}.
 * The stems can be decoded from compressed format or preserved as compressed format.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
class DictionaryIterator implements Iterator<WordData>{

	private final byte separator;

	private final Iterator<ByteBuffer> entriesItr;
	private final SequenceEncoderInterface sequenceEncoder;


	DictionaryIterator(final Dictionary dictionary, final boolean decodeStems){
		separator = dictionary.metadata.getSeparator();

		entriesItr = dictionary.fsa.iterator();
		sequenceEncoder = (decodeStems? dictionary.metadata.getSequenceEncoderType().get(): new NoEncoder());
	}

	@Override
	public final boolean hasNext(){
		return entriesItr.hasNext();
	}

	@Override
	public final WordData next(){
		final ByteBuffer entryBuffer = entriesItr.next();
		final byte[] array = entryBuffer.array();
		final int limit = entryBuffer.remaining();

		//entries are typically: inflected<SEP>stem<SEP>tag so try to find this split:

		//find the separator byte's position splitting the inflection instructions from the tag
		int separatorIndex = ArrayUtils.indexOf(array, separator);
		final int len = (separatorIndex >= 0? separatorIndex: array.length);
		final byte[] inflection = new byte[len];
		System.arraycopy(array, 0, inflection, 0, len);

		final WordData entry = new WordData();
		entry.setWord(inflection);

		//find the next separator byte's position splitting word form and tag
		if(separatorIndex >= 0){
			final int start = separatorIndex + 1;
			separatorIndex = ArrayUtils.indexOf(array, separator, start);
			if(separatorIndex < 0 || separatorIndex > limit)
				separatorIndex = limit;

			//decode the stem into stem buffer
			final byte[] stem = new byte[separatorIndex - start];
			System.arraycopy(array, start, stem, 0, separatorIndex - start);
			entry.setStem(sequenceEncoder.decode(inflection, stem));

			//skip separator character, if present
			if(separatorIndex + 1 <= limit)
				separatorIndex ++;

			//decode the tag data
			final byte[] tag = new byte[limit - separatorIndex];
			System.arraycopy(array, separatorIndex, tag, 0, limit - separatorIndex);
			entry.setTag(tag);
		}

		return entry;
	}

}
