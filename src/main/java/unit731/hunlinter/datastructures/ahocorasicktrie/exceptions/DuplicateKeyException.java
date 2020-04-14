package unit731.hunlinter.datastructures.ahocorasicktrie.exceptions;


public class DuplicateKeyException extends RuntimeException{

	private static final long serialVersionUID = 7292569015644514555L;


	public DuplicateKeyException(){
		super();
	}

	public DuplicateKeyException(final String msg){
		super(msg);
	}

}
