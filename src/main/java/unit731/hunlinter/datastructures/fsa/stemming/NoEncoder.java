package unit731.hunlinter.datastructures.fsa.stemming;


/**
 * No relative encoding at all (full target form is returned).
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class NoEncoder implements SequenceEncoderInterface{

	@Override
	public byte[] encode(final byte[] source, final byte[] target){
		return target;
	}

	@Override
	public byte[] decode(final byte[] source, final byte[] encoded){
		return encoded;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
