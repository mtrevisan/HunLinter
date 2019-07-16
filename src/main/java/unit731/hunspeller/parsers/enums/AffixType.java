package unit731.hunspeller.parsers.enums;

import java.util.Arrays;


public enum AffixType{

	SUFFIX(AffixTag.SUFFIX),
	PREFIX(AffixTag.PREFIX);


	private final AffixTag tag;

	AffixType(final AffixTag tag){
		this.tag = tag;
	}

	public static AffixType createFromCode(final String code){
		return Arrays.stream(values())
			.filter(t -> t.tag.getCode().equals(code))
			.findFirst()
			.orElse(null);
	}

	public boolean is(final String flag){
		return this.tag.getCode().equals(flag);
	}

	public AffixTag getTag(){
		return tag;
	}

}
