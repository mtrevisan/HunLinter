package unit731.hunspeller.parsers.hyphenation;


public class HyphenationOptions{

	private int leftMin;
	private int rightMin;

	private final int minDefault;


	public HyphenationOptions(final int minDefault){
		this.minDefault = Math.max(minDefault, 0);

		clear();
	}

	public int getLeftMin(){
		return leftMin;
	}

	public int getRightMin(){
		return rightMin;
	}

	public int getMinDefault(){
		return minDefault;
	}

	public final void clear(){
		leftMin = minDefault;
		rightMin = minDefault;
	}

	public void setLeftMin(final int value){
		leftMin = Math.max(value, minDefault);
	}

	public void setRightMin(final int value){
		rightMin = Math.max(value, minDefault);
	}

	public int getMinimumLength(){
		return leftMin + rightMin;
	}

}
