package unit731.hunspeller.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class Memoizer<T, U>{

	private final Map<T, U> cache = new ConcurrentHashMap<>();


	//thread-safe and recursion-safe implementation using a re-entrant lock
//    private final Map<T, U> cache = new HashMap<>();
//    private final ReentrantLock lock = new ReentrantLock();
	public static <T, U> Function<T, U> memoize(Function<T, U> function){
		return new Memoizer<T, U>().doMemoize(function);
	}

	private Function<T, U> doMemoize(Function<T, U> function){
		return input -> cache.computeIfAbsent(input, function::apply);
	}

	//thread-safe and recursion-safe implementation using a re-entrant lock
	//https://opencredo.com/lambda-memoization-in-java-8/
//    private Function<T, U> doMemoize(Function<T, U> function){
//        return input -> {
//            lock.lock();
//            try{
//                return cache.computeIfAbsent(input, function::apply);
//            }
//            finally{
//                lock.unlock();
//            }
//        };
//    }

}
