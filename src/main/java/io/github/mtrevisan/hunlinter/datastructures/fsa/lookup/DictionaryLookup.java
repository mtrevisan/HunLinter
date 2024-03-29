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

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.Dictionary;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.DictionaryMetadata;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * This class implements a dictionary lookup.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class DictionaryLookup implements Iterable<WordData>{

	/** An FSA used for look-ups. */
	private final FSATraversal matcher;

	/** An iterator for walking along the final states of the given FSA. */
	private final ByteSequenceIterator finalStatesIterator;

	/** Private internal array of reusable word data objects. */
	private final List<WordData> forms = new ArrayList<>(0);

	/** The {@link Dictionary} this lookup is using. */
	private final Dictionary dictionary;

	private final SequenceEncoderInterface sequenceEncoder;


	/**
	 * Creates a new object of this class using the given FSA for word lookups
	 * and encoding for converting characters to bytes.
	 *
	 * @param dictionary	The dictionary to use for look-ups.
	 * @throws IllegalArgumentException	If FSA's root node cannot be acquired (dictionary is empty).
	 */
	public DictionaryLookup(final Dictionary dictionary) throws IllegalArgumentException{
		Objects.requireNonNull(dictionary, "Dictionary cannot be null");
		final FSAAbstract fsa = dictionary.fsa;
		final DictionaryMetadata metadata = dictionary.metadata;
		Objects.requireNonNull(fsa, "FSA cannot be null");
		Objects.requireNonNull(metadata, "Metadata cannot be null");

		this.dictionary = dictionary;

		sequenceEncoder = metadata.getSequenceEncoderType().get();
		matcher = new FSATraversal(fsa);
		finalStatesIterator = new ByteSequenceIterator(fsa, fsa.getRootNode());
	}

	/**
	 * Returns a list of {@link WordData} entries for a given word. The returned
	 * list is never {@code null}. Depending on the stemmer's
	 * implementation the {@link WordData} may carry the stem and additional
	 * information (tag) or just the stem.
	 * <p>
	 * The returned list and any object it contains are not usable after a
	 * subsequent call to this method. Any data that should be stored in between
	 * must be copied by the caller.
	 *
	 * @param word	The word (typically inflected) to look up base forms for.
	 * @return	A list of {@link WordData} entries (possibly empty).
	 */
	public final List<WordData> lookup(String word){
		final byte separator = dictionary.metadata.getSeparator();

		if(!dictionary.metadata.getInputConversionPairs().isEmpty())
			word = applyReplacements(word, dictionary.metadata.getInputConversionPairs());

		//encode word characters into bytes in the same encoding as the FSA's
		final byte[] wordAsByteArray = word.getBytes(dictionary.metadata.getCharset());
		if(ArrayUtils.indexOf(wordAsByteArray, separator) >= 0)
			throw new IllegalArgumentException("No valid input can contain the separator: " + word);

		//clear the output list
		forms.clear();

		//try to find a partial match in the dictionary
		final FSAMatchResult match = matcher.match(wordAsByteArray, dictionary.fsa.getRootNode());

		if(match.kind == FSAMatchResult.PREFIX_MATCH){
			//the entire sequence exists in the dictionary, a separator should be the next symbol
			final int arc = dictionary.fsa.getArc(match.node, separator);

			//the situation when the arc points to a final node should NEVER happen,
			//after all, we want the word to have SOME base form
			if(arc != 0 && !dictionary.fsa.isArcFinal(arc)){
				finalStatesIterator.restartFrom(dictionary.fsa.getEndNode(arc));
				//there is such a word in the dictionary, return its base forms
				while(finalStatesIterator.hasNext()){
					final ByteBuffer bb = finalStatesIterator.next();

					//find the separator byte's position splitting the inflection instructions from the tag
					final byte[] bbArray = bb.array();
					int separatorIndex = ArrayUtils.indexOf(bbArray, separator);

					//now, expand the prefix/suffix 'compression' and store the base form
					final WordData wordData = new WordData();
					final Map<String, String> outputConversionPairs = dictionary.metadata.getOutputConversionPairs();
					wordData.setWord((outputConversionPairs.isEmpty()? word: applyReplacements(word, outputConversionPairs))
						.getBytes(dictionary.metadata.getCharset()));

					//decode the stem into stem buffer
					final byte[] copy = new byte[separatorIndex];
					System.arraycopy(bbArray, 0, copy, 0, separatorIndex);
					wordData.setStem(sequenceEncoder.decode(wordAsByteArray, copy));

					//skip separator character
					separatorIndex ++;

					//decode the tag data
					final int tagSize = bb.remaining() - separatorIndex;
					if(tagSize > 0){
						final byte[] tag = new byte[tagSize];
						System.arraycopy(bbArray, separatorIndex, tag, 0, tagSize);
						wordData.setTag(tag);
					}

					forms.add(wordData);
				}
			}
		}
		else if(match.kind == FSAMatchResult.EXACT_MATCH){
			//there is such a word in the dictionary (used for containment only!!), return its base form

			//now, expand the prefix/suffix 'compression' and store the base form
			final WordData wordData = new WordData();
			final Map<String, String> outputConversionPairs = dictionary.metadata.getOutputConversionPairs();
			wordData.setWord((outputConversionPairs.isEmpty()? word: applyReplacements(word, outputConversionPairs))
				.getBytes(dictionary.metadata.getCharset()));

			forms.add(wordData);
		}
		return forms;
	}

	/**
	 * Apply partial string replacements from a given map.
	 * <p>
	 * Useful if the word needs to be normalized somehow (i.e., ligatures, apostrophes and such).
	 *
	 * @param word         The word to apply replacements to.
	 * @param replacements A map of replacements (from-&gt;to).
	 * @return new string with all replacements applied.
	 */
	public static String applyReplacements(final String word, final Map<String, String> replacements){
		//quite horrible from performance point of view; this should really be a Finite State Transducer
		//(like https://github.com/ChrisBlom/Effestee)
		final StringBuilder sb = new StringBuilder(word);
		for(final Map.Entry<String, String> e : replacements.entrySet()){
			final String key = e.getKey();
			int index = sb.indexOf(key);
			while(index != - 1){
				sb.replace(index, index + key.length(), e.getValue());
				index = sb.indexOf(key, index + key.length());
			}
		}
		return sb.toString();
	}

	/** Return an iterator over all {@link WordData} entries available in the embedded {@link Dictionary}. */
	@Override
	public final Iterator<WordData> iterator(){
		return new DictionaryIterator(dictionary, true);
	}

}
