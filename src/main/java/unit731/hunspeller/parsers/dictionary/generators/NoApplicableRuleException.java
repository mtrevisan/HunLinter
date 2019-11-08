package unit731.hunspeller.parsers.dictionary.generators;

import unit731.hunspeller.parsers.workers.exceptions.HunspellException;


public class NoApplicableRuleException extends HunspellException{

	private static final long serialVersionUID = 2059064935572242745L;


	public NoApplicableRuleException(String text){
		super(text);
	}

}
