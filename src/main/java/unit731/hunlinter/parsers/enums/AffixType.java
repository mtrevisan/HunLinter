package unit731.hunlinter.parsers.enums;

import static unit731.hunlinter.services.system.LoopHelper.match;


public enum AffixType{

	SUFFIX(AffixOption.SUFFIX),
	PREFIX(AffixOption.PREFIX);


	private final AffixOption option;

	AffixType(final AffixOption option){
		this.option = option;
	}

	public static AffixType createFromCode(final String code){
		return match(values(), tag -> tag.option.is(code));
	}

	public AffixOption getOption(){
		return option;
	}

}
