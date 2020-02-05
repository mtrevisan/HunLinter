package unit731.hunlinter.parsers.dictionary.generators;

import unit731.hunlinter.parsers.workers.exceptions.LinterException;


public class NoApplicableRuleException extends LinterException{

	private static final long serialVersionUID = 2059064935572242745L;


	public NoApplicableRuleException(String text){
		super(text);
	}

}
