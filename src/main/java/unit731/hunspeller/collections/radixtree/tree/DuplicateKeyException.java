package unit731.hunspeller.collections.radixtree.tree;


public class DuplicateKeyException extends RuntimeException{

	public DuplicateKeyException(){
		super();
	}

	public DuplicateKeyException(String msg){
		super(msg);
	}

	public DuplicateKeyException(String msg, Throwable cause){
		super(msg, cause);
	}

}
