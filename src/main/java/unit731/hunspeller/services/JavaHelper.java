package unit731.hunspeller.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;


public class JavaHelper{

	public static <T> Stream<T> nullableToStream(final T[] array){
		return Optional.ofNullable(array).stream()
			.flatMap(Arrays::stream);
	}

	public static <T> Stream<T> nullableToStream(final Collection<T> collection){
		return Optional.ofNullable(collection).stream()
			.flatMap(Collection::stream);
	}

}
