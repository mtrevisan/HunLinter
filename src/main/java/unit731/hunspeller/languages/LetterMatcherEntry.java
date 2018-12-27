package unit731.hunspeller.languages;

import java.text.MessageFormat;
import unit731.hunspeller.parsers.dictionary.vos.Production;


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
			if(production.hasContinuationFlag(flag))
				throw new IllegalArgumentException(messagePattern.format(new Object[]{masterLetter, flag, correctRule}));
	}
	
}
