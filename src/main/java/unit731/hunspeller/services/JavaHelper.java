package unit731.hunspeller.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;


public class JavaHelper{

	public static <T> Stream<T> nullableToStream(final T[] array){
		return Optional.ofNullable(array)
			.map(Arrays::stream)
			.orElseGet(Stream::empty);
	}

	public static <T> Stream<T> nullableToStream(final Collection<T> collection){
		return Optional.ofNullable(collection)
			.map(Collection::stream)
			.orElseGet(Stream::empty);
	}

}
