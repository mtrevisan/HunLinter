package unit731.hunlinter.parsers.enums;

import static unit731.hunlinter.services.system.LoopHelper.match;


public enum MorphologicalTag{

	//default morphological fields:
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


	private final String code;


	MorphologicalTag(final String code){
		this.code = code;
	}

	public static MorphologicalTag createFromCode(final String code){
		return match(values(), tag -> tag.isSupertypeOf(code));
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
