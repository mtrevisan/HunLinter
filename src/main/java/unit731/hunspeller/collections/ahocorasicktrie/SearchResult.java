package unit731.hunspeller.collections.ahocorasicktrie;


/**
 * A result output
 *
 * @param <V>	The type of values stored in the tree
 */
public class SearchResult<V>{

	/** the beginning index, inclusive */
	private final int start;
	/** the ending index, exclusive */
	private final int end;
	/** the value assigned to the keyword */
	private final V value;


	public SearchResult(int begin, int end, V value){
		this.start = begin;
		this.end = end;
		this.value = value;
	}

	public int getIndexBegin(){
		return start;
	}

	public int getIndexEnd(){
		return end;
	}

	public int getMatchLength(){
		return end - start;
	}

	public V getValue(){
		return value;
	}

	@Override
	public String toString(){
		return String.format("[%d:%d] = %s", start, end, value);
	}

}
