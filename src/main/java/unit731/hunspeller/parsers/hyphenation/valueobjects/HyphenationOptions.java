package unit731.hunspeller.parsers.hyphenation.valueobjects;

import lombok.Getter;


@Getter
public class HyphenationOptions{

	private int leftMin;
	private int rightMin;

	private final int minDefault;


	public HyphenationOptions(int minDefault){
		this.minDefault = Math.max(minDefault, 0);

		clear();
	}

	public final void clear(){
		leftMin = minDefault;
		rightMin = minDefault;
	}

	public void setLeftMin(int value){
		leftMin = Math.max(value, 0);
	}

	public void setRightMin(int value){
		rightMin = Math.max(value, 0);
	}

	public int getMinimumLength(){
		return leftMin + rightMin;
	}

}
