package unit731.hunlinter.parsers.enums;


import unit731.hunlinter.services.system.LoopHelper;


public enum AffixType{

	SUFFIX(AffixOption.SUFFIX),
	PREFIX(AffixOption.PREFIX);


	private final AffixOption option;

	AffixType(final AffixOption option){
		this.option = option;
	}

	public static AffixType createFromCode(final String code){
		return LoopHelper.match(values(), tag -> tag.option.getCode().equals(code));
	}

	public boolean is(final String code){
		return this.option.getCode().equals(code);
	}

	public AffixOption getOption(){
		return option;
	}

}
