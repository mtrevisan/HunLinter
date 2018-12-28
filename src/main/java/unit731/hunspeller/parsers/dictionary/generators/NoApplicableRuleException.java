package unit731.hunspeller.parsers.dictionary.generators;


public class NoApplicableRuleException extends IllegalArgumentException{

	private static final long serialVersionUID = 2059064935572242745L;


	public NoApplicableRuleException(String text){
		super(text);
	}
	
}
