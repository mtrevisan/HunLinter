package unit731.hunspeller.languages;

import java.text.MessageFormat;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class RuleMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterFlag;
	private final String[] wrongFlags;


	public RuleMatcherEntry(final MessageFormat messagePattern, final String masterFlag, final String[] wrongFlags){
		this.messagePattern = messagePattern;
		this.masterFlag = masterFlag;
		this.wrongFlags = wrongFlags;
	}

	public void match(final Production production) throws IllegalArgumentException{
		for(final String flag : wrongFlags)
			if(production.hasContinuationFlag(flag))
				throw new IllegalArgumentException(messagePattern.format(new Object[]{masterFlag, flag}));
	}
	
}
