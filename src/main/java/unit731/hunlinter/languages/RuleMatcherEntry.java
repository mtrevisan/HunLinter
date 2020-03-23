package unit731.hunlinter.languages;

import java.text.MessageFormat;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.exceptions.LinterException;


public class RuleMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterFlag;
	private final String[] wrongFlags;


	public RuleMatcherEntry(final MessageFormat messagePattern, final String masterFlag, final String[] wrongFlags){
		this.messagePattern = messagePattern;
		this.masterFlag = masterFlag;
		this.wrongFlags = wrongFlags;
	}

	public void match(final Inflection inflection){
		for(final String flag : wrongFlags)
			if(inflection.hasContinuationFlag(flag))
				throw new LinterException(messagePattern.format(new Object[]{masterFlag, flag}));
	}

}
