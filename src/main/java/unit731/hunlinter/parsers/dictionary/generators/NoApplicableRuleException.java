package unit731.hunlinter.parsers.dictionary.generators;

import unit731.hunlinter.parsers.workers.exceptions.HunLintException;


public class NoApplicableRuleException extends HunLintException{

	private static final long serialVersionUID = 2059064935572242745L;


	public NoApplicableRuleException(String text){
		super(text);
	}

}
