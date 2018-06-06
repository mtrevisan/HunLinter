package unit731.hunspeller.parsers.dictionary;


public class NoApplicableRuleException extends IllegalArgumentException{

	NoApplicableRuleException(String text){
		super(text);
	}
	
}
