package unit731.hunspeller.collections.ahocorasicktrie;


/**
 * A result output
 *
 * @param <V>	The type of values stored in the tree
 */
public class Hit<V>{

	/** the beginning index, inclusive */
	public final int begin;
	/** the ending index, exclusive */
	public final int end;
	/** the value assigned to the keyword */
	public final V value;


	public Hit(int begin, int end, V value){
		this.begin = begin;
		this.end = end;
		this.value = value;
	}

	@Override
	public String toString(){
		return String.format("[%d:%d] = %s", begin, end, value);
	}

}
