package unit731.hunspeller.languages;

import java.text.MessageFormat;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;


public class RuleMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterFlag;
	private final String[] wrongFlags;


	public RuleMatcherEntry(final MessageFormat messagePattern, final String masterFlag, final String[] wrongFlags){
		this.messagePattern = messagePattern;
		this.masterFlag = masterFlag;
		this.wrongFlags = wrongFlags;
	}

	public void match(final Production production){
		for(final String flag : wrongFlags)
			if(production.hasContinuationFlag(flag))
				throw new HunspellException(messagePattern.format(new Object[]{masterFlag, flag}));
	}
	
}
