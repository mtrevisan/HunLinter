package unit731.hunspeller.languages.builders;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import unit731.hunspeller.languages.vec.DictionaryCorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.WordVEC;


public class ComparatorBuilder{

	public static final Comparator<String> COMPARATOR_LENGTH = (r1, r2) -> Integer.compare(r1.length(), r2.length());
	public static final Comparator<String> COMPARATOR_DEFAULT = (r1, r2) -> r1.compareTo(r2);

	private static final Map<String, Comparator<String>> COMPARATORS = new HashMap<>();
	static{
		COMPARATORS.put(DictionaryCorrectnessCheckerVEC.LANGUAGE, WordVEC.sorterComparator());
	}


	private ComparatorBuilder(){}

	public static Comparator<String> getComparator(String language){
		return COMPARATORS.getOrDefault(language, COMPARATOR_DEFAULT);
	}

}
