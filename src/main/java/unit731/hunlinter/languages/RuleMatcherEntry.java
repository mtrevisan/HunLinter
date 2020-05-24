package unit731.hunlinter.languages;

import java.text.MessageFormat;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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


	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final RuleMatcherEntry rhs = (RuleMatcherEntry)obj;
		return new EqualsBuilder()
			.append(messagePattern, rhs.messagePattern)
			.append(masterFlag, rhs.masterFlag)
			.append(wrongFlags, rhs.wrongFlags)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(messagePattern)
			.append(masterFlag)
			.append(wrongFlags)
			.toHashCode();
	}

}
