package unit731.hunspeller.collections.bloomfilter.decompose;

import java.nio.charset.Charset;


/**
 * The default implementation of {@link Decomposer} that decomposes the object by converting it to a {@link String} object using the
 * {@link Object#toString()} method.
 *
 * To convert the {@link String} thus obtained into bytes, the default platform {@link Charset} encoding is used.
 *
 * @param <T> the type of objects to be stored in the filter
 */
public class DefaultDecomposer<T> implements Decomposer<T>{

	/**
	 * Decompose the object
	 */
	@Override
	public void decompose(final T object, final ByteSink sink, final Charset charset){
		if(object != null){
			byte[] bytes;
			if(String.class.isAssignableFrom(object.getClass()))
				bytes = ((String)object).getBytes(charset);
			else
				bytes = object.toString().getBytes(charset);
			sink.putBytes(bytes);
		}
	}

}
