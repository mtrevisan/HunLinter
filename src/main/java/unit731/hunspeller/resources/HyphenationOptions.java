package unit731.hunspeller.resources;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class HyphenationOptions{

	@Builder.Default private int leftMin = 2;
	@Builder.Default private int rightMin = 2;
	private int leftCompoundMin;
	private int rightCompoundMin;
	private String[] noHyphen;


	public void clear(){
		leftMin = 2;
		rightMin = 2;
		leftCompoundMin = 0;
		rightCompoundMin = 0;
		noHyphen = null;
	}

}
