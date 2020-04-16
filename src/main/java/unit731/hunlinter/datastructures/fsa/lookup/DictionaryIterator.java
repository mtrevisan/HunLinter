package unit731.hunlinter.datastructures.fsa.lookup;

import unit731.hunlinter.datastructures.fsa.stemming.BufferUtils;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;


/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}. The stems can be decoded from compressed format or
 * the compressed form can be preserved.
 */
public class DictionaryIterator implements Iterator<WordData>{

	private final Charset charset;
	private final Iterator<ByteBuffer> entriesIter;
	private final WordData entry;
	private final byte separator;
	private final boolean decodeStems;

	private byte[] inflectedBuffer = new byte[0];
	private CharBuffer inflectedCharBuffer = CharBuffer.allocate(0);
	private ByteBuffer temp = ByteBuffer.allocate(0);
	private final SequenceEncoderInterface sequenceEncoder;


	public DictionaryIterator(final Dictionary dictionary, final Charset charset, final boolean decodeStems){
		this.entriesIter = dictionary.fsa.iterator();
		this.separator = dictionary.metadata.getSeparator();
		this.sequenceEncoder = dictionary.metadata.getSequenceEncoderType().get();
		this.charset = charset;
		this.entry = new WordData();
		this.decodeStems = decodeStems;
	}

	public boolean hasNext(){
		return entriesIter.hasNext();
	}

	public WordData next(){
		//FIXME
//		final ByteBuffer entryBuffer = entriesIter.next();
//
//		//entries are typically: inflected<SEP>codedBase<SEP>tag so try to find this split
//		byte[] ba = entryBuffer.array();
//		int bbSize = entryBuffer.remaining();
//
//		int sepPos;
//		for(sepPos = 0; sepPos < bbSize; sepPos ++)
//			if(ba[sepPos] == separator)
//				break;
//
//		if(sepPos == bbSize)
//			throw new RuntimeException("Invalid dictionary " + "entry format (missing separator).");
//
//		inflectedBuffer = new byte[sepPos];
//		System.arraycopy(ba, 0, inflectedBuffer, 0, sepPos);
//
//		inflectedCharBuffer = BufferUtils.bytesToChars(charset, inflectedBuffer, inflectedCharBuffer);
//		entry.update(inflectedBuffer, inflectedCharBuffer);
//
//		temp = BufferUtils.clearAndEnsureCapacity(temp, bbSize - sepPos);
//		sepPos ++;
//		temp.put(ba, sepPos, bbSize - sepPos);
//		temp.flip();
//
//		ba = temp.array();
//		bbSize = temp.remaining();
//
//		//find the next separator byte's position splitting word form and tag
//		assert sequenceEncoder.prefixBytes() <= bbSize: sequenceEncoder.getClass() + " >? " + bbSize;
//
//		sepPos = sequenceEncoder.prefixBytes();
//		for(; sepPos < bbSize; sepPos ++)
//			if(ba[sepPos] == separator)
//				break;
//
//		//decode the stem into stem buffer
//		if(decodeStems)
//			entry.stemBuffer = sequenceEncoder.decode(inflectedBuffer, ByteBuffer.wrap(ba, 0, sepPos));
//		else{
//			entry.stemBuffer = new byte[sepPos];
//			System.arraycopy(ba, 0, entry.stemBuffer, 0, sepPos);
//		}
//
//		//skip separator character, if present
//		if(sepPos + 1 <= bbSize)
//			sepPos ++;
//
//		//decode the tag data
//		entry.tagBuffer = new byte[bbSize - sepPos];
//		System.arraycopy(ba, sepPos, entry.tagBuffer, 0, bbSize - sepPos);

		return entry;
	}

}
