package unit731.hunspeller.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;


public class Memoizer<T, U>{

	private static final Object DEFAULT_OBJECT = new Object();


	private Memoizer(){}

	public static <T> Supplier<T> memoize(Supplier<T> supplier){
		Map<Object, T> cache = new ConcurrentHashMap<>();
		return () -> cache.computeIfAbsent(DEFAULT_OBJECT, t -> supplier.get());
	}

	public static <T, U> Function<T, U> memoize(Function<T, U> function){
		Map<T, U> cache = new ConcurrentHashMap<>();
		return input -> cache.computeIfAbsent(input, function);
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock
	 *
	 * @param <T>			Type of input to the function
	 * @param supplier	The function to be memoized
	 * @return				The new memoized function
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <T> Supplier<T> memoizeThreadAndRecursionSafe(Supplier<T> supplier){
		Map<Object, T> cache = new HashMap<>();
		ReentrantLock lock = new ReentrantLock();
		return () -> {
			lock.lock();
			try{
				return cache.computeIfAbsent(DEFAULT_OBJECT, t -> supplier.get());
			}
			finally{
				lock.unlock();
			}
		};
	}

	/**
	 * Thread-safe and recursion-safe implementation using a re-entrant lock
	 *
	 * @param <T>			Type of input to the function
	 * @param <U>			Type of output from the function
	 * @param function	The function to be memoized
	 * @return				The new memoized function
	 * @see <a href="https://opencredo.com/lambda-memoization-in-java-8/">Lambda memoization in Java 8</a>
	 */
	public static <T, U> Function<T, U> memoizeThreadAndRecursionSafe(Function<T, U> function){
		Map<T, U> cache = new HashMap<>();
		ReentrantLock lock = new ReentrantLock();
		return input -> {
			lock.lock();
			try{
				return cache.computeIfAbsent(input, function::apply);
			}
			finally{
				lock.unlock();
			}
		};
	}

}
