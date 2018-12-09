package unit731.hunspeller.languages.valueobjects;

import java.text.MessageFormat;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;


public class RuleMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterFlag;
	private final String[] wrongFlags;


	public RuleMatcherEntry(MessageFormat messagePattern, String masterFlag, String[] wrongFlags){
		this.messagePattern = messagePattern;
		this.masterFlag = masterFlag;
		this.wrongFlags = wrongFlags;
	}

	public void match(Production production) throws IllegalArgumentException{
		for(String flag : wrongFlags)
			if(production.hasContinuationFlag(flag)){
				String message = messagePattern.format(new Object[]{masterFlag, flag, production.getWord()});
				throw new IllegalArgumentException(message);
			}
	}
	
}
