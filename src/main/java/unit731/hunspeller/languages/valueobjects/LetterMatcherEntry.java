package unit731.hunspeller.languages.valueobjects;

import java.text.MessageFormat;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;


public class LetterMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterLetter;
	private final String[] wrongFlags;
	private final String correctRule;


	public LetterMatcherEntry(MessageFormat messagePattern, String masterLetter, String[] wrongFlags, String correctRule){
		this.messagePattern = messagePattern;
		this.masterLetter = masterLetter;
		this.wrongFlags = wrongFlags;
		this.correctRule = correctRule;
	}

	public void match(Production production) throws IllegalArgumentException{
		for(String flag : wrongFlags)
			if(production.hasContinuationFlag(flag)){
				String message = messagePattern.format(new Object[]{masterLetter, flag, correctRule, production.getWord()});
				throw new IllegalArgumentException(message);
			}
	}
	
}
