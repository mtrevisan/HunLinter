package unit731.hunlinter.services.fsa.builders;

public class MinMax{

	public final int min;
	public final int max;


	MinMax(int min, int max){
		this.min = Math.min(min, max);
		this.max = Math.max(min, max);
	}

	public int range(){
		return max - min;
	}

}
