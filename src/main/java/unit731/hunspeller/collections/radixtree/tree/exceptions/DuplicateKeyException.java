package unit731.hunspeller.collections.radixtree.tree.exceptions;


public class DuplicateKeyException extends RuntimeException{

	private static final long serialVersionUID = 7292569015644514555L;


	public DuplicateKeyException(){
		super();
	}

	public DuplicateKeyException(String msg){
		super(msg);
	}

}
