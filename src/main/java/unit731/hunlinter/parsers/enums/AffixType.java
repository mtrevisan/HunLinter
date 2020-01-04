package unit731.hunlinter.parsers.enums;

import java.util.Arrays;


public enum AffixType{

	SUFFIX(AffixOption.SUFFIX),
	PREFIX(AffixOption.PREFIX);


	private final AffixOption option;

	AffixType(final AffixOption option){
		this.option = option;
	}

	public static AffixType createFromCode(final String code){
		return Arrays.stream(values())
			.filter(t -> t.option.getCode().equals(code))
			.findFirst()
			.orElse(null);
	}

	public boolean is(final String code){
		return this.option.getCode().equals(code);
	}

	public AffixOption getOption(){
		return option;
	}

}
