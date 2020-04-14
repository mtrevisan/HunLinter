package unit731.hunlinter.services.log;

import org.apache.commons.lang3.builder.ToStringStyle;


public class ShortPrefixNotNullToStringStyle extends ToStringStyle{

	public static final ToStringStyle SHORT_PREFIX_NOT_NULL_STYLE = new ShortPrefixNotNullToStringStyle();


	private ShortPrefixNotNullToStringStyle(){
		super();

		this.setUseShortClassName(true);
		this.setUseIdentityHashCode(false);
	}

	@Override
	public void append(final StringBuffer buffer, final String fieldName, final Object value, final Boolean fullDetail){
		if(value != null){
			super.append(buffer, fieldName, value, fullDetail);
		}
	}

	@SuppressWarnings({"unused", "SameReturnValue"})
	private Object readResolve(){
		return SHORT_PREFIX_NOT_NULL_STYLE;
	}

}
