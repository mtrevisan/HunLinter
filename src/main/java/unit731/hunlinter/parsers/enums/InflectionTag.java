package unit731.hunlinter.parsers.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public enum InflectionTag{

	SINGULAR("singular", "s"),
	PLURAL("plural", "p"),
	MASCULINE("masculine", "m"),
	FEMENINE("femenine", "f"),
	SINGULAR_MASCULINE("singular+masculine", "s", "m"),
	SINGULAR_FEMENINE("singular+femenine", "s", "f"),
	PLURAL_MASCULINE("plural+masculine", "p", "m"),
	PLURAL_FEMENINE("plural+femenine", "p", "f"),

	FIRST_SINGULAR("first+singular", "1", "s"),
	FIRST_PLURAL("first+plural", "1", "p"),
	FIRST_SINGULAR_MASCULINE("first+singular+masculine", "1", "s", "m"),
	FIRST_PLURAL_MASCULINE("first+plural+masculine", "1", "p", "m"),
	FIRST_SINGULAR_FEMENINE("first+singular+femenine", "1", "s", "f"),
	FIRST_PLURAL_FEMENINE("first+plural+femenine", "1", "p", "f"),

	SECOND_SINGULAR("second+singular", "2", "s"),
	SECOND_PLURAL("second+plural", "2", "p"),
	SECOND_SINGULAR_MASCULINE("second+singular+masculine", "2", "s", "m"),
	SECOND_PLURAL_MASCULINE("second+plural+masculine", "2", "p", "m"),
	SECOND_SINGULAR_FEMENINE("second+singular+femenine", "2", "s", "f"),
	SECOND_PLURAL_FEMENINE("second+plural+femenine", "2", "p", "f"),

	THIRD("third", "3"),
	THIRD_SINGULAR("third+singular", "3", "s"),
	THIRD_PLURAL("third+plural", "3", "p"),
	THIRD_SINGULAR_MASCULINE("third+singular+masculine", "3", "s", "m"),
	THIRD_PLURAL_MASCULINE("third+plural+masculine", "3", "p", "m"),
	THIRD_SINGULAR_FEMENINE("third+singular+femenine", "3", "s", "f"),
	THIRD_PLURAL_FEMENINE("third+plural+femenine", "3", "p", "f"),

	NON_ENUMERABLE("non_enumerable", "ne"),
	PLURAL_NON_ENUMERABLE("plural+non_enumerable", "p", "ne"),
	SINGULAR_MASCULINE_NON_ENUMERABLE("singular+masculine+non_enumerable", "s", "m", "ne"),
	SINGULAR_FEMENINE_NON_ENUMERABLE("singular+femenine+non_enumerable", "s", "f", "ne"),

	PROCOMPLEMENTAR("procomplementar", "pc"),
	INTERROGATIVE("interrogative", "in"),

	NORDIC("nordic", "n");


	private static final Map<String, InflectionTag> VALUES = new HashMap<>();
	static{
		for(final InflectionTag tag : EnumSet.allOf(InflectionTag.class))
			VALUES.put(MorphologicalTag.INFLECTIONAL_SUFFIX.getCode() + tag.code, tag);
	}

	private final String code;
	private final String[] tags;


	InflectionTag(final String code, final String... tags){
		this.code = code;
		this.tags = tags;
	}

	public static InflectionTag createFromCodeAndValue(final String codeAndValue){
		return VALUES.get(codeAndValue);
	}

	public String getCode(){
		return code;
	}

	public String[] getTags(){
		return tags;
	}

}
