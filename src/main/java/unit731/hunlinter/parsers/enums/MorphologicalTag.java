package unit731.hunlinter.parsers.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/** Default morphological fields */
public enum MorphologicalTag{

	STEM("st:"),
	ALLOMORPH("al:"),
	PART_OF_SPEECH("po:"),

	DERIVATIONAL_PREFIX("dp:"),
	INFLECTIONAL_PREFIX("ip:"),
	TERMINAL_PREFIX("tp:"),

	DERIVATIONAL_SUFFIX("ds:"),
	INFLECTIONAL_SUFFIX("is:"),
	TERMINAL_SUFFIX("ts:"),

	SURFACE_PREFIX("sp:"),

	FREQUENCY("fr:"),
	PHONETIC("ph:"),
	HYPHENATION("hy:"),
	PART("pa:"),
	FLAG("fl:");


	private static final Map<String, MorphologicalTag> VALUES = new HashMap<>();
	static{
		for(final MorphologicalTag tag : EnumSet.allOf(MorphologicalTag.class))
			VALUES.put(tag.getCode(), tag);
	}

	private final String code;


	MorphologicalTag(final String code){
		this.code = code;
	}

	public static MorphologicalTag createFromCode(final String code){
		return VALUES.get(code.substring(0, 3));
	}

	public String getCode(){
		return code;
	}

	public boolean isSupertypeOf(final String codeAndValue){
		return codeAndValue.startsWith(code);
	}

	public String attachValue(final String value){
		return code + value;
	}

}
