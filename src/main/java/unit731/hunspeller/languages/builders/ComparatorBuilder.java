package unit731.hunspeller.languages.builders;

import java.util.Comparator;
import unit731.hunspeller.languages.vec.Word;


public class ComparatorBuilder{

	private static final String LANGUAGE_VENETAN = "vec";

	public static final Comparator<String> DEFAULT_COMPARATOR = (r1, r2) -> r1.compareTo(r2);


	public static Comparator<String> getComparator(String language){
		Comparator<String> cmp;
		switch(language){
			case LANGUAGE_VENETAN:
				cmp = Word.sorterComparator();
				break;

			default:
				cmp = DEFAULT_COMPARATOR;
		}
		return cmp;
	}

}
