package unit731.hunspeller.collections.bloomfilter.decompose;

import java.nio.charset.Charset;


/**
 * Contract for implementations that wish to help in decomposing an object to a byte-array so that various hashes can be computed
 * over the same.
 *
 * @param <T> the type of object over which this decomposer works
 */
public interface Decomposer<T>{

	/**
	 * Decompose the object into the given {@link ByteSink}
	 *
	 * @param object	The object to be decomposed
	 * @param sink	The sink to which the object is decomposed
	 * @param charset	The charset to be used
	 */
	void decompose(T object, ByteSink sink, Charset charset);

}
