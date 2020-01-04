package unit731.hunlinter.parsers.enums;

import java.util.Arrays;


public enum InflectionTag{

	TAG_SINGULAR("singular", "s"),
	TAG_PLURAL("plural", "p"),
	TAG_MASCULINE("masculine", "m"),
	TAG_FEMENINE("femenine", "f"),
	TAG_SINGULAR_MASCULINE("singular_masculine", "s", "m"),
	TAG_SINGULAR_FEMENINE("singular_femenine", "s", "f"),
	TAG_PLURAL_MASCULINE("plural_masculine", "p", "m"),
	TAG_PLURAL_FEMENINE("plural_femenine", "p", "f"),

	TAG_FIRST_SINGULAR("first_singular", "1", "s"),
	TAG_FIRST_PLURAL("first_plural", "1", "p"),
	TAG_FIRST_SINGULAR_MASCULINE("first_singular_masculine", "1", "s", "m"),
	TAG_FIRST_PLURAL_MASCULINE("first_plural_masculine", "1", "p", "m"),
	TAG_FIRST_SINGULAR_FEMENINE("first_singular_femenine", "1", "s", "f"),
	TAG_FIRST_PLURAL_FEMENINE("first_plural_femenine", "1", "p", "f"),

	TAG_SECOND_SINGULAR("second_singular", "2", "s"),
	TAG_SECOND_PLURAL("second_plural", "2", "p"),
	TAG_SECOND_SINGULAR_MASCULINE("second_singular_masculine", "2", "s", "m"),
	TAG_SECOND_PLURAL_MASCULINE("second_plural_masculine", "2", "p", "m"),
	TAG_SECOND_SINGULAR_FEMENINE("second_singular_femenine", "2", "s", "f"),
	TAG_SECOND_PLURAL_FEMENINE("second_plural_femenine", "2", "p", "f"),

	TAG_THIRD("third", "3"),
	TAG_THIRD_SINGULAR("third_singular", "3", "s"),
	TAG_THIRD_PLURAL("third_plural", "3", "p"),
	TAG_THIRD_SINGULAR_MASCULINE("third_singular_masculine", "3", "s", "m"),
	TAG_THIRD_PLURAL_MASCULINE("third_plural_masculine", "3", "p", "m"),
	TAG_THIRD_SINGULAR_FEMENINE("third_singular_femenine", "3", "s", "f"),
	TAG_THIRD_PLURAL_FEMENINE("third_plural_femenine", "3", "p", "f"),

	TAG_NON_ENUMERABLE("non_enumerable", "ne"),

	TAG_PROCOMPLEMENTAR("procomplementar", "pc"),
	TAG_INTERROGATIVE("interrogative", "in"),

	TAG_NORDIC("nordic", "n");


	private final String code;
	private final String[] tags;


	InflectionTag(final String code, final String... tags){
		this.code = code;
		this.tags = tags;
	}

	public static InflectionTag createFromCode(final String code){
		return Arrays.stream(values())
			.filter(tag -> tag.code.equals(code))
			.findFirst()
			.orElse(null);
	}

	public String getCode(){
		return code;
	}

	public String[] getTags(){
		return tags;
	}

}
