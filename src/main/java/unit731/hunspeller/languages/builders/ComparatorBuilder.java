package unit731.hunspeller.languages.builders;

import java.util.Comparator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.languages.vec.CorrectnessCheckerVEC;
import unit731.hunspeller.languages.vec.WordVEC;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ComparatorBuilder{

	public static final Comparator<String> COMPARATOR_LENGTH = (r1, r2) -> Integer.compare(r1.length(), r2.length());
	public static final Comparator<String> COMPARATOR_DEFAULT = (r1, r2) -> r1.compareTo(r2);


	public static Comparator<String> getComparator(String language){
		Comparator<String> cmp;
		switch(language){
			case CorrectnessCheckerVEC.LANGUAGE:
				cmp = WordVEC.sorterComparator();
				break;

			default:
				cmp = COMPARATOR_DEFAULT;
		}
		return cmp;
	}

}
