package unit731.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.ArrayUtils;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.NoEncoder;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;

import java.nio.ByteBuffer;
import java.util.Iterator;


/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}.
 * The stems can be decoded from compressed format or preserved as compressed format.
 */
public class DictionaryIterator implements Iterator<WordData>{

	private final byte separator;

	private final Iterator<ByteBuffer> entriesItr;
	private final SequenceEncoderInterface sequenceEncoder;


	public DictionaryIterator(final Dictionary dictionary, final boolean decodeStems){
		this.separator = dictionary.metadata.getSeparator();

		entriesItr = dictionary.fsa.iterator();
		sequenceEncoder = (decodeStems? dictionary.metadata.getSequenceEncoderType().get(): new NoEncoder());
	}

	public boolean hasNext(){
		return entriesItr.hasNext();
	}

	public WordData next(){
		final ByteBuffer entryBuffer = entriesItr.next();
		final byte[] array = entryBuffer.array();
		final int limit = entryBuffer.remaining();

		//entries are typically: inflected<SEP>stem<SEP>tag so try to find this split:

		//find the separator byte's position splitting the inflection instructions from the tag
		int separatorIndex = ArrayUtils.indexOf(array, separator);
		if(separatorIndex < 0 || separatorIndex > limit)
			throw new RuntimeException("Invalid dictionary entry format (missing separator).");

		final byte[] inflection = new byte[separatorIndex];
		System.arraycopy(array, 0, inflection, 0, separatorIndex);

		final WordData entry = new WordData();
		entry.setWord(inflection);

		//find the next separator byte's position splitting word form and tag
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

		return entry;
	}

}
