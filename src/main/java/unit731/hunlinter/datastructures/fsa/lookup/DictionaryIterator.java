package unit731.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.ArrayUtils;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Iterator;


/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}. The stems can be decoded from compressed format or
 * the compressed form can be preserved.
 */
public class DictionaryIterator implements Iterator<WordData>{

	private final Dictionary dictionary;
	private final boolean decodeStems;

	private final Iterator<ByteBuffer> entriesIter;
	private final SequenceEncoderInterface sequenceEncoder;

	private CharBuffer inflectedCharBuffer = CharBuffer.allocate(0);


	public DictionaryIterator(final Dictionary dictionary, final boolean decodeStems){
		this.dictionary = dictionary;
		this.decodeStems = decodeStems;

		this.entriesIter = dictionary.fsa.iterator();
		this.sequenceEncoder = dictionary.metadata.getSequenceEncoderType().get();
	}

	public boolean hasNext(){
		return entriesIter.hasNext();
	}

	public WordData next(){
		final ByteBuffer entryBuffer = entriesIter.next();
		byte[] array = entryBuffer.array();
		int limit = entryBuffer.remaining();

		//entries are typically: inflected<SEP>codedBase<SEP>tag so try to find this split:

		//find the separator byte's position splitting the inflection instructions from the tag
		int separatorIndex = ArrayUtils.indexOf(array, dictionary.metadata.getSeparator());
		if(separatorIndex == limit)
			throw new RuntimeException("Invalid dictionary entry format (missing separator).");

		final byte[] inflection = new byte[separatorIndex];
		System.arraycopy(array, 0, inflection, 0, separatorIndex);

		final WordData entry = new WordData();
		entry.setWord(inflection);

		//find the next separator byte's position splitting word form and tag
		final int start = separatorIndex + 1;
		separatorIndex = ArrayUtils.indexOf(array, dictionary.metadata.getSeparator(), start);

		//decode the stem into stem buffer
//		if(decodeStems)
//			entry.stemBuffer = sequenceEncoder.decode(inflection, ByteBuffer.wrap(array, 0, separatorIndex));
//		else{
//			entry.stemBuffer = new byte[separatorIndex];
//			System.arraycopy(array, 0, entry.stemBuffer, 0, separatorIndex);
//		}

//		//skip separator character, if present
//		if(separatorIndex + 1 <= limit)
//			separatorIndex ++;
//
//		//decode the tag data
//		entry.tagBuffer = new byte[limit - separatorIndex];
//		System.arraycopy(array, separatorIndex, entry.tagBuffer, 0, limit - separatorIndex);

		return entry;
	}

}
