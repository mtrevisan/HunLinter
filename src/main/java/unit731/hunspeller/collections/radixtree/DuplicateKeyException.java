package unit731.hunspeller.collections.radixtree;


public class DuplicateKeyException extends RuntimeException{

	public DuplicateKeyException(){
		super();
	}

	public DuplicateKeyException(String msg){
		super(msg);
	}

}
