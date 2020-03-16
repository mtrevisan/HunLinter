package unit731.hunlinter.parsers.enums;


public enum AffixType{

	SUFFIX(AffixOption.SUFFIX),
	PREFIX(AffixOption.PREFIX);


	private final AffixOption option;

	AffixType(final AffixOption option){
		this.option = option;
	}

	public static AffixType createFromCode(final String code){
		for(final AffixType tag : values())
			if(tag.option.getCode().equals(code))
				return tag;
		return null;
	}

	public boolean is(final String code){
		return this.option.getCode().equals(code);
	}

	public AffixOption getOption(){
		return option;
	}

}
