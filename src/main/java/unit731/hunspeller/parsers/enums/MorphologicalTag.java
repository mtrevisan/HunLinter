package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum MorphologicalTag{

	//default morphological fields:
	TAG_STEM("st:"),
	TAG_ALLOMORPH("al:"),
	TAG_PART_OF_SPEECH("po:"),

	TAG_DERIVATIONAL_PREFIX("dp:"),
	TAG_INFLECTIONAL_PREFIX("ip:"),
	TAG_TERMINAL_PREFIX("tp:"),

	TAG_DERIVATIONAL_SUFFIX("ds:"),
	TAG_INFLECTIONAL_SUFFIX("is:"),
	TAG_TERMINAL_SUFFIX("ts:"),

	TAG_SURFACE_PREFIX("sp:"),

	TAG_FREQUENCY("fr:"),
	TAG_PHONETIC("ph:"),
	TAG_HYPHENATION("hy:"),
	TAG_PART("pa:"),
	TAG_FLAG("fl:");


	private final String code;


	MorphologicalTag(final String code){
		this.code = code;
	}

	public static MorphologicalTag createFromCode(final String code){
		return Arrays.stream(values())
			.filter(tag -> tag.code.equals(code))
			.findFirst()
			.orElse(null);
	}

	public String getCode(){
		return code;
	}

	public String attachValue(final String value){
		return code + value;
	}

}
