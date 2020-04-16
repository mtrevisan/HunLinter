package unit731.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.ArrayUtils;
import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.datastructures.fsa.ByteSequenceIterator;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.NoEncoder;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;
import unit731.hunlinter.datastructures.fsa.stemming.TrimInfixAndSuffixEncoder;
import unit731.hunlinter.datastructures.fsa.stemming.TrimPrefixAndSuffixEncoder;
import unit731.hunlinter.datastructures.fsa.stemming.TrimSuffixEncoder;

import java.nio.ByteBuffer;
import java.util.Arrays;
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
		this.sequenceEncoder = dictionary.metadata.getSequenceEncoderType().get();
		this.matcher = new FSATraversal(dictionary.fsa);
		this.finalStatesIterator = new ByteSequenceIterator(dictionary.fsa, dictionary.fsa.getRootNode());
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

		/**
		 * The number of encoded form's prefix bytes that should be ignored (needed for separator lookup).
		 * An ugly workaround for GH-85, should be fixed by prior knowledge of whether the dictionary contains tags;
		 * then we can scan for separator right-to-left.
		 * @see "https://github.com/morfologik/morfologik-stemming/issues/85"
		 */
		int prefixBytes = -1;
		if(sequenceEncoder instanceof NoEncoder)
			prefixBytes = 0;
		else if(sequenceEncoder instanceof TrimSuffixEncoder)
			prefixBytes = 1;
		else if(sequenceEncoder instanceof TrimPrefixAndSuffixEncoder)
			prefixBytes = 2;
		else if(sequenceEncoder instanceof TrimInfixAndSuffixEncoder)
			prefixBytes = 3;

		if(!dictionary.metadata.getInputConversionPairs().isEmpty())
			word = applyReplacements(word, dictionary.metadata.getInputConversionPairs());

		//encode word characters into bytes in the same encoding as the FSA's
		final byte[] wordAsByteArray = word.getBytes(dictionary.metadata.getCharset());
		Arrays.sort(wordAsByteArray);
		if(ArrayUtils.indexOf(wordAsByteArray, separator) >= 0)
			throw new IllegalArgumentException("No valid input can contain the separator as in " + word);

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
					final byte[] ba = bb.array();
					final int bbSize = bb.remaining();

					//now, expand the prefix/ suffix 'compression' and store the base form
					final WordData wordData = new WordData();
					if(dictionary.metadata.getOutputConversionPairs().isEmpty())
						wordData.setWord(word);
					else
						wordData.setWord(applyReplacements(word, dictionary.metadata.getOutputConversionPairs()));

					//find the separator byte's position splitting the inflection instructions
					//from the tag
					assert prefixBytes <= bbSize: sequenceEncoder.getClass() + " >? " + bbSize;

					int sepPos;
					for(sepPos = prefixBytes; sepPos < bbSize; sepPos ++)
						if(ba[sepPos] == separator)
							break;

					//decode the stem into stem buffer
					wordData.setStem(sequenceEncoder.decode(wordAsByteArray, ByteBuffer.wrap(ba, 0, sepPos)));

					//skip separator character
					sepPos ++;

					//decode the tag data
					final int tagSize = bbSize - sepPos;
					if(tagSize > 0){
						//FIXME mmm...
						wordData.setTag(new byte[tagSize]);
						System.arraycopy(ba, sepPos, wordData, 0, tagSize);
					}

					forms.add(wordData);
				}
			}
		}
		else{
			/*
			 * this case is somewhat confusing: we should have hit the separator
			 * first... I don't really know how to deal with it at the time being.
			 */
		}
		return forms.extractCopyOrNull();
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
		return new DictionaryIterator(dictionary, dictionary.metadata.getCharset(), true);
	}

	public Dictionary getDictionary(){
		return dictionary;
	}

}
