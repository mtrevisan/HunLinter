package unit731.hunlinter.workers.core;


public class IndexDataPair<T>{

	private final int index;
	private final T data;


	public static <T> IndexDataPair of(final int index, final T data){
		return new IndexDataPair(index, data);
	}

	private IndexDataPair(final int index, final T data){
		this.index = index;
		this.data = data;
	}

	public int getIndex(){
		return index;
	}

	public T getData(){
		return data;
	}

}
