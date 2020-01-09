package unit731.hunlinter.parsers.enums;

import java.util.Arrays;


public enum InflectionTag{

	TAG_SINGULAR("singular", "s"),
	TAG_PLURAL("plural", "p"),
	TAG_MASCULINE("masculine", "m"),
	TAG_FEMENINE("femenine", "f"),
	TAG_SINGULAR_MASCULINE("singular+masculine", "s", "m"),
	TAG_SINGULAR_FEMENINE("singular+femenine", "s", "f"),
	TAG_PLURAL_MASCULINE("plural+masculine", "p", "m"),
	TAG_PLURAL_FEMENINE("plural+femenine", "p", "f"),

	TAG_FIRST_SINGULAR("first+singular", "1", "s"),
	TAG_FIRST_PLURAL("first+plural", "1", "p"),
	TAG_FIRST_SINGULAR_MASCULINE("first+singular+masculine", "1", "s", "m"),
	TAG_FIRST_PLURAL_MASCULINE("first+plural+masculine", "1", "p", "m"),
	TAG_FIRST_SINGULAR_FEMENINE("first+singular+femenine", "1", "s", "f"),
	TAG_FIRST_PLURAL_FEMENINE("first+plural+femenine", "1", "p", "f"),

	TAG_SECOND_SINGULAR("second+singular", "2", "s"),
	TAG_SECOND_PLURAL("second+plural", "2", "p"),
	TAG_SECOND_SINGULAR_MASCULINE("second+singular+masculine", "2", "s", "m"),
	TAG_SECOND_PLURAL_MASCULINE("second+plural+masculine", "2", "p", "m"),
	TAG_SECOND_SINGULAR_FEMENINE("second+singular+femenine", "2", "s", "f"),
	TAG_SECOND_PLURAL_FEMENINE("second+plural+femenine", "2", "p", "f"),

	TAG_THIRD("third", "3"),
	TAG_THIRD_SINGULAR("third+singular", "3", "s"),
	TAG_THIRD_PLURAL("third+plural", "3", "p"),
	TAG_THIRD_SINGULAR_MASCULINE("third+singular+masculine", "3", "s", "m"),
	TAG_THIRD_PLURAL_MASCULINE("third+plural+masculine", "3", "p", "m"),
	TAG_THIRD_SINGULAR_FEMENINE("third+singular+femenine", "3", "s", "f"),
	TAG_THIRD_PLURAL_FEMENINE("third+plural+femenine", "3", "p", "f"),

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
