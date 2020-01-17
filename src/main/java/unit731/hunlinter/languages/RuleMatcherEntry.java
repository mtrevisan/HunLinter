package unit731.hunlinter.languages;

import java.text.MessageFormat;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;


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
				throw new HunLintException(messagePattern.format(new Object[]{masterFlag, flag}));
	}

}