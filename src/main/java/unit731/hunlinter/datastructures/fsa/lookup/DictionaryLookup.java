package unit731.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.ArrayUtils;
import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;


/**
 * This class implements a dictionary lookup of an inflected word over a
 * dictionary previously compiled using the
 * <code>dict_compile</code> tool.
 */
public class DictionaryLookup implements Iterable<WordData>{

	/** An FSA used for lookups */
	private final FSATraversal matcher;

	/** An iterator for walking along the final states of the given FSA */
	private final ByteSequenceIterator finalStatesIterator;

	/** Private internal array of reusable word data objects */
	private final SimpleDynamicArray<WordData> forms = new SimpleDynamicArray<>(WordData.class);

	/** The {@link Dictionary} this lookup is using */
	private final Dictionary dictionary;

	private final SequenceEncoderInterface sequenceEncoder;


	/**
	 * Creates a new object of this class using the given FSA for word lookups
	 * and encoding for converting characters to bytes.
	 *
	 * @param dictionary	The dictionary to use for lookups.
	 * @throws IllegalArgumentException	If FSA's root node cannot be acquired (dictionary is empty).
	 */
	public DictionaryLookup(final Dictionary dictionary) throws IllegalArgumentException{
		Objects.requireNonNull(dictionary);
		Objects.requireNonNull(dictionary.fsa);
		Objects.requireNonNull(dictionary.metadata);

		this.dictionary = dictionary;

		sequenceEncoder = dictionary.metadata.getSequenceEncoderType().get();
		matcher = new FSATraversal(dictionary.fsa);
		finalStatesIterator = new ByteSequenceIterator(dictionary.fsa, dictionary.fsa.getRootNode());
	}

	/**
	 * Returns a list of {@link WordData} entries for a given word. The returned
	 * list is never <code>null</code>. Depending on the stemmer's
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
	public WordData[] lookup(String word){
		final byte separator = dictionary.metadata.getSeparator();

		if(!dictionary.metadata.getInputConversionPairs().isEmpty())
			word = applyReplacements(word, dictionary.metadata.getInputConversionPairs());

		//encode word characters into bytes in the same encoding as the FSA's
		final byte[] wordAsByteArray = word.getBytes(dictionary.metadata.getCharset());
		if(ArrayUtils.indexOf(wordAsByteArray, separator) >= 0)
			throw new IllegalArgumentException("No valid input can contain the separator: " + word);

		forms.reset();

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

					//now, expand the prefix/ suffix 'compression' and store the base form
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
			//this case is somewhat confusing: we should have hit the separator first...
			//I don't really know how to deal with it at the time being.
			throw new IllegalArgumentException("what?!?!");
		}
		return forms.extractCopy();
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
	static String applyReplacements(final String word, final Map<String, String> replacements){
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

	/** Return an iterator over all {@link WordData} entries available in the embedded {@link Dictionary} */
	@Override
	public Iterator<WordData> iterator(){
		return new DictionaryIterator(dictionary, true);
	}

}
