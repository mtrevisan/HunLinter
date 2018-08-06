package unit731.hunspeller.languages.builders;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.vec.OrthographyVEC;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrthographyBuilder{

	private static final String LANGUAGE_VENETAN = "vec";


	public static Orthography getOrthography(String language){
		Orthography ortho;
		switch(language){
			case LANGUAGE_VENETAN:
				ortho = OrthographyVEC.getInstance();
				break;

			default:
				ortho = Orthography.getInstance();
		}
		return ortho;
	}

}
